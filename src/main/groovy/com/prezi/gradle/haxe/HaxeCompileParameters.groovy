package com.prezi.gradle.haxe

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.FileCollection

class HaxeCompileParameters {

	private final Project project

	public HaxeCompileParameters(Project project)
	{
		this.project = project
	}

	Configuration configuration

	public boolean hasConfiguration()
	{
		return configuration != null
	}

	public Configuration getConfiguration()
	{
		if (configuration == null)
		{
			return project.configurations[Dependency.DEFAULT_CONFIGURATION]
		}
		return configuration
	}

	public void configuration(Configuration configuration)
	{
		this.configuration = configuration
	}

	List<String> macros = []
	public macro(String m)
	{
		macros.add(m)
	}

	LinkedHashSet<String> includes = []

	public include(String thing)
	{
		includes.add(thing)
	}

	LinkedHashSet<String> excludes = []

	public exclude(String thing)
	{
		excludes.add(thing)
	}

	LinkedHashSet<String> flagList = []

	public void flag(String... flag)
	{
		flagList.addAll(flag)
	}

	boolean debug

	def sources = []

	public srcDir(Object... srcDirs) {
		sources.addAll(srcDirs)
	}

	public FileCollection getSourceDirectories() {
		return project.files(sources.flatten().toArray())
	}

	LinkedHashMap<String, File> embeddedResources = [:]

	public embed(String name, Object file)
	{
		embeddedResources.put(name, project.file(file))
	}

	public embed(Object file)
	{
		def realFile = project.file(file)
		embed(realFile.name, realFile)
	}

	public embed(Map<String, ?> resources)
	{
		resources.each { String name, Object file ->
			embed(name, file)
		}
	}

	public embedAll(Object directory)
	{
		def realDir = project.file(directory)
		if (!realDir.directory)
		{
			throw new IllegalArgumentException("embedAll requires a directory: " + directory)
		}
		realDir.eachFileRecurse { embed(it) }
	}

	String spaghetti

	public spaghetti(String output) {
		if (!(output in ["module", "application"])) {
			throw new IllegalArgumentException("spaghetti argument must be either 'module' or 'application'")
		}
		this.spaghetti = output
	}

	// Clone

	protected void copyTo(HaxeCompileParameters params)
	{
		params.configuration = configuration
		params.macros.addAll(macros)
		params.includes.addAll(includes)
		params.excludes.addAll(excludes)
		params.flagList.addAll(flagList)
		params.debug = debug
		params.embeddedResources.putAll(embeddedResources)
	}

	@Override
	String toString()
	{
		def s = new StringBuilder()
		def separator = "\n\t"
		s.append "Haxe compiler config"
		s.append separator
		s.append "Configuration: ${configuration ? configuration.name : null}"
		s.append separator
		s.append "Macros: ${macros}"
		s.append separator
		s.append "Includes: ${includes}"
		s.append separator
		s.append "Excludes: ${excludes}"
		s.append separator
		s.append "Flags: ${flagList}"
		s.append separator
		s.append "Debug: ${debug}"
		s.append separator
		s.append "Embedded resources: ${embeddedResources}"
		return s.toString()
	}
}
