package com.prezi.gradle.haxe;

import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.internal.AbstractNamedDomainObjectContainer;
import org.gradle.internal.reflect.Instantiator;

/*
 * Required because if this was Groovy, it would fail to compile with an error
 * about incompatible return types for {@link #withType(Class)}.
 */
abstract public class BaseTargetPlatformContainer extends AbstractNamedDomainObjectContainer<TargetPlatform> implements TargetPlatformContainer {
	public BaseTargetPlatformContainer(Instantiator instantiator)
	{
		super(TargetPlatform.class, instantiator);
	}

	@Override
	protected TargetPlatform doCreate(String name)
	{
		return getInstantiator().newInstance(DefaultTargetPlatform.class, name, getInstantiator());
	}

	@Override
	public TargetPlatform create(String name, Action<? super TargetPlatform> configureAction) throws InvalidUserDataException
	{
		return super.create(name, configureAction);
	}
}
