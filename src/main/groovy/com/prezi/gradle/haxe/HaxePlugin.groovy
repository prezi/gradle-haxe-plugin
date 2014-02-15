package com.prezi.gradle.haxe

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
import org.gradle.api.plugins.BasePlugin
import org.gradle.internal.reflect.Instantiator
import org.gradle.language.base.BinaryContainer
import org.gradle.language.base.FunctionalSourceSet
import org.gradle.language.base.LanguageSourceSet
import org.gradle.language.base.ProjectSourceSet
import org.gradle.language.base.internal.BinaryInternal
import org.gradle.language.base.plugins.LanguageBasePlugin
import org.gradle.language.jvm.ResourceSet
import org.gradle.language.jvm.internal.DefaultResourceSet
import org.slf4j.LoggerFactory

import javax.inject.Inject

class HaxePlugin implements Plugin<Project> {

	private static final logger = LoggerFactory.getLogger(HaxePlugin)

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
		project.getPlugins().apply(BasePlugin.class)
		project.getPlugins().apply(LanguageBasePlugin.class)

		def binaryContainer = project.getExtensions().getByType(BinaryContainer.class)

		def projectSourceSet = project.getExtensions().getByType(ProjectSourceSet.class)

		// Add functional source sets for main code
		def main = projectSourceSet.maybeCreate("main")
		def test = projectSourceSet.maybeCreate("test")
		logger.debug("Created ${main} and ${test}")
		Configuration mainCompile = maybeCreateCompileConfigurationFor(project, "main")
		Configuration testCompile = maybeCreateCompileConfigurationFor(project, "test")
		testCompile.extendsFrom mainCompile
		logger.debug("Created ${mainCompile} and ${testCompile}")

		// For each source set create a configuration and language source sets
		projectSourceSet.all(new Action<FunctionalSourceSet>() {
			@Override
			void execute(FunctionalSourceSet functionalSourceSet) {
				// Inspired by JavaBasePlugin
				// Add Haxe source set for "src/<name>/haxe"
				def compileConfiguration = project.configurations.getByName(functionalSourceSet.name)
				def haxeSourceSet = instantiator.newInstance(DefaultHaxeSourceSet, HAXE_SOURCE_SET_NAME, functionalSourceSet, compileConfiguration, fileResolver)
				haxeSourceSet.source.srcDir(String.format("src/%s/haxe", functionalSourceSet.name))
				functionalSourceSet.add(haxeSourceSet)
				logger.debug("Added ${haxeSourceSet}")

				// Add resources if not exists yet
				if (!functionalSourceSet.findByName(RESOURCE_SET_NAME)) {
					def resourcesDirectorySet = instantiator.newInstance(DefaultSourceDirectorySet, String.format("%s resources", functionalSourceSet.name), fileResolver)
					resourcesDirectorySet.srcDir(String.format("src/%s/haxe", functionalSourceSet.name))
					def resourceSet = instantiator.newInstance(DefaultResourceSet, RESOURCE_SET_NAME, resourcesDirectorySet, functionalSourceSet)
					functionalSourceSet.add(resourceSet)
					logger.debug("Added ${resourceSet}")
				}

				// Add Haxe resource set to be used for embedded resources
				def haxeResourceSet = instantiator.newInstance(DefaultHaxeResourceSet, HAXE_RESOURCE_SET_NAME, functionalSourceSet, fileResolver)
				functionalSourceSet.add(haxeResourceSet)
				logger.debug("Added ${haxeResourceSet}")
			}
		})

		// Add "targetPlatforms"
		def targetPlatforms = project.getExtensions().create(
				"targetPlatforms",
				DefaultTargetPlatformContainer.class,
				instantiator
		);

		// For each target platform add functional source sets
		targetPlatforms.all(new Action<TargetPlatform>() {
			@Override
			void execute(TargetPlatform targetPlatform) {
				logger.debug("Configuring ${targetPlatform}")

				// Create platform configurations
				Configuration platformMainCompile = maybeCreateCompileConfigurationFor(project, targetPlatform.name)
				Configuration platformTestCompile = maybeCreateCompileConfigurationFor(project, targetPlatform.name + "Test")
				platformMainCompile.extendsFrom mainCompile
				platformTestCompile.extendsFrom testCompile
				platformTestCompile.extendsFrom platformMainCompile
				logger.debug("Added ${platformMainCompile} and ${platformTestCompile}")

				def platformMain = projectSourceSet.maybeCreate(targetPlatform.name)
				def platformTest = projectSourceSet.maybeCreate(targetPlatform.name + "Test")
				logger.debug("Added ${platformMain} and ${platformTest}")

				def mainLanguageSets = getLanguageSets(main, platformMain)
				def testLanguageSets = getLanguageSets(test, platformTest)

				createBinariesAndTasks(project, targetPlatform.name, targetPlatform, null, mainLanguageSets, testLanguageSets)

				// Add some flavor
				targetPlatform.flavors.all(new Action<Flavor>() {
					@Override
					void execute(Flavor flavor) {
						logger.debug("Configuring ${targetPlatform} with ${flavor}")

						def flavorName = targetPlatform.name + flavor.name.capitalize()

						Configuration flavorMainCompile = maybeCreateCompileConfigurationFor(project, flavorName)
						Configuration flavorTestCompile = maybeCreateCompileConfigurationFor(project, flavorName + "Test")
						flavorMainCompile.extendsFrom platformMainCompile
						flavorTestCompile.extendsFrom platformTestCompile
						flavorTestCompile.extendsFrom flavorMainCompile
						logger.debug("Added ${flavorMainCompile} and ${flavorTestCompile}")

						def flavorMain = projectSourceSet.maybeCreate(flavorName)
						def flavorTest = projectSourceSet.maybeCreate(flavorName + "Test")
						logger.debug("Added ${flavorMain} and ${flavorTest}")

						def flavorMainLanguageSets = getLanguageSets(main, platformMain, flavorMain)
						def flavorTestLanguageSets = getLanguageSets(test, platformTest, flavorTest)

						createBinariesAndTasks(project, flavorName, targetPlatform, flavor, flavorMainLanguageSets, flavorTestLanguageSets)
					}
				})
			}
		})

