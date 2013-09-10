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

	void extractDependenciesFrom(Configuration configuration, Set<File> sourcePath, Set<File> resourcePath)
	{
		configuration.hierarchy.each { Configuration config ->
			extractDependenciesFromInternal(config, sourcePath, resourcePath)
		}
	}

	private void extractDependenciesFromInternal(Configuration configuration, Set<File> sourcePath, Set<File> resourcePath)
	{
		configuration.allDependencies.each { ModuleDependency dependency ->
			if (dependency instanceof ProjectDependency)
			{
				def projectDependency = dependency as ProjectDependency
				def dependentConfiguration = projectDependency.projectConfiguration
				extractDependenciesFromInternal(dependentConfiguration, sourcePath, resourcePath)

				dependentConfiguration.allArtifacts.withType(HarPublishArtifact) { HarPublishArtifact artifact ->
					def libName = artifact.name + (artifact.classifier ? "-" + artifact.classifier : "")
					extractFile(libName, artifact.file, sourcePath, resourcePath)
				}
			}
			else
			{
				configuration.files(dependency).each { File file ->
					extractFile(file.name, file, sourcePath, resourcePath)
				}
			}
		}
	}

	private void extractFile(String libName, File file, Set<File> sourcePath, Set<File> resourcePath)
	{
		def targetPath = project.file("${project.buildDir}/${EXTRACTED_HAXELIBS_DIR}/${libName}")
		project.logger.info("Extracting Haxe library file: {} into {}", file, targetPath)

		def sync = new FileCopyActionImpl(instantiator, fileResolver, new SyncCopySpecVisitor(new FileCopySpecVisitor()));
		def zip = project.zipTree(file)
		sync.from(zip)
		sync.into targetPath
		sync.execute()

		HaxelibType type = HaxelibType.VERSION_0_X
		zip.visit { FileVisitDetails details ->
			if (details.name == "MANIFEST.MF"
					&& details.relativePath.parent
					&& details.relativePath.parent.getLastName() == "META-INF"
					&& details.relativePath.parent.parent
					&& !details.relativePath.parent.parent.parent)
			{
				def manifest = new DefaultManifest(details.file, fileResolver)
				if (manifest.getAttributes().get("Library-Version") == "1.0")
				{
					type = HaxelibType.VERSION_1_0
					details.stopVisiting()
				}
			}
			else if (details.name == "haxelib.xml"
					&& details.relativePath.parent
					&& !details.relativePath.parent.parent)
			{
				type = HaxelibType.HAXELIB
				details.stopVisiting()
			}
		}

		switch (type) {
			case HaxelibType.VERSION_1_0:
				def sources = new File(targetPath, "sources")
				def resources = new File(targetPath, "resources")
				if (sources.exists()) sourcePath.add(sources)
				if (resources.exists()) resourcePath.add(resources)
				break

			case HaxelibType.VERSION_0_X:
				legacyPlatformPaths.each { String legacyPlatformPath ->
					def platformPath = new File(targetPath, legacyPlatformPath)
					if (platformPath.directory)
					{
						sourcePath.add(platformPath)
					}
				}
				break

			case HaxelibType.HAXELIB:
				sourcePath.add(targetPath)
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
