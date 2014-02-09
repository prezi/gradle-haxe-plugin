package com.prezi.gradle.haxe

import com.prezi.gradle.DeprecationLogger
import com.prezi.spaghetti.gradle.ModuleExtractor
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction

import java.util.regex.Pattern

class MUnit extends DefaultTask implements HaxeTask {
	@Delegate(deprecated = true)
	final HaxeCompileParameters params

	static final String WORKING_DIRECTORY_PREFIX = "munit-work"
	static final Pattern SUCCESSFUL_TEST_PATTERN = ~/(?m)^PLATFORMS TESTED: \d+, PASSED: \d+, FAILED: 0, ERRORS: 0, TIME:/

	public MUnit()
	{
		this.params = new HaxeCompileParameters(project)
	}

	@TaskAction
	void munit()
	{
		def workDir = getWorkingDirectory()
		project.mkdir(workDir)

		// Copy all tests into one directory
		def testSourcesDirectory = new File(workDir, "tests")
		project.copy {
			from getSourceFiles().files
			into testSourcesDirectory
		}

		// Extract Require JS
		File requireJsFile = null
		if (compileTask.targetPlatform == "js") {
			def requireJsProps = new Properties()
			requireJsProps.load(this.class.getResourceAsStream("/META-INF/maven/org.webjars/requirejs/pom.properties"))
			def requireJsVersion = requireJsProps.getProperty("version")
			requireJsFile = new File(workDir, "require.js")
			requireJsFile.delete()
			requireJsFile << this.class.getResourceAsStream("/META-INF/resources/webjars/requirejs/${requireJsVersion}/require.js")
		}

		LinkedHashSet<File> sourcePath = []
		LinkedHashSet<File> resourcePath = []
		def extractor = new HaxelibDependencyExtractor(project, [ compileTask.legacyPlatformPaths, legacyPlatformPaths ].flatten())

		sourcePath.add(testSourcesDirectory)
		LinkedHashMap<String, File> testEmbeddedResources = [:]
		extractor.extractDependenciesFrom(getConfiguration(), sourcePath, resourcePath, testEmbeddedResources)
		testEmbeddedResources.putAll(embeddedResources)

		sourcePath.addAll(compileTask.getSourceFiles().files)
		LinkedHashMap<String, File> compilerEmbeddedResources = [:]
		extractor.extractDependenciesFrom(compileTask.getConfiguration(), sourcePath, resourcePath, compilerEmbeddedResources)
		compilerEmbeddedResources.putAll(compileTask.embeddedResources)

		LinkedHashMap<String, File> allEmbeddedResources = [:]
		allEmbeddedResources.putAll(compilerEmbeddedResources)
		allEmbeddedResources.putAll(testEmbeddedResources)

		resourcePath.addAll(compileTask.getResourceFiles().files)
		resourcePath.addAll(getResourceFiles().files)

		def output = getOutput()
		project.mkdir(output.parentFile)

		def haxeCmdParts = new HaxeCommandBuilder(project)
				.withIncludes(compileTask.includes)
				.withExcludes(compileTask.excludes)
				.withIncludes(includes)
				.withExcludes(excludes)
				.withSources(sourcePath)
				.withSources(resourcePath)
				.withEmbeddedResources(allEmbeddedResources)
				.withMacros(compileTask.macros)
				.withFlags(flagList)
				.withFlags(compileTask.flagList.findAll({ it != "--js-modern" && it != "--no-traces" }))
				.withDebugFlags(compileTask.debug)
				.withTarget(compileTask.targetPlatform, output)
				.withMain("TestMain")
				.build()

		def haxeCmd = "";
		haxeCmdParts.each {
			if (it.startsWith("-")) {
				haxeCmd += "\n"
			} else {
				haxeCmd += " "
			}
			haxeCmd += it
		}

		if (compileTask.targetPlatform == "js") {
			def bundleFile = project.getPlugins().getPlugin(HaxePlugin).getSpaghettiBundleTool(project)
			haxeCmd += "\n-cmd haxe -cp ${bundleFile.parentFile} --run SpaghettiBundler module ${output}"
			ModuleExtractor.extractModules(configuration, workDir).each { bundle ->
				haxeCmd += " ${bundle.name.fullyQualifiedName}"
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
		if (compileTask.targetPlatform == "js")
		{
			munitConfig << "templates=${workDir}/templates\n"
			def templatesDir = new File(workDir, "templates")
			project.mkdir(templatesDir)
			def jsRunnerTemplate = new File(templatesDir, "js_runner-html.mtt")
			jsRunnerTemplate.delete()
			jsRunnerTemplate << this.class.getResourceAsStream("/js_runner-html.mtt")
		}

		def munitCmd = new MUnitCommandBuilder(project)
				.build()

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

		HarUtils.createArchive(project, temporaryDirFactory, project.buildDir,
				getFullName(), getSourceFiles(), getResourceFiles(), embeddedResources)
	}

	private PublishArtifact testSourceBundle

	public PublishArtifact getTests()
	{
		if (testSourceBundle == null)
		{
			testSourceBundle = new HarPublishArtifact(this, getTestSourceArchive())
		}
		return testSourceBundle
	}

	public File getTestSourceArchive()
	{
		return new File(project.buildDir, getFullName() + ".har")
	}

	public String getBaseName()
	{
		return compileTask.baseName
	}

	private String getFullName()
	{
		return compileTask.getBaseName() + "-" + getClassifier()
	}

	private File getOutput()
	{
		switch (compileTask.targetPlatform)
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
				throw new IllegalStateException("Cannot test platform " + targetPlatform)
		}
	}

	private HaxeCompile compileTask

	public void test(HaxeCompile compileTask)
	{
		this.compileTask = compileTask
		dependsOn(compileTask)
	}

	public Configuration getConfiguration()
	{
		return params.hasConfiguration() ? params.configuration : compileTask.configuration
	}

	@Deprecated
	public void testConfiguration(Configuration conf)
	{
		DeprecationLogger.nagUserOfReplacedProperty("testConfiguration", "configuration")
		configuration(conf)
	}

	@InputFiles
	@SkipWhenEmpty
	public FileCollection getSourceFiles()
	{
		return project.files(sourcePaths)
	}

	@Deprecated
	public void testSource(path)
	{
		DeprecationLogger.nagUserOfReplacedProperty("testSource", "source")
		source(path)
	}

	@InputFiles
	@SkipWhenEmpty
	public FileCollection getResourceFiles()
	{
		return project.files(resourcePaths)
	}

	@Deprecated
	public testResource(path)
	{
		DeprecationLogger.nagUserOfReplacedProperty("testReource", "resource")
		resource(path)
	}

	@InputFiles
	@SkipWhenEmpty
	public FileCollection getEmbeddedResourceFiles()
	{
		return project.files(embeddedResources.values())
	}

	private String classifier

	public String getClassifier()
	{
		if (classifier)
		{
			return classifier
		}

		def result = compileTask.classifier
		return result ? result + "-tests" : "tests"
	}

	public classifier(String classifier)
	{
		this.classifier = classifier
	}

	private File workingDirectory

	public File getWorkingDirectory()
	{
		if (workingDirectory == null)
		{
			return project.file("${project.buildDir}/" + WORKING_DIRECTORY_PREFIX + "/" + compileTask.name)
		}
		return workingDirectory
	}

	public workingDirectory(Object workingDirectory)
	{
		this.workingDirectory = project.file(workingDirectory)
	}

	@Deprecated
	public void testFlag(String... flags)
	{
		DeprecationLogger.nagUserOfReplacedProperty("testFlag", "flag")
		flag(flags)
	}
}
