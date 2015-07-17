package com.prezi.haxe.gradle;

import com.google.common.io.Resources;
import com.prezi.io.BufferedOutputExecFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.internal.ExecAction;
import org.gradle.process.internal.ExecActionFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MUnitNode extends MUnit {
	public static final String MUNIT_NODE_RUNNER_JS = "munit_node_runner.js";
	private final ExecActionFactory execActionFactory;
	private File nodeModulesDirectory;

	@InputDirectory
	public File getNodeModulesDirectory() {
		return nodeModulesDirectory;
	}

	public void setNodeModulesDirectory(Object nodeModulesDirectory) {
		this.nodeModulesDirectory = getProject().file(nodeModulesDirectory);
	}

	public void nodeModulesDirectory(Object nodeModulesDirectory) {
		setNodeModulesDirectory(nodeModulesDirectory);
	}

	@Inject
	public MUnitNode(ExecActionFactory execActionFactory) {
		this.execActionFactory = execActionFactory;
	}

	@Override
	protected void prepareEnvironment(File workDir) throws IOException {
		copyCompiledTest(workDir);
		setupRunner(workDir);
	}

	@TaskAction
	@Override
	public void munit() throws IOException, InterruptedException {
		File workDir = getWorkingDirectory();
		FileUtils.deleteDirectory(workDir);
		FileUtils.forceMkdir(workDir);

		prepareEnvironment(workDir);

		File munitNodeRunner = new File(workDir, MUNIT_NODE_RUNNER_JS);
		munitNodeRunner.setExecutable(true);
		ExecAction exec = BufferedOutputExecFactory.createExecAction(execActionFactory, this);
		exec.workingDir(workDir);
		exec.commandLine("./" + munitNodeRunner.getName());
		exec.environment("NODE_PATH", getNodeModulesDirectory());
		exec.execute();
	}

	private void setupRunner(File workDir) throws IOException {
		FileOutputStream fos = new FileOutputStream(new File(workDir, MUNIT_NODE_RUNNER_JS));
		try {
			Resources.copy(Resources.getResource(MUnitNode.class, "/munit_node_resources/" + MUNIT_NODE_RUNNER_JS),
					fos);
		} finally {
			IOUtils.closeQuietly(fos);
		}
	}

	@Override
	public boolean shouldRunAutomatically() {
		return !getProject().hasProperty("munit.usebrowser") || getProject().property("munit.usebrowser").equals("false");
	}
}
