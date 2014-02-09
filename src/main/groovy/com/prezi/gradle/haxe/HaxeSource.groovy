package com.prezi.gradle.haxe

import org.gradle.api.tasks.bundling.Jar

/**
 * Created by lptr on 09/02/14.
 */
class HaxeSource extends Jar {
	public HaxeSource() {
		extension = HarUtils.DEFAULT_EXTENSION
	}
}
