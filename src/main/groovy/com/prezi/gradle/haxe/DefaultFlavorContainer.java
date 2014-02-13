package com.prezi.gradle.haxe;

import org.gradle.api.internal.AbstractNamedDomainObjectContainer;
import org.gradle.internal.reflect.Instantiator;

public class DefaultFlavorContainer extends AbstractNamedDomainObjectContainer<Flavor> implements FlavorContainer {
	public DefaultFlavorContainer(Instantiator instantiator)
	{
		super(Flavor.class, instantiator);
	}

	@Override
	protected Flavor doCreate(String name)
	{
		return getInstantiator().newInstance(DefaultFlavor.class, name);
	}

}
