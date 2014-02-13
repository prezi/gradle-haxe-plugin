package com.prezi.gradle.haxe

import org.gradle.api.Named

/**
 * Created by lptr on 09/02/14.
 */
interface Flavor extends Named {
	String getCollapsedName()
	HaxeCompileParameters getParams()
}
