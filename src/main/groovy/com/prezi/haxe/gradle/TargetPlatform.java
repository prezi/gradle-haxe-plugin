package com.prezi.haxe.gradle;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectContainer;

public interface TargetPlatform extends Named, HaxeCompilerParametersSupport {
	void flavors(Action<? super NamedDomainObjectContainer<Flavor>> action);
	void flavors(Closure closure);
	NamedDomainObjectContainer<Flavor> getFlavors();
}
