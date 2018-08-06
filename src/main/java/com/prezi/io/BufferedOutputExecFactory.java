package com.prezi.io;

import org.gradle.api.Task;
import org.gradle.process.*;
import org.gradle.process.internal.ExecAction;
import org.gradle.process.internal.ExecActionFactory;
import org.gradle.process.internal.ExecException;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class BufferedOutputExecFactory {
	public static ExecAction createExecAction(ExecActionFactory factory, Task task) {
		if (task.getProject().getGradle().getStartParameter().isParallelProjectExecutionEnabled()) {
			return new WrappedExecAction(factory.newExecAction(), task.getPath());
		} else {
			return factory.newExecAction();
		}
	}

	private static class WrappedExecAction implements ExecAction {
		private ExecAction execAction;
		private String taskPath;

		public WrappedExecAction(ExecAction execAction, String taskPath) {
			this.execAction = execAction;
			this.taskPath = taskPath;
		}

		@Override
		public ExecResult execute() throws ExecException {
			BufferedStdOutputStreams os = new BufferedStdOutputStreams();
			execAction.setStandardOutput(os.getStdOutOutputStream());
			execAction.setErrorOutput(os.getStdErrOutputStream());
			try {
				return execAction.execute();
			} catch (ExecException e) {
				throw new RuntimeException(e.getMessage() + "\n" + "Executed by " + taskPath + " in directory " + getWorkingDir() + "\n" + os.getErrorOut(), e);
			} finally {
				System.out.println(taskPath);
				os.printOutput();
			}
		}

		@Override
		public void setCommandLine(List<String> list) {
			execAction.setCommandLine(list);
		}

		@Override
		public void setCommandLine(Object... objects) {
			execAction.setCommandLine(objects);
		}

		@Override
		public void setCommandLine(Iterable<?> iterable) {
			execAction.setCommandLine(iterable);
		}

		@Override
		public ExecSpec commandLine(Object... objects) {
			return execAction.commandLine(objects);
		}

		@Override
		public ExecSpec commandLine(Iterable<?> iterable) {
			return execAction.commandLine(iterable);
		}

		@Override
		public ExecSpec args(Object... objects) {
			return execAction.args(objects);
		}

		@Override
		public ExecSpec args(Iterable<?> iterable) {
			return execAction.args(iterable);
		}

		@Override
		public ExecSpec setArgs(List<String> list) {
			return execAction.setArgs(list);
		}

		@Override
		public ExecSpec setArgs(Iterable<?> iterable) {
			return execAction.setArgs(iterable);
		}

		@Override
		public List<String> getArgs() {
			return execAction.getArgs();
		}

		@Override
		public List<CommandLineArgumentProvider> getArgumentProviders() {
			return execAction.getArgumentProviders();
		}

		@Override
		public BaseExecSpec setIgnoreExitValue(boolean b) {
			return execAction.setIgnoreExitValue(b);
		}

		@Override
		public boolean isIgnoreExitValue() {
			return execAction.isIgnoreExitValue();
		}

		@Override
		public BaseExecSpec setStandardInput(InputStream inputStream) {
			return execAction.setStandardInput(inputStream);
		}

		@Override
		public InputStream getStandardInput() {
			return execAction.getStandardInput();
		}

		@Override
		public BaseExecSpec setStandardOutput(OutputStream outputStream) {
			return execAction.setStandardOutput(outputStream);
		}

		@Override
		public OutputStream getStandardOutput() {
			return execAction.getStandardOutput();
		}

		@Override
		public BaseExecSpec setErrorOutput(OutputStream outputStream) {
			return execAction.setErrorOutput(outputStream);
		}

		@Override
		public OutputStream getErrorOutput() {
			return execAction.getErrorOutput();
		}

		@Override
		public List<String> getCommandLine() {
			return execAction.getCommandLine();
		}

		@Override
		public String getExecutable() {
			return execAction.getExecutable();
		}

		@Override
		public void setExecutable(String s) {
			execAction.setExecutable(s);
		}

		@Override
		public void setExecutable(Object o) {
			execAction.setExecutable(o);
		}

		@Override
		public ProcessForkOptions executable(Object o) {
			return execAction.executable(o);
		}

		@Override
		public File getWorkingDir() {
			return execAction.getWorkingDir();
		}

		@Override
		public void setWorkingDir(File file) {
			execAction.setWorkingDir(file);
		}

		@Override
		public void setWorkingDir(Object o) {
			execAction.setWorkingDir(o);
		}

		@Override
		public ProcessForkOptions workingDir(Object o) {
			return execAction.workingDir(o);
		}

		@Override
		public Map<String, Object> getEnvironment() {
			return execAction.getEnvironment();
		}

		@Override
		public void setEnvironment(Map<String, ?> map) {
			execAction.setEnvironment(map);
		}

		@Override
		public ProcessForkOptions environment(Map<String, ?> map) {
			return execAction.environment(map);
		}

		@Override
		public ProcessForkOptions environment(String s, Object o) {
			return execAction.environment(s, o);
		}

		@Override
		public ProcessForkOptions copyTo(ProcessForkOptions processForkOptions) {
			return execAction.copyTo(processForkOptions);
		}
	}
}
