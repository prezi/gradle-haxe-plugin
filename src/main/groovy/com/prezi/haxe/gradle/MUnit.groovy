package com.prezi.haxe.gradle

import org.gradle.api.DomainObjectSet
import org.gradle.api.internal.DefaultDomainObjectSet
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.language.base.LanguageSourceSet

import java.util.regex.Pattern

class MUnit extends AbstractHaxeCompileTask {
	final LinkedHashSet<Object> testSources = []
	LinkedHashMap<String, File> embeddedTestResources = [:]

	static final Pattern SUCCESSFUL_TEST_PATTERN = ~/(?m)^PLATFORMS TESTED: \d+, PASSED: \d+, FAILED: 0, ERRORS: 0, TIME:/

	@TaskAction
	void munit()
	{
		def workDir = getWorkingDirectory()
		workDir.delete() || workDir.deleteDir()
		workDir.mkdirs()

		def sources = getSourceSets()
		def testSources = getTestSourceSets()
		Map<String, File> allResources = getEmbeddedResources() + getEmbeddedTestResources()

		// Copy all tests into one directory
		project.copy {
			from testSources*.source*.srcDirs
			into testSourcesDirectory
		}

		def output = getOutput()
		project.mkdir(output.parentFile)

		def haxeCmdParts = configureHaxeCommandLine(output, workDir, sources, testSources, allResources).build()
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

		def munitCmd = new MUnitCommandBuilder(project).build()

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

	protected HaxeCommandBuilder configureHaxeCommandLine(File output, File workDir, DomainObjectSet<LanguageSourceSet> sources, Set<LanguageSourceSet> testSources, Map<String, File> allResources) {
		Set<LanguageSourceSet> allSources = sources + testSources

		return new HaxeCommandBuilder(project)
				.withSources([testSourcesDirectory])
				.withSources(getAllSourceDirectories(sources))
				.withSourceSets(allSources, allResources)
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

	public testSource(Object... sources) {
		testSources.addAll(sources)
	}

	protected DomainObjectSet<LanguageSourceSet> getTestSourceSets() {
		def testSourceSets = getTestSources().collectMany { notationParser.parseNotation(it) }
		return new DefaultDomainObjectSet<LanguageSourceSet>(LanguageSourceSet, testSourceSets)
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

	@Override
	@InputFiles
	Set<File> getInputFiles() {
		return super.getInputFiles() + getAllSourceDirectories(getTestSourceSets()) + getEmbeddedTestResources().values()
	}

	File workingDirectory

	public workingDirectory(Object workingDirectory)
	{
		this.workingDirectory = project.file(workingDirectory)
	}
}
