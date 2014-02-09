package com.prezi.gradle.haxe

import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.language.base.LanguageSourceSet
import org.gradle.nativebinaries.internal.SourceSetNotationParser

class HaxeCompile extends ConventionTask implements HaxeTask {

	private static final notationParser = SourceSetNotationParser.parser()

	String targetPlatform
	String main
	Set<LanguageSourceSet> sources = []
	FileCollection classPath

	@TaskAction
	void compile()
	{
		def extractor = new HaxelibDependencyExtractor(project)

		LinkedHashSet<File> sourcePath = []
		LinkedHashSet<File> resourcePath = []
		LinkedHashMap<String, File> allEmbeddedResources = [:]
//		extractor.extractDependenciesFrom(classPath, sourcePath, resourcePath, allEmbeddedResources)
//		allEmbeddedResources.putAll(getEmbeddedResources())

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

		// HarUtils.createArchive(project, temporaryDirFactory, project.buildDir, getFullName(), getSourceDirectories(), [], embeddedResources)
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
		println "-----> Input dirs: ${dirs}"
		return project.files(dirs)
	}

	private String getFullName()
	{
		def fullName = getBaseName()
		if (classifier)
		{
			fullName += "-" + classifier
		}
		return fullName
	}

	File getAndCreateOutput()
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

//	@InputFiles
//	public FileCollection getEmbeddedResourceFiles()
//	{
//		return project.files(embeddedResources.values())
//	}

	private File outputFile

	@OutputFile
	@Optional
	public File getOutputFile()
	{
		if (outputFile != null)
		{
			return outputFile
		}
		else if (outputDirectory != null)
		{
			return null
		}
		else
		{
			switch (getTargetPlatform())
			{
				case "js":
					return project.file("${project.buildDir}/compiled-haxe/${getFullName()}.js")
				case "swf":
					return project.file("${project.buildDir}/compiled-haxe/${getFullName()}.swc")
				case "neko":
					return project.file("${project.buildDir}/compiled-haxe/${getFullName()}.n")
				default:
					throw new IllegalStateException("Unsupported platform: " + getTargetPlatform());
			}
		}
	}

	public void setOutputFile(Object file)
	{
		this.outputFile = project.file(file)
		this.outputDirectory = null
	}

	private File outputDirectory

	@OutputDirectory
	@Optional
	public File getOutputDirectory()
	{
		if (outputFile != null)
		{
			return null
		}
		else if (outputDirectory != null)
		{
			return outputDirectory
		}
		else
		{
			switch (getTargetPlatform())
			{
				case "as3":
					return project.file("${project.buildDir}/compiled-haxe/${name}-as3")
				case "java":
					return project.file("${project.buildDir}/compiled-haxe/${name}-java")
				default:
					return null
			}
		}
	}

	public void setOutputDirectory(Object dir)
	{
		this.outputDirectory = project.file(dir)
		this.outputFile = null
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
		return getTargetPlatform() in [ "as3", "java" ]
	}

	String baseName

	public baseName(String baseName)
	{
		this.baseName = baseName
	}

	public String getBaseName()
	{
		return baseName ? baseName : project.name
	}

	String classifier

	public classifier(String classifier)
	{
		this.classifier = classifier
	}
}
