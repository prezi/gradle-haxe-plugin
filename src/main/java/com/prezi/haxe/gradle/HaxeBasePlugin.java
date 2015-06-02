package com.prezi.haxe.gradle;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.prezi.haxe.gradle.incubating.BinaryContainer;
import com.prezi.haxe.gradle.incubating.BinaryInternal;
import com.prezi.haxe.gradle.incubating.BinaryNamingScheme;
import com.prezi.haxe.gradle.incubating.DefaultResourceSet;
import com.prezi.haxe.gradle.incubating.FunctionalSourceSet;
import com.prezi.haxe.gradle.incubating.LanguageSourceSet;
import com.prezi.haxe.gradle.incubating.ProjectSourceSet;
import com.prezi.haxe.gradle.incubating.ResourceSet;
import com.prezi.haxe.gradle.nodetest.HaxeNodeTestCompile;
import org.gradle.api.Action;
import org.gradle.api.DomainObjectCollection;
import org.gradle.api.DomainObjectSet;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.internal.DefaultDomainObjectSet;
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact;
import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.internal.reflect.Instantiator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

public class HaxeBasePlugin implements Plugin<Project> {
	private static final Logger logger = LoggerFactory.getLogger(HaxeBasePlugin.class);

	public static final String HAXE_SOURCE_SET_NAME = "haxe";
	public static final String RESOURCE_SET_NAME = "resources";
	public static final String HAXE_RESOURCE_SET_NAME = "haxeResources";
	public static final String CHECK_HAXE_VERSION_TASK_NAME = "checkHaxeVersion";
	public static final String COMPILE_TASK_NAME = "compile";
	public static final String COMPILE_TASKS_GROUP = "compile";
	public static final String CHECK_TASK_NAME = "check";
	public static final String BUILD_TASK_NAME = "build";
	public static final String TEST_TASK_NAME = "test";
	public static final String VERIFICATION_GROUP = "verification";

	private final Instantiator instantiator;
	private final FileResolver fileResolver;

	@Inject
	public HaxeBasePlugin(Instantiator instantiator, FileResolver fileResolver) {
		this.instantiator = instantiator;
		this.fileResolver = fileResolver;
	}

