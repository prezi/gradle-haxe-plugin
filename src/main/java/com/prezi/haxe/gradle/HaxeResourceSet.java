package com.prezi.haxe.gradle;

import com.prezi.haxe.gradle.incubating.ResourceSet;

import java.io.File;
import java.util.Map;

public interface HaxeResourceSet extends ResourceSet {
	Map<String, File> getEmbeddedResources();

	void embed(String name, Object file);

	void embed(Object file);

	void embedAll(Object directory);
}
