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
		if (target == "swf")
		{
			append("-swf-version", 11)
		}
		this
	}

	HaxeCommandBuilder withIncludes(def inlcudes)
	{
		inlcudes.each { append("--macro", "include('$it')") }
		this
	}

	HaxeCommandBuilder withExcludes(def excludes)
	{
		excludes.each { append("--macro", "exclude('$it')") }
		this
	}

	HaxeCommandBuilder withMacros(def macros)
	{
		macros.each { append("--macro", it) }
		this
	}

	HaxeCommandBuilder withEmbeddedResources(Map<String, File> embeddedResources)
	{
		embeddedResources.each { String name, File file ->
			def filePath = file.getAbsolutePath()
			append("-resource", "${filePath}@${name}")
		}
		this
	}

	HaxeCommandBuilder withSources(def sources)
	{
		sources.each { append("-cp", it) }
		this
	}

	HaxeCommandBuilder withFlags(def flags)
	{
		flags.each { String flag ->
			append(flag.split(" "))
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

	HaxeCommandBuilder withGenerateExterns(boolean generate)
	{
		if (generate)
		{
			def externGenerator = project.plugins.getPlugin(HaxePlugin).getExternGenerator(project)
			append("-cp", externGenerator.getAbsolutePath())
			append("--macro", "prezi.macros.GenerateExterns.generate()")
		}
		this
	}
}
