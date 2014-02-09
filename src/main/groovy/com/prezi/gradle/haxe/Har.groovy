package com.prezi.gradle.haxe

import org.gradle.api.tasks.bundling.Jar

/**
 * Created by lptr on 09/02/14.
 */
class Har extends Jar {
	public static final String DEFAULT_EXTENSION = 'har'

	public static final String MANIFEST_ATTR_LIBRARY_VERSION = "Library-Version"
	public static final String MANIFEST_ATTR_EMBEDDED_RESOURCES = "Embedded-Resources"

	Map<String, File> embeddedResources = [:]

	public Har() {
		extension = DEFAULT_EXTENSION
	}

	@Override
	protected void copy() {
		manifest.attributes.put(MANIFEST_ATTR_LIBRARY_VERSION, "1.0")
		def embeddedResources = getEmbeddedResources()
		println "--------> Embedded resources: ${embeddedResources}"

		if (!embeddedResources.empty) {
			manifest.attributes.put(MANIFEST_ATTR_EMBEDDED_RESOURCES, EmbeddedResourceEncoding.encode(embeddedResources))
		}

		super.copy()
	}
}
