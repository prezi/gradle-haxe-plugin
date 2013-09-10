package com.prezi.gradle.haxe

import org.gradle.api.Project
import org.gradle.process.internal.ExecException

class CommandExecutor {
	public static String execute(Project project, String[] cmd, File dir = null)
	{
		project.logger.info("Executing in {}: {}", dir, cmd)
		def output = new StringWriter()
		def proc = cmd.execute((String[]) null, dir)
		proc.in.eachLine { line ->
			project.logger.info("{}", line)
			output.println(line)
		}
		proc.err.eachLine { line ->
			project.logger.warn("{}", line)
			output.println(line)
		}
		proc.waitFor()
		if (proc.exitValue() != 0)
		{
			throw new ExecException("Command finished with non-zero exit value (${proc.exitValue()}):\n${cmd}")
		}
		return output.toString()
	}
}
