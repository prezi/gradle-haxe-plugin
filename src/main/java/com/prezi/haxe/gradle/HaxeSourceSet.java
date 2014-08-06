package com.prezi.haxe.gradle;

import com.prezi.haxe.gradle.incubating.LanguageSourceSet;
import org.gradle.api.artifacts.Configuration;

public interface HaxeSourceSet extends LanguageSourceSet {
	Configuration getCompileClassPath();
}
