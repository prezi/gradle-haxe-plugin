package com.prezi.haxe.gradle;

import com.prezi.haxe.gradle.incubating.BinaryContainer;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class HaxePlugin implements Plugin<Project> {
	@Override
	public void apply(final Project project) {
		project.getPlugins().apply(HaxeBasePlugin.class);

		BinaryContainer binaryContainer = project.getExtensions().getByType(HaxeExtension.class).getBinaries();

		// Add a compile, source and munit task for each compiled binary
		binaryContainer.withType(HaxeBinary.class).all(new Action<HaxeBinary>() {
			public void execute(HaxeBinary binary) {
				HaxeBasePlugin.createCompileTask(project, binary, HaxeCompile.class);
				HaxeBasePlugin.createSourceTask(project, binary, Har.class);
			}
		});
		binaryContainer.withType(HaxeTestBinary.class).all(new Action<HaxeTestBinary>() {
			@Override
			public void execute(HaxeTestBinary binary) {
				HaxeBasePlugin.createTestCompileTask(project, binary, HaxeTestCompile.class);
				HaxeBasePlugin.createSourceTask(project, binary, Har.class);
				HaxeBasePlugin.createMUnitTask(project, binary, MUnit.class);
			}
		});
	}
}
