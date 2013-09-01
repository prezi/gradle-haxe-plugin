package com.prezi.gradle

import org.gradle.api.Project
import org.gradle.process.internal.ExecException

class CommandExecutor {
	public static void execute(Project project, String cmd, File dir = null)
	{
		def res = project.exec {
			executable = 'bash'
			args "-c", cmd
			setIgnoreExitValue true
			if (dir != null)
			{
				workingDir dir
			}
		}
		if (res.exitValue != 0)
		{
			throw new ExecException("Command finished with non-zero exit value (${res.exitValue}):\n${cmd}")
		}
	}
}
