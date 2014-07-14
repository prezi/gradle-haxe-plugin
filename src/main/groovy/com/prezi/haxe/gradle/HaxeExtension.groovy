package com.prezi.haxe.gradle

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

import javax.inject.Inject

/**
 * Created by lptr on 15/02/14.
 */
class HaxeExtension extends DefaultHaxeCompilerParametersSupport {
	private final NamedDomainObjectContainer<TargetPlatform> targetPlatforms

	@Inject
	HaxeExtension(Project project) {
		this.targetPlatforms = project.container(TargetPlatform, { platformName ->
			new DefaultTargetPlatform(platformName, project)
		})
	}

	NamedDomainObjectContainer<TargetPlatform> getTargetPlatforms() {
		return targetPlatforms
	}

	public void targetPlatforms(Action<? super NamedDomainObjectContainer<TargetPlatform>> action) {
		action.execute(targetPlatforms)
	}
}
