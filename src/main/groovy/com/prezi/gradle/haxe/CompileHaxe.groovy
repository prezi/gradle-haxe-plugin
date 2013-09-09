package com.prezi.gradle.haxe

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.UnionFileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.reflect.Instantiator

class CompileHaxe extends DefaultTask implements HaxeTask {

	@TaskAction
	void compile()
	{
		Instantiator instantiator = getServices().get(Instantiator.class)
		FileResolver fileResolver = getServices().get(FileResolver.class)
		def extractor = new HaxelibDependencyExtractor(project, legacyPlatformPaths, instantiator, fileResolver)

		LinkedHashSet<File> sourcePath = []
		LinkedHashSet<File> resourcePath = []
		sourcePath.addAll(sourceTree.files)
		resourcePath.addAll(resourceTree.files)
		extractor.extractDependenciesFrom(getConfiguration(), sourcePath, resourcePath)

		String cmd = new HaxeCommandBuilder("haxe", " ", "", true)
				.withMain(main)
				.withTarget(targetPlatform, getAndCreateOutput())
				.withMacros(macros)
				.withIncludePackages(includePackages)
				.withExcludePackages(excludePackages)
				.withSources(sourcePath)
				.withResources(resourcePath)
				.withFlags(flagList)
				.withDebugFlags(debug)
				.build()

		CommandExecutor.execute(project, cmd)

		def copyAction = new HarCopyAction(instantiator, fileResolver, temporaryDirFactory,
				getSourceArchive(), sourceTree, resourceTree)
		copyAction.execute()
	}

	private PublishArtifact artifactBundle

	public PublishArtifact getArtifact()
	{
		if (artifactBundle == null)
		{
			switch (targetPlatform)
			{
				case "js":
				case "swf":
					artifactBundle = new HaxeBuildPublishArtifact(this)
					break;
				default:
					throw new IllegalStateException("Build artifact is not supported for target platform " + targetPlatform)
			}
		}
		return artifactBundle
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

	Configuration configuration

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

	@InputFiles
	@SkipWhenEmpty
	FileCollection sourceTree = new UnionFileCollection()
	LinkedHashSet<String> legacyPlatformPaths = []

	public void source(Object path)
	{
		sourceTree.add(project.files(path))
		if (path instanceof String
				&& path.startsWith("src/"))
		{
			legacyPlatformPaths << path.substring(4)
		}
	}

	@InputFiles
	@SkipWhenEmpty
	FileCollection resourceTree = new UnionFileCollection()

	public resource(Object path)
	{
		resourceTree.add(project.files(path))
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

	String targetPlatform

	String main

	List<String> macros = []

	public macro(String m)
	{
		macros.add(m)
	}

	LinkedHashSet<String> includePackages = []

	public includePackage(String pkg)
	{
		includePackages.add(pkg)
	}

	LinkedHashSet<String> excludePackages = []

	public excludePackage(String pkg)
	{
		excludePackages.add(pkg)
	}

	LinkedHashSet<String> flagList = []

	/**
	 * Use {@link #flag(String)} instead.
	 * @param flag
	 */
	@Deprecated
	public void setFlags(String flagsToAdd)
	{
		((" " + flagsToAdd.trim()).split(" -")).each { if (it) flag("-$it") }
		this
	}

	public void flag(String flag)
	{
		flagList.add(flag)
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

	boolean debug
}
