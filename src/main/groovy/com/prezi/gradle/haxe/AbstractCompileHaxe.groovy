package com.prezi.gradle.haxe

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.collections.SimpleFileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.reflect.Instantiator
import org.gradle.process.internal.ExecException

abstract class AbstractCompileHaxe extends DefaultTask implements HaxeTask {

	@Delegate(deprecated = true)
	final HaxeCompileParameters params
	final String targetPlatform
	private PublishArtifact sourceBundle

	String baseName
	String classifier

	AbstractCompileHaxe(String targetPlatform)
	{
		this.params = new HaxeCompileParameters(project)
		this.targetPlatform = targetPlatform
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

		def commandBuilder = new HaxeCommandBuilder(project, "haxe")
		commandBuilder
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
		configureCommandBuilder(commandBuilder)
		def cmd = commandBuilder.build()

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

	public baseName(String baseName)
	{
		this.baseName = baseName
	}

	public String getBaseName()
	{
		return baseName ? baseName : project.name
	}

	public classifier(String classifier)
	{
		this.classifier = classifier
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

	public PublishArtifact getSources()
	{
		if (sourceBundle == null)
		{
			sourceBundle = new HarPublishArtifact(this, getSourceArchive())
		}
		return sourceBundle
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

	abstract protected File getAndCreateOutput();

	protected void configureCommandBuilder(HaxeCommandBuilder builder)
	{
		// Do nothing by default
	}
}
