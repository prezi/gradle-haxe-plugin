package com.prezi.haxe.gradle;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import org.apache.commons.io.FileUtils;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

public class MUnit extends ConventionTask {

	private static final Pattern SUCCESSFUL_TEST_PATTERN = Pattern.compile("(?m)^PLATFORMS TESTED: \\d+, PASSED: \\d+, FAILED: 0, ERRORS: 0, TIME:");
	private String targetPlatform;
	private File inputFile;
	private File workingDirectory;

	@Input
	public String getTargetPlatform() {
		return targetPlatform;
	}

	public void setTargetPlatform(String targetPlatform) {
		this.targetPlatform = targetPlatform;
	}

	public void targetPlatform(String targetPlatform) {
		this.targetPlatform = targetPlatform;
	}

	@InputFile
	public File getInputFile() {
		return inputFile;
	}

	public void setInputFile(Object file) {
		this.inputFile = getProject().file(file);
	}

	public void inputFile(Object file) {
		setInputFile(file);
	}

	@TaskAction
	public void munit() throws IOException, InterruptedException {
		File workDir = getWorkingDirectory();
		FileUtils.deleteDirectory(workDir);
		FileUtils.forceMkdir(workDir);

		prepareEnvironment(workDir);
		if (getProject().hasProperty("munit.skiprunner")) {
			return;
		}
		final List<String> cmd = getMUnitCommandLine();
		CommandExecutor.execute(cmd, getWorkingDirectory(), new DefaultExecutionResultHandler(cmd) {
			@Override
			public void handleResult(int exitValue, String output) {
				super.handleResult(exitValue, output);
				if (!isTestSuccessful(output)) {
					throw new RuntimeException("There are failing tests");
				}
			}

			@Override
			protected boolean shouldPrintResult(int exitValue, String output) {
				return super.shouldPrintResult(exitValue, output) || !isTestSuccessful(output);
			}

			private boolean isTestSuccessful(String output) {
				return SUCCESSFUL_TEST_PATTERN.matcher(output).find();
			}
		});
	}

	protected List<String> getMUnitCommandLine() {
		return new MUnitCommandBuilder(getProject()).build();
	}

	protected void prepareEnvironment(File workDir) throws IOException {
		final String testBinaryName = copyCompiledTest(workDir);

		File testHxml = new File(workDir, "test.hxml");
		Files.write("-" + getTargetPlatform() + " " + testBinaryName + "\n", testHxml, Charsets.UTF_8);

		File munitConfig = new File(workDir, ".munit");
		Files.write("bin=.\n"
				+ "report=report\n"
				+ "hxml=test.hxml\n"
				+ "resources=.\n",
				munitConfig, Charsets.UTF_8);

		// Issue #1 -- Use UTF-8 compatible JS runner template
		if (getTargetPlatform().equals("js")) {
			Files.append("templates=templates\n", munitConfig, Charsets.UTF_8);
			File templatesDir = new File(workDir, "templates");
			FileUtils.forceMkdir(templatesDir);
			File jsRunnerTemplate = new File(templatesDir, "js_runner-html.mtt");
			Files.copy(Resources.newInputStreamSupplier(getMUnitJsHtmlTemplate()), jsRunnerTemplate);
		}

	}

	protected String copyCompiledTest(File workDir) throws IOException {
		String testBinaryName = getTargetPlatform() + "_test.js";
		File testFile = new File(workDir, testBinaryName);
		getLogger().debug("Copying test file from {} to {}", getInputFile(), testFile);
		Files.copy(getInputFile(), testFile);
		return ((String) (testBinaryName));
	}

	public File getWorkingDirectory() {
		return workingDirectory;
	}

	public void setWorkingDirectory(Object workingDirectory) {
		this.workingDirectory = getProject().file(workingDirectory);
	}

	public void workingDirectory(Object workingDirectory) {
		setWorkingDirectory(workingDirectory);
	}

	protected URL getMUnitJsHtmlTemplate() {
		return this.getClass().getResource("/js_runner-html.mtt");
	}

	public boolean shouldRunAutomatically() {
		return !getProject().hasProperty("munit.usenode") || getProject().property("munit.usenode").equals("false");
	}
}
