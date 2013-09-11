package com.prezi.gradle.haxe

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.internal.tasks.TaskResolver
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Upload
import org.gradle.configuration.project.ProjectConfigurationActionContainer
import org.gradle.internal.reflect.Instantiator

import javax.inject.Inject

class HaxePlugin implements Plugin<Project> {

	private static final String BUILD_TASKS_GROUP = "build"
	private static final String TEST_TASKS_GROUP = "test"

	private final Instantiator instantiator
	private final ProjectConfigurationActionContainer configurationActions
	private final TaskResolver taskResolver

	@Inject
	public HaxePlugin(Instantiator instantiator, ProjectConfigurationActionContainer configurationActions)
	{
		this.taskResolver = taskResolver
		this.configurationActions = configurationActions
		this.instantiator = instantiator
	}

	@Override
	void apply(Project project)
	{
		// Add clean task
		def cleanTask = project.tasks.create("clean", Delete)
		cleanTask.delete(project.buildDir)

		// Create default configuration
		if (project.configurations.findByName(Dependency.DEFAULT_CONFIGURATION) == null)
		{
			project.configurations.create(Dependency.DEFAULT_CONFIGURATION)
		}

		// Map default values
		def haxeExtension = project.extensions.create "haxe", HaxeExtension
		project.tasks.withType(CompileHaxe) { CompileHaxe compileTask ->
			haxeExtension.mapTo(compileTask)
		}

		// Add "archives" and "testArchives" configuration
		Configuration archivesConfig = project.configurations.findByName("archives")
		if (archivesConfig == null)
		{
			archivesConfig = project.configurations.create("archives")
		}
		Configuration testArchivesConfig = project.configurations.findByName("testArchives")
		if (testArchivesConfig == null)
		{
			testArchivesConfig = project.configurations.create("testArchives")
		}

		// Add build task
		def buildTask = project.tasks.findByName("build")
		if (buildTask == null)
		{
			buildTask = project.tasks.create("build")
			buildTask.group = BUILD_TASKS_GROUP
			buildTask.description = "Build all Haxe artifacts"
		}
		project.beforeEvaluate {
			project.tasks.withType(CompileHaxe) { CompileHaxe compileTask ->
				compileTask.group = BUILD_TASKS_GROUP
				buildTask.dependsOn compileTask
			}
		}

		// Add test task
		def testAllTask = project.tasks.findByName("test")
		if (testAllTask == null)
		{
			testAllTask = project.tasks.create("test")
			testAllTask.group = TEST_TASKS_GROUP
			testAllTask.description = "Test built Haxe artifacts"
		}
		project.afterEvaluate {
			project.tasks.withType(MUnit) { MUnit munitTask ->
				munitTask.group = TEST_TASKS_GROUP
				testAllTask.dependsOn munitTask
			}
		}

		// Add install tasks
		Upload installTask = project.tasks.create(name: "install", type: Upload)
		installTask.configuration = archivesConfig
		Upload installTestsTask = project.tasks.create(name: "installTests", type: Upload)
		installTestsTask.configuration = testArchivesConfig

		// Add uploadArchives tasks
		Upload uploadArchivesTask = project.tasks.create(name: "uploadArchives", type: Upload)
		uploadArchivesTask.configuration = archivesConfig
		Upload uploadTestArchivesTask = project.tasks.create(name: "uploadTestArchives", type: Upload)
		uploadTestArchivesTask.configuration = testArchivesConfig

		project.afterEvaluate {
			project.tasks.withType(CompileHaxe) { CompileHaxe compileTask ->
				def artifacts = [ compileTask.artifact, compileTask.sources ]
				compileTask.configuration.artifacts.addAll(artifacts)
				archivesConfig.artifacts.addAll(artifacts)

				compileTask.configuration.allDependencies.withType(ProjectDependency) { ProjectDependency dependency ->
					dependency.projectConfiguration.allArtifacts.withType(HarPublishArtifact) { HarPublishArtifact artifact ->
						compileTask.dependsOn(artifact)
						compileTask.inputs.file(artifact.file)
					}
				}
			}
			project.tasks.withType(MUnit) { MUnit munitTask ->
				munitTask.testConfiguration.artifacts.add(munitTask.tests)
				testArchivesConfig.artifacts.add(munitTask.tests)

				munitTask.testConfiguration.allDependencies.withType(ProjectDependency) { ProjectDependency dependency ->
					dependency.projectConfiguration.allArtifacts.withType(HarPublishArtifact) { HarPublishArtifact artifact ->
						munitTask.dependsOn(artifact)
						munitTask.inputs.file(artifact.file)
					}
				}
			}
		}
	}
}
