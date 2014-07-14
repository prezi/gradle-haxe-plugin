package com.prezi.haxe.gradle;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.Project;
import org.gradle.util.ConfigureUtil;

import java.io.Serializable;

public class DefaultTargetPlatform extends DefaultHaxeCompilerParameters implements TargetPlatform {
	private final String name;
	private final NamedDomainObjectContainer<Flavor> flavors;

	public DefaultTargetPlatform(String name, Project project) {
		this.name = name;
		this.flavors = project.container(Flavor.class, new FlavorNamedDomainObjectFactory());
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public NamedDomainObjectContainer<Flavor> getFlavors() {
		return flavors;
	}

	@Override
	public void flavors(Action<? super NamedDomainObjectContainer<Flavor>> action) {
		action.execute(getFlavors());
	}

	@Override
	public void flavors(Closure closure) {
		ConfigureUtil.configure(closure, getFlavors());
	}

	@Override
	public String toString() {
		return "platform: " + name + " " + Collections2.transform(flavors, new Function<Flavor, String>() {
			@Override
			public String apply(Flavor flavor) {
				return flavor.getName();
			}
		});
	}

	private static class FlavorNamedDomainObjectFactory implements NamedDomainObjectFactory<Flavor>, Serializable {
		@Override
		public Flavor create(String name) {
			return new DefaultFlavor(name);
		}
	}
}
