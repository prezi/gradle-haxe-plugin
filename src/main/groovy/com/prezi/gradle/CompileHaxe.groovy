package com.prezi.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.UnionFileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.process.internal.ExecException

class CompileHaxe extends DefaultTask {

	@InputFiles
	@SkipWhenEmpty
	FileCollection sources = new UnionFileCollection()

	@InputFiles
	@SkipWhenEmpty
	FileCollection resources = new UnionFileCollection()

	@OutputFile
	@Optional
	File outputFile

	@OutputDirectory
	@Optional
	File outputDir

	List<Configuration> configurations = [];

	public void configuration(Configuration configuration)
	{
		configurations.add(configuration)
	}

	String main = ""

	List macros = []

	List includePackages = []

	List excludePackages = []

	String flags = ''

	String targetPlatform

	boolean debug

	@TaskAction
	void compile()
	{
		String cmd = new CmdBuilder()
				.withMain(main)
				.withTarget(targetPlatform, outputFile == null ? outputDir : outputFile)
				.withMacros(macros)
				.withIncludePackages(includePackages)
				.withExcludePackages(excludePackages)
				.withResources(resources.files)
				// .withHaxelibs(project, collectTargetDirs())
				.withSources(sources.files)
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
		if ("js".equals(platform))
		{
			outputFile = outputFile ?: project.file("${project.buildDir}/${project.name}.js")
		}
		if ("swf".equals(platform))
		{
			outputFile = outputFile ?: project.file("${project.buildDir}/${project.name}.swc")
		}
		if ("as3".equals(platform))
		{
			outputDir = outputDir ?: project.file("${project.buildDir}/as3")
		}
	}

	public void setOutputFile(Object file)
	{
		outputFile = project.file(file)
	}

	public void setOutputDir(Object dir)
	{
		outputDir = project.file(dir)
	}

	public void flag(String flag)
	{
		flags += " $flag"
	}

}
