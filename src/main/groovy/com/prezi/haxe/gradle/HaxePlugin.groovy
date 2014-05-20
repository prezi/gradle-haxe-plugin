package com.prezi.haxe.gradle

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.language.base.BinaryContainer

class HaxePlugin implements Plugin<Project> {

	@Override
	void apply(Project project) {
		project.plugins.apply(HaxeBasePlugin)

		def binaryContainer = project.extensions.getByType(BinaryContainer)

		// Add a compile, source and munit task for each compiled binary
		binaryContainer.withType(HaxeBinary).all(new Action<HaxeBinary>() {
			public void execute(final HaxeBinary binary) {
				HaxeBasePlugin.createCompileTask(project, binary, HaxeCompile)
				HaxeBasePlugin.createSourceTask(project, binary, Har)
			}
		})
		binaryContainer.withType(HaxeTestBinary).all(new Action<HaxeTestBinary>() {
			@Override
			void execute(HaxeTestBinary binary) {
				HaxeBasePlugin.createTestCompileTask(project, binary, HaxeTestCompile)
				HaxeBasePlugin.createMUnitTask(project, binary, MUnit)
			}
		});
	}
}
