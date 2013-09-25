package com.prezi.gradle.haxe

import org.gradle.api.Project

class HaxeCommandBuilder {
	private final Project project
	private List<String> cmd

	public HaxeCommandBuilder(Project project, String... cmd)
	{
		this.project = project
		this.cmd = cmd
	}

	String[] build()
	{
		cmd
	}

	private append(Object... what)
	{
		what.each { cmd.push(String.valueOf(it)) }
	}

	HaxeCommandBuilder withMain(String main)
	{
		if (main)
		{
			append("-main", main)
		}
		this
	}

	HaxeCommandBuilder withTarget(String target, File output)
	{
		append("-$target", output)
		this
	}

	HaxeCommandBuilder withIncludes(def packages)
	{
		packages.each { append("--macro", "include('$it')") }
		this
	}

	HaxeCommandBuilder withExcludes(def packages)
	{
		packages.each { append("--macro", "exclude('$it')") }
		this
	}

	HaxeCommandBuilder withMacros(def macros)
	{
		macros.each { append("--macro", it) }
		this
	}

	HaxeCommandBuilder withResources(def resources)
	{
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
				append("-resource", "${filePath}@${handle}")
			}
		}
	}

	HaxeCommandBuilder withSources(Iterable<?> sources)
	{
		sources.each { append("-cp", it) }
		this
	}

	HaxeCommandBuilder withFlags(Iterable<String> flags)
	{
		flags.each { String flag ->
			withFlag(flag)
		}
		this
	}
	HaxeCommandBuilder withFlags(String... flags)
	{
		flags.each { String flag ->
			withFlag(flag)
		}
		this
	}
	private withFlag(String flag)
	{
		append(flag.split(" "))
	}

	HaxeCommandBuilder withDebugFlags(boolean debug)
	{
		if (debug)
		{
			withFlags("-D fdb", "-debug")
		}
		this
	}
}
