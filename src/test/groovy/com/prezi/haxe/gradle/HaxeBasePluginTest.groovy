package com.prezi.haxe.gradle

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.language.base.LanguageSourceSet
import org.gradle.language.base.ProjectSourceSet
import org.gradle.language.jvm.ResourceSet
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

/**
 * Created by lptr on 27/04/14.
 */
@SuppressWarnings("GroovyPointlessBoolean")
class HaxeBasePluginTest extends Specification {
	Project project
	HaxeExtension extension

	def setup() {
		this.project = ProjectBuilder.builder().withProjectDir(new File("root")).build()
		this.project.apply plugin: "haxe"
		this.extension = project.extensions.getByType(HaxeExtension)
	}

	def "empty project"() {
		def projectSourceSet = project.extensions.getByType(ProjectSourceSet)
		def mainSources = projectSourceSet.getByName("main")
		def mainSourceSet = mainSources.getByName("haxe")
		def mainResourceSet = mainSources.getByName("resources")
		def mainHaxeResourceSet = mainSources.getByName("haxeResources")
		def compileTask = project.tasks.getByName("compile")
		def testTask = project.tasks.getByName("test")

		expect:
		// Extension should be uninitialized
		extension != null
		extension.debug == false
		extension.includes.empty
		extension.excludes.empty
		extension.flagList.empty
		extension.macros.empty
		extension.targetPlatforms.empty

		// Main source sets should point to the default locations
		mainSourceSet instanceof HaxeSourceSet
		sourceDirs(mainSourceSet) == [project.file("src/main/haxe")]
		mainResourceSet instanceof ResourceSet
		sourceDirs(mainResourceSet) == [project.file("src/main/resources")]
		mainHaxeResourceSet instanceof HaxeResourceSet
		sourceDirs(mainHaxeResourceSet).empty

		// No tasks should be defined
		project.tasks.withType(HaxeCompile).empty
		project.tasks.withType(Har).empty
		project.tasks.withType(MUnit).empty

		// Configurations should be the bare minimum
		project.configurations*.name.sort() == ["archives", "default", "main", "test"]

		// Group tasks should be created, but should not depend on anything
		compileTask.dependsOn.findAll { it instanceof Task }.empty
		testTask.dependsOn.findAll { it instanceof Task }.empty
	}

	def "js target platform creates sources"() {
		extension.targetPlatforms {
			js
		}
		def projectSourceSet = project.extensions.getByType(ProjectSourceSet)
		def jsSources = projectSourceSet.getByName("js")
		def jsTestSources = projectSourceSet.getByName("jsTest")

		expect:
		sourceDirs(jsSources.getByName("haxe")) == [project.file("src/js/haxe")]
		sourceDirs(jsSources.getByName("resources")) == [project.file("src/js/resources")]
		sourceDirs(jsSources.getByName("haxeResources")).empty
		sourceDirs(jsTestSources.getByName("haxe")) == [project.file("src/jsTest/haxe")]
		sourceDirs(jsTestSources.getByName("resources")) == [project.file("src/jsTest/resources")]
		sourceDirs(jsTestSources.getByName("haxeResources")).empty
	}

	def "js target platform creates configurations"() {
		extension.targetPlatforms {
			js
		}

		expect:
		extension.targetPlatforms*.name == ["js"]

		// Configurations need to be created
		project.configurations*.name.sort() == ["archives", "default", "js", "jsTest", "main", "test"]
		project.configurations.getByName("js").extendsFrom*.name == ["main"]
		project.configurations.getByName("jsTest").extendsFrom*.name.sort() == ["js", "test"]
	}

	private static List<File> sourceDirs(LanguageSourceSet sourceSet) {
		sourceSet.source.srcDirs.sort()
	}
}
