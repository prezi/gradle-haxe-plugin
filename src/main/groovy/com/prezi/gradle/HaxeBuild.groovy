package com.prezi.gradle

import groovy.transform.ToString
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.UnionFileCollection
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.internal.tasks.DefaultTaskDependency
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SkipWhenEmpty

@ToString(includeNames = true)
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

	File outputFile

	File outputDirectory

	Configuration configuration

	String componentName

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
		this.configuration = configuration
	}

	public componentName(String componentName)
	{
		this.componentName = componentName
	}

	public String getComponentName()
	{
		if (componentName == null)
		{
			return "haxe";
		}
		return componentName;
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

	@OutputFile
	@OutputDirectory
	public File getOutput()
	{
		File output
		File dirToMake
		if (outputFile != null)
		{
			output = outputFile
			dirToMake = outputFile.parentFile
		}
		else if (outputDirectory != null)
		{
			output = outputDirectory
			dirToMake = outputDirectory
		}
		else
		{
			switch (targetPlatform)
			{
				case "js":
					output = project.file("${project.buildDir}/compiled-haxe/${name}.js")
					dirToMake = output.parentFile
					break;
				case "swf":
					output = project.file("${project.buildDir}/compiled-haxe/${name}.swc")
					dirToMake = output.parentFile
					break;
				case "as3":
					output = project.file("${project.buildDir}/compiled-haxe/${name}-as3")
					dirToMake = output
					break;
				default:
					throw new IllegalStateException("No output specified")
			}
		}
		project.mkdir(dirToMake)
		return output
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

	public archive(boolean archive)
	{
		this.archive = archive
	}

	public String getBaseName()
	{
		return baseName == null ? name : baseName
	}

	public classifier(String classifier)
	{
		this.classifier = classifier
	}
}
