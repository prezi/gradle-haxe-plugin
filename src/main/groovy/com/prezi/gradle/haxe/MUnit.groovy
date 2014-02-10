package com.prezi.gradle.haxe

import com.prezi.spaghetti.gradle.ModuleExtractor
import org.gradle.api.DomainObjectSet
import org.gradle.api.internal.DefaultDomainObjectSet
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.language.base.LanguageSourceSet

import java.util.regex.Pattern

class MUnit extends AbstractHaxeCompileTask {
	final DomainObjectSet<LanguageSourceSet> testSources = new DefaultDomainObjectSet<>(LanguageSourceSet)
	LinkedHashMap<String, File> embeddedTestResources = [:]

	static final Pattern SUCCESSFUL_TEST_PATTERN = ~/(?m)^PLATFORMS TESTED: \d+, PASSED: \d+, FAILED: 0, ERRORS: 0, TIME:/

	@TaskAction
	void munit()
	{
		def workDir = getWorkingDirectory()
		workDir.mkdirs()

		println ">>>>>>>>>>> Params: ${params}"

		// Copy all tests into one directory
		def testSourcesDirectory = new File(workDir, "tests")
		testSourcesDirectory.mkdirs()
		project.copy {
			from testSources*.source*.srcDirs
			into testSourcesDirectory
		}

		// Extract Require JS
		File requireJsFile = null
		if (getTargetPlatform().name == "js") {
			def requireJsProps = new Properties()
			requireJsProps.load(this.class.getResourceAsStream("/META-INF/maven/org.webjars/requirejs/pom.properties"))
			def requireJsVersion = requireJsProps.getProperty("version")
			requireJsFile = new File(workDir, "require.js")
			requireJsFile.delete()
			requireJsFile << this.class.getResourceAsStream("/META-INF/resources/webjars/requirejs/${requireJsVersion}/require.js")
		}

		def output = getOutput()
		project.mkdir(output.parentFile)

		def builder = new HaxeCommandBuilder(project)
				.withSources([testSourcesDirectory])
				.withSources(getAllSourceDirectories(sources))
				.withSourceSets(sources)
				.withEmbeddedResources(getEmbeddedResources())
				.withSourceSets(testSources)
				.withEmbeddedResources(getEmbeddedTestResources())
				.withIncludes(getIncludes())
				.withExcludes(getExcludes())
				.withMacros(getMacros())
				.withFlags(getFlagList())
				.withDebugFlags(getDebug())
				.withTarget(getTargetPlatform().name, output)
				.withMain("TestMain")
		def haxeCmdParts = builder.build()

		def haxeCmd = "";
		haxeCmdParts.each {
			if (it.startsWith("-")) {
				haxeCmd += "\n"
			} else {
				haxeCmd += " "
			}
			haxeCmd += it
		}

		if (getTargetPlatform().name == "js") {
			def bundleFile = project.getPlugins().getPlugin(HaxePlugin).getSpaghettiBundleTool(project)
			haxeCmd += "\n-cmd haxe -cp ${bundleFile.parentFile} --run SpaghettiBundler module ${output}"
			[ sources, testSources ].each {
				it.withType(HaxeSourceSet).each { haxeSourceSet ->
					ModuleExtractor.extractModules(haxeSourceSet.compileClassPath, workDir).each { bundle ->
						haxeCmd += " ${bundle.name.fullyQualifiedName}"
					}
				}
			}
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
			jsRunnerTemplate << this.class.getResourceAsStream("/js_runner-html.mtt")
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

	public testSource(Object... sources) {
		sources.each { source ->
			this.testSources.addAll(notationParser.parseNotation(source))
		}
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
		return super.getInputFiles() + getAllSourceDirectories(testSources) + getEmbeddedTestResources()
	}

	File workingDirectory

	public workingDirectory(Object workingDirectory)
	{
		this.workingDirectory = project.file(workingDirectory)
	}
}
