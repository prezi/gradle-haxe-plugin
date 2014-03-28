package com.prezi.gradle.haxe

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer

/**
 * Created by lptr on 09/02/14.
 */
interface TargetPlatform extends Named {
	void flavors(Action<? super NamedDomainObjectContainer<Flavor>> action)
	void flavors(Closure closure)
	NamedDomainObjectContainer<Flavor> getFlavors()
	HaxeCompileParameters getParams()
}
