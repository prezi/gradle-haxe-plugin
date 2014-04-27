package com.prezi.haxe.gradle

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

import javax.inject.Inject

/**
 * Created by lptr on 15/02/14.
 */
class HaxeExtension {
	@Delegate(deprecated = true)
	private final HaxeCompileParameters params
	private final NamedDomainObjectContainer<TargetPlatform> targetPlatforms

	@Inject
	HaxeExtension(Project project) {
		this.params = new HaxeCompileParameters()
		this.targetPlatforms = project.container(TargetPlatform, { platformName ->
			new DefaultTargetPlatform(platformName, project)
		})
	}

	HaxeCompileParameters getParams() {
		return params
	}

	NamedDomainObjectContainer<TargetPlatform> getTargetPlatforms() {
		return targetPlatforms
	}

	public void targetPlatforms(Action<? super NamedDomainObjectContainer<TargetPlatform>> action) {
		action.execute(targetPlatforms)
	}
}
