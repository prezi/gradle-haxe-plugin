package com.prezi.haxe.gradle

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

/**
 * Created by lptr on 09/02/14.
 */
class DefaultTargetPlatform extends DefaultHaxeCompilerParametersSupport implements TargetPlatform {
	private final String name
	private final NamedDomainObjectContainer<Flavor> flavors

	public DefaultTargetPlatform(String name, Project project) {
		this.name = name
		this.flavors = project.container(DefaultFlavor)
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
	String toString() {
		return "platform: ${name} ${flavors*.name}"
	}
}
