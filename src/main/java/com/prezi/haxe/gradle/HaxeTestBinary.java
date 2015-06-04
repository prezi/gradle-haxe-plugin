package com.prezi.haxe.gradle;

import org.gradle.api.artifacts.Configuration;

public class HaxeTestBinary extends AbstractHaxeBinary<HaxeTestCompile> {

	private Class<? extends HaxeTestCompile> compileClass;

	protected HaxeTestBinary(String parentName, Configuration configuration, TargetPlatform targetPlatform, Flavor flavor, Class<? extends HaxeTestCompile> compileClass) {
		super(parentName + "Test", configuration, targetPlatform, flavor);
		this.compileClass = compileClass;
	}

	public Class<? extends HaxeTestCompile> getCompileClass() {
		return compileClass;
	}
}
