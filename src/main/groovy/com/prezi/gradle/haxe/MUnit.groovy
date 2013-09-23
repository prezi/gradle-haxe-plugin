package com.prezi.gradle.haxe

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.copy.FileCopyActionImpl
import org.gradle.api.internal.file.copy.FileCopySpecVisitor
import org.gradle.api.internal.file.copy.SyncCopySpecVisitor
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.reflect.Instantiator

import java.util.regex.Pattern

class MUnit extends DefaultTask implements HaxeTask {
	static final String WORKING_DIRECTORY_PREFIX = "munit-work"
	static final Pattern SUCCESSFUL_TEST_PATTERN = ~/(?m)^PLATFORMS TESTED: \d+, PASSED: \d+, FAILED: 0, ERRORS: 0, TIME:/

	@TaskAction
	void munit()
	{
		Instantiator instantiator = getServices().get(Instantiator.class)
		FileResolver fileResolver = getServices().get(FileResolver.class)

		def workDir = getWorkingDirectory()
		project.mkdir(workDir)

		// Copy all tests into one directory
		def testSourcesDirectory = new File(workDir, "tests")
		def copyTestSources = instantiator.newInstance(FileCopyActionImpl.class, instantiator, fileResolver,
				new SyncCopySpecVisitor(new FileCopySpecVisitor()));
		copyTestSources.from(getTestSourceFiles().files)
		copyTestSources.into(testSourcesDirectory)
		copyTestSources.execute()

		LinkedHashSet<File> sourcePath = []
		LinkedHashSet<File> resourcePath = []
		def extractor = new HaxelibDependencyExtractor(project, compileTask.legacyPlatformPaths, instantiator, fileResolver)

		sourcePath.add(testSourcesDirectory)
		extractor.extractDependenciesFrom(getTestConfiguration(), sourcePath, resourcePath)
		sourcePath.addAll(compileTask.getSourceFiles().files)
		extractor.extractDependenciesFrom(compileTask.getConfiguration(), sourcePath, resourcePath)

		resourcePath.addAll(getTestResourceFiles().files)
		resourcePath.addAll(compileTask.getResourceFiles().files)

		def output = getOutput()
		project.mkdir(output.parentFile)

		def haxeCmdParts = new HaxeCommandBuilder()
				.withIncludePackages(compileTask.includePackages)
				.withExcludePackages(compileTask.excludePackages)
				.withIncludePackages(includePackages)
				.withExcludePackages(excludePackages)
				.withSources(sourcePath)
				.withResources(resourcePath)
				.withMacros(compileTask.macros)
				.withFlags(testFlags)
				.withFlags(compileTask.flagList.findAll({ it != "--js-modern" }))
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
			jsRunnerTemplate << this.class.getResourceAsStream("/js_runner-html.mtt")
		}

		def munitCmd = new MUnitCommandBuilder(project)
				.build()

		def result = CommandExecutor.execute(project, munitCmd, workDir)
		if (!SUCCESSFUL_TEST_PATTERN.matcher(result).find())
		{
			logger.warn("{}", result)
			throw new RuntimeException("There are failing tests");
		}

		def copyAction = new HarCopyAction(instantiator, fileResolver, temporaryDirFactory,
				getTestSourceArchive(), getTestSourceFiles(), getTestResourceFiles())
		copyAction.execute()
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
			default:
				throw new IllegalStateException("Cannot test platform " + targetPlatform)
		}
	}

	private CompileHaxe compileTask

	public void test(CompileHaxe compileTask)
	{
		this.compileTask = compileTask
		dependsOn(compileTask)
	}

	private Configuration testConfiguration

	public Configuration getTestConfiguration()
	{
		if (testConfiguration == null)
		{
			return project.configurations[Dependency.DEFAULT_CONFIGURATION]
		}
		return testConfiguration
	}

	public void testConfiguration(Configuration configuration)
	{
		this.testConfiguration = configuration
	}

	List<Object> testSourcePaths = []

	@InputFiles
	@SkipWhenEmpty
	public FileCollection getTestSourceFiles()
	{
		return project.files(testSourcePaths)
	}

	public void testSource(path)
	{
		testSourcePaths.add(path)
	}

	List<Object> testResourcePaths = []

	@InputFiles
	public FileCollection getTestResourceFiles()
	{
		return project.files(testResourcePaths)
	}

	public testResource(path)
	{
		testResourcePaths.add(path)
	}

	LinkedHashSet<String> includePackages = []

	public includePackage(String pkg)
	{
		includePackages.add(pkg)
	}

	LinkedHashSet<String> excludePackages = []

	public excludePackage(String pkg)
	{
		excludePackages.add(pkg)
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

	private List<String> testFlags = []

	public void testFlag(String... flag)
	{
		testFlags.addAll(flag)
	}
}
