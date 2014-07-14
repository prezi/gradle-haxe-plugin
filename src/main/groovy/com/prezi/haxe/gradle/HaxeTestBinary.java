package com.prezi.haxe.gradle;

import org.gradle.api.artifacts.Configuration;

public class HaxeTestBinary extends AbstractHaxeBinary<HaxeTestCompile> {
	protected HaxeTestBinary(String parentName, Configuration configuration, TargetPlatform targetPlatform, Flavor flavor) {
		super(parentName + "Test", configuration, targetPlatform, flavor);
	}
}
