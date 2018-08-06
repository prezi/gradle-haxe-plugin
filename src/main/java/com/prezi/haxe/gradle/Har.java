package com.prezi.haxe.gradle;

import com.google.common.base.Throwables;
import groovy.lang.Closure;
import org.apache.commons.io.IOUtils;
import org.gradle.api.Action;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.file.copy.CopySpecInternal;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.java.archives.Manifest;
import org.gradle.api.java.archives.internal.DefaultManifest;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.util.ConfigureUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class Har extends Zip {

	public static final String DEFAULT_EXTENSION = "har";
	public static final String MANIFEST_ATTR_LIBRARY_VERSION = "Library-Version";
	public static final String MANIFEST_ATTR_EMBEDDED_RESOURCES = "Embedded-Resources";

	private Manifest manifest;
	private Map<String, File> embeddedResources = new LinkedHashMap<String, File>();
	private final CopySpecInternal metaInf;

	public Har() {
		this.setExtension(DEFAULT_EXTENSION);
		this.manifest = new DefaultManifest(getServices().get(FileResolver.class));
		this.metaInf = getRootSpec().addFirst();
		this.metaInf.into("META-INF");
		this.metaInf.addChild().from(new Callable<File>() {
			@Override
			public File call() throws Exception {
				File manifestFile = new File(getTemporaryDir(), "MANIFEST.MF");
				Manifest manifest = getManifest();
				if (manifest == null) {
					manifest = new DefaultManifest(null);
				}
				manifest.writeTo(manifestFile);
				return manifestFile;
			}
		});
		getMainSpec().eachFile(new Action<FileCopyDetails>() {
			@Override
			public void execute(FileCopyDetails details) {
				if (details.getPath().equalsIgnoreCase("META-INF/MANIFEST.MF")) {
					details.exclude();
				}
			}
		});
	}

	/**
	 * Returns the manifest for this JAR archive.
	 *
	 * @return The manifest
	 */
	public Manifest getManifest() {
		return manifest;
	}

	/**
	 * Sets the manifest for this JAR archive.
	 *
	 * @param manifest The manifest. May be null.
	 */
	public void setManifest(Manifest manifest) {
		this.manifest = manifest;
	}

	/**
	 * Configures the manifest for this JAR archive.
	 *
	 * <p>The given closure is executed to configure the manifest. The {@link org.gradle.api.java.archives.Manifest}
	 * is passed to the closure as its delegate.</p>
	 *
	 * @param configureClosure The closure.
	 * @return This.
	 */
	public Har manifest(Closure configureClosure) {
		if (getManifest() == null) {
			manifest = new DefaultManifest(((ProjectInternal) getProject()).getFileResolver());
		}
		ConfigureUtil.configure(configureClosure, getManifest());
		return this;
	}

	public CopySpec getMetaInf() {
		return metaInf.addChild();
	}

	/**
	 * Adds content to this JAR archive's META-INF directory.
	 * 
	 * <p>The given closure is executed to configure a {@code CopySpec}. The {@link CopySpec} is passed to the closure
	 * as its delegate.</p>
	 *
	 * @param configureClosure The closure.
	 * @return The created {@code CopySpec}
	 */
	public CopySpec metaInf(Closure configureClosure) {
		return ConfigureUtil.configure(configureClosure, getMetaInf());
	}

	@Override
	protected void copy() {
		getManifest().getAttributes().put(MANIFEST_ATTR_LIBRARY_VERSION, "1.0");
		Map<String, File> embeddedResources = getEmbeddedResources();

		if (!embeddedResources.isEmpty()) {
			getManifest().getAttributes().put(MANIFEST_ATTR_EMBEDDED_RESOURCES, EmbeddedResourceEncoding.encode(embeddedResources));
		}

		super.copy();
	}

	public Map<String, File> getEmbeddedResources() {
		return embeddedResources;
	}

	public void setEmbeddedResources(Map<String, File> embeddedResources) {
		this.embeddedResources = embeddedResources;
	}
}
