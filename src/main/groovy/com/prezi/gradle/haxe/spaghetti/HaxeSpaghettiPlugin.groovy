package com.prezi.gradle.haxe.spaghetti

import com.prezi.gradle.haxe.HaxeBinary
import com.prezi.gradle.haxe.HaxePlugin
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
				logger.debug("Adding ${spaghettiGeneratedSourceSet} to binaries in ${project.path}")
				binaryContainer.withType(HaxeBinary).all(new Action<HaxeBinary>() {
					@Override
					void execute(HaxeBinary compiledBinary) {
						compiledBinary.source.add spaghettiGeneratedSourceSet
						HaxeSpaghettiPlugin.logger.debug("Added ${spaghettiGeneratedSourceSet} to ${compiledBinary} in ${project.path}")
					}
				})
			}
		})

		// Create Spaghetti compatible binary
		binaryContainer.withType(HaxeBinary).all(new Action<HaxeBinary>() {
			@Override
			void execute(HaxeBinary binary) {
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
