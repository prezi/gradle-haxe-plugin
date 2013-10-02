package com.prezi.gradle.haxe

import org.gradle.api.Project
import org.gradle.api.internal.ClosureBackedAction

class HaxeExtension {

	@Delegate(deprecated = true)
	private final HaxeCompileParameters compileParams
	private final HaxeCompileParameters testParams

	public HaxeExtension(Project project)
	{
		this.compileParams = new HaxeCompileParameters(project)
		this.testParams = new HaxeCompileParameters(project)
	}

	void mapTo(CompileHaxe compileTask)
	{
		compileParams.copyTo compileTask.params
	}

	void mapTo(MUnit testTask)
	{
		testParams.copyTo testTask.params
	}

	public void test(Closure c)
	{
		new ClosureBackedAction<>(c).execute(testParams)
	}
}
