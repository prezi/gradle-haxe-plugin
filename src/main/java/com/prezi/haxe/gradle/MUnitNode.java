package com.prezi.haxe.gradle;

import com.google.common.io.Resources;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.internal.ExecAction;
import org.gradle.process.internal.ExecActionFactory;
import sun.net.www.protocol.file.FileURLConnection;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
		ExecAction exec = execActionFactory.newExecAction();
		exec.workingDir(workDir);
		exec.commandLine("./" + munitNodeRunner.getName());
		exec.environment("NODE_PATH", getNodeModulesDirectory());
		exec.execute();
	}

	private void setupRunner(File workDir) throws IOException {
		Resources.copy(Resources.getResource(MUnitNode.class, "/munit_node_resources/" + MUNIT_NODE_RUNNER_JS),
				new FileOutputStream(new File(workDir, MUNIT_NODE_RUNNER_JS)));
	}

	@Override
	public boolean shouldRunAutomatically() {
		return getProject().hasProperty("munit.usenode") && !getProject().property("munit.usenode").equals("false");
	}
}
