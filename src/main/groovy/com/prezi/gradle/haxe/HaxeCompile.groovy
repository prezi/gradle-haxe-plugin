package com.prezi.gradle.haxe

import org.gradle.api.DomainObjectSet
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.language.base.LanguageSourceSet

class HaxeCompile extends AbstractHaxeCompileTask {

	@TaskAction
	void compile()
	{
		def output = getAndRecreateOutput()
		def sources = getSourceSets()
		String[] cmd = configureHaxeCommandBuilder(output, sources).build()

		CommandExecutor.execute(project, cmd, null) { ExecutionResult result ->
			if (result.exitValue != 0)
			{
				throw new RuntimeException("Command finished with non-zero exit value (${result.exitValue}):\n${cmd.join(" ")}")
			}
		}
	}

	protected HaxeCommandBuilder configureHaxeCommandBuilder(File output, DomainObjectSet<LanguageSourceSet> sources) {
		return new HaxeCommandBuilder(project, "haxe")
				.withMain(getMain())
				.withTarget(getTargetPlatform().name, output)
				.withSources(getAllSourceDirectories(sources))
				.withSourceSets(sources, getEmbeddedResources())
				.withMacros(getMacros())
				.withIncludes(getIncludes())
				.withExcludes(getExcludes())
				.withFlags(getFlagList())
				.withDebugFlags(getDebug())
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

	private File getAndRecreateOutput()
	{
		File output
		File dirToMake
		if (isOutputInADirectory())
		{
			output = getOutputDirectory()
			output.delete() || output.deleteDir()
			dirToMake = output
		}
		else
		{
			output = getOutputFile()
			output.delete()
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
