package com.prezi.gradle.task

import org.gradle.api.tasks.TaskAction;

/**
 * Only here for backwards compatibility, use {@link com.prezi.gradle.haxe.CompileHaxe} instead.
 */
@Deprecated
class CompileHaxe extends com.prezi.gradle.haxe.CompileHaxe {
	CompileHaxe()
	{
		logger.warn("Please don\'t use '${CompileHaxe}' as it is deprecated. Use '${com.prezi.gradle.haxe.CompileHaxe}' instead.")
	}
}
