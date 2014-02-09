package com.prezi.gradle.haxe

/**
 * Created by lptr on 09/02/14.
 */
class DefaultCompiledHaxeBinary extends AbstractHaxeBinary implements CompiledHaxeBinary {
	public DefaultCompiledHaxeBinary(String parentName, TargetPlatform targetPlatform) {
		super(parentName, targetPlatform)
	}
}
