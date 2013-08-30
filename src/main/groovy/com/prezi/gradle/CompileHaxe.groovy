package com.prezi.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.UnionFileCollection
import org.gradle.api.internal.file.copy.FileCopyActionImpl
import org.gradle.api.internal.file.copy.FileCopySpecVisitor
import org.gradle.api.internal.file.copy.SyncCopySpecVisitor
import org.gradle.api.tasks.*
import org.gradle.internal.reflect.Instantiator
import org.gradle.process.internal.ExecException

class CompileHaxe extends DefaultTask {

	public static final String EXTRACTED_HAXELIBS_PATH = "haxelibs"

	@TaskAction
	void compile()
	{
		Set<File> sourcePath = []
		Set<File> resourcePath = []

		sourcePath.addAll(sources.files)
		resourcePath.addAll(resources.files)

		if (configuration != null)
		{
			configuration.hierarchy.each { Configuration config ->
				extractDependenciesFrom(config, sourcePath, resourcePath)
			}
		}

		String cmd = new CmdBuilder()
				.withMain(main)
				.withTarget(targetPlatform, getAndCreateOutput())
				.withMacros(macros)
				.withIncludePackages(includePackages)
				.withExcludePackages(excludePackages)
				.withSources(sourcePath)
				.withResources(resourcePath)
				.withFlags(flags)
				.withDebugFlags(debug)
				.build()

		def res = project.exec {
			executable = 'bash'
			args "-c", cmd
			setIgnoreExitValue true
		}
		if (res.exitValue != 0)
		{
			throw new ExecException("Command finished with non-zero exit value (${res.exitValue}):\n${cmd}")
		}

		if (archive)
		{
			FileResolver fileResolver = getServices().get(FileResolver.class)
			Instantiator instantiator = getServices().get(Instantiator.class)
			def copyAction = new HarCopyAction(instantiator, fileResolver, temporaryDirFactory,
					getSourceArchive(), sources, resources)
			copyAction.execute()
		}
	}

	void extractDependenciesFrom(Configuration configuration, Set<File> sourcePath, Set<File> resourcePath)
	{
		configuration.dependencies.each { ModuleDependency dependency ->
			configuration.files(dependency).each { File file ->
				Instantiator instantiator = getServices().get(Instantiator.class);
				FileResolver fileResolver = getServices().get(FileResolver.class);

				def targetPath = project.file("${project.buildDir}/${EXTRACTED_HAXELIBS_PATH}/${dependency.name}")

				def copy = new FileCopyActionImpl(instantiator, fileResolver, new SyncCopySpecVisitor(new FileCopySpecVisitor()));
				copy.from(project.zipTree(file))
				copy.into targetPath
				copy.execute()

				if (dependency.group == "haxelib")
				{
					sourcePath.add(targetPath)
				}
				else
				{
					def sources = new File(targetPath, "sources")
					def resources = new File(targetPath, "resources")
					if (sources.exists()) sourcePath.add(sources)
					if (resources.exists()) sourcePath.add(resources)
				}
			}
		}
	}

	public PublishArtifact getSourceBundle()
	{
		return new HarPublishArtifact(this, getSourceArchive())
	}

	public File getSourceArchive()
	{
		if (!archive)
		{
			return null
		}
		return new File(project.buildDir, getFullName() + ".har")
	}

	private String getFullName()
	{
		def fullName = getBaseName()
		if (classifier != null && classifier != "")
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
		} else
		{
			output = getOutputFile()
			dirToMake = output.parentFile
		}
		project.mkdir(dirToMake)
		return output
	}

	@InputFiles
	@SkipWhenEmpty
	FileCollection sources = new UnionFileCollection()

	@InputFiles
	@SkipWhenEmpty
	FileCollection resources = new UnionFileCollection()

	File outputFile

	File outputDirectory

	Configuration configuration

	String main = ""

	List macros = []

	List includePackages = []

	List excludePackages = []

	String flags = ''

	String targetPlatform

	boolean debug

	boolean archive = true

	String classifier

	String baseName

	public void configuration(Configuration configuration)
	{
		this.configuration = configuration
	}

	public macro(String m)
	{
		macros.add(m)
	}

	public includePackage(String pkg)
	{
		includePackages.add(pkg)
	}

	public excludePackage(String pkg)
	{
		excludePackages.add(pkg)
	}

	public void source(paths)
	{
		sources.add(project.files(paths))
	}

	public resource(paths)
	{
		resources.add(project.files(paths))
	}

	public void setTargetPlatform(String platform)
	{
		this.targetPlatform = platform
	}

	boolean isOutputInADirectory()
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

	@OutputFile
	@Optional
	public File getOutputFile()
	{
		if (outputFile != null)
		{
			return outputFile
		} else if (outputDirectory != null)
		{
			return null
		} else
		{
			switch (targetPlatform)
			{
				case "js":
					return project.file("${project.buildDir}/compiled-haxe/${name}.js")
				case "swf":
					return project.file("${project.buildDir}/compiled-haxe/${name}.swc")
				default:
					return null
			}
		}
	}

	@OutputDirectory
	@Optional
	public File getOutputDirectory()
	{
		if (outputFile != null)
		{
			return null
		} else if (outputDirectory != null)
		{
			return outputDirectory
		} else
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

	public void setOutputFile(Object file)
	{
		outputFile = project.file(file)
		outputDirectory = null
	}

	public void setOutputDirectory(Object dir)
	{
		outputDirectory = project.file(dir)
		outputFile = null
	}

	public void flag(String flag)
	{
		flags += " $flag"
	}

	public archive(boolean archive)
	{
		this.archive = archive
	}

	public baseName(String baseName)
	{
		this.baseName = baseName
	}

	public String getBaseName()
	{
		return baseName == null ? project.name : baseName
	}

	public classifier(String classifier)
	{
		this.classifier = classifier
	}

	String describe()
	{
		return "Builds " + componentName
	}
}
