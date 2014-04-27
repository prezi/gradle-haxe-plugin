package com.prezi.haxe.gradle

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.language.base.BinaryContainer
import org.gradle.language.base.LanguageSourceSet
import org.gradle.language.base.ProjectSourceSet
import org.gradle.language.jvm.ResourceSet
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

/**
 * Created by lptr on 27/04/14.
 */
@SuppressWarnings("GroovyPointlessBoolean")
class HaxePluginTest extends Specification {
	Project project
	HaxeExtension extension

	def setup() {
		this.project = ProjectBuilder.builder().withProjectDir(new File("root")).build()
		this.project.apply plugin: "haxe"
		this.extension = project.extensions.getByType(HaxeExtension)
	}

	def "empty project"() {
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
		sourceDirs("main", "haxe", HaxeSourceSet) == files("src/main/haxe")
		sourceDirs("main", "resources", ResourceSet) == files("src/main/resources")
		sourceDirs("main", "haxeResources", HaxeResourceSet).empty

		// No tasks should be defined
		project.tasks.withType(HaxeCompile).empty
		project.tasks.withType(Har).empty
		project.tasks.withType(MUnit).empty

		// Configurations should be the bare minimum
		project.configurations*.name.sort() == ["archives", "default", "main", "test"]

		// Group tasks should be created, but should not depend on other tasks
		compileTask.dependsOn.findAll { it instanceof Task }.empty
		testTask.dependsOn.findAll { it instanceof Task }.empty

		// There should be no binaries
		project.extensions.getByType(BinaryContainer).empty
	}

	@SuppressWarnings("GroovyAssignabilityCheck")
	private void configureWithJsTargetPlatform() {
		extension.targetPlatforms {
			js
		}
	}

	def "js target platform creates sources"() {
		configureWithJsTargetPlatform()

		expect:
		sourceDirs("js", "haxe", HaxeSourceSet) == files("src/js/haxe")
		sourceDirs("js", "resources", ResourceSet) == files("src/js/resources")
		sourceDirs("js", "haxeResources", HaxeResourceSet).empty
		sourceDirs("jsTest", "haxe", HaxeSourceSet) == files("src/jsTest/haxe")
		sourceDirs("jsTest", "resources", ResourceSet) == files("src/jsTest/resources")
		sourceDirs("jsTest", "haxeResources", HaxeResourceSet).empty
	}

	def "js target platform creates configurations"() {
		configureWithJsTargetPlatform()

		expect:
		project.configurations*.name.sort() == ["archives", "default", "js", "jsTest", "main", "test"]
		project.configurations.getByName("js").extendsFrom*.name == ["main"]
		project.configurations.getByName("jsTest").extendsFrom*.name.sort() == ["js", "test"]
	}

	def "js target platform creates binaries"() {
		configureWithJsTargetPlatform()
		def binaryContainer = project.extensions.getByType(BinaryContainer)
		HaxeBinary binary = binaryContainer.iterator().next() as HaxeBinary

		expect:
		binaryContainer.size() == 1
		binary.name == "js"
		binary.buildDependencies.getDependencies(null)*.name == ["js"]
		binary.compileTask.name == "compileJs"
		binary.sourceHarTask.name == "bundleJsSource"
		binary.targetPlatform.name == "js"
		binary.flavor == null
		binary.configuration.name == "js"
		binary.testConfiguration.name == "jsTest"

		sourceDirs(binary.source) == files(
				"src/main/haxe",
				"src/main/resources",
				"src/js/haxe",
				"src/js/resources"
		)

		sourceDirs(binary.testSource) == files(
				"src/test/haxe",
				"src/test/resources",
				"src/jsTest/haxe",
				"src/jsTest/resources"
		)
	}

	def "js target platform creates tasks"() {
		configureWithJsTargetPlatform()
		HaxeCompile compileTask = project.tasks.getByName("compileJs") as HaxeCompile
		MUnit munitTask = project.tasks.getByName("testJs") as MUnit
		// TODO Somehow test the source bundle task

		expect:
		compileTask.outputFile == project.file("${project.buildDir}/compiled-haxe/js/js.js")
		compileTask.outputDirectory == null
		compileTask.targetPlatform.name == "js"
		sourceDirs(compileTask.sourceSets) == files(
				"src/main/haxe",
				"src/main/resources",
				"src/js/haxe",
				"src/js/resources"
		)

		sourceDirs(munitTask.sourceSets) == files(
				"src/main/haxe",
				"src/main/resources",
				"src/js/haxe",
				"src/js/resources"
		)

		sourceDirs(munitTask.testSourceSets) == files(
				"src/test/haxe",
				"src/test/resources",
				"src/jsTest/haxe",
				"src/jsTest/resources"
		)
	}

	private List<File> sourceDirs(String functionalSourceSet, String languageSourceSet, Class<? extends LanguageSourceSet> type = LanguageSourceSet) {
		def sourceSet = project.extensions.getByType(ProjectSourceSet).getByName(functionalSourceSet).getByName(languageSourceSet)
		if (!(type.isAssignableFrom(sourceSet.getClass()))) {
			throw new ClassCastException("Expected \"${languageSourceSet}\" in \"${functionalSourceSet}\" to be ${type.name} but got ${sourceSet.getClass().name}")
		}
		sourceDirs(sourceSet)
	}

	private static List<File> sourceDirs(Collection<LanguageSourceSet> sourceSet) {
		sourceDirs(sourceSet.toArray(new LanguageSourceSet[0]))
	}

	private static List<File> sourceDirs(LanguageSourceSet... sourceSet) {
		(sourceSet*.source.srcDirs).flatten().sort()
	}

	private files(String... name) {
		project.files(name).files.sort()
	}
}
