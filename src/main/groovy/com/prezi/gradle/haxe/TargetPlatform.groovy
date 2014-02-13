package com.prezi.gradle.haxe

import org.gradle.api.Action
import org.gradle.api.Named

/**
 * Created by lptr on 09/02/14.
 */
interface TargetPlatform extends Named {
	void flavors(Action<? super FlavorContainer> action)
	FlavorContainer getFlavors()
	HaxeCompileParameters getParams()
}
