package com.prezi.gradle.haxe

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.language.base.LanguageSourceSet
import org.gradle.nativebinaries.internal.SourceSetNotationParser

class HaxeCompile extends DefaultTask {

	private static final notationParser = SourceSetNotationParser.parser()

	String targetPlatform
	String main
	Set<LanguageSourceSet> sources = []
	LinkedHashMap<String, File> embeddedResources = [:]

	@TaskAction
	void compile()
	{
		def extractor = new HaxelibDependencyExtractor(project)

		LinkedHashSet<File> sourcePath = []
		LinkedHashSet<File> resourcePath = []
		LinkedHashMap<String, File> allEmbeddedResources = [:]
		sources.each { source ->
			if (source instanceof HaxeSourceSet) {
				extractor.extractDependenciesFrom(source.compileClassPath, sourcePath, resourcePath, allEmbeddedResources)
			}
		}

		allEmbeddedResources.putAll(getEmbeddedResources())

		def output = getAndCreateOutput()
		def builder = new HaxeCommandBuilder(project, "haxe")
				.withMain(getMain())
				.withTarget(getTargetPlatform(), output)
//				.withMacros(getMacros())
//				.withIncludes(getIncludes())
//				.withExcludes(getExcludes())
				.withSources(getInputDirectories())
				.withSources(sourcePath)
				.withSources(resourcePath)
				.withEmbeddedResources(allEmbeddedResources)
//				.withFlags(getFlagList())
//				.withDebugFlags(getDebug())
//				.withSpaghetti(getSpaghetti(), output, getConfiguration())
		String[] cmd = builder.build()

		CommandExecutor.execute(project, cmd, null) { ExecutionResult result ->
			if (result.exitValue != 0)
			{
				throw new RuntimeException("Command finished with non-zero exit value (${result.exitValue}):\n${cmd.join(" ")}")
			}
		}
	}

	public source(Object... sources) {
		sources.each { source ->
			this.sources.addAll(notationParser.parseNotation(source))
		}
	}

	@InputFiles
	public FileCollection getInputDirectories()
	{
		def dirs = (sources*.source*.srcDirs).flatten()
		return project.files(dirs)
	}

	@OutputFile
	@Optional
	File outputFile

	void outputFile(Object file) {
		this.outputFile = project.file(file)
	}

	@OutputDirectory
	@Optional
	File outputDirectory

	void outputDirectory(Object file) {
		this.outputDirectory = project.file(file)
	}

	private File getAndCreateOutput()
	{
		File output
		File dirToMake
		if (isOutputInADirectory())
		{
			output = getOutputDirectory()
			dirToMake = output
		}
		else
		{
			output = getOutputFile()
			dirToMake = output.parentFile
		}
		project.mkdir(dirToMake)
		return output
	}

	private boolean isOutputInADirectory()
	{
		if (getOutputFile() != null)
		{
			return false;
		}
		if (getOutputDirectory() != null)
		{
			return true;
		}
		throw new RuntimeException("Neither output file or directory is set")
	}

	@InputFiles
	public FileCollection getEmbeddedResourceFiles()
	{
		return project.files(getEmbeddedResources())
	}

}
