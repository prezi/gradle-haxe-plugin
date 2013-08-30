package com.prezi.gradle

import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.collections.FileTreeAdapter
import org.gradle.api.internal.file.collections.MapFileTree
import org.gradle.api.internal.file.copy.CopySpecImpl
import org.gradle.api.java.archives.Manifest
import org.gradle.api.java.archives.internal.DefaultManifest
import org.gradle.api.tasks.bundling.Zip
import org.gradle.util.ConfigureUtil

public class Har extends Zip {
	public static final String DEFAULT_EXTENSION = 'har'

	public HaxeBuild build

	private Manifest manifest
	private final CopySpecImpl metaInf
	private final CopySpecImpl sources
	private final CopySpecImpl resources

	Har() {
		extension = DEFAULT_EXTENSION
		manifest = new DefaultManifest(getServices().get(FileResolver))
		// Add these as separate specs, so they are not affected by the changes to the main spec
		metaInf = copyAction.rootSpec.addFirst().into('META-INF')
		metaInf.addChild().from {
			MapFileTree manifestSource = new MapFileTree(temporaryDirFactory)
			manifestSource.add('MANIFEST.MF') {OutputStream outstr ->
				Manifest manifest = getManifest() ?: new DefaultManifest(null)
				manifest.writeTo(new OutputStreamWriter(outstr))
			}
			return new FileTreeAdapter(manifestSource)
		}
		copyAction.mainSpec.eachFile { FileCopyDetails details ->
			if (details.path.equalsIgnoreCase('META-INF/MANIFEST.MF')) {
				details.exclude()
			}
		}
		sources = copyAction.rootSpec.addChild().into('sources')
		resources = copyAction.rootSpec.addChild().into('resources')
	}

	@Override
	protected void copy()
	{
		sources.from(build.sources)
		resources.from(build.resources)
		super.copy()
	}

	/**
	 * Returns the manifest for this HAR archive.
	 * @return The manifest
	 */
	public Manifest getManifest() {
		return manifest
	}

	/**
	 * Sets the manifest for this HAR archive.
	 *
	 * @param manifest The manifest. May be null.
	 */
	public void setManifest(Manifest manifest) {
		this.manifest = manifest
	}

	/**
	 * Configures the manifest for this HAR archive.
	 *
	 * <p>The given closure is executed to configure the manifest. The {@link org.gradle.api.java.archives.Manifest}
	 * is passed to the closure as its delegate.</p>
	 *
	 * @param configureClosure The closure.
	 * @return This.
	 */
	public Har manifest(Closure configureClosure) {
		if (getManifest() == null) {
			manifest = new DefaultManifest(project.fileResolver)
		}
		ConfigureUtil.configure(configureClosure, getManifest())
		return this
	}

	public CopySpec getMetaInf() {
		return metaInf.addChild()
	}

	/**
	 * Adds content to this HAR archive's META-INF directory.
	 *
	 * <p>The given closure is executed to configure a {@code CopySpec}. The {@link CopySpec} is passed to the closure
	 * as its delegate.</p>
	 *
	 * @param configureClosure The closure.
	 * @return The created {@code CopySpec}
	 */
	public CopySpec metaInf(Closure configureClosure) {
		return ConfigureUtil.configure(configureClosure, getMetaInf())
	}

	public CopySpec getSources() {
		return sources.addChild()
	}

	public CopySpec sources(Closure configureClosure) {
		return ConfigureUtil.configure(configureClosure, getSources())
	}

	public CopySpec getResources() {
		return resources.addChild()
	}

	public CopySpec resources(Closure configureClosure) {
		return ConfigureUtil.configure(configureClosure, getResources())
	}
}
