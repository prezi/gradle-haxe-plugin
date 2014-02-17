package com.prezi.gradle.haxe

/**
 * Created by lptr on 09/02/14.
 */
class DefaultHaxeCompiledBinary extends AbstractHaxeBinary implements HaxeCompiledBinary {
	HaxeCompile compileTask

	public DefaultHaxeCompiledBinary(String parentName, TargetPlatform targetPlatform, Flavor flavor) {
		super(parentName, targetPlatform, flavor)
	}
}
