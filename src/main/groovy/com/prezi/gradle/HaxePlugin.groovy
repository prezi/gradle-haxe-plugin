package com.prezi.gradle

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete

class HaxePlugin implements Plugin<Project> {

	@Override
	void apply(Project project)
	{
		// Add clean task
		def cleanTask = project.tasks.create("clean", Delete)
		cleanTask.delete(project.buildDir)
	}
}
