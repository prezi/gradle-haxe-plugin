package com.prezi.gradle.haxe

/**
 * Created by lptr on 09/02/14.
 */
class DefaultFlavor implements Flavor {
	private final String name
	@Delegate(deprecated = true)
	private final HaxeCompileParameters params

	public DefaultFlavor(String name) {
		this.name = name
		this.params = new HaxeCompileParameters()
	}

	@Override
	String getName() {
		return name
	}

	@Override
	HaxeCompileParameters getParams() {
		return params
	}

	@Override
	String toString() {
		return "flavor: ${name}"
	}
}