	@Override
	public void apply(final Project project) {
		project.getPlugins().apply(BasePlugin.class);

		// Add "haxe" extension
		final HaxeExtension extension = project.getExtensions().create("haxe", HaxeExtension.class, project, instantiator);

		final ProjectSourceSet projectSourceSet = extension.getSources();

		// Add functional source sets for main code
		final FunctionalSourceSet main = projectSourceSet.maybeCreate("main");
		final FunctionalSourceSet test = projectSourceSet.maybeCreate("test");
		logger.debug("Created {} and {} in {}", main, test, project.getPath());
		final Configuration mainCompile = maybeCreateCompileConfigurationFor(project, "main");
		final Configuration testCompile = maybeCreateCompileConfigurationFor(project, "test");
		testCompile.extendsFrom(mainCompile);
		logger.debug("Created {} and {} in {}", mainCompile, testCompile, project.getPath());

		// For each source set create a configuration and language source sets
		projectSourceSet.all(new Action<FunctionalSourceSet>() {
			@Override
			public void execute(FunctionalSourceSet functionalSourceSet) {
				// Inspired by JavaBasePlugin
				// Add Haxe source set for "src/<name>/haxe"
				Configuration compileConfiguration = project.getConfigurations().getByName(functionalSourceSet.getName());
				DefaultHaxeSourceSet haxeSourceSet = instantiator.newInstance(DefaultHaxeSourceSet.class, HAXE_SOURCE_SET_NAME, functionalSourceSet, compileConfiguration, fileResolver);
				haxeSourceSet.getSource().srcDir(String.format("src/%s/haxe", functionalSourceSet.getName()));
				functionalSourceSet.add(haxeSourceSet);
				logger.debug("Added {} in {}", haxeSourceSet, project.getPath());

				// Add resources if not exists yet
				if (functionalSourceSet.findByName(RESOURCE_SET_NAME) == null) {
					DefaultSourceDirectorySet resourcesDirectorySet = instantiator.newInstance(DefaultSourceDirectorySet.class, String.format("%s resources", functionalSourceSet.getName()), fileResolver);
					resourcesDirectorySet.srcDir(String.format("src/%s/resources", functionalSourceSet.getName()));
					DefaultResourceSet resourceSet = instantiator.newInstance(DefaultResourceSet.class, RESOURCE_SET_NAME, resourcesDirectorySet, functionalSourceSet);
					functionalSourceSet.add(resourceSet);
					logger.debug("Added {} in {}", resourceSet, project.getPath());
				}


				// Add Haxe resource set to be used for embedded resources
				DefaultHaxeResourceSet haxeResourceSet = instantiator.newInstance(DefaultHaxeResourceSet.class, HAXE_RESOURCE_SET_NAME, functionalSourceSet, fileResolver);
				functionalSourceSet.add(haxeResourceSet);
				logger.debug("Added {} in {}", haxeResourceSet, project.getPath());
			}

		});

		NamedDomainObjectContainer<TargetPlatform> targetPlatforms = extension.getTargetPlatforms();

		// For each target platform add functional source sets
		targetPlatforms.all(new Action<TargetPlatform>() {
			@Override
			public void execute(final TargetPlatform targetPlatform) {
				logger.debug("Configuring {} in {}", targetPlatform, project.getPath());

				// Create platform configurations
				final Configuration platformMainCompile = maybeCreateCompileConfigurationFor(project, targetPlatform.getName());
				final Configuration platformTestCompile = maybeCreateCompileConfigurationFor(project, targetPlatform.getName() + "Test");
				platformMainCompile.extendsFrom(mainCompile);
				platformTestCompile.extendsFrom(testCompile);
				platformTestCompile.extendsFrom(platformMainCompile);
				logger.debug("Added {} and {} in {}", platformMainCompile, platformTestCompile, project.getPath());

				final FunctionalSourceSet platformMain = projectSourceSet.maybeCreate(targetPlatform.getName());
				final FunctionalSourceSet platformTest = projectSourceSet.maybeCreate(targetPlatform.getName() + "Test");
				logger.debug("Added {} and {} in {}", platformMain, platformTest, project.getPath());

				DomainObjectSet<LanguageSourceSet> mainLanguageSets = getLanguageSets(main, platformMain);
				DomainObjectSet<LanguageSourceSet> testLanguageSets = getLanguageSets(test, platformTest);

				createBinaries(project, targetPlatform.getName(), targetPlatform, null, mainLanguageSets, testLanguageSets, platformMainCompile, platformTestCompile);

				// Add some flavor
				targetPlatform.getFlavors().all(new Action<Flavor>() {
					@Override
					public void execute(Flavor flavor) {
						logger.debug("Configuring {} with {} in {}", targetPlatform, flavor, project.getPath());

						String flavorName = targetPlatform.getName() + Character.toUpperCase(flavor.getName().charAt(0)) + flavor.getName().substring(1);

						Configuration flavorMainCompile = maybeCreateCompileConfigurationFor(project, flavorName);
						Configuration flavorTestCompile = maybeCreateCompileConfigurationFor(project, flavorName + "Test");
						flavorMainCompile.extendsFrom(platformMainCompile);
						flavorTestCompile.extendsFrom(platformTestCompile);
						flavorTestCompile.extendsFrom(flavorMainCompile);
						logger.debug("Added {} and {} in {}", flavorMainCompile, flavorTestCompile, project.getPath());

						FunctionalSourceSet flavorMain = projectSourceSet.maybeCreate(flavorName);
						FunctionalSourceSet flavorTest = projectSourceSet.maybeCreate(flavorName + "Test");
						logger.debug("Added {} and {} in {}", flavorMain, flavorTest, project.getPath());

						DomainObjectSet<LanguageSourceSet> flavorMainLanguageSets = getLanguageSets(main, platformMain, flavorMain);
						DomainObjectSet<LanguageSourceSet> flavorTestLanguageSets = getLanguageSets(test, platformTest, flavorTest);

						createBinaries(project, flavorName, targetPlatform, flavor, flavorMainLanguageSets, flavorTestLanguageSets, flavorMainCompile, flavorTestCompile);
					}
				});
			}
		});

		// Add checkHaxeVersion task
		final CheckHaxeVersion checkVersionTask = project.getTasks().create(CHECK_HAXE_VERSION_TASK_NAME, CheckHaxeVersion.class);
		checkVersionTask.getConventionMapping().map("compilerVersions", new Callable<Set<Object>>() {
			@Override
			public Set<Object> call() throws Exception {
				return extension.getCompilerVersions();
			}
		});
		checkVersionTask.setDescription("Checks if Haxe compiler is the right version.");
		checkVersionTask.setGroup(VERIFICATION_GROUP);
		project.getTasks().withType(AbstractHaxeCompileTask.class).all(new Action<AbstractHaxeCompileTask>() {
			@Override
			public void execute(AbstractHaxeCompileTask compileTask) {
				compileTask.dependsOn(checkVersionTask);
			}
		});

		// Add compile all task
		Task compileTask = project.getTasks().findByName(COMPILE_TASK_NAME);
		if (compileTask == null) {
			compileTask = project.getTasks().create(COMPILE_TASK_NAME);
			compileTask.setGroup(COMPILE_TASKS_GROUP);
			compileTask.setDescription("Compile all Haxe artifacts");
		}
		final Task _compileTask = compileTask;

		project.getTasks().withType(HaxeCompile.class).all(new Action<HaxeCompile>() {
			@Override
			public void execute(HaxeCompile task) {
				task.setGroup(COMPILE_TASKS_GROUP);
				_compileTask.dependsOn(task);
			}
		});

		// Add test all task
		Task testTask = project.getTasks().findByName(TEST_TASK_NAME);
		if (testTask == null) {
			testTask = project.getTasks().create(TEST_TASK_NAME);
			testTask.setGroup(VERIFICATION_GROUP);
			testTask.setDescription("Runs all unit tests.");
		}
		final Task _testTask = testTask;

		Task checkTask = project.getTasks().findByName(CHECK_TASK_NAME);
		if (checkTask == null) {
			checkTask = project.getTasks().create(CHECK_TASK_NAME);
			checkTask.setGroup(VERIFICATION_GROUP);
			checkTask.setDescription("Runs all checks.");
		}

		checkTask.dependsOn(testTask);
		project.getTasks().withType(MUnit.class).all(new Action<MUnit>() {
			@Override
			public void execute(MUnit task) {
				task.setGroup(VERIFICATION_GROUP);
				_testTask.dependsOn(task);
			}
		});

		Task buildTask = project.getTasks().findByName(BUILD_TASK_NAME);
		if (buildTask == null) {
			buildTask = project.getTasks().create(BUILD_TASK_NAME);
			buildTask.setDescription("Assembles and tests this project.");
			buildTask.setGroup(BasePlugin.BUILD_GROUP);
		}
		buildTask.dependsOn(BasePlugin.ASSEMBLE_TASK_NAME);
		buildTask.dependsOn(checkTask);
	}

