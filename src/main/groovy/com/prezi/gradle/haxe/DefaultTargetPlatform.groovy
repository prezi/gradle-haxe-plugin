package com.prezi.gradle.haxe

/**
 * Created by lptr on 09/02/14.
 */
class DefaultTargetPlatform implements TargetPlatform {
	private final String name

	public DefaultTargetPlatform(String name) {
		this.name = name
	}

	@Override
	String getName() {
		return name
	}
}
