package com.prezi.gradle

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.UnionFileCollection
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.internal.tasks.DefaultTaskDependency
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SkipWhenEmpty

class HaxeBuild implements Named {
	String name
	Project project
	DefaultTaskDependency taskDependencies;

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
	File outputDirectory

	List<Configuration> configurations = [];

	String main = ""

	List macros = []

	List includePackages = []

	List excludePackages = []

	String flags = ''

	String targetPlatform

	boolean debug

	boolean archive = true

	public HaxeBuild(String name, ProjectInternal project)
	{
		this.name = name
		this.project = project;
		this.taskDependencies = new DefaultTaskDependency(project.tasks)
	}

	public dependsOn(Object... task)
	{
		taskDependencies.add(task)
	}

	public void configuration(Configuration configuration)
	{
		configurations.add(configuration)
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

	public File createAndGetOutput()
	{
		if (outputFile != null)
		{
			return outputFile
		}
		if (outputDirectory != null)
		{
			project.mkdir(outputDirectory)
			return outputDirectory
		}
		switch (targetPlatform)
		{
			case "js":
				return project.file("${project.buildDir}/compiled-haxe/${name}.js")
			case "swf":
				return project.file("${project.buildDir}/compiled-haxe/${name}.swc")
			case "as3":
				def dir = project.file("${project.buildDir}/compiled-haxe/${name}-as3")
				project.mkdir(dir)
				return dir
			default:
				return null
		}
	}

	public void setOutputFile(Object file)
	{
		outputFile = project.file(file)
	}

	public void setOutputDirectory(Object dir)
	{
		outputDirectory = project.file(dir)
	}

	public void flag(String flag)
	{
		flags += " $flag"
	}
}
