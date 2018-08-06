package com.prezi.haxe.gradle;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.internal.file.FileOperations;
import org.gradle.api.internal.project.ProjectInternal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

public class HaxelibDependencyExtractor {
	public HaxelibDependencyExtractor(Project project) {
		this.project = project;
	}

	public void extractDependenciesFrom(FileCollection classPath, Set<File> sourcePath, Set<File> resourcePath, Map<String, File> embeds) {
		if (classPath instanceof Configuration) {
			for (Configuration config : ((Configuration) classPath).getHierarchy()) {
				extractDependenciesFromInternal(config, sourcePath, resourcePath, embeds);
			}
		} else {
			for (File file : classPath.getFiles()) {
				extractFile(file.getName(), file, sourcePath, resourcePath, embeds);
			}
		}
	}

	private void extractDependenciesFromInternal(Configuration configuration, Set<File> sourcePath, Set<File> resourcePath, Map<String, File> embeds) {
		for (Dependency dependency : configuration.getAllDependencies()) {
			if (dependency instanceof ProjectDependency) {
				ProjectDependency projectDependency = (ProjectDependency) dependency;
				Configuration dependentConfiguration = projectDependency.getDependencyProject().getConfigurations().getByName(projectDependency.getTargetConfiguration());
				extractDependenciesFromInternal(dependentConfiguration, sourcePath, resourcePath, embeds);

				Iterable<PublishArtifact> harArtifacts = Iterables.filter(dependentConfiguration.getAllArtifacts(), new Predicate<PublishArtifact>() {
					@Override
					public boolean apply(PublishArtifact artifact) {
						return artifact.getType().equals(Har.DEFAULT_EXTENSION);
					}
				});
				for (PublishArtifact artifact : harArtifacts) {
					String libName = artifact.getName() + (!Strings.isNullOrEmpty(artifact.getClassifier()) ? "-" + artifact.getClassifier() : "");
					extractFile(libName, artifact.getFile(), sourcePath, resourcePath, embeds);
				}
			} else {
				for (File file : configuration.files(dependency)) {
					extractFile(file.getName(), file, sourcePath, resourcePath, embeds);
				}
			}
		}
	}

	private void extractFile(final String libName, final File file, Set<File> sourcePath, Set<File> resourcePath, Map<String, File> embeddedResources) {
		final File targetPath = project.file(String.valueOf(project.getBuildDir()) + "/" + EXTRACTED_HAXELIBS_DIR + "/" + libName);
		project.getLogger().debug("Extracting Haxe library file: {} into {}", file, targetPath);

		final FileTree zip = project.zipTree(file);
		((ProjectInternal) project).getServices().get(FileOperations.class).sync(new Action<CopySpec>() {
			@Override
			public void execute(CopySpec copySpec) {
				copySpec.from(zip);
				copySpec.into(targetPath);
			}
		});

		HaxelibTypeDetector typeDetector = new HaxelibTypeDetector(targetPath);
		zip.visit(typeDetector);
		final File libraryRoot = typeDetector.getLibraryRoot();
		final Manifest manifest = typeDetector.getManifest();
		final HaxelibType type = typeDetector.getType();

		if (type == null) {
			project.getLogger().warn("Unsupported library type: " + file);
		} else {
			switch (type) {
				case VERSION_1_0:
					File sources = new File(libraryRoot, "sources");
					File resources = new File(libraryRoot, "resources");
					File embedded = new File(libraryRoot, "embedded");
					if (sources.exists()) {
						project.getLogger().debug("Prezi Haxelib 1.0, adding sources at {}", sources);
						sourcePath.add(sources);
					}

					if (resources.exists()) {
						project.getLogger().debug("Prezi Haxelib 1.0, adding resources at {}", resources);
						resourcePath.add(resources);
					}

					if (embedded.exists()) {
						project.getLogger().debug("Prezi Haxelib 1.0, adding embedded resources at {}", embedded);
						resourcePath.add(embedded);
						embeddedResources.putAll(EmbeddedResourceEncoding.decode(manifest.getMainAttributes().getValue(Har.MANIFEST_ATTR_EMBEDDED_RESOURCES), embedded));
					}

					break;
				case HAXELIB:
					project.getLogger().debug("Official Haxelib, adding root at {}", libraryRoot);
					sourcePath.add(libraryRoot);
					break;
			}
		}

	}

	private static final String EXTRACTED_HAXELIBS_DIR = "haxelibs";
	private final Project project;

	private static class HaxelibTypeDetector implements FileVisitor {
		private File libraryRoot;
		private Manifest manifest;
		private HaxelibType type;

		public HaxelibTypeDetector(File targetPath) {
			this.libraryRoot = targetPath;
		}

		@Override
		public void visitDir(FileVisitDetails details) {
		}

		@Override
		public void visitFile(FileVisitDetails details) {
			String path = details.getPath();
			if (path.startsWith("/")) {
				path = path.substring(1);
			}

			if (path.equals("META-INF/MANIFEST.MF")) {
				try {
					FileInputStream fis = new FileInputStream(details.getFile());
					try {
						manifest = new Manifest(fis);
						if ("1.0".equals(manifest.getMainAttributes().getValue(Har.MANIFEST_ATTR_LIBRARY_VERSION))) {
							type = HaxelibType.VERSION_1_0;
							details.stopVisiting();
						}
					} finally {
						fis.close();
					}
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			} else if ((details.getName().equals("haxelib.json") || details.getName().equals("haxelib.xml")) && details.getRelativePath().getParent() != null) {
				type = HaxelibType.HAXELIB;
				libraryRoot = details.getRelativePath().getParent().getFile(libraryRoot);
				details.stopVisiting();
			}
		}

		public File getLibraryRoot() {
			return libraryRoot;
		}

		public Manifest getManifest() {
			return manifest;
		}

		public HaxelibType getType() {
			return type;
		}
	}
}
