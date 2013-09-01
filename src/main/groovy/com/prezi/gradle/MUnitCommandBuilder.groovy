package com.prezi.gradle

class MUnitCommandBuilder {
	private String cmd = "haxelib run munit test"

	String build()
	{
		cmd
	}

	MUnitCommandBuilder withDebug(boolean debug)
	{
		if (debug)
		{
			cmd += " -debug"
		}
		this
	}
}
