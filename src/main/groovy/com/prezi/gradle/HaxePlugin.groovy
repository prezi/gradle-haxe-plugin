package com.prezi.gradle

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact
import org.gradle.api.internal.tasks.TaskResolver
import org.gradle.api.tasks.Delete
import org.gradle.configuration.project.ProjectConfigurationActionContainer
import org.gradle.internal.reflect.Instantiator

import javax.inject.Inject

class HaxePlugin implements Plugin<Project> {

	private static final String BUILD_TASKS_GROUP = "build"

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

		def buildTask = project.tasks.create("build")
		buildTask.group = BUILD_TASKS_GROUP
		buildTask.description = "Build all Haxe artifacts"

		def builds = instantiator.newInstance(HaxeBuildContainer.class, instantiator)
		project.getExtensions().create(HaxeExtension.NAME, DefaultHaxeExtension, builds)
		project.getExtensions().configure(HaxeExtension, new Action<HaxeExtension>() {
			@Override
			void execute(HaxeExtension extension)
			{
				extension.getBuilds().registerDefaultFactory(new HaxeBuildFactory(instantiator, project))
			}
		})

		configurationActions.add{
			// Trigger the configuration of the extension
			def extension = project.getExtensions().getByType(DefaultHaxeExtension.class)
			extension.getBuilds().all() { HaxeBuild build ->
				println "Build during configuration: " + build

				// Create compile task
				def compileTask = project.tasks.create("compile" + build.name.capitalize(), CompileHaxe)
				compileTask.group = BUILD_TASKS_GROUP
				compileTask.description = "Compiles ${build.name}"
				build.taskDependencies.each {
					println "Depending on " + it
					compileTask.dependsOn it
				}
				compileTask.build = build
				buildTask.dependsOn(compileTask)

				// Create HAR task
				if (build.archive)
				{
					Har sourcesTask = project.tasks.create("sources" + build.name.capitalize(), Har)
					sourcesTask.group = BUILD_TASKS_GROUP
					sourcesTask.description = "Bundle sources for ${build.name}"
					sourcesTask.build = build
					compileTask.dependsOn sourcesTask

					def sourcesArtifact = new ArchivePublishArtifact(sourcesTask)
					def runtimeConfiguration = build.configuration
					runtimeConfiguration.getArtifacts().add(sourcesArtifact)
					println "Components before adding ${build.name}: " + project.getComponents()
					project.getComponents().add(new HaxeLibrary(build.componentName, sourcesArtifact, runtimeConfiguration.getAllDependencies()))
					println "Components after adding ${build.name}: " + project.getComponents()
				}
			}
		}
	}

	private class HaxeBuildFactory implements NamedDomainObjectFactory<HaxeBuild> {
		private final Instantiator instantiator
		private final Project project

		private HaxeBuildFactory(Instantiator instantiator, Project project)
		{
			this.instantiator = instantiator
			this.project = project
		}

		public HaxeBuild create(String name)
		{
			println "Making build: " + name
			return instantiator.newInstance(
					HaxeBuild.class,
					name, project
			)
		}
	}
}
