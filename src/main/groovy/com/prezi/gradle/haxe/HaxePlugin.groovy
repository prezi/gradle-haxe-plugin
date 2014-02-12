package com.prezi.gradle.haxe

import com.prezi.gradle.PreziPlugin
import org.gradle.api.Action
import org.gradle.api.DomainObjectCollection
import org.gradle.api.DomainObjectSet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.internal.DefaultDomainObjectSet
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.tasks.TaskResolver
import org.gradle.internal.reflect.Instantiator
import org.gradle.language.base.BinaryContainer
import org.gradle.language.base.FunctionalSourceSet
import org.gradle.language.base.LanguageSourceSet
import org.gradle.language.base.ProjectSourceSet
import org.gradle.language.base.internal.BinaryInternal
import org.gradle.language.jvm.ResourceSet
import org.gradle.language.jvm.internal.DefaultResourceSet
import org.gradle.util.GUtil

import javax.inject.Inject

class HaxePlugin implements Plugin<Project> {

	public static final String COMPILE_TASK_NAME = "compile"
	public static final String COMPILE_TASKS_GROUP = "compile"
	public static final String TEST_TASK_NAME = "test"
	public static final String TEST_TASKS_GROUP = "test"

	public static final String HAXE_SOURCE_SET_NAME = "haxe"
	public static final String RESOURCE_SET_NAME = "resources"
	public static final String HAXE_RESOURCE_SET_NAME = "haxeResources"

	private final Instantiator instantiator
	private final FileResolver fileResolver

	@Inject
	public HaxePlugin(Instantiator instantiator, FileResolver fileResolver) {
		this.instantiator = instantiator
		this.fileResolver = fileResolver
	}

