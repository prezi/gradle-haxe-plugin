package com.prezi.gradle.haxe

import org.gradle.api.tasks.OutputDirectory

class CompileHaxeWithDirectoryTarget extends AbstractCompileHaxe {
	CompileHaxeWithDirectoryTarget(String targetPlatform)
	{
		super(targetPlatform)
	}

	File outputDirectory

	@OutputDirectory
	public File getOutputDirectory()
	{
		if (outputDirectory != null)
		{
			return outputDirectory
		}
		return project.file("${project.buildDir}/compiled-haxe/${name}-${targetPlatform}")
	}

	public void setOutputDirectory(Object dir)
	{
		outputDirectory = project.file(dir)
	}

	@Override
	protected File getAndCreateOutput()
	{
		def output = getOutputDirectory()
		project.mkdir(output)
		return output
	}
}
