package com.prezi.gradle.haxe

import org.gradle.api.Project

class CommandExecutor {
	public static void execute(Project project, String[] cmd, File dir, Closure c)
	{
		project.logger.info("Executing in {}: {}", dir, cmd.join(" "))
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
		def result = new ExecutionResult(output.toString(), proc.exitValue())
		c(result)
	}
}

class ExecutionResult {
	final String output;
	final int exitValue;
	public ExecutionResult(String output, int exitValue)
	{
		this.output = output
		this.exitValue = exitValue
	}
}