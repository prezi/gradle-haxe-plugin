package com.prezi.haxe.gradle;

import org.gradle.api.artifacts.Configuration;
import org.gradle.language.base.LanguageSourceSet;

public interface HaxeSourceSet extends LanguageSourceSet {
	Configuration getCompileClassPath();
}
