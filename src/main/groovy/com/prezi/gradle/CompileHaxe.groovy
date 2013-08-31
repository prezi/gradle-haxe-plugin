package com.prezi.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.UnionFileCollection
import org.gradle.api.internal.file.copy.FileCopyActionImpl
import org.gradle.api.internal.file.copy.FileCopySpecVisitor
import org.gradle.api.internal.file.copy.SyncCopySpecVisitor
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.reflect.Instantiator
import org.gradle.process.internal.ExecException

class CompileHaxe extends DefaultTask {

	public static final String EXTRACTED_HAXELIBS_PATH = "haxelibs"

	PublishArtifact sourceBundle

	@TaskAction
	void compile()
	{
		Set<File> sourcePath = []
		Set<File> resourcePath = []

		sourcePath.addAll(sources.files)
		resourcePath.addAll(resources.files)

		def configuration = getConfiguration()

		configuration.hierarchy.each { Configuration config ->
			extractDependenciesFrom(config, sourcePath, resourcePath)
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

		FileResolver fileResolver = getServices().get(FileResolver.class)
		Instantiator instantiator = getServices().get(Instantiator.class)
		def copyAction = new HarCopyAction(instantiator, fileResolver, temporaryDirFactory,
				getSourceArchive(), sources, resources)
		copyAction.execute()
	}

	public PublishArtifact getSourceBundle()
	{
		if (sourceBundle == null)
		{
			sourceBundle = new HarPublishArtifact(this, getSourceArchive())
		}
		return sourceBundle
	}

	void extractDependenciesFrom(Configuration configuration, Set<File> sourcePath, Set<File> resourcePath)
	{
		configuration.dependencies.each { ModuleDependency dependency ->
			if (dependency instanceof ProjectDependency)
			{
				def projectDependency = dependency as ProjectDependency
				def dependentConfiguration = projectDependency.projectConfiguration
				extractDependenciesFrom(dependentConfiguration, sourcePath, resourcePath)

				dependentConfiguration.allArtifacts.withType(HarPublishArtifact) { HarPublishArtifact artifact ->
					extractFile(
							artifact.name + (artifact.classifier == null ? "" : "-" + artifact.classifier),
							artifact.file,
							false,
							sourcePath,
							resourcePath)
				}
			}
			else
			{
				configuration.files(dependency).each { File file ->
					extractFile(dependency.name, file, dependency.group == "haxelib", sourcePath, resourcePath)
				}
			}
		}
	}

	private void extractFile(String name, File file, boolean legacyHaxelib, Set<File> sourcePath, Set<File> resourcePath)
	{
		def targetPath = project.file("${project.buildDir}/${EXTRACTED_HAXELIBS_PATH}/${name}")
		println "Extracting Haxe library file: " + file
		Instantiator instantiator = getServices().get(Instantiator.class);
		FileResolver fileResolver = getServices().get(FileResolver.class);

		def copy = new FileCopyActionImpl(instantiator, fileResolver, new SyncCopySpecVisitor(new FileCopySpecVisitor()));
		copy.from(project.zipTree(file))
		copy.into targetPath
		copy.execute()

		// TODO Determine this based on the manifest
		if (legacyHaxelib)
		{
			sourcePath.add(targetPath)
		} else
		{
			def sources = new File(targetPath, "sources")
			def resources = new File(targetPath, "resources")
			if (sources.exists()) sourcePath.add(sources)
			if (resources.exists()) sourcePath.add(resources)
		}
	}

	public File getSourceArchive()
	{
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

	String classifier

	String baseName

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
