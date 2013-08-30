package com.prezi.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.process.internal.ExecException

class CompileHaxe extends DefaultTask {

	HaxeBuild build

	@TaskAction
	void compile()
	{
		String cmd = new CmdBuilder()
				.withMain(build.main)
				.withTarget(build.targetPlatform, build.createAndGetOutput())
				.withMacros(build.macros)
				.withIncludePackages(build.includePackages)
				.withExcludePackages(build.excludePackages)
				.withResources(build.resources.files)
				// .withHaxelibs(project, collectTargetDirs())
				.withSources(build.sources.files)
				.withFlags(build.flags)
				.withDebugFlags(build.debug)
				.build()

		def res = project.exec {
			executable = 'bash'
			args "-c", cmd
			setIgnoreExitValue true
		}
		if (res.exitValue != 0)
		{
			throw new ExecException("Command finished with non-zero exit value (${res.exitValue}):\n${cmd}")
		}
	}
}
