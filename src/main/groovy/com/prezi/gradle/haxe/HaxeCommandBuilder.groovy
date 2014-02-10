package com.prezi.gradle.haxe

import com.prezi.spaghetti.ModuleBundle
import com.prezi.spaghetti.ModuleDefinition
import com.prezi.spaghetti.gradle.ModuleDefinitionLookup
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
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

	HaxeCommandBuilder withSourceSets(DomainObjectSet<LanguageSourceSet> sources) {
		LinkedHashSet<File> sourcePath = []
		LinkedHashSet<File> resourcePath = []
		LinkedHashMap<String, File> allEmbeddedResources = [:]

		sources.withType(HaxeSourceSet) { source ->
			extractor.extractDependenciesFrom(source.compileClassPath, sourcePath, resourcePath, allEmbeddedResources)
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

	HaxeCommandBuilder withSpaghetti(String spaghettiType, File output, Collection<Configuration> configurations)
	{
		if (spaghettiType != null)
		{
			def bundleFile = project.getPlugins().getPlugin(HaxePlugin).getSpaghettiBundleTool(project)
			append("--next", "-cp", bundleFile.parentFile, "--run", "SpaghettiBundler", spaghettiType, output)
			def bundles = configurations.collectMany(new HashSet<ModuleBundle>()) { configuration ->
				ModuleDefinitionLookup.getAllBundles(configuration)
			}
			append(bundles.collect { ModuleBundle bundle ->
				bundle.name.fullyQualifiedName
			}.sort { it }.unique().toArray())
		}
		this
	}
}
