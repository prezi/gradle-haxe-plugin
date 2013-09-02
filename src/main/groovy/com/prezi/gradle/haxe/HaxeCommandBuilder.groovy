package com.prezi.gradle.haxe

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
		println ">>> Resources: " + resources
		resources.each { File resource ->
			if (resource.isDirectory())
			{
				appendResources(resource.listFiles(), "")
			}
			else
			{
				appendResources([resource], "")
			}
		}
		this
	}

	private appendResources(def resources, String prefix)
	{
		resources.each { File resource ->
			if (resource.directory)
			{
				def subPrefix = prefix + resource.name + "/"
				appendResources(resource.listFiles(), subPrefix)
			}
			else
			{
				def handle = prefix + resource.name
				def filePath = resource.getAbsolutePath()
				append("-resource \"${filePath.replaceAll('\"', '\\\"')}@${handle.replaceAll('\"', '\\\"')}\"")
			}
		}
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
			withFlags(["-D fdb", "-debug"])
		}
		this
	}
}
