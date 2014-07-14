package com.prezi.haxe.gradle;

public interface ExecutionResultHandler {
	public abstract void handleResult(int exitValue, String output);
}
