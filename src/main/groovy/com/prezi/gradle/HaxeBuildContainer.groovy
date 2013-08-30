package com.prezi.gradle

import org.gradle.api.internal.DefaultPolymorphicDomainObjectContainer
import org.gradle.internal.reflect.Instantiator

class HaxeBuildContainer extends DefaultPolymorphicDomainObjectContainer<HaxeBuild>
{
	public HaxeBuildContainer(Instantiator instantiator)
	{
		super(HaxeBuild, instantiator)
	}
}
