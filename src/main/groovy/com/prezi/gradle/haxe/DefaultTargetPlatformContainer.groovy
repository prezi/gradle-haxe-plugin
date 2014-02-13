package com.prezi.gradle.haxe

import org.gradle.internal.reflect.Instantiator

public class DefaultTargetPlatformContainer extends BaseTargetPlatformContainer {
	@Delegate(deprecated = true)
	private final HaxeCompileParameters params

	public DefaultTargetPlatformContainer(Instantiator instantiator)
	{
		super(instantiator)
		this.params = new HaxeCompileParameters()
	}

	@Override
	HaxeCompileParameters getParams() {
		return params
	}
}
