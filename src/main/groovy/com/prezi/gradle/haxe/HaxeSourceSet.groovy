package com.prezi.gradle.haxe

import org.gradle.api.file.FileCollection
import org.gradle.language.base.LanguageSourceSet

/**
 * Created by lptr on 08/02/14.
 */
interface HaxeSourceSet extends LanguageSourceSet {
	FileCollection getCompileClassPath()

	FileCollection getOutput()

	String getMain()
	void setMain(String main)
}
