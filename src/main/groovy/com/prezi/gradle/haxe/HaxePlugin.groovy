package com.prezi.gradle.haxe

import com.prezi.gradle.PreziPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.Upload
import org.gradle.configuration.project.ProjectConfigurationActionContainer
import org.gradle.internal.reflect.Instantiator

import javax.inject.Inject

class HaxePlugin implements Plugin<Project> {

	public static final String COMPILE_TASK_NAME = "compile"
	public static final String COMPILE_TASKS_GROUP = "compile"
	public static final String TEST_TASK_NAME = "test"
	public static final String TEST_TASKS_GROUP = "test"
	public static final String INSTALL_TESTS_TASK_NAME = "installTests"
	public static final String UPLOAD_TESTS_TASK_NAME = "uploadTestArchives"
	public static final String TEST_ARCHIVES_CONFIG_NAME = "testArchives"

	private final Instantiator instantiator
	private final ProjectConfigurationActionContainer configurationActions

	@Inject
	public HaxePlugin(Instantiator instantiator, ProjectConfigurationActionContainer configurationActions)
	{
		this.configurationActions = configurationActions
		this.instantiator = instantiator
	}

	@Override
	void apply(Project project)
	{
		project.getPlugins().apply(PreziPlugin.class);

		// Map default values
		def haxeExtension = project.extensions.create "haxe", HaxeExtension, project
		project.tasks.withType(CompileHaxe) { CompileHaxe task ->
			haxeExtension.mapTo(task)
		}
		project.tasks.withType(MUnit) { MUnit task ->
			haxeExtension.mapTo(task)
		}

		// Add compile task
		def compileTask = project.tasks.findByName(COMPILE_TASK_NAME)
		if (compileTask == null)
		{
			compileTask = project.tasks.create(COMPILE_TASK_NAME)
			compileTask.group = COMPILE_TASKS_GROUP
			compileTask.description = "Compile all Haxe artifacts"
		}
		project.afterEvaluate {
			project.tasks.withType(CompileHaxe) { CompileHaxe task ->
				task.group = COMPILE_TASKS_GROUP
				compileTask.dependsOn task
			}
		}

		// Add test task
		def testTask = project.tasks.findByName(TEST_TASK_NAME)
		if (testTask == null)
		{
			testTask = project.tasks.create(TEST_TASK_NAME)
			testTask.group = TEST_TASKS_GROUP
			testTask.description = "Test built Haxe artifacts"
		}
		project.afterEvaluate {
			project.tasks.withType(MUnit) { MUnit task ->
				task.group = TEST_TASKS_GROUP
				testTask.dependsOn task
			}
		}

		def archivesConfig = project.configurations.getByName(PreziPlugin.ARCHIVES_CONFIG_NAME)

		// Add "testArchives" configuration
		Configuration testArchivesConfig = project.configurations.findByName(TEST_ARCHIVES_CONFIG_NAME)
		if (testArchivesConfig == null)
		{
			testArchivesConfig = project.configurations.create(TEST_ARCHIVES_CONFIG_NAME)
		}

		// Add installTests tasks
		Upload installTask = project.tasks.getByName(PreziPlugin.INSTALL_TASK_NAME) as Upload
		if (project.tasks.findByName(INSTALL_TESTS_TASK_NAME) == null)
		{
			project.tasks.create(name: INSTALL_TESTS_TASK_NAME, type: Upload) {
				description = "Installs the '${TEST_ARCHIVES_CONFIG_NAME}' artifacts into the local Maven repository."
				group = PreziPlugin.INSTALL_TASKS_GROUP
				configuration = testArchivesConfig
				repositories.addAll(installTask.repositories)
				uploadDescriptor = installTask.uploadDescriptor
				descriptorDestination = new File(project.getBuildDir(), 'ivy-installTest.xml')
			}
		}

		// Add uploadTestArchives tasks
		Upload uploadArchivesTask = project.tasks.getByName(PreziPlugin.UPLOAD_TASK_NAME) as Upload
		if (project.tasks.findByName(UPLOAD_TESTS_TASK_NAME) == null)
		{
			project.tasks.create(name: UPLOAD_TESTS_TASK_NAME, type: Upload) {
				description = "Uploads all artifacts belonging to configuration ':${TEST_ARCHIVES_CONFIG_NAME}'"
				group = PreziPlugin.UPLOAD_TASKS_GROUP
				configuration = testArchivesConfig
				repositories.addAll(uploadArchivesTask.repositories)
				uploadDescriptor = uploadArchivesTask.uploadDescriptor
				descriptorDestination = new File(project.getBuildDir(), 'ivy-uploadTest.xml')
			}
		}

		project.afterEvaluate {
			project.tasks.withType(CompileHaxe) { CompileHaxe task ->
				def sources = task.sources
				task.configuration.artifacts.add(sources)
				archivesConfig.artifacts.add(sources)

				task.configuration.allDependencies.withType(ProjectDependency) { ProjectDependency dependency ->
					task.dependsOn task.configuration
					dependency.projectConfiguration.allArtifacts.withType(HarPublishArtifact) { HarPublishArtifact artifact ->
						task.dependsOn artifact
						task.inputs.file artifact.file
					}
				}
			}
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
		}
	}
}
