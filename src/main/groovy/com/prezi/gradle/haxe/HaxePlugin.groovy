package com.prezi.gradle.haxe

import com.prezi.gradle.PreziPlugin
import org.apache.commons.lang.StringUtils
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.internal.file.FileResolver
import org.gradle.configuration.project.ProjectConfigurationActionContainer
import org.gradle.internal.reflect.Instantiator
import org.gradle.language.base.BinaryContainer
import org.gradle.language.base.FunctionalSourceSet
import org.gradle.language.base.ProjectSourceSet
import org.gradle.language.base.internal.BinaryInternal
import org.gradle.language.jvm.ResourceSet
import org.gradle.util.GUtil

import javax.inject.Inject

class HaxePlugin implements Plugin<Project> {

	public static final String COMPILE_TASK_NAME = "compile"
	public static final String COMPILE_TASKS_GROUP = "compile"
	public static final String TEST_TASK_NAME = "test"
	public static final String TEST_TASKS_GROUP = "test"
	public static final String TEST_ARCHIVES_CONFIG_NAME = "testArchives"

	private final Instantiator instantiator
	private final FileResolver fileResolver
	private final ProjectConfigurationActionContainer configurationActions

	@Inject
	public HaxePlugin(Instantiator instantiator, FileResolver fileResolver, ProjectConfigurationActionContainer configurationActions) {
		this.instantiator = instantiator
		this.fileResolver = fileResolver
		this.configurationActions = configurationActions
	}

	@Override
	void apply(Project project) {
		project.getPlugins().apply(PreziPlugin.class)

		// Add "haxe" source set
		def projectSourceSet = project.getExtensions().getByType(ProjectSourceSet.class)
		projectSourceSet.create("main")
		projectSourceSet.create("test")

		// Add "targetPlatforms"
		def targetPlatforms = project.getExtensions().create(
				"targetPlatforms",
				DefaultTargetPlatformContainer.class,
				instantiator
		);

		def binaryContainer = project.getExtensions().getByType(BinaryContainer.class)
		projectSourceSet.all(new Action<FunctionalSourceSet>() {
			@Override
			void execute(FunctionalSourceSet functionalSourceSet) {
				// Inspired by JavaBasePlugin
				// Get/create configurations for source sets
				ConfigurationContainer configurations = project.getConfigurations()
				Configuration compileConfiguration = configurations.findByName(getCompileConfigurationName(functionalSourceSet))
				if (compileConfiguration == null) {
					compileConfiguration = configurations.create(getCompileConfigurationName(functionalSourceSet))
				}
				compileConfiguration.setVisible(false)
				compileConfiguration.setDescription(String.format("Compile classpath for %s.", functionalSourceSet))

//				Configuration runtimeConfiguration = configurations.findByName(getRuntimeConfigurationName(functionalSourceSet))
//				if (runtimeConfiguration == null) {
//					runtimeConfiguration = configurations.create(getRuntimeConfigurationName(functionalSourceSet))
//				}
//				runtimeConfiguration.setVisible(false)
//				runtimeConfiguration.extendsFrom(compileConfiguration)
//				runtimeConfiguration.setDescription(String.format("Runtime classpath for %s.", functionalSourceSet))

				// Register source set factory
				functionalSourceSet.registerFactory(HaxeSourceSet) { name ->
					instantiator.newInstance(DefaultHaxeSourceSet, name, functionalSourceSet, compileConfiguration, fileResolver, project.tasks)
				}

				// Add a single Haxe source set for "src/<name>/haxe"
				def haxeSourceSet = functionalSourceSet.create("haxe", HaxeSourceSet)
				haxeSourceSet.source.srcDir(String.format("src/%s/haxe", functionalSourceSet.name))
				def resourceSet = functionalSourceSet.getByName("resources")
				def haxeResourceSet = new DefaultHaxeResourceSet("haxeResources", functionalSourceSet, fileResolver)
				functionalSourceSet.add(haxeResourceSet)

				// Add binaries for each target platform
				targetPlatforms.all(new Action<TargetPlatform>() {
					@Override
					void execute(TargetPlatform targetPlatform) {
						def compiledHaxe = new DefaultCompiledHaxeBinary(functionalSourceSet.name, targetPlatform)
						compiledHaxe.source.add(resourceSet)
						compiledHaxe.source.add(haxeResourceSet)
						compiledHaxe.source.add(haxeSourceSet)
						binaryContainer.add(compiledHaxe)

						def sourceHaxe = new DefaultSourceHaxeBinary("source" + functionalSourceSet.name.capitalize(), targetPlatform)
						sourceHaxe.source.add(resourceSet)
						sourceHaxe.source.add(haxeResourceSet)
						sourceHaxe.source.add(haxeSourceSet)
						binaryContainer.add(sourceHaxe)
					}
				})
			}
		})

		// Add a a compile task for each binary
		binaryContainer.withType(CompiledHaxeBinary.class).all(new Action<CompiledHaxeBinary>() {
			public void execute(final CompiledHaxeBinary binary) {
				def compileTask = createCompileTask(project, binary)
				binary.builtBy(compileTask)
			}
		});

		// Add a a compile task for each binary
		binaryContainer.withType(SourceHaxeBinary.class).all(new Action<SourceHaxeBinary>() {
			public void execute(final SourceHaxeBinary binary) {
				def sourceTask = createSourceTask(project, binary)
				binary.builtBy(sourceTask)
			}
		});

		// Map default values
		def haxeExtension = project.extensions.create "haxe", HaxeExtension, project
		project.tasks.withType(HaxeCompile) { HaxeCompile task ->
			haxeExtension.mapTo(task)
		}
		project.tasks.withType(MUnit) { MUnit task ->
			haxeExtension.mapTo(task)
		}

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


//		project.afterEvaluate {
//			project.tasks.withType(HaxeCompile) { HaxeCompile task ->
//				def sources = task.sources
//				task.configuration.artifacts.add(sources)
//				archivesConfig.artifacts.add(sources)
//
//				task.configuration.allDependencies.withType(ProjectDependency) { ProjectDependency dependency ->
//					task.dependsOn task.configuration
//					dependency.projectConfiguration.allArtifacts.withType(HarPublishArtifact) { HarPublishArtifact artifact ->
//						task.dependsOn artifact
//						task.inputs.file artifact.file
//					}
//				}
//			}
//			project.tasks.withType(MUnit) { MUnit task ->
//				def tests = task.tests
//				task.testConfiguration.artifacts.add(tests)
//				testArchivesConfig.artifacts.add(tests)
//
//				task.testConfiguration.allDependencies.withType(ProjectDependency) { ProjectDependency dependency ->
//					dependency.projectConfiguration.allArtifacts.withType(HarPublishArtifact) { HarPublishArtifact artifact ->
//						task.dependsOn artifact
//						task.inputs.file artifact.file
//					}
//				}
//			}
//		}
	}

