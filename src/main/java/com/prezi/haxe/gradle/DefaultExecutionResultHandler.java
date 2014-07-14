package com.prezi.haxe.gradle;

import com.google.common.base.Joiner;
import org.gradle.api.internal.artifacts.dsl.DefaultRepositoryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultExecutionResultHandler implements ExecutionResultHandler {

	private static final Logger logger = LoggerFactory.getLogger(DefaultRepositoryHandler.class);

	private final Iterable<?> cmd;
	private String output;

	DefaultExecutionResultHandler(Iterable<?> cmd) {
		this.cmd = cmd;
	}

	@Override
	public void handleResult(int exitValue, String output) {
		if (shouldPrintResult(exitValue, output)) {
			logger.warn("{}", output);
		}
		if (exitValue != 0) {
			throw new RuntimeException("Command finished with non-zero exit value (" + exitValue + "):\n" + Joiner.on("").join(cmd));
		}
	}

	protected boolean shouldPrintResult(int exitValue, String output) {
		return exitValue != 0;
	}
}
