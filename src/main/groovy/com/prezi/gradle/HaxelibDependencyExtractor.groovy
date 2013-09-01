package com.prezi.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.copy.FileCopyActionImpl
import org.gradle.api.internal.file.copy.FileCopySpecVisitor
import org.gradle.api.internal.file.copy.SyncCopySpecVisitor
import org.gradle.internal.reflect.Instantiator

class HaxelibDependencyExtractor {
	static final String EXTRACTED_HAXELIBS_DIR = "haxelibs"

	private final Project project
	private final Instantiator instantiator
	private final FileResolver fileResolver

	HaxelibDependencyExtractor(Project project, Instantiator instantiator, FileResolver fileResolver)
	{
		this.fileResolver = fileResolver
		this.instantiator = instantiator
		this.project = project
	}

	void extractDependenciesFrom(Configuration configuration, Set<File> sourcePath, Set<File> resourcePath)
	{
		configuration.hierarchy.each { Configuration config ->
			extractDependenciesFromInternal(config, sourcePath, resourcePath)
		}
	}

	private void extractDependenciesFromInternal(Configuration configuration, Set<File> sourcePath, Set<File> resourcePath)
	{
		configuration.dependencies.each { ModuleDependency dependency ->
			if (dependency instanceof ProjectDependency)
			{
				def projectDependency = dependency as ProjectDependency
				def dependentConfiguration = projectDependency.projectConfiguration
				extractDependenciesFromInternal(dependentConfiguration, sourcePath, resourcePath)

				dependentConfiguration.allArtifacts.withType(HarPublishArtifact) { HarPublishArtifact artifact ->
					def libName = artifact.name + (artifact.classifier == null ? "" : "-" + artifact.classifier)
					extractFile(libName, artifact.file, false, sourcePath, resourcePath)
				}
			}
			else
			{
				configuration.files(dependency).each { File file ->
					extractFile(dependency.name, file, dependency.group == "haxelib", sourcePath, resourcePath)
				}
			}
		}
	}

	private void extractFile(String libName, File file, boolean legacyHaxelib, Set<File> sourcePath, Set<File> resourcePath)
	{
		def targetPath = project.file("${project.buildDir}/${EXTRACTED_HAXELIBS_DIR}/${libName}")
		println "Extracting Haxe library file: " + file

		def copy = new FileCopyActionImpl(instantiator, fileResolver, new SyncCopySpecVisitor(new FileCopySpecVisitor()));
		copy.from(project.zipTree(file))
		copy.into targetPath
		copy.execute()

		// TODO Determine this based on the manifest
		if (legacyHaxelib)
		{
			sourcePath.add(targetPath)
		}
		else
		{
			def sources = new File(targetPath, "sources")
			def resources = new File(targetPath, "resources")
			if (sources.exists()) sourcePath.add(sources)
			if (resources.exists()) resourcePath.add(resources)
		}
	}
}
