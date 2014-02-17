package com.prezi.gradle.haxe;

import org.gradle.api.internal.AbstractNamedDomainObjectContainer;
import org.gradle.internal.reflect.Instantiator;

abstract public class BaseFlavorContainer extends AbstractNamedDomainObjectContainer<Flavor> implements FlavorContainer {
	public BaseFlavorContainer(Instantiator instantiator)
	{
		super(Flavor.class, instantiator);
	}

	@Override
	protected Flavor doCreate(String name)
	{
		return getInstantiator().newInstance(DefaultFlavor.class, name);
	}

}
