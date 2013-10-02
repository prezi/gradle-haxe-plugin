package com.prezi.gradle.haxe

import org.gradle.api.Project

class HaxeExtension {

	@Delegate(deprecated = true)
	private final HaxeCompileParameters params

	public HaxeExtension(Project project)
	{
		this.params = new HaxeCompileParameters(project)
	}

	void mapTo(AbstractCompileHaxe compileTask)
	{
		params.copyTo compileTask.params
	}
}
