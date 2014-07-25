package com.prezi.haxe.gradle;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class CommandExecutor {
	private static final Logger logger = LoggerFactory.getLogger(CommandExecutor.class);

	public static void execute(List<String> cmd, File dir, ExecutionResultHandler handler) throws IOException, InterruptedException {
		logger.info("Executing in {}: {}", dir, Joiner.on(" ").join(cmd));
		ProcessBuilder builder = new ProcessBuilder(cmd);
		builder.redirectErrorStream(true);
		builder.directory(dir);
		Process process = builder.start();
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ByteStreams.copy(process.getInputStream(), bytes);
		process.waitFor();

		String output = bytes.toString(Charsets.UTF_8.name());

		handler.handleResult(process.exitValue(), output);
	}
}
