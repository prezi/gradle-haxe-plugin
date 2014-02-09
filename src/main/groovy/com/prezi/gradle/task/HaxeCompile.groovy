package com.prezi.gradle.task

import com.prezi.gradle.DeprecationLogger

/**
 * Only here for backwards compatibility, use {@link com.prezi.gradle.haxe.HaxeCompile} instead.
 */
@Deprecated
class HaxeCompile extends com.prezi.gradle.haxe.HaxeCompile {
	HaxeCompile()
	{
		DeprecationLogger.nagUserOfReplacedTaskType(name, com.prezi.gradle.haxe.HaxeCompile.name)

		// Legacy projects work with 'hxsrc' as their default configuration
		configuration = project.configurations.findByName("hxsrc")
	}
}
