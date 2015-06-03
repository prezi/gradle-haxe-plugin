package com.prezi.haxe.gradle.nodetest;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.prezi.haxe.gradle.HaxeCommandBuilder;
import com.prezi.haxe.gradle.HaxeCompile;
import com.prezi.haxe.gradle.HaxeTestCompile;
import com.prezi.haxe.gradle.incubating.LanguageSourceSet;
import org.apache.commons.io.FileUtils;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public class HaxeNodeTestCompile extends HaxeTestCompile {

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

    @Override
    public void compile() throws IOException, InterruptedException {
		getServices().get(FileOperations.class).sync(new Action<CopySpec>() {
            @Override
            public void execute(CopySpec copySpec) {
                copySpec.from(getInputDirectory());
                copySpec.into(getWorkingDirectory());
            }
        });

        File jsRunnerTemplate = new File(getTestsDirectory(), "TestMain.hx");
        Files.copy(Resources.newInputStreamSupplier(this.getClass().getResource("/TestMain.hx")), jsRunnerTemplate);

        super.compile();
    }

    @Override
    protected String getMainClass() {
        return "TestMain";
    }

    @Override
    protected Set<File> getSourceDirectories(DomainObjectSet<LanguageSourceSet> sources) {
        return Collections.singleton(getTestsDirectory());
    }

    public File getTestsDirectory() {
        return new File(getWorkingDirectory(), "tests");
    }


}
