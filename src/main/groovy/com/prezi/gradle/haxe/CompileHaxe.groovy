package com.prezi.gradle.haxe

import com.prezi.gradle.DeprecationLogger
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
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
				.withIncludePackages(includePackages)
				.withExcludePackages(excludePackages)
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

	List<Object> sourcePaths = []
	LinkedHashSet<String> legacyPlatformPaths = []

	@InputFiles
	@SkipWhenEmpty
	public FileCollection getSourceFiles()
	{
		return project.files(sourcePaths)
	}

	public void source(Object path)
	{
		sourcePaths.add(path)
		if (path instanceof String
				&& path.startsWith("src/"))
		{
			legacyPlatformPaths << path.substring(4)
		}
	}

	@Deprecated
	public void legacySource(String path)
	{
		DeprecationLogger.nagUserOfReplacedProperty("legacySource", "includeLegacyPlatform")
		if (path.startsWith("src/"))
		{
			legacyPlatformPaths << path.substring(4)
		}
		else
		{
			throw new IllegalArgumentException("Invalid legacy source path (should start with 'src/'): " + path)
		}
	}

	public void includeLegacyPlatform(String platform)
	{
		legacyPlatformPaths << path
	}

	List<Object> resourcePaths = []

	@InputFiles
	@SkipWhenEmpty
	public FileCollection getResourceFiles()
	{
		return project.files(resourcePaths)
	}

	public resource(Object path)
	{
		resourcePaths.add(path)
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

	public embedAll(Object directory)
	{
		def realDir = project.file(directory)
		if (!realDir.directory)
		{
			throw new IllegalArgumentException("embedAll requires a directory: " + directory)
		}
		realDir.eachFileRecurse { embed(it) }
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
		DeprecationLogger.nagUserOfReplacedProperty("flags", "flag")
		((" " + flagsToAdd.trim()).split(" -")).each { if (it) flag("-$it") }
		this
	}

	public void flag(String... flag)
	{
		flagList.addAll(flag)
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
