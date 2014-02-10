package com.prezi.gradle.haxe

import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class HaxeCompile extends AbstractHaxeCompileTask {

	String main

	@TaskAction
	void compile()
	{
		def output = getAndCreateOutput()
		def builder = new HaxeCommandBuilder(project, "haxe")
				.withMain(getMain())
				.withTarget(getTargetPlatform().name, output)
				.withSources(getAllSourceDirectories(sources))
				.withSourceSets(sources)
				.withEmbeddedResources(getEmbeddedResources())
				.withMacros(getMacros())
				.withIncludes(getIncludes())
				.withExcludes(getExcludes())
				.withFlags(getFlagList())
				.withDebugFlags(getDebug())
				.withSpaghetti(getSpaghetti(), output, sources.withType(HaxeSourceSet)*.compileClassPath)
		String[] cmd = builder.build()

		CommandExecutor.execute(project, cmd, null) { ExecutionResult result ->
			if (result.exitValue != 0)
			{
				throw new RuntimeException("Command finished with non-zero exit value (${result.exitValue}):\n${cmd.join(" ")}")
			}
		}
	}

	@OutputFile
	@Optional
	File outputFile

	void outputFile(Object file) {
		this.outputFile = project.file(file)
	}

	@OutputDirectory
	@Optional
	File outputDirectory

	void outputDirectory(Object file) {
		this.outputDirectory = project.file(file)
	}

	private File getAndCreateOutput()
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

	private boolean isOutputInADirectory()
	{
		if (getOutputFile() != null)
		{
			return false;
		}
		if (getOutputDirectory() != null)
		{
			return true;
		}
		throw new RuntimeException("Neither output file or directory is set")
	}
}
