package com.prezi.haxe.gradle

import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.internal.file.FileResolver
import org.gradle.language.base.FunctionalSourceSet
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
		if (!file.exists()) {
			throw new IllegalArgumentException("File not found: " + file)
		}
		if (!file.file)
		{
			throw new IllegalArgumentException("Not a file: " + file)
		}
		embeddedResources.put(name, file)
	}

	@Override
	void embedAll(Object directory) {
		def resolvedDir = fileResolver.resolve(directory)
		if (!resolvedDir.exists()) {
			throw new IllegalArgumentException("Directory to embed does not exist: " + directory)
		}
		if (!resolvedDir.directory) {
			throw new IllegalArgumentException("Requires a directory to embed all files from, but it was not: " + directory)
		}
		resolvedDir.eachFile {
			if (it.file) {
				embed(it)
			}
		}
	}
}
