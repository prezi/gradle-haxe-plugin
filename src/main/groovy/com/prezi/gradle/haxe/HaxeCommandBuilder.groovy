package com.prezi.gradle.haxe

class HaxeCommandBuilder {
	private String cmd
	private final String prefix
	private final String suffix
	private final boolean useQuote

	public HaxeCommandBuilder(String cmd, String prefix, String suffix, boolean useQuote)
	{
		this.cmd = cmd
		this.suffix = suffix
		this.prefix = prefix
		this.useQuote = useQuote
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
		append("-$target " + quote(output))
		this
	}

	HaxeCommandBuilder withIncludePackages(def packages)
	{
		packages.each { append("--macro " + quote("include('$it')")) }
		this
	}

	HaxeCommandBuilder withExcludePackages(def packages)
	{
		packages.each { append("--macro " + quote("exclude('$it')")) }
		this
	}

	HaxeCommandBuilder withMacros(def macros)
	{
		macros.each { append("--macro " + quote(it)) }
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
				append("-resource " + quote("${filePath}@${handle}"))
			}
		}
	}

	HaxeCommandBuilder withSources(def sources)
	{
		sources.each { append("-cp " + quote(it)) }
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

	String quote(Object object)
	{
		if (useQuote)
		{
			return '"' + String.valueOf(object).replaceAll('"', '\\"') + '"'
		}
		else
		{
			return String.valueOf(object)
		}
	}
}
