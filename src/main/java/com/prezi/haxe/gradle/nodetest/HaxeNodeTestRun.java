package com.prezi.haxe.gradle.nodetest;

import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.prezi.haxe.gradle.HaxeCompile;
import com.prezi.haxe.gradle.incubating.LanguageSourceSet;
import org.gradle.api.Action;
import org.gradle.api.DomainObjectSet;
import org.gradle.api.file.CopySpec;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.internal.file.FileOperations;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

public class HaxeNodeTestRun extends ConventionTask {

	private File workingDirectory;


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

	@TaskAction
    public void run() throws IOException, InterruptedException {
    }

}
