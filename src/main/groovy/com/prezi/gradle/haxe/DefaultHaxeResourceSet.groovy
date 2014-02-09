package com.prezi.gradle.haxe

import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.collections.DefaultConfigurableFileCollection
import org.gradle.api.internal.tasks.TaskResolver
import org.gradle.language.base.FunctionalSourceSet
import org.gradle.language.base.internal.AbstractLanguageSourceSet
import org.gradle.language.jvm.internal.DefaultResourceSet

/**
 * Created by lptr on 08/02/14.
 */
class DefaultHaxeResourceSet extends DefaultResourceSet implements HaxeResourceSet {

	private final LinkedHashMap<String, File> embeddedResources = [:]
	private final FileResolver fileResolver

	public DefaultHaxeResourceSet(String name, FunctionalSourceSet parent, FileResolver fileResolver) {
		super(name, new DefaultSourceDirectorySet("resource", fileResolver), parent)
		this.fileResolver = fileResolver
	}

	@Override
	Map<String, File> getEmbeddedResources() {
		return embeddedResources
	}

	@Override
	void embed(String name, Object file) {
		embedInternal(name, fileResolver.resolve(file))
	}

	@Override
	void embed(Object file) {
		def resolvedFile = fileResolver.resolve(file)
		embedInternal(resolvedFile.name, resolvedFile)
	}

	void embedInternal(String name, File file) {
		if (!file.file)
		{
			throw new IllegalArgumentException("Not a file: " + file)
		}
		embeddedResources.put(name, file)
	}

	@Override
	void embedAll(Object directory) {
		def resolvedDir = fileResolver.resolve(directory)
		if (!resolvedDir.directory)
		{
			throw new IllegalArgumentException("embedAll requires a directory: " + directory)
		}
		resolvedDir.eachFile {
			if (it.file) {
				embed(it)
			}
		}
	}
}
