package com.prezi.gradle.haxe

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

/**
 * Created by lptr on 09/02/14.
 */
class DefaultTargetPlatform implements TargetPlatform {
	private final String name
	private final NamedDomainObjectContainer<Flavor> flavors
	@Delegate(deprecated = true)
	private final HaxeCompileParameters params

	public DefaultTargetPlatform(String name, Project project) {
		this.name = name
		this.flavors = project.container(DefaultFlavor)
		this.params = new HaxeCompileParameters()
	}

	@Override
	String getName() {
		return name
	}

	@Override
	NamedDomainObjectContainer<Flavor> getFlavors() {
		return flavors
	}

	@Override
	void flavors(Action<? super NamedDomainObjectContainer<Flavor>> action) {
		action.execute(getFlavors())
	}

	@Override
	void flavors(Closure closure) {
		ConfigureUtil.configure(closure, getFlavors())
	}

	@Override
	HaxeCompileParameters getParams() {
		return params
	}

	@Override
	String toString() {
		return "platform: ${name} ${flavors*.name}"
	}
}