	@Override
	void apply(Project project) {
		project.getPlugins().apply(PreziPlugin.class)

		def binaryContainer = project.getExtensions().getByType(BinaryContainer.class)

		// Add "targetPlatforms"
		def targetPlatforms = project.getExtensions().create(
				"targetPlatforms",
				DefaultTargetPlatformContainer.class,
				instantiator
		);

		def projectSourceSet = project.getExtensions().getByType(ProjectSourceSet.class)

		// Add functional source sets for main code
		def main = projectSourceSet.maybeCreate("main")
		def test = projectSourceSet.maybeCreate("test")
		Configuration mainCompile = maybeCreateCompileConfigurationFor(project, "main")
		Configuration testCompile = maybeCreateCompileConfigurationFor(project, "test")
		testCompile.extendsFrom mainCompile

		// For each source set create a configuration and language source sets
		projectSourceSet.all(new Action<FunctionalSourceSet>() {
			@Override
			void execute(FunctionalSourceSet functionalSourceSet) {
				// Inspired by JavaBasePlugin
				// Add Haxe source set for "src/<name>/haxe"
				def compileConfiguration = project.configurations.getByName(functionalSourceSet.name)
				def haxeSourceSet = instantiator.newInstance(DefaultHaxeSourceSet, HAXE_SOURCE_SET_NAME, functionalSourceSet, compileConfiguration, fileResolver, (TaskResolver) project.tasks)
				haxeSourceSet.source.srcDir(String.format("src/%s/haxe", functionalSourceSet.name))
				functionalSourceSet.add(haxeSourceSet)

				// Add resources if not exists yet
				if (!functionalSourceSet.findByName(RESOURCE_SET_NAME)) {
					def resourcesDirectorySet = instantiator.newInstance(DefaultSourceDirectorySet, String.format("%s resources", functionalSourceSet.name), fileResolver)
					resourcesDirectorySet.srcDir(String.format("src/%s/haxe", functionalSourceSet.name))
					def resourceSet = instantiator.newInstance(DefaultResourceSet, RESOURCE_SET_NAME, resourcesDirectorySet, functionalSourceSet)
					functionalSourceSet.add(resourceSet)
				}

				// Add Haxe resource set to be used for embedded resources
				def haxeResourceSet = instantiator.newInstance(DefaultHaxeResourceSet, HAXE_RESOURCE_SET_NAME, functionalSourceSet, fileResolver)
				functionalSourceSet.add(haxeResourceSet)
			}
		})

		// For each target platform add functional source sets
		targetPlatforms.all(new Action<TargetPlatform>() {
			@Override
			void execute(TargetPlatform targetPlatform) {
				// Create platform configurations
				Configuration platformMainCompile = maybeCreateCompileConfigurationFor(project, targetPlatform.name)
				Configuration platformTestCompile = maybeCreateCompileConfigurationFor(project, targetPlatform.name + "Test")
				platformMainCompile.extendsFrom mainCompile
				platformTestCompile.extendsFrom testCompile
				platformTestCompile.extendsFrom platformMainCompile

				def platformMain = projectSourceSet.maybeCreate(targetPlatform.name)
				def platformTest = projectSourceSet.maybeCreate(targetPlatform.name + "Test")

				def mainLanguageSets = getLanguageSets(main, platformMain)
				def testLanguageSets = getLanguageSets(test, platformTest)

				// Add compiled binary
				def compiledHaxe = new DefaultHaxeCompiledBinary(targetPlatform.name, targetPlatform)
				compiledHaxe.source.addAll(mainLanguageSets)
				binaryContainer.add(compiledHaxe)

				// Add source bundle binary
				def sourceHaxe = new DefaultHaxeSourceBinary("source" + targetPlatform.name.capitalize(), targetPlatform)
				sourceHaxe.source.addAll(mainLanguageSets)
				binaryContainer.add(sourceHaxe)

				createMUnitTask(project, mainLanguageSets, testLanguageSets, targetPlatform)
			}
		})

		// Add a compile task for each compiled binary
		binaryContainer.withType(HaxeCompiledBinary.class).all(new Action<HaxeCompiledBinary>() {
			public void execute(final HaxeCompiledBinary binary) {
				def compileTask = createCompileTask(project, binary)
				binary.builtBy(compileTask)
			}
		});

		// Add a source task for each source binary
		binaryContainer.withType(HaxeSourceBinary.class).all(new Action<HaxeSourceBinary>() {
			public void execute(final HaxeSourceBinary binary) {
				def sourceTask = createSourceTask(project, binary)
				binary.builtBy(sourceTask)

				binary.source.withType(HaxeSourceSet)*.compileClassPath.each { Configuration configuration ->
					project.artifacts.add(configuration.name, sourceTask) {
						type = "har"
					}
				}
			}
		});

		// Map default values
		def haxeExtension = project.extensions.create "haxe", HaxeExtension, project
		project.tasks.withType(HaxeCompile).all(new Action<HaxeCompile>() {
			@Override
			void execute(HaxeCompile haxeCompile) {
				haxeExtension.mapTo(haxeCompile)
			}
		})
		project.tasks.withType(MUnit).all(new Action<MUnit>() {
			@Override
			void execute(MUnit munit) {
				haxeExtension.mapTo(munit)
			}
		})

		// Add compile all task
		def compileTask = project.tasks.findByName(COMPILE_TASK_NAME)
		if (compileTask == null) {
			compileTask = project.tasks.create(COMPILE_TASK_NAME)
			compileTask.group = COMPILE_TASKS_GROUP
			compileTask.description = "Compile all Haxe artifacts"
		}
		project.tasks.withType(HaxeCompile).all(new Action<HaxeCompile>() {
			@Override
			void execute(HaxeCompile task) {
				task.group = COMPILE_TASKS_GROUP
				compileTask.dependsOn task
			}
		})

		// Add test all task
		def testTask = project.tasks.findByName(TEST_TASK_NAME)
		if (testTask == null) {
			testTask = project.tasks.create(TEST_TASK_NAME)
			testTask.group = TEST_TASKS_GROUP
			testTask.description = "Test built Haxe artifacts"
		}
		project.tasks.withType(MUnit).all(new Action<MUnit>() {
			@Override
			void execute(MUnit task) {
				task.group = TEST_TASKS_GROUP
				testTask.dependsOn task
			}
		})
	}

	private static DomainObjectSet<LanguageSourceSet> getLanguageSets(FunctionalSourceSet... sets) {
		def result = new DefaultDomainObjectSet<>(LanguageSourceSet);
		sets.each { set ->
			result.add set.getByName(HAXE_SOURCE_SET_NAME)
			result.add set.getByName(RESOURCE_SET_NAME)
			result.add set.getByName(HAXE_RESOURCE_SET_NAME)
		}
		return result
	}

	private static Configuration maybeCreateCompileConfigurationFor(Project project, String name) {
		def config = project.configurations.findByName(name)
		if (!config) {
			config = project.configurations.create(name)
			config.visible = false
			config.description = "Compile classpath for ${name}."
		}
		return config
	}

