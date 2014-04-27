package com.prezi.gradle.haxe

import org.gradle.api.Project
import org.gradle.language.base.LanguageSourceSet

class HaxeCommandBuilder {
	private final Project project
	private final HaxelibDependencyExtractor extractor
	private List<String> cmd

	public HaxeCommandBuilder(Project project, String... cmd)
	{
		this.project = project
		this.extractor = new HaxelibDependencyExtractor(project)
		this.cmd = cmd
	}

	String[] build()
	{
		cmd
	}

	public append(Object... what)
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

	private HaxeCommandBuilder withEmbeddedResources(Map<String, File> embeddedResources)
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

	HaxeCommandBuilder withSourceSets(Set<LanguageSourceSet> sources, Map<String, File> embeddedResources) {
		LinkedHashSet<File> sourcePath = []
		LinkedHashSet<File> resourcePath = []
		LinkedHashMap<String, File> allEmbeddedResources = new LinkedHashMap<>(embeddedResources)

		sources.each { source ->
			if (source instanceof HaxeSourceSet) {
				extractor.extractDependenciesFrom(source.compileClassPath, sourcePath, resourcePath, allEmbeddedResources)
			}
		}

		withSources(sourcePath)
		withSources(resourcePath)
		withEmbeddedResources(allEmbeddedResources)
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
}
