package com.prezi.gradle.haxe

import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.java.archives.Manifest
import org.gradle.api.java.archives.internal.DefaultManifest

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Created by lptr on 06/12/13.
 */
class HarUtils {
	public static final String DEFAULT_EXTENSION = 'har'

	public static final String MANIFEST_ATTR_LIBRARY_VERSION = "Library-Version"
	public static final String MANIFEST_ATTR_EMBEDDED_RESOURCES = "Embedded-Resources"

	static Manifest createArchive(Project project, org.gradle.internal.Factory<File> temporaryDirFactory,
								  File outputDirectory, String name, FileCollection sources, FileCollection resources, Map<String, File> embeddedResources)
	{
		def manifest = new DefaultManifest(null)
		manifest.attributes.put(MANIFEST_ATTR_LIBRARY_VERSION, "1.0")
		if (!embeddedResources.isEmpty())
		{
			manifest.attributes.put(MANIFEST_ATTR_EMBEDDED_RESOURCES, EmbeddedResourceEncoding.encode(embeddedResources))
		}

		def tempDirectory = temporaryDirFactory.create()
		def manifestFile = new File(tempDirectory, "META-INF/MANIFEST.MF")
		manifestFile.parentFile.mkdirs()
		manifestFile.delete()
		manifestFile.withWriter { writer -> manifest.writeTo(writer) }

		project.copy {
			duplicatesStrategy = DuplicatesStrategy.EXCLUDE
			into(new File(tempDirectory, "sources"))
			from sources.files.toArray().reverse()
		}
		project.copy {
			duplicatesStrategy = DuplicatesStrategy.EXCLUDE
			from resources.files.toArray().reverse()
			into(new File(tempDirectory, "resources"))
		}
		project.copy {
			duplicatesStrategy = DuplicatesStrategy.EXCLUDE
			from embeddedResources.values().toArray().reverse()
			into(new File(tempDirectory, "embedded"))
		}

		zip(tempDirectory, new File(outputDirectory, name + "." + DEFAULT_EXTENSION))

		return manifest
	}

	static void zip(File source, File zipFile)
	{
		zipFile.delete()
		zipFile.parentFile.mkdirs()
		zipFile.withOutputStream { fileOutput ->
			ZipOutputStream zipOutput = new ZipOutputStream(fileOutput);

			int topDirLength = source.absolutePath.length()

			source.eachFileRecurse { file ->
				def relative = file.absolutePath.substring(topDirLength).replace('\\', '/')
				if (file.isDirectory() && !relative.endsWith('/'))
				{
					relative += "/"
				}
//				if (ignorePattern.matcher(relative).find())
//				{
//					return
//				}
				println relative

				ZipEntry entry = new ZipEntry(relative)
				entry.time = file.lastModified()
				zipOutput.putNextEntry(entry)
				if (file.isFile())
				{
					file.withInputStream { input -> zipOutput << input }
				}
			}
			zipOutput.close()
		}
	}
}
