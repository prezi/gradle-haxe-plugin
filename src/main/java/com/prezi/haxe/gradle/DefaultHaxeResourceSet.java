package com.prezi.haxe.gradle;

import com.google.common.collect.Maps;
import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.language.base.FunctionalSourceSet;
import org.gradle.language.jvm.internal.DefaultResourceSet;

import java.io.File;
import java.util.Map;

public class DefaultHaxeResourceSet extends DefaultResourceSet implements HaxeResourceSet {
	private final Map<String, File> embeddedResources = Maps.newLinkedHashMap();
	private final FileResolver fileResolver;

	public DefaultHaxeResourceSet(String name, FunctionalSourceSet parent, FileResolver fileResolver) {
		super(name, new DefaultSourceDirectorySet("resource", fileResolver), parent);
		this.fileResolver = fileResolver;
	}

	@Override
	public Map<String, File> getEmbeddedResources() {
		return embeddedResources;
	}

	@Override
	public void embed(String name, Object file) {
		embedInternal(name, fileResolver.resolve(file));
	}

	@Override
	public void embed(Object file) {
		File resolvedFile = fileResolver.resolve(file);
		embedInternal(resolvedFile.getName(), resolvedFile);
	}

	public void embedInternal(String name, File file) {
		if (!file.exists()) {
			throw new IllegalArgumentException("File not found: " + file);
		}

		if (!file.isFile()) {
			throw new IllegalArgumentException("Not a file: " + file);
		}

		embeddedResources.put(name, file);
	}

	@Override
	public void embedAll(Object directory) {
		File resolvedDir = fileResolver.resolve(directory);
		if (!resolvedDir.exists()) {
			throw new IllegalArgumentException("Directory to embed does not exist: " + directory);
		}

		if (!resolvedDir.isDirectory()) {
			throw new IllegalArgumentException("Requires a directory to embed all files from, but it was not: " + directory);
		}

		File[] files = resolvedDir.listFiles();
		if (files != null) {
			for (File it : files) {
				if (it.isFile()) {
					embed(it);
				}
			}
		}
	}

}
