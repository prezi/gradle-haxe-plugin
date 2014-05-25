package com.prezi.haxe.gradle

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
		def cmd = getHaxeCommandToExecute()

		CommandExecutor.execute(project, cmd, null) { ExecutionResult result ->
			if (result.exitValue != 0)
			{
				throw new RuntimeException("Command finished with non-zero exit value (${result.exitValue}):\n${cmd.join(" ")}")
			}
		}
	}

	public List<String> getHaxeCommandToExecute() {
		return configureHaxeCommandBuilder(getAndRecreateOutput(), getSourceSets()).build()
	}

	protected HaxeCommandBuilder configureHaxeCommandBuilder(File output, DomainObjectSet<LanguageSourceSet> sources) {
		return new HaxeCommandBuilder(project, "haxe")
				.withMain(getMainClass())
				.withTarget(getTargetPlatform().name, output)
				.withSources(getSourceDirectories(sources))
				.withSourceSets(sources, getEmbeddedResources())
				.withMacros(getMacros())
				.withIncludes(getIncludes())
				.withExcludes(getExcludes())
				.withFlags(getFlagList())
				.withDebugFlags(getDebug())
	}

	protected Set<File> getSourceDirectories(DomainObjectSet<LanguageSourceSet> sources) {
		return getAllSourceDirectories(sources)
	}

	protected String getMainClass() {
		return getMain()
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
		dirToMake.mkdirs()
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
