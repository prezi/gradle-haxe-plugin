package com.prezi.haxe.gradle.nodetest;

import org.gradle.api.Action;
import org.gradle.api.file.CopySpec;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.internal.file.FileOperations;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;

public class HaxeNodeTestCompile extends ConventionTask {

	private File workingDirectory;
	private File inputDirectory;

	@OutputDirectory
	public File getWorkingDirectory() {
		return workingDirectory;
	}

	public void setWorkingDirectory(Object workingDirectory) {
		this.workingDirectory = getProject().file(workingDirectory);
	}

	public void workingDirectory(Object workingDirectory) {
		setWorkingDirectory(workingDirectory);
	}

	@InputDirectory
	public File getInputDirectory() {
		return inputDirectory;
	}

	public void setInputDirectory(Object inputDirectory) {
		this.inputDirectory = getProject().file(inputDirectory);
	}

	public void inputDirectory(Object inputDirectory) {
		setInputDirectory(inputDirectory);
	}

	@TaskAction
	public void generate() {
		getServices().get(FileOperations.class).sync(new Action<CopySpec>() {
			@Override
			public void execute(CopySpec copySpec) {
				copySpec.from(getInputDirectory());
				copySpec.into(getWorkingDirectory());
			}
		});
	}

}
