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

	void mapTo(HaxeCompile compileTask)
	{
		HaxeCompileParameters.setConvention compileTask, compileParams
	}

	// TODO Filter out --no-traces so tests can trace
	void mapTo(MUnit munitTask)
	{
		HaxeCompileParameters.setConvention munitTask, compileParams, testParams
	}

	public void test(Closure c)
	{
		new ClosureBackedAction<>(c).execute(testParams)
	}
}
