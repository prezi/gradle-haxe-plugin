package com.prezi.gradle.haxe

import org.gradle.api.tasks.OutputFile

abstract class CompileHaxeWithFileTarget extends AbstractCompileHaxe {
	final String defaultExtension

	CompileHaxeWithFileTarget(String targetPlatform, String defaultExtension)
	{
		super(targetPlatform)
		this.defaultExtension = defaultExtension
	}

	File outputFile

	@OutputFile
	public File getOutputFile()
	{
		if (outputFile != null)
		{
			return outputFile
		}
		return project.file("${project.buildDir}/compiled-haxe/${name}.${defaultExtension}")
	}

	public void setOutputFile(Object file)
	{
		outputFile = project.file(file)
	}

	@Override
	protected File getAndCreateOutput()
	{
		def output = getOutputFile()
		project.mkdir(output.parentFile)
		return output
	}
}
