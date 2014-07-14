package com.prezi.haxe.gradle

/**
 * Created by lptr on 09/02/14.
 */
class DefaultFlavor extends DefaultHaxeCompilerParametersSupport implements Flavor {
	private final String name

	public DefaultFlavor(String name) {
		this.name = name
	}

	@Override
	String getName() {
		return name
	}

	@Override
	String toString() {
		return "flavor: ${name}"
	}
}
