package com.prezi.gradle.task

import com.prezi.gradle.haxe.DeprecationLogger

/**
 * Only here for backwards compatibility, use {@link com.prezi.gradle.haxe.CompileHaxe} instead.
 */
@Deprecated
class CompileHaxe extends com.prezi.gradle.haxe.CompileHaxe {
	CompileHaxe()
	{
		DeprecationLogger.nagUserOfReplacedTaskType(name, com.prezi.gradle.haxe.CompileHaxe.name)
	}
}
