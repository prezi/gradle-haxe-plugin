package com.prezi.gradle.haxe;

import org.gradle.api.internal.AbstractNamedDomainObjectContainer;
import org.gradle.internal.reflect.Instantiator;

public class DefaultTargetPlatformContainer extends AbstractNamedDomainObjectContainer<TargetPlatform> implements TargetPlatformContainer {
	public DefaultTargetPlatformContainer(Instantiator instantiator)
	{
		super(TargetPlatform.class, instantiator);
	}

	@Override
	protected TargetPlatform doCreate(String name)
	{
		return getInstantiator().newInstance(DefaultTargetPlatform.class, name);
	}

}