	private static HaxeCompile createCompileTask(Project project, HaxeCompiledBinary binary) {
		def namingScheme = ((BinaryInternal) binary).namingScheme

		def compileTaskName = namingScheme.getTaskName("compile", null)
		HaxeCompile compileTask = project.task(compileTaskName, type: HaxeCompile) {
			description = "Compiles $binary"
		} as HaxeCompile

		compileTask.source(binary.source)
		compileTask.conventionMapping.targetPlatform = { binary.targetPlatform }
		compileTask.conventionMapping.embeddedResources = { gatherEmbeddedResources(binary.source) }
		compileTask.conventionMapping.outputFile = { project.file("${project.buildDir}/compiled-haxe/${namingScheme.outputDirectoryBase}/${binary.name}.${binary.targetPlatform.name}") }
		project.tasks.getByName(namingScheme.getLifecycleTaskName()).dependsOn compileTask
		// Not sure why we don't need this
		// compileTask.dependsOn binary.source.withType(HaxeSourceSet)*.compileClassPath
		return compileTask
	}

	private static MUnit createMUnitTask(Project project, DomainObjectSet<LanguageSourceSet> main, DomainObjectSet<LanguageSourceSet> test, TargetPlatform targetPlatform) {
		def munitTask = project.task("test" + targetPlatform.name.capitalize(), type: MUnit) {
			description = "Runs ${targetPlatform.name} tests"
		} as MUnit
		munitTask.source(main.withType(HaxeSourceSet) + main.withType(ResourceSet))
		munitTask.testSource(test.withType(HaxeSourceSet) + test.withType(ResourceSet))
		munitTask.conventionMapping.targetPlatform = { targetPlatform }
		munitTask.conventionMapping.embeddedResources = { gatherEmbeddedResources(main.withType(HaxeResourceSet)) }
		munitTask.conventionMapping.embeddedTestResources = { gatherEmbeddedResources(test.withType(HaxeResourceSet)) }
		munitTask.conventionMapping.workingDirectory = { project.file("${project.buildDir}/munit-work/" + targetPlatform.name) }
		// Not sure why we don't need these
		// munitTask.dependsOn main.withType(HaxeSourceSet)*.compileClassPath
		// munitTask.dependsOn test.withType(HaxeSourceSet)*.compileClassPath
		return munitTask
	}

	private static Har createSourceTask(Project project, HaxeSourceBinary binary) {
		def namingScheme = ((BinaryInternal) binary).namingScheme

		def sourceTaskName = namingScheme.getTaskName("bundle")
		Har sourceTask = project.task(sourceTaskName, type: Har) {
			description = "Bundles the sources of $binary"
		} as Har

		sourceTask.conventionMapping.baseName = { project.name }
		sourceTask.conventionMapping.destinationDir = { project.file("${project.buildDir}/haxe-source/${namingScheme.outputDirectoryBase}") }
		sourceTask.conventionMapping.embeddedResources = { gatherEmbeddedResources(binary.source) }
		sourceTask.into "sources", {
			duplicatesStrategy = DuplicatesStrategy.EXCLUDE
			from binary.source.withType(HaxeSourceSet)*.source
		}
		sourceTask.into RESOURCE_SET_NAME, {
			duplicatesStrategy = DuplicatesStrategy.EXCLUDE
			from binary.source.withType(ResourceSet)*.source
		}
		sourceTask.into "embedded", {
			duplicatesStrategy = DuplicatesStrategy.EXCLUDE
			from binary.source.withType(HaxeResourceSet)*.embeddedResources*.values()
		}
		project.tasks.getByName(namingScheme.getLifecycleTaskName()).dependsOn sourceTask
		return sourceTask
	}

	public static LinkedHashMap<String, File> gatherEmbeddedResources(DomainObjectCollection<LanguageSourceSet> source) {
		return source.withType(HaxeResourceSet)*.embeddedResources.flatten().inject([:]) { acc, val -> acc + val }
	}

	private static String getTaskBaseName(FunctionalSourceSet set) {
		return set.name.equals("main") ? "" : GUtil.toCamelCase(set.name)
	}

	File getSpaghettiBundleTool(Project project) {
		def workDir = project.file("${project.buildDir}/spaghetti-haxe")
		workDir.mkdirs()
		def bundleFile = new File(workDir, "SpaghettiBundler.hx")
		bundleFile.delete()
		bundleFile << this.class.getResourceAsStream("/SpaghettiBundler.hx").text
		return bundleFile
	}
}
