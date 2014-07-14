package com.prezi.haxe.gradle;

public class DefaultFlavor extends DefaultHaxeCompilerParameters implements Flavor {
	private final String name;

	public DefaultFlavor(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "flavor: " + name;
	}
}
