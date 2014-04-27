package com.prezi.gradle.haxe.spaghetti

import com.prezi.gradle.haxe.HaxeCommandBuilder
import com.prezi.gradle.haxe.HaxeCompile
import com.prezi.gradle.haxe.HaxeSourceSet
import org.gradle.api.DomainObjectSet
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.language.base.LanguageSourceSet

/**
 * Created by lptr on 27/04/14.
 */
class HaxeCompileWithSpaghetti extends HaxeCompile {
	@Input
	@Optional
	String wrap
	public wrap(String output) {
		if (!(output in ["module", "application"])) {
			throw new IllegalArgumentException("spaghetti argument must be either 'module' or 'application'")
		}
		this.wrap = output
	}

	@Override
	protected HaxeCommandBuilder configureHaxeCommandBuilder(File output, DomainObjectSet<LanguageSourceSet> sources) {
		def builder = super.configureHaxeCommandBuilder(output, sources)

		if (getWrap()) {
			def allClassPaths = sources.withType(HaxeSourceSet)*.compileClassPath
			def bundleCommand = HaxeCommandUtils.spaghettiBundlerCommand(getWrap(), output, project.buildDir, allClassPaths, { it.name })
			builder.append("--next", *bundleCommand)
		}
		return builder
	}
}
