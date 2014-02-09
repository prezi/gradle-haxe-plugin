package com.prezi.gradle.haxe

import org.gradle.api.file.FileCollection
import org.gradle.language.base.LanguageSourceSet
import org.gradle.language.jvm.ResourceSet

/**
 * Created by lptr on 08/02/14.
 */
interface HaxeResourceSet extends ResourceSet {
	Map<String, File> getEmbeddedResources()
	void embed(String name, Object file)
	void embed(Object file)
	void embedAll(Object directory)
}
