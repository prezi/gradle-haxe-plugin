package com.prezi.gradle.haxe

import org.gradle.api.Action
import org.gradle.internal.reflect.Instantiator

/**
 * Created by lptr on 09/02/14.
 */
class DefaultTargetPlatform implements TargetPlatform {
	private final String name
	private final FlavorContainer flavors

	public DefaultTargetPlatform(String name, Instantiator instantiator) {
		this.name = name
		this.flavors = instantiator.newInstance(DefaultFlavorContainer, instantiator)
	}

	@Override
	String getName() {
		return name
	}

	@Override
	FlavorContainer getFlavors() {
		return flavors
	}

	@Override
	void flavors(Action<? super FlavorContainer> action) {
		action.execute(getFlavors())
	}

	@Override
	String toString() {
		return "platform: ${name} ${flavors}"
	}
}
