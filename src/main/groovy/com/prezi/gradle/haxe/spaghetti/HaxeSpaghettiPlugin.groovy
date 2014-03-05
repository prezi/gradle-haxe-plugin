package com.prezi.gradle.haxe.spaghetti

import com.prezi.gradle.haxe.HaxeCompiledBinary
import com.prezi.gradle.haxe.HaxePlugin
import com.prezi.gradle.haxe.HaxeSourceBinary
import com.prezi.gradle.haxe.MUnit
import com.prezi.spaghetti.gradle.SpaghettiBasePlugin
import com.prezi.spaghetti.gradle.SpaghettiExtension
import com.prezi.spaghetti.gradle.SpaghettiGeneratedSourceSet
import com.prezi.spaghetti.gradle.SpaghettiPlugin
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.reflect.Instantiator
import org.gradle.language.base.BinaryContainer
import org.gradle.language.base.ProjectSourceSet
import org.slf4j.LoggerFactory

import javax.inject.Inject

/**
 * Add Spaghetti support to Haxe.
 */
class HaxeSpaghettiPlugin implements Plugin<Project> {

	private static final logger = LoggerFactory.getLogger(HaxeSpaghettiPlugin)

	private final Instantiator instantiator

	@Inject
	HaxeSpaghettiPlugin(Instantiator instantiator) {
		this.instantiator = instantiator
	}

	@Override
	void apply(Project project) {
		// Spaghetti will be working with Haxe, might as well set it
		project.plugins.apply(SpaghettiBasePlugin)
		def spaghettiExtension = project.extensions.getByType(SpaghettiExtension)
		spaghettiExtension.platform = "haxe"

		project.plugins.apply(HaxePlugin)
		project.plugins.apply(SpaghettiPlugin)

		def binaryContainer = project.extensions.getByType(BinaryContainer)
		def projectSourceSet = project.extensions.getByType(ProjectSourceSet)

		// We'll be needing a "js" platform
		def targetPlatforms = HaxePlugin.getExtension(project).targetPlatforms
		targetPlatforms.maybeCreate("js")

		// Tests should always depend on modules
		def testConfiguration = project.configurations.getByName("test")
		testConfiguration.extendsFrom spaghettiExtension.configuration

		// Add Spaghetti generated sources to compile and source tasks
		projectSourceSet.findByName("main").withType(SpaghettiGeneratedSourceSet).all(new Action<SpaghettiGeneratedSourceSet>() {
			@Override
			void execute(SpaghettiGeneratedSourceSet spaghettiGeneratedSourceSet) {
				logger.debug("Adding ${spaghettiGeneratedSourceSet} to binaries")
				binaryContainer.withType(HaxeCompiledBinary).all(new Action<HaxeCompiledBinary>() {
					@Override
					void execute(HaxeCompiledBinary compiledBinary) {
						compiledBinary.source.add spaghettiGeneratedSourceSet
						HaxeSpaghettiPlugin.logger.debug("Added ${spaghettiGeneratedSourceSet} to ${compiledBinary}")
					}
				})

				binaryContainer.withType(HaxeSourceBinary).all(new Action<HaxeSourceBinary>() {
					@Override
					void execute(HaxeSourceBinary sourceBinary) {
						sourceBinary.source.add spaghettiGeneratedSourceSet
						HaxeSpaghettiPlugin.logger.debug("Added ${spaghettiGeneratedSourceSet} to ${sourceBinary}")
					}
				})

				project.tasks.withType(MUnit).all(new Action<MUnit>() {
					@Override
					void execute(MUnit testTask) {
						testTask.source(spaghettiGeneratedSourceSet)
						testTask.dependsOn spaghettiGeneratedSourceSet
						HaxeSpaghettiPlugin.logger.debug("Added ${spaghettiGeneratedSourceSet} to ${testTask}")
					}
				})
			}
		})

		// Create Spaghetti compatible binary
		binaryContainer.withType(HaxeCompiledBinary).all(new Action<HaxeCompiledBinary>() {
			@Override
			void execute(HaxeCompiledBinary binary) {
				def jsBinary = instantiator.newInstance(DefaultHaxeCompiledSpaghettiCompatibleJavaScriptBinary, binary)
				jsBinary.builtBy(binary.getBuildDependencies())
				binaryContainer.add(jsBinary)
			}
		})
	}

	public static File getSpaghettiBundleTool(Project project) {
		def workDir = project.file("${project.buildDir}/spaghetti-haxe")
		workDir.mkdirs()
		def bundleFile = new File(workDir, "SpaghettiBundler.hx")
		bundleFile.delete()
		bundleFile << HaxeSpaghettiPlugin.class.classLoader.getResourceAsStream("SpaghettiBundler.hx").text
		return bundleFile
	}
}
