package com.prezi.gradle.haxe

import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.collections.DefaultConfigurableFileCollection
import org.gradle.api.internal.tasks.TaskResolver
import org.gradle.language.base.FunctionalSourceSet
import org.gradle.language.base.internal.AbstractLanguageSourceSet

/**
 * Created by lptr on 08/02/14.
 */
class DefaultHaxeSourceSet extends AbstractLanguageSourceSet implements HaxeSourceSet {

	private final Configuration compileClassPath
	private final FileCollection output

	public DefaultHaxeSourceSet(String name, FunctionalSourceSet parent, Configuration compileClassPath, FileResolver fileResolver, TaskResolver taskResolver) {
		super(name, parent, "Haxe source", new DefaultSourceDirectorySet("source", fileResolver))
		this.compileClassPath = compileClassPath
		this.output = new DefaultConfigurableFileCollection("${name} output", fileResolver, taskResolver)
	}

	@Override
	FileCollection getOutput() {
		return output
	}

	@Override
	Configuration getCompileClassPath() {
		return compileClassPath
	}
}
