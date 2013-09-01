package com.prezi.gradle

class HaxeCommandBuilder {
	private String cmd
	private final String prefix
	private final String suffix

	public HaxeCommandBuilder(String cmd, String prefix, String suffix)
	{
		this.cmd = cmd
		this.suffix = suffix
		this.prefix = prefix
	}

	String build()
	{
		cmd
	}

	private append(String what)
	{
		cmd += prefix + what + suffix
	}

	HaxeCommandBuilder withMain(String main)
	{
		if (main)
		{
			append("-main $main")
		}
		this
	}

	HaxeCommandBuilder withTarget(String target, File output)
	{
		append("-$target $output")
		this
	}

	HaxeCommandBuilder withIncludePackages(def packages)
	{
		packages.each { append("--macro \"include('$it')\"") }
		this
	}

	HaxeCommandBuilder withExcludePackages(def packages)
	{
		packages.each { append("--macro \"exclude('$it')\"") }
		this
	}

	HaxeCommandBuilder withMacros(def macros)
	{
		macros.each { append("--macro \"${it.replaceAll('"', '\\"')}\"") }
		this
	}

	HaxeCommandBuilder withResources(def resources)
	{
		resources.each {
			def fileName = it.name
			def filePath = it.getAbsolutePath()
			append("-resource \"${filePath.replaceAll('\"', '\\\"')}@${fileName.replaceAll('\"', '\\\"')}\"")
		}
		this
	}

	HaxeCommandBuilder withSources(def sources)
	{
		sources.each { append("-cp ${it}") }
		this
	}

	HaxeCommandBuilder withFlags(def flags)
	{
		flags.each { String flag ->
			append(flag)
		}
		this
	}

	HaxeCommandBuilder withDebugFlags(boolean debug)
	{
		if (debug)
		{
			withFlags([ "-D fdb", "-debug" ])
		}
		this
	}
}
