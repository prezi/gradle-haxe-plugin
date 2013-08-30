package com.prezi.gradle

import org.gradle.api.Action

interface HaxeExtension {
	String NAME = "haxe"

	HaxeBuildContainer getBuilds()

	void builds(Action<? super HaxeBuildContainer> builds)
}
