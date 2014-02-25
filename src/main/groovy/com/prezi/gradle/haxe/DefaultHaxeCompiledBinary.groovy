package com.prezi.gradle.haxe

import org.gradle.api.artifacts.Configuration

/**
 * Created by lptr on 09/02/14.
 */
class DefaultHaxeCompiledBinary extends AbstractHaxeBinary implements HaxeCompiledBinary {
	HaxeCompile compileTask

	public DefaultHaxeCompiledBinary(String parentName, Configuration configuration, TargetPlatform targetPlatform, Flavor flavor) {
		super(parentName, configuration, targetPlatform, flavor)
	}
}