	private static HaxeCompile createCompileTask(Project project, CompiledHaxeBinary binary) {
		def namingScheme = ((BinaryInternal) binary).namingScheme

		def compileTaskName = namingScheme.getTaskName("compile", null)
		HaxeCompile compileTask = project.task(compileTaskName, type: HaxeCompile) {
			description = "Compiles the $binary"
		} as HaxeCompile

		compileTask.source(binary.source)
		compileTask.conventionMapping.main = { binary.source.withType(HaxeSourceSet)*.main.flatten().find() { it } }
		compileTask.conventionMapping.targetPlatform = { binary.targetPlatform.name }
		compileTask.conventionMapping.embeddedResources = {
			def embeddedResources = binary.source.withType(HaxeResourceSet)*.embeddedResources.flatten().inject([:]) { acc, val -> acc + val}
			println ">>>>>> EMBEDDED: ${embeddedResources}"
			return embeddedResources
		}
		compileTask.conventionMapping.outputFile = { project.file("${project.buildDir}/compiled-haxe/${namingScheme.outputDirectoryBase}/${binary.name}.${compileTask.targetPlatform}") }
		println ">>>> SRC DIRS OUT: ${compileTask.inputDirectories.files}"
		return compileTask
	}

	private static HaxeSource createSourceTask(Project project, SourceHaxeBinary binary) {
		def namingScheme = ((BinaryInternal) binary).namingScheme

		def sourceTaskName = namingScheme.getTaskName(null, null)
		HaxeSource sourceTask = project.task(sourceTaskName, type: HaxeSource) {
			description = "Bundles the sources of $binary"
		} as HaxeSource

		def manifest = sourceTask.manifest

//		if (!embeddedResources.isEmpty())
//		{
//			manifest.attributes.put(HarUtils.MANIFEST_ATTR_EMBEDDED_RESOURCES, EmbeddedResourceEncoding.encode(embeddedResources))
//		}

		sourceTask.into "sources", {
			duplicatesStrategy = DuplicatesStrategy.EXCLUDE
			from { binary.source.withType(HaxeSourceSet)*.source }
		}
		sourceTask.into "resources", {
			duplicatesStrategy = DuplicatesStrategy.EXCLUDE
			from { binary.source.withType(ResourceSet)*.source }
		}
		sourceTask.into "embedded", {
			duplicatesStrategy = DuplicatesStrategy.EXCLUDE
			from { binary.source.withType(HaxeResourceSet)*.embeddedResources*.values() }
		}
		sourceTask.conventionMapping.baseName = { project.name }
		sourceTask.conventionMapping.destinationDir = { project.file("${project.buildDir}/haxe-source/${namingScheme.outputDirectoryBase}") }
		return sourceTask
	}

	private static String getTaskBaseName(FunctionalSourceSet set) {
		return set.name.equals("main") ? "" : GUtil.toCamelCase(set.name)
	}

	public static String getCompileConfigurationName(FunctionalSourceSet set) {
		return StringUtils.uncapitalize(String.format("%sCompile", getTaskBaseName(set)))
	}

	public static String getRuntimeConfigurationName(FunctionalSourceSet set) {
		return StringUtils.uncapitalize(String.format("%sRuntime", getTaskBaseName(set)))
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