	private static void createBinaries(Project project, String name, TargetPlatform targetPlatform, Flavor flavor, DomainObjectSet<LanguageSourceSet> mainLanguageSets, DomainObjectSet<LanguageSourceSet> testLanguageSets, Configuration mainConfiguration, Configuration testConfiguration) {
		BinaryContainer binaryContainer = project.getExtensions().getByType(HaxeExtension.class).getBinaries();

		// Add compiled binary
		final HaxeBinary compileBinary = new HaxeBinary(name, mainConfiguration, targetPlatform, flavor);
		final HaxeTestBinary testBinary = new HaxeTestBinary(name, testConfiguration, targetPlatform, flavor);
		mainLanguageSets.all(new Action<LanguageSourceSet>() {
			@Override
			public void execute(LanguageSourceSet it) {
				compileBinary.getSource().add(it);
				testBinary.getSource().add(it);
			}

		});
		testLanguageSets.all(new Action<LanguageSourceSet>() {
			@Override
			public void execute(LanguageSourceSet it) {
				testBinary.getSource().add(it);
			}

		});
		binaryContainer.add(compileBinary);
		binaryContainer.add(testBinary);
		logger.debug("Added binaries {} and {} in {}", compileBinary, testBinary, project.getPath());
	}

	private static DomainObjectSet<LanguageSourceSet> getLanguageSets(FunctionalSourceSet... functionalSourceSets) {
		DomainObjectSet<LanguageSourceSet> result = new DefaultDomainObjectSet<LanguageSourceSet>(LanguageSourceSet.class);
		for (FunctionalSourceSet functionalSourceSet : functionalSourceSets) {
			result.add(functionalSourceSet.getByName(HAXE_SOURCE_SET_NAME));
			result.add(functionalSourceSet.getByName(RESOURCE_SET_NAME));
			result.add(functionalSourceSet.getByName(HAXE_RESOURCE_SET_NAME));
		}

		return result;
	}

	private static Configuration maybeCreateCompileConfigurationFor(Project project, final String name) {
		Configuration config = project.getConfigurations().findByName(name);
		if (config == null) {
			config = project.getConfigurations().create(name);
			config.setVisible(false);
			config.setDescription("Compile classpath for " + name + ".");
		}

		return config;
	}

