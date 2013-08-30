package com.prezi.gradle

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
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

		println "About to create extension"
		def builds = instantiator.newInstance(HaxeBuildContainer.class, instantiator)
		project.getExtensions().create(HaxeExtension.NAME, DefaultHaxeExtension, builds)

		println "About to configure extension"
		configurationActions.add(new Action<ProjectInternal>() {
			@Override
			void execute(ProjectInternal t)
			{
				// Trigger the configuration of the extension
				def extension = project.getExtensions().getByType(DefaultHaxeExtension.class)
			}
		})
		println "Configured extension"
		project.getExtensions().configure(HaxeExtension, new Action<HaxeExtension>() {
			@Override
			void execute(HaxeExtension extension)
			{
				extension.getBuilds().registerDefaultFactory(new HaxeBuildFactory(instantiator, project))
				extension.getBuilds().all() { HaxeBuild build ->
					println "Build: " + build.name

					// Create compile task
					def compileTask = project.tasks.create("compile" + build.name.capitalize(), CompileHaxe)
					compileTask.group = BUILD_TASKS_GROUP
					compileTask.description = "Compiles ${build.name}"
					build.taskDependencies.each {
						compileTask.dependsOn it
					}
					compileTask.build = build
					buildTask.dependsOn(compileTask)

					// Create HAR task
					if (build.archive)
					{
						def sourcesTask = project.tasks.create("sources" + build.name.capitalize(), Har)
						sourcesTask.group = BUILD_TASKS_GROUP
						sourcesTask.description = "Bundle sources for ${build.name}"
						sourcesTask.build = build
						compileTask.dependsOn sourcesTask
					}
				}
			}
		});
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
