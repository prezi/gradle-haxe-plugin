package com.prezi.haxe.gradle

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction

import java.util.regex.Pattern

class MUnit extends ConventionTask {
	static final Pattern SUCCESSFUL_TEST_PATTERN = ~/(?m)^PLATFORMS TESTED: \d+, PASSED: \d+, FAILED: 0, ERRORS: 0, TIME:/

	@Input
	TargetPlatform targetPlatform
	void targetPlatform(String targetPlatform) {
		this.targetPlatform = project.extensions.getByType(HaxeExtension).targetPlatforms.maybeCreate(targetPlatform)
	}

	@Input
	File inputFile
	void inputFile(Object file) {
		this.inputFile = project.file(file)
	}

	@TaskAction
	void munit()
	{
		def workDir = getWorkingDirectory()
		workDir.delete() || workDir.deleteDir()
		workDir.mkdirs()

		prepareEnvironment(workDir)

		def cmd = getMUnitCommandLine()
		CommandExecutor.execute(project, cmd, getWorkingDirectory()) { ExecutionResult result ->
			def errorExit = result.exitValue != 0
			def testsFailing = !SUCCESSFUL_TEST_PATTERN.matcher(result.output).find()
			if (errorExit || testsFailing)
			{
				logger.warn("{}", result.output)
			}
			if (errorExit)
			{
				throw new RuntimeException("Error while running tests")
			}
			else if (testsFailing)
			{
				throw new RuntimeException("There are failing tests");
			}
		}
	}

	protected List<String> getMUnitCommandLine() {
		return new MUnitCommandBuilder(project).build()
	}

	protected void prepareEnvironment(File workDir) {
		String testBinaryName = copyCompiledTest(workDir)

		def testHxml = new File(workDir, "test.hxml")
		testHxml << "-${getTargetPlatform().name} ${testBinaryName}\n"

		def munitConfig = new File(workDir, ".munit")
		munitConfig << "bin=.\n"
		munitConfig << "report=report\n"
		munitConfig << "hxml=test.hxml\n"
		munitConfig << "resources=.\n"

		// Issue #1 -- Use UTF-8 compatible JS runner template
		if (getTargetPlatform().name == "js") {
			munitConfig << "templates=templates\n"
			def templatesDir = new File(workDir, "templates")
			project.mkdir(templatesDir)
			def jsRunnerTemplate = new File(templatesDir, "js_runner-html.mtt")
			jsRunnerTemplate << getMUnitJsHtmlTemplate()
		}
	}

	protected String copyCompiledTest(File workDir) {
		def testBinaryName = "${getTargetPlatform().name}_test.js"
		def testFile = new File(workDir, testBinaryName)
		logger.debug "Copying test file from {} to {}", getInputFile(), testFile
		testFile << getInputFile().text
		return testBinaryName
	}

	@InputDirectory
	File workingDirectory
	public workingDirectory(Object workingDirectory)
	{
		this.workingDirectory = project.file(workingDirectory)
	}

	protected InputStream getMUnitJsHtmlTemplate() {
		return this.class.getResourceAsStream("/js_runner-html.mtt")
	}
}
