package com.prezi.gradle.haxe

import org.gradle.api.Project

class MUnitCommandBuilder {
	private final Project project
	private List<String> cmd

	public MUnitCommandBuilder(Project project, String... cmd)
	{
		this.project = project
		this.cmd = cmd
	}

	String[] build()
	{
		if (isSet('munit.haxeRunner'))
		{
			cmd << "haxe" << "--run" << "tools.haxelib.Main"
		}
		else
		{
			cmd << "haxelib"
		}
		cmd << "run" << "munit" << "test"
		processCommandLineOptions()
		cmd
	}

	private String processCommandLineOptions()
	{
		if (isSet('munit.nogen'))
		{
			cmd << "-nogen"
		}
		if (isSet('munit.platform'))
		{
			cmd << "-${get('munit.platform')}"
		}
		if (isSet('munit.browser'))
		{
			cmd << "-browser" << get('munit.browser')
		}
		if (isSet('munit.kill-browser'))
		{
			cmd << "-kill-browser"
		}
		if (isSet('munit.debug'))
		{
			cmd << "-debug"
		}
		cmd
	}

	private def get(String prop)
	{
		project.properties[prop]
	}

	private boolean isSet(String prop)
	{
		project.properties[prop] != null
	}
}