		// Add a compile task for each compiled binary
		binaryContainer.withType(HaxeCompiledBinary).all(new Action<HaxeCompiledBinary>() {
			public void execute(final HaxeCompiledBinary binary) {
				def compileTask = createCompileTask(project, binary)
				binary.setCompileTask(compileTask)
				binary.builtBy(compileTask)
				logger.debug("Created compile task ${compileTask} for ${binary}")
			}
		})

		// Add a source task for each source binary
		binaryContainer.withType(HaxeSourceBinary).all(new Action<HaxeSourceBinary>() {
			public void execute(final HaxeSourceBinary binary) {
				def sourceTask = createSourceTask(project, binary)
				binary.builtBy(sourceTask)

				binary.source.withType(HaxeSourceSet)*.compileClassPath.each { Configuration configuration ->
					project.artifacts.add(configuration.name, sourceTask) {
						name = project.name + "-" + binary.name
						type = "har"
					}
				}
				logger.debug("Created source source task ${sourceTask} for ${binary}")
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

	private static void createBinariesAndTasks(
			Project project, String name, TargetPlatform targetPlatform, Flavor flavor,
			DomainObjectSet<LanguageSourceSet> mainLanguageSets, DomainObjectSet<LanguageSourceSet> testLanguageSets) {
		def binaryContainer = project.getExtensions().getByType(BinaryContainer.class)

		// Add compiled binary
		def compiledHaxe = new DefaultHaxeCompiledBinary(name, targetPlatform, flavor)
		compiledHaxe.source.addAll(mainLanguageSets)
		binaryContainer.add(compiledHaxe)
		logger.debug("Added compiled binary ${compiledHaxe}")

		// Add source bundle binary
		def sourceHaxe = new DefaultHaxeSourceBinary("source" + name.capitalize(), targetPlatform, flavor)
		sourceHaxe.source.addAll(mainLanguageSets)
		binaryContainer.add(sourceHaxe)
		logger.debug("Added source binary ${sourceHaxe}")

		def munit = createMUnitTask(project, name, targetPlatform, flavor, mainLanguageSets, testLanguageSets)
		logger.debug("Added MUnit task ${munit}")
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

		HaxeCompileParameters.setConvention(compileTask, getParams(project, binary.targetPlatform, binary.flavor))

		project.tasks.getByName(namingScheme.getLifecycleTaskName()).dependsOn compileTask
		// Let' depend on the input configurations
		compileTask.dependsOn binary.source.withType(HaxeSourceSet)*.compileClassPath
		compileTask.dependsOn binary.source
		return compileTask
	}

	private static MUnit createMUnitTask(Project project, String name, TargetPlatform targetPlatform, Flavor flavor, DomainObjectSet<LanguageSourceSet> main, DomainObjectSet<LanguageSourceSet> test) {
		def munitTask = project.task("test" + name.capitalize(), type: MUnit) {
			description = "Runs ${targetPlatform.name} tests"
		} as MUnit
		munitTask.source(main.withType(HaxeSourceSet) + main.withType(ResourceSet))
		munitTask.testSource(test.withType(HaxeSourceSet) + test.withType(ResourceSet))
		munitTask.conventionMapping.targetPlatform = { targetPlatform }
		munitTask.conventionMapping.embeddedResources = { gatherEmbeddedResources(main.withType(HaxeResourceSet)) }
		munitTask.conventionMapping.embeddedTestResources = { gatherEmbeddedResources(test.withType(HaxeResourceSet)) }
		munitTask.conventionMapping.workingDirectory = { project.file("${project.buildDir}/munit-work/" + targetPlatform.name) }

		HaxeCompileParameters.setConvention(munitTask, getParams(project, targetPlatform, flavor))

		// Let' depend on the input configurations (both from main and test)
		munitTask.dependsOn main.withType(HaxeSourceSet)*.compileClassPath
		munitTask.dependsOn test.withType(HaxeSourceSet)*.compileClassPath
		munitTask.dependsOn main, test
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
		sourceTask.dependsOn binary.source
		project.tasks.getByName(namingScheme.getLifecycleTaskName()).dependsOn sourceTask
		return sourceTask
	}

	private static Set<HaxeCompileParameters> getParams(Project project, TargetPlatform targetPlatform, Flavor flavor) {
		def rootParams = project.getExtensions().getByType(TargetPlatformContainer).params
		def platformParams = targetPlatform.params
		def flavorParams = flavor?.params
		return [ rootParams, platformParams, flavorParams ] - null
	}

	public static LinkedHashMap<String, File> gatherEmbeddedResources(DomainObjectCollection<LanguageSourceSet> source) {
		return source.withType(HaxeResourceSet)*.embeddedResources.flatten().inject([:]) { acc, val -> acc + val }
	}
}
