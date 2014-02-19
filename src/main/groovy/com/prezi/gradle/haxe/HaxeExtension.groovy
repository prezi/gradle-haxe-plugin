package com.prezi.gradle.haxe

import org.gradle.api.Action
import org.gradle.internal.reflect.Instantiator

import javax.inject.Inject

/**
 * Created by lptr on 15/02/14.
 */
class HaxeExtension {
	@Delegate(deprecated = true)
	private final HaxeCompileParameters params
	private final TargetPlatformContainer targetPlatforms

	@Inject
	HaxeExtension(Instantiator instantiator) {
		this.params = new HaxeCompileParameters()
		this.targetPlatforms = instantiator.newInstance(DefaultTargetPlatformContainer, instantiator)
	}

	HaxeCompileParameters getParams() {
		return params
	}

	TargetPlatformContainer getTargetPlatforms() {
		return targetPlatforms
	}

	public void targetPlatforms(Action<TargetPlatformContainer> action) {
		action.execute(targetPlatforms)
	}
}
