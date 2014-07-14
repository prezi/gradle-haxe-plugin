package com.prezi.haxe.gradle;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.language.base.FunctionalSourceSet;
import org.gradle.language.base.internal.AbstractLanguageSourceSet;

public class DefaultHaxeSourceSet extends AbstractLanguageSourceSet implements HaxeSourceSet {

	private final Configuration compileClassPath;

	public DefaultHaxeSourceSet(String name, FunctionalSourceSet parent, Configuration compileClassPath, FileResolver fileResolver) {
		super(name, parent, "Haxe source", new DefaultSourceDirectorySet("source", fileResolver));
		this.compileClassPath = compileClassPath;
	}

	@Override
	public Configuration getCompileClassPath() {
		return compileClassPath;
	}
}
