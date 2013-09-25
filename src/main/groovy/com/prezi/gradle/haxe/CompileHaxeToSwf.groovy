package com.prezi.gradle.haxe

class CompileHaxeToSwf extends CompileHaxeWithFileTarget {

	String swfVersion = "11"

	public CompileHaxeToSwf()
	{
		super("swf", "swf")
	}

	public swfVersion(String swfVersion)
	{
		this.swfVersion = swfVersion
	}

	@Override
	protected void configureCommandBuilder(HaxeCommandBuilder builder)
	{
		builder.withFlags("-swf-version ${swfVersion}")
	}
}
