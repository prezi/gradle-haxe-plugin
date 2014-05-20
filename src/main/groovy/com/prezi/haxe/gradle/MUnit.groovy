package com.prezi.haxe.gradle

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction

import java.util.regex.Pattern

class MUnit extends ConventionTask {
	static final Pattern SUCCESSFUL_TEST_PATTERN = ~/(?m)^PLATFORMS TESTED: \d+, PASSED: \d+, FAILED: 0, ERRORS: 0, TIME:/

	@TaskAction
	void munit()
	{
		def cmd = ["haxelib", "run", "munit", "run"]
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

	@InputDirectory
	File workingDirectory
	public workingDirectory(Object workingDirectory)
	{
		this.workingDirectory = project.file(workingDirectory)
	}
}
