package com.prezi.haxe.gradle;

import com.google.common.collect.Lists;
import org.gradle.api.Project;

import java.util.Arrays;
import java.util.List;

public class MUnitCommandBuilder {
	private final Project project;
	private final List<String> cmd;

	public MUnitCommandBuilder(Project project) {
		this.project = project;
		this.cmd = Lists.newArrayList();
	}

	public List<String> build() {
		if (project.hasProperty("munit.haxeRunner")) {
			cmd.addAll(Arrays.asList("haxe", "--run", "tools.haxelib.Main"));
		} else {
			cmd.add("haxelib");
		}

		cmd.addAll(Arrays.asList("run", "munit", "run"));
		processCommandLineOptions();
		return cmd;
	}

	private void processCommandLineOptions() {
		if (project.hasProperty("munit.platform")) {
			cmd.add("-" + project.property("munit.platform"));
		}

		if (project.hasProperty("munit.browser")) {
			cmd.addAll(Arrays.asList("-browser", String.valueOf(project.property("munit.browser"))));
		}

		if (project.hasProperty("munit.kill-browser")) {
			cmd.add("-kill-browser");
		}

	}
}