	public static <T extends HaxeTestCompile> T createTestCompileTask(final Project project, final HaxeTestBinary binary, Class<T> compileType) {
		T compileTask = createCompileTask(project, binary, compileType);
		compileTask.getConventionMapping().map("workingDirectory", new Callable<File>() {
			@Override
			public File call() throws Exception {
				return project.file(project.getBuildDir() + "/haxe-test-compile/" + binary.getName());
			}
		});
		return compileTask;
	}

	public static <T extends HaxeCompile> T createCompileTask(final Project project, final HaxeBinaryBase<? super T> binary, Class<T> compileType) {
		BinaryNamingScheme namingScheme = ((BinaryInternal) binary).getNamingScheme();
		String compileTaskName = namingScheme.getTaskName("compile");

		T compileTask = createCompileTaskInternal(project, binary, compileType, compileTaskName);

		project.getTasks().getByName(namingScheme.getLifecycleTaskName()).dependsOn(compileTask);
		binary.setCompileTask(compileTask);
		binary.builtBy(compileTask);

		logger.debug("Created compile task {} for {} in {}", compileTask, binary, project.getPath());
		return compileTask;
	}

	public static <T extends HaxeCompile> T createCompileTaskInternal(final Project project, final HaxeBinaryBase<? super T> binary, Class<T> compileType, String compileTaskName) {
		final T compileTask = project.getTasks().create(compileTaskName, compileType);
		compileTask.setDescription("Compiles " + binary);
		compileTask.getConventionMapping().map("embeddedResources", new Callable<Map<String, File>>() {
			@Override
			public Map<String, File> call() throws Exception {
				return gatherEmbeddedResources(binary.getSource());
			}
		});
		compileTask.getConventionMapping().map("outputFile", new Callable<File>() {
			@Override
			public File call() throws Exception {
				return getDefaultCompileTarget(project, binary);
			}
		});
		compileTask.getConventionMapping().map("targetPlatform", new Callable<String>() {
			@Override
			public String call() throws Exception {
				return binary.getTargetPlatform().getName();
			}
		});
		compileTask.setConventionMapping(project.getExtensions().getByType(HaxeExtension.class), binary.getTargetPlatform(), binary.getFlavor());
		binary.getSource().all(new Action<LanguageSourceSet>() {
			@Override
			public void execute(LanguageSourceSet it) {
				compileTask.source(it);
			}
		});
		compileTask.dependsOn(binary.getConfiguration());
		compileTask.dependsOn(binary.getSource());
		return compileTask;
	}

	private static File getDefaultCompileTarget(final Project project, final HaxeBinaryBase binary) {
		final BinaryNamingScheme namingScheme = ((BinaryInternal) binary).getNamingScheme();
		return project.file(project.getBuildDir() + "/compiled-haxe/" + namingScheme.getOutputDirectoryBase() + "/compiled." + binary.getTargetPlatform().getName());
	}

	public static <T extends MUnit> T createMUnitTask(final Project project, final HaxeTestBinary binary, Class<T> munitType) {
		BinaryNamingScheme namingScheme = binary.getNamingScheme();
		String munitTaskName = namingScheme.getTaskName("run");
		T munitTask = project.getTasks().create(munitTaskName, munitType);
		munitTask.setDescription("Runs MUnit on " + binary);
		setMunitTaskProperties(project, binary, munitTask);

		munitTask.dependsOn(binary.getCompileTask());
		project.getTasks().getByName(namingScheme.getLifecycleTaskName()).dependsOn(munitTask);
		logger.debug("Created munit task {} for {} in {}", munitTask, binary, project.getPath());

		HaxeNodeTestCompile haxeNodeTestCompile = project.getTasks().create(namingScheme.getTaskName("runNode"), HaxeNodeTestCompile.class);
		haxeNodeTestCompile.getConventionMapping().map("inputDirectory", new Callable<File>() {
			@Override
			public File call() throws Exception {
				return project.file(project.getBuildDir() + "/haxe-test-compile/" + binary.getName());
			}
		});
		haxeNodeTestCompile.getConventionMapping().map("workingDirectory", new Callable<File>() {
			@Override
			public File call() throws Exception {
				return project.file(project.getBuildDir() + "/haxe-test-compile/" + "node" + binary.getName());
			}
		});
		haxeNodeTestCompile.dependsOn(binary.getCompileTask());

		return munitTask;
	}

