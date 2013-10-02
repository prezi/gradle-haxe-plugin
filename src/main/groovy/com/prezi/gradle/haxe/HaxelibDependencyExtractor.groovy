package com.prezi.gradle.haxe

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.copy.FileCopyActionImpl
import org.gradle.api.internal.file.copy.FileCopySpecVisitor
import org.gradle.api.internal.file.copy.SyncCopySpecVisitor
import org.gradle.api.java.archives.Manifest
import org.gradle.api.java.archives.internal.DefaultManifest
import org.gradle.internal.reflect.Instantiator

class HaxelibDependencyExtractor {
	static final String EXTRACTED_HAXELIBS_DIR = "haxelibs"

	private final Project project
	private final Iterable<String> legacyPlatformPaths
	private final Instantiator instantiator
	private final FileResolver fileResolver

	HaxelibDependencyExtractor(Project project, Iterable<String> legacyPlatformPaths, Instantiator instantiator, FileResolver fileResolver)
	{
		this.project = project
		this.legacyPlatformPaths = legacyPlatformPaths
		this.fileResolver = fileResolver
		this.instantiator = instantiator
	}

	void extractDependenciesFrom(Configuration configuration, Set<File> sourcePath, Set<File> resourcePath, Map<String, File> embeds)
	{
		configuration.hierarchy.each { Configuration config ->
			extractDependenciesFromInternal(config, sourcePath, resourcePath, embeds)
		}
	}

	private void extractDependenciesFromInternal(Configuration configuration, Set<File> sourcePath, Set<File> resourcePath, Map<String, File> embeds)
	{
		configuration.allDependencies.each { ModuleDependency dependency ->
			if (dependency instanceof ProjectDependency)
			{
				def projectDependency = dependency as ProjectDependency
				def dependentConfiguration = projectDependency.projectConfiguration
				extractDependenciesFromInternal(dependentConfiguration, sourcePath, resourcePath, embeds)

				dependentConfiguration.allArtifacts.withType(HarPublishArtifact) { HarPublishArtifact artifact ->
					def libName = artifact.name + (artifact.classifier ? "-" + artifact.classifier : "")
					extractFile(libName, artifact.file, sourcePath, resourcePath, embeds)
				}
			}
			else
			{
				configuration.files(dependency).each { File file ->
					extractFile(file.name, file, sourcePath, resourcePath, embeds)
				}
			}
		}
	}

	private void extractFile(String libName, File file, Set<File> sourcePath, Set<File> resourcePath, Map<String, File> embeddedResources)
	{
		def targetPath = project.file("${project.buildDir}/${EXTRACTED_HAXELIBS_DIR}/${libName}")
		project.logger.info("Extracting Haxe library file: {} into {}", file, targetPath)

		def sync = new FileCopyActionImpl(instantiator, fileResolver, new SyncCopySpecVisitor(new FileCopySpecVisitor()));
		def zip = project.zipTree(file)
		sync.from(zip)
		sync.into targetPath
		sync.execute()

		File libraryRoot = targetPath

		Manifest manifest = null
		HaxelibType type = HaxelibType.VERSION_0_X
		zip.visit { FileVisitDetails details ->
			if (details.name == "MANIFEST.MF"
					&& details.relativePath.parent
					&& details.relativePath.parent.getLastName() == "META-INF"
					&& details.relativePath.parent.parent
					&& !details.relativePath.parent.parent.parent)
			{
				manifest = new DefaultManifest(details.file, fileResolver)
				if (manifest.getAttributes().get(HarCopyAction.MANIFEST_ATTR_LIBRARY_VERSION) == "1.0")
				{
					type = HaxelibType.VERSION_1_0
					details.stopVisiting()
				}
			}
			else if ((details.name == "haxelib.json" || details.name == "haxelib.xml")
					&& details.relativePath.parent)
			{
				type = HaxelibType.HAXELIB
				libraryRoot = details.relativePath.parent.getFile(targetPath)
				details.stopVisiting()
			}
		}

		switch (type) {
			case HaxelibType.VERSION_1_0:
				def sources = new File(libraryRoot, "sources")
				def resources = new File(libraryRoot, "resources")
				def embedded = new File(libraryRoot, "embedded")
				if (sources.exists())
				{
					project.logger.debug("Prezi Haxelib 1.0, adding sources at {}", sources)
					sourcePath.add(sources)
				}
				if (resources.exists())
				{
					project.logger.debug("Prezi Haxelib 1.0, adding resources at {}", resources)
					resourcePath.add(resources)
				}
				if (embedded.exists())
				{
					project.logger.debug("Prezi Haxelib 1.0, adding embedded resources at {}", embedded)
					resourcePath.add(embedded)
					embeddedResources.putAll EmbeddedResourceEncoding.decode(
							(String) manifest.getAt(HarCopyAction.MANIFEST_ATTR_EMBEDDED_RESOURCES),
							embedded)
				}
				break

			case HaxelibType.VERSION_0_X:
				def platformAdded = false
				legacyPlatformPaths.each { String legacyPlatformPath ->
					def platformPath = new File(libraryRoot, legacyPlatformPath)
					if (platformPath.directory)
					{
						project.logger.debug("Prezi Haxelib 0.x, adding platform {} at {}",
								legacyPlatformPath, platformPath)
						sourcePath.add(platformPath)
						platformAdded = true
					}
				}
				if (!platformAdded)
				{
					project.logger.debug("Prezi Haxelib 0.x, adding root at {}", libraryRoot)
					sourcePath.add(libraryRoot)
				}
				break

			case HaxelibType.HAXELIB:
				project.logger.debug("Official Haxelib, adding root at {}", libraryRoot)
				sourcePath.add(libraryRoot)
				break
		}
	}
}

enum HaxelibType {
	/**
	 * Normal library.
	 */
	VERSION_1_0,

	/**
	 * Legacy library built with Haxe plugin 0.x.
	 */
	VERSION_0_X,

	/**
	 * Haxelib downloaded from official Haxe repositories.
	 */
	HAXELIB;
}
