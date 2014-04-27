package com.prezi.haxe.gradle

import org.gradle.api.artifacts.Configuration
import org.gradle.language.base.LanguageSourceSet

/**
 * Created by lptr on 08/02/14.
 */
interface HaxeSourceSet extends LanguageSourceSet {
	Configuration getCompileClassPath()
}
