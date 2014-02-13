package com.prezi.gradle.haxe

/**
 * Created by lptr on 09/02/14.
 */
class DefaultFlavor implements Flavor {
	private final String name
	private final String collapsedName
	@Delegate(deprecated = true)
	private final HaxeCompileParameters params

	public DefaultFlavor(String name) {
		this(name, name)
	}

	DefaultFlavor(String name, String collapsedName) {
		this.name = name
		this.collapsedName = collapsedName
		this.params = new HaxeCompileParameters()
	}

	@Override
	String getName() {
		return name
	}

	@Override
	String getCollapsedName() {
		return collapsedName
	}

	@Override
	HaxeCompileParameters getParams() {
		return params
	}

	@Override
	String toString() {
		return name
	}
}