	private static void setMunitTaskProperties(final Project project, final HaxeTestBinary binary, ConventionTask munitTask) {
		munitTask.getConventionMapping().map("workingDirectory", new Callable<File>() {
			@Override
			public File call() throws Exception {
				return project.file(project.getBuildDir() + "/munit/" + binary.getName());
			}
		});
		munitTask.getConventionMapping().map("targetPlatform", new Callable<String>() {
			@Override
			public String call() throws Exception {
				return binary.getTargetPlatform().getName();
			}
		});
		munitTask.getConventionMapping().map("inputFile", new Callable<File>() {
			@Override
			public File call() throws Exception {
				return binary.getCompileTask().getOutputFile();
			}
		});
	}

//	private static void createPrepareMunitEnvironmentTask(final Project project, final HaxeTestBinary binary, final MUnit mUnitTask) {
//		PrepareMunitEnvironment prepareMunitEnvironment = project.getTasks().create(mUnitTask.getName() + "Prepare", PrepareMunitEnvironment.class);
//		prepareMunitEnvironment.dependsOn(binary.getCompileTask());
//		setMunitTaskProperties(project, binary, prepareMunitEnvironment);
//		mUnitTask.dependsOn(prepareMunitEnvironment);
//	}

	public static <T extends Har> T createSourceTask(final Project project, final HaxeBinaryBase<?> binary, Class<T> harType) {
		final BinaryNamingScheme namingScheme = ((BinaryInternal) binary).getNamingScheme();

		String sourceTaskName = namingScheme.getTaskName("bundle", "source");
		T sourceTask = project.getTasks().create(sourceTaskName, harType);
		sourceTask.setDescription("Bundles the sources of " + binary);
		sourceTask.getConventionMapping().map("baseName", new Callable<String>() {
			@Override
			public String call() throws Exception {
				return project.getName();
			}
		});
		sourceTask.getConventionMapping().map("destinationDir", new Callable<File>() {
			@Override
			public File call() throws Exception {
				return project.file(project.getBuildDir() + "/haxe-source/" + namingScheme.getOutputDirectoryBase());
			}
		});
		sourceTask.getConventionMapping().map("embeddedResources", new Callable<Map<String, File>>() {
			@Override
			public Map<String, File> call() throws Exception {
				return gatherEmbeddedResources(binary.getSource());
			}
		});
		CopySpec sources = sourceTask.getRootSpec().addChild().into("sources");
		sources.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE);
		sources.from(getSources(HaxeSourceSet.class, binary));
		CopySpec resources = sourceTask.getRootSpec().addChild().into(RESOURCE_SET_NAME);
		resources.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE);
		resources.from(getSources(ResourceSet.class, binary));
		CopySpec embedded = sourceTask.getRootSpec().addChild().into("embedded");
		embedded.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE);
		embedded.from(Collections2.transform(binary.getSource().withType(HaxeResourceSet.class), new Function<HaxeResourceSet, Collection<File>>() {
			@Override
			public Collection<File> apply(HaxeResourceSet resourceSet) {
				return resourceSet.getEmbeddedResources().values();
			}
		}));
		sourceTask.dependsOn(binary.getSource());
		project.getTasks().getByName(namingScheme.getLifecycleTaskName()).dependsOn(sourceTask);
		binary.setSourceHarTask(sourceTask);
		binary.builtBy(sourceTask);

		// TODO This should state more clearly what it does
		ArchivePublishArtifact artifact = (ArchivePublishArtifact) project.getArtifacts().add(binary.getConfiguration().getName(), sourceTask);
		artifact.setName(project.getName() + "-" + binary.getName());
		artifact.setType("har");
		logger.debug("Created source source task {} for {} in {}", sourceTask, binary, project.getPath());
		return sourceTask;
	}

	private static Collection<SourceDirectorySet> getSources(Class<? extends LanguageSourceSet> type, HaxeBinaryBase<?> binary) {
		return Collections2.transform(binary.getSource().withType(type), new Function<LanguageSourceSet, SourceDirectorySet>() {
			@Override
			public SourceDirectorySet apply(LanguageSourceSet sourceSet) {
				return sourceSet.getSource();
			}
		});
	}

	public static Map<String, File> gatherEmbeddedResources(DomainObjectCollection<LanguageSourceSet> source) {
		Map<String, File> result = Maps.newLinkedHashMap();
		for (HaxeResourceSet resourceSet : source.withType(HaxeResourceSet.class)) {
			result.putAll(resourceSet.getEmbeddedResources());
		}
		return result;
	}
}
