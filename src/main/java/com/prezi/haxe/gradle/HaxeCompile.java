package com.prezi.haxe.gradle;

import org.apache.commons.io.FileUtils;
import org.gradle.api.DomainObjectSet;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.language.base.LanguageSourceSet;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class HaxeCompile extends AbstractHaxeCompileTask {

	private File outputFile;
	private File outputDirectory;

	@TaskAction
	public void compile() throws IOException, InterruptedException {
		List<String> cmd = getHaxeCommandToExecute();
		CommandExecutor.execute(cmd, getProject().getProjectDir(), new DefaultExecutionResultHandler(cmd));
	}

	public List<String> getHaxeCommandToExecute() throws IOException {
		return configureHaxeCommandBuilder(getAndRecreateOutput(), getSourceSets()).build();
	}

	protected HaxeCommandBuilder configureHaxeCommandBuilder(File output, DomainObjectSet<LanguageSourceSet> sources) {
		return new HaxeCommandBuilder(getProject(), "haxe")
				.withMain(getMainClass())
				.withTarget(getTargetPlatform(), output)
				.withSources(getSourceDirectories(sources))
				.withSourceSets(sources, getEmbeddedResources())
				.withMacros(getMacros())
				.withIncludes(getIncludes())
				.withExcludes(getExcludes())
				.withFlags(getFlagList())
				.withDebugFlags(isDebug());
	}

	protected Set<File> getSourceDirectories(DomainObjectSet<LanguageSourceSet> sources) {
		return AbstractHaxeCompileTask.getAllSourceDirectories(sources);
	}

	protected String getMainClass() {
		return getMain();
	}

	@OutputFile
	@Optional
	public File getOutputFile() {
		return outputFile;
	}

	public void setOutputFile(Object file) {
		this.outputFile = getProject().file(file);
	}

	public void outputFile(Object file) {
		setOutputFile(file);
	}

	@OutputDirectory
	@Optional
	public File getOutputDirectory() {
		return outputDirectory;
	}

	public void setOutputDirectory(Object directory) {
		this.outputDirectory = getProject().file(directory);
	}

	public void outputDirectory(Object directory) {
		setOutputDirectory(directory);
	}

	private File getAndRecreateOutput() throws IOException {
		File output;
		File dirToMake;
		if (isOutputInADirectory()) {
			output = getOutputDirectory();
			FileUtils.deleteDirectory(output);
			dirToMake = output;
		} else {
			output = getOutputFile();
			output.delete();
			dirToMake = output.getParentFile();
		}

		FileUtils.forceMkdir(dirToMake);
		return output;
	}

	private boolean isOutputInADirectory() {
		if (getOutputFile() != null) {
			return false;
		}

		if (getOutputDirectory() != null) {
			return true;
		}

		throw new RuntimeException("Neither output file or directory is set");
	}
}
