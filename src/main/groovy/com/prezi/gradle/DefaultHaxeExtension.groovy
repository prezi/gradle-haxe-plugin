package com.prezi.gradle

import org.gradle.api.Action
import org.gradle.api.plugins.DeferredConfigurable

@DeferredConfigurable
class DefaultHaxeExtension implements HaxeExtension {

	private HaxeBuildContainer builds

	public DefaultHaxeExtension(HaxeBuildContainer builds)
	{
		this.builds = builds
	}

	@Override
	HaxeBuildContainer getBuilds()
	{
		return builds
	}

	@Override
	void builds(Action<? super HaxeBuildContainer> configure)
	{
		configure.execute(builds)
	}
}
