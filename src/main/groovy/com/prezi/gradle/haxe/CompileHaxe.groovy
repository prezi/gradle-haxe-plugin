package com.prezi.gradle.haxe

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.collections.SimpleFileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.reflect.Instantiator
import org.gradle.process.internal.ExecException

class CompileHaxe extends DefaultTask implements HaxeTask {

	@Delegate(deprecated = true)
	final HaxeCompileParameters params

	public CompileHaxe()
	{
		this.params = new HaxeCompileParameters(project)
	}

	@TaskAction
	void compile()
	{
		Instantiator instantiator = getServices().get(Instantiator.class)
		FileResolver fileResolver = getServices().get(FileResolver.class)
		def extractor = new HaxelibDependencyExtractor(project, legacyPlatformPaths, instantiator, fileResolver)

		LinkedHashSet<File> sourcePath = []
		LinkedHashSet<File> resourcePath = []
		sourcePath.addAll(getSourceFiles().files)
		resourcePath.addAll(getResourceFiles().files)
		Map<String, File> allEmbeddedResources = [:]
		extractor.extractDependenciesFrom(getConfiguration(), sourcePath, resourcePath, allEmbeddedResources)
		allEmbeddedResources.putAll(embeddedResources)

		String[] cmd = new HaxeCommandBuilder(project, "haxe")
				.withMain(main)
				.withTarget(targetPlatform, getAndCreateOutput())
				.withMacros(macros)
				.withIncludes(includes)
				.withExcludes(excludes)
				.withSources(sourcePath)
				.withSources(resourcePath)
				.withEmbeddedResources(allEmbeddedResources)
				.withFlags(flagList)
				.withDebugFlags(debug)
				.build()

		CommandExecutor.execute(project, cmd, null) { ExecutionResult result ->
			if (result.exitValue != 0)
			{
				throw new ExecException("Command finished with non-zero exit value (${result.exitValue}):\n${cmd}")
			}
		}

		def copyAction = new HarCopyAction(instantiator, fileResolver, temporaryDirFactory,
				getSourceArchive(), getSourceFiles(), getResourceFiles(), embeddedResources)
		copyAction.execute()
	}

	private PublishArtifact sourceBundle

	public PublishArtifact getSources()
	{
		if (sourceBundle == null)
		{
			sourceBundle = new HarPublishArtifact(this, getSourceArchive())
		}
		return sourceBundle
	}

	private File getSourceArchive()
	{
		return new File(project.buildDir, getFullName() + ".har")
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

	@InputFiles
	@SkipWhenEmpty
	public FileCollection getSourceFiles()
	{
		return project.files(sourcePaths)
	}

	@InputFiles
	@SkipWhenEmpty
	public FileCollection getResourceFiles()
	{
		return project.files(resourcePaths)
	}

	@InputFiles
	@SkipWhenEmpty
	public FileCollection getEmbeddedResourceFiles()
	{
		return new SimpleFileCollection(embeddedResources.values())
	}

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
			switch (targetPlatform)
			{
				case "js":
					return project.file("${project.buildDir}/compiled-haxe/${getFullName()}.js")
				case "swf":
					return project.file("${project.buildDir}/compiled-haxe/${getFullName()}.swc")
				case "neko":
					return project.file("${project.buildDir}/compiled-haxe/${getFullName()}.n")
				default:
					throw new IllegalStateException("Unsopported platform: " + targetPlatform);
			}
		}
	}

	public void setOutputFile(Object file)
	{
		outputFile = project.file(file)
		outputDirectory = null
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
			switch (targetPlatform)
			{
				case "as3":
					return project.file("${project.buildDir}/compiled-haxe/${name}-as3")
				default:
					return null
			}
		}
	}

	public void setOutputDirectory(Object dir)
	{
		outputDirectory = project.file(dir)
		outputFile = null
	}

	private boolean isOutputInADirectory()
	{
		if (outputFile != null)
		{
			return false;
		}
		if (outputDirectory != null)
		{
			return true;
		}
		return targetPlatform == "as3"
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
