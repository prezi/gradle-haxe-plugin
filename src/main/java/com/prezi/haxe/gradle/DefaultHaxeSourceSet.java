package com.prezi.haxe.gradle;

import com.prezi.haxe.gradle.incubating.AbstractLanguageSourceSet;
import com.prezi.haxe.gradle.incubating.FunctionalSourceSet;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.internal.file.FileResolver;

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
