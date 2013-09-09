package com.prezi.gradle.haxe

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.UnionFileCollection
import org.gradle.api.internal.file.copy.FileCopyActionImpl
import org.gradle.api.internal.file.copy.FileCopySpecVisitor
import org.gradle.api.internal.file.copy.SyncCopySpecVisitor
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.reflect.Instantiator

class MUnit extends DefaultTask implements HaxeTask {
	static final String WORKING_DIRECTORY_PREFIX = "munit-work"

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
		copyTestSources.from(testSourceTree)
		copyTestSources.into(testSourcesDirectory)
		copyTestSources.execute()

		LinkedHashSet<File> sourcePath = []
		LinkedHashSet<File> resourcePath = []
		def extractor = new HaxelibDependencyExtractor(project, compileTask.legacyPlatformPaths, instantiator, fileResolver)

		sourcePath.add(testSourcesDirectory)
		extractor.extractDependenciesFrom(getTestConfiguration(), sourcePath, resourcePath)
		sourcePath.addAll(compileTask.sourceTree.files)
		extractor.extractDependenciesFrom(compileTask.getConfiguration(), sourcePath, resourcePath)

		resourcePath.addAll(testResourceTree.files)
		resourcePath.addAll(compileTask.resourceTree.files)

		def output = getOutput()
		project.mkdir(output.parentFile)

		def haxeCmd = new HaxeCommandBuilder("", "", "\n", false)
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
		// munitConfig << "templates=src/munit/templates\n"

		def munitCmd = new MUnitCommandBuilder()
				.withDebug(debug)
				.build()

		CommandExecutor.execute(project, munitCmd, workDir)

		def copyAction = new HarCopyAction(instantiator, fileResolver, temporaryDirFactory,
				getTestSourceArchive(), testSourceTree, testResourceTree)
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
				return new File(getWorkingDirectory(), "swf_test.swc")
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

	@InputFiles
	@SkipWhenEmpty
	private FileCollection testSourceTree = new UnionFileCollection()

	public void testSource(paths)
	{
		testSourceTree.add(project.files(paths))
	}

	@InputFiles
	private FileCollection testResourceTree = new UnionFileCollection()

	public testResource(paths)
	{
		testResourceTree.add(project.files(paths))
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

	private String testFlags = ''

	public void testFlag(String flag)
	{
		testFlags += " $flag"
	}

	boolean debug
}
