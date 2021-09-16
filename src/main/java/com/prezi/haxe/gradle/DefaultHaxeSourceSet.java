package com.prezi.haxe.gradle;

import com.prezi.haxe.gradle.incubating.AbstractLanguageSourceSet;
import com.prezi.haxe.gradle.incubating.FunctionalSourceSet;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.file.collections.DefaultDirectoryFileTreeFactory;
import org.gradle.api.model.ObjectFactory;

public class DefaultHaxeSourceSet extends AbstractLanguageSourceSet implements HaxeSourceSet {

	private final Configuration compileClassPath;

	public DefaultHaxeSourceSet(String name, FunctionalSourceSet parent, Configuration compileClassPath, ObjectFactory objectFactory) {
		super(name, parent, "Haxe source", objectFactory.sourceDirectorySet("source", "source"));
		this.compileClassPath = compileClassPath;
	}

	@Override
	public Configuration getCompileClassPath() {
		return compileClassPath;
	}
}
