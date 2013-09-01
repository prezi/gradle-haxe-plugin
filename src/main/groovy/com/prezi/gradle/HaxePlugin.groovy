package com.prezi.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.internal.tasks.TaskResolver
import org.gradle.api.tasks.Delete
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
		def testAllTask = project.tasks.findByName("munit")
		if (testAllTask == null)
		{
			testAllTask = project.tasks.create("munit")
			testAllTask.group = TEST_TASKS_GROUP
			testAllTask.description = "Test built Haxe artifacts"
		}
		project.beforeEvaluate {
			project.tasks.withType(MUnit) { MUnit munitTask ->
				munitTask.group = TEST_TASKS_GROUP
				testAllTask.dependsOn munitTask
			}
		}

		project.afterEvaluate {
			project.tasks.withType(CompileHaxe) { CompileHaxe compileTask ->
				compileTask.configuration.artifacts.add(compileTask.sources)

				compileTask.configuration.dependencies.withType(ProjectDependency) { ProjectDependency dependency ->
					dependency.projectConfiguration.artifacts.withType(HarPublishArtifact) { HarPublishArtifact artifact ->
						compileTask.dependsOn(artifact.buildDependencies.getDependencies(compileTask))
					}
				}
			}
			project.tasks.withType(MUnit) { MUnit munitTask ->
				munitTask.testConfiguration.artifacts.add(munitTask.tests)

				munitTask.testConfiguration.dependencies.withType(ProjectDependency) { ProjectDependency dependency ->
					dependency.projectConfiguration.artifacts.withType(HarPublishArtifact) { HarPublishArtifact artifact ->
						munitTask.dependsOn(artifact.buildDependencies.getDependencies(munitTask))
					}
				}
			}
		}
	}
}
