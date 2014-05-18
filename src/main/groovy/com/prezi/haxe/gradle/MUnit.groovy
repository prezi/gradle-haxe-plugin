package com.prezi.haxe.gradle

import org.gradle.api.tasks.TaskAction

import java.util.regex.Pattern

class MUnit extends AbstractHaxeCompileTask {
	static final Pattern SUCCESSFUL_TEST_PATTERN = ~/(?m)^PLATFORMS TESTED: \d+, PASSED: \d+, FAILED: 0, ERRORS: 0, TIME:/

	@TaskAction
	void munit()
	{
		def workDir = getWorkingDirectory()
		workDir.delete() || workDir.deleteDir()
		workDir.mkdirs()

		def haxeCmdParts = getHaxeCommandLine()
		def haxeCmd = "";
		haxeCmdParts.each {
			if (it.startsWith("-")) {
				haxeCmd += "\n"
			} else {
				haxeCmd += " "
			}
			haxeCmd += it
		}

		def testHxml = new File(workDir, "test.hxml")
		testHxml.delete()
		testHxml << haxeCmd

		def munitConfig = new File(workDir, ".munit")
		munitConfig.delete()
		munitConfig << "version=2.0.0\n"
		munitConfig << "src=${testSourcesDirectory}\n"
		munitConfig << "bin=${workDir}\n"
		munitConfig << "report=${workDir}/report\n"
		munitConfig << "hxml=${testHxml}\n"

		// Issue #1 -- Use UTF-8 compatible JS runner template
		if (getTargetPlatform().name == "js")
		{
			munitConfig << "templates=${workDir}/templates\n"
			def templatesDir = new File(workDir, "templates")
			project.mkdir(templatesDir)
			def jsRunnerTemplate = new File(templatesDir, "js_runner-html.mtt")
			jsRunnerTemplate.delete()
			jsRunnerTemplate << getMUnitJsHtmlTemplate()
		}

		def munitCmd = getMUnitCommandLine()

		CommandExecutor.execute(project, munitCmd, workDir) { ExecutionResult result ->
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

	protected InputStream getMUnitJsHtmlTemplate() {
		return this.class.getResourceAsStream("/js_runner-html.mtt")
	}

	public List<String> getHaxeCommandLine() {
		def sources = getSourceSets()
		// Copy all sources into one directory
		project.copy {
			from sources*.source*.srcDirs
			into testSourcesDirectory
		}

		def output = getOutput()
		output.parentFile.mkdirs()

		return configureHaxeCommandLine(output).build()
	}

	public List<String> getMUnitCommandLine() {
		return new MUnitCommandBuilder(project).build()
	}

	protected HaxeCommandBuilder configureHaxeCommandLine(File output) {
		return new HaxeCommandBuilder(project)
				.withSources([testSourcesDirectory])
				.withSourceSets(getSourceSets(), getEmbeddedResources())
				.withIncludes(getIncludes())
				.withExcludes(getExcludes())
				.withMacros(getMacros())
				.withFlags(getFlagList())
				.withDebugFlags(getDebug())
				.withTarget(getTargetPlatform().name, output)
				.withMain("TestMain")
	}

	protected File getTestSourcesDirectory() {
		def testSourcesDirectory = new File(getWorkingDirectory(), "tests")
		testSourcesDirectory.mkdirs()
		return testSourcesDirectory
	}

	private File getOutput()
	{
		switch (getTargetPlatform().name)
		{
			case "js":
				return new File(getWorkingDirectory(), "js_test.js")
			case "swf":
				return new File(getWorkingDirectory(), "swf_test.swf")
			case "neko":
				return new File(getWorkingDirectory(), "neko_test.n")
			case "as3":
			case "java":
			default:
				throw new IllegalStateException("Cannot test platform " + getTargetPlatform())
		}
	}

	File workingDirectory

	public workingDirectory(Object workingDirectory)
	{
		this.workingDirectory = project.file(workingDirectory)
	}
}
