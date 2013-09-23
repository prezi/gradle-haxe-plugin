package com.prezi.gradle.haxe

import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.collections.FileTreeAdapter
import org.gradle.api.internal.file.collections.MapFileTree
import org.gradle.api.java.archives.Manifest
import org.gradle.api.java.archives.internal.DefaultManifest
import org.gradle.internal.Factory
import org.gradle.internal.reflect.Instantiator
import org.gradle.util.ConfigureUtil

public class HarCopyAction extends AbstractHarCopyAction {
	public static final String DEFAULT_EXTENSION = 'har'

	private Manifest manifest
	private final CopySpec metaInf
	private final CopySpec sources
	private final CopySpec resources

	HarCopyAction(Instantiator instantiator, FileResolver fileResolver, Factory<File> temporaryDirFactory,
				  File archivePath, FileCollection sources, FileCollection resources)
	{
		super(instantiator, fileResolver, archivePath);
		this.sources = rootSpec.addChild()
				.into('sources')
				.from(sources.files.toArray().reverse())
		this.resources = rootSpec.addChild()
				.into('resources')
				.from(resources.files.toArray().reverse())

		duplicatesStrategy = DuplicatesStrategy.EXCLUDE

		manifest = new DefaultManifest(fileResolver)
		// Add these as separate specs, so they are not affected by the changes to the main spec
		metaInf = rootSpec.addFirst().into('META-INF')
		metaInf.addChild().from {
			MapFileTree manifestSource = new MapFileTree(temporaryDirFactory)
			manifestSource.add('MANIFEST.MF') { OutputStream outstr ->
				Manifest manifest = getManifest() ?: new DefaultManifest(null)
				manifest.attributes.put("Library-Version", "1.0")
				manifest.writeTo(new OutputStreamWriter(outstr))
			}
			return new FileTreeAdapter(manifestSource)
		}
		mainSpec.eachFile { FileCopyDetails details ->
			if (details.path.equalsIgnoreCase('META-INF/MANIFEST.MF'))
			{
				details.exclude()
			}
		}
	}

	/**
	 * Returns the manifest for this HAR archive.
	 * @return The manifest
	 */
	public Manifest getManifest()
	{
		return manifest
	}

	/**
	 * Sets the manifest for this HAR archive.
	 *
	 * @param manifest The manifest. May be null.
	 */
	public void setManifest(Manifest manifest)
	{
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
	public HarCopyAction manifest(Closure configureClosure)
	{
		if (getManifest() == null)
		{
			manifest = new DefaultManifest(project.fileResolver)
		}
		ConfigureUtil.configure(configureClosure, getManifest())
		return this
	}

	public CopySpec getMetaInf()
	{
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
	public CopySpec metaInf(Closure configureClosure)
	{
		return ConfigureUtil.configure(configureClosure, getMetaInf())
	}

	public CopySpec getSources()
	{
		return sources.addChild()
	}

	public CopySpec sources(Closure configureClosure)
	{
		return ConfigureUtil.configure(configureClosure, getSources())
	}

	public CopySpec getResources()
	{
		return resources.addChild()
	}

	public CopySpec resources(Closure configureClosure)
	{
		return ConfigureUtil.configure(configureClosure, getResources())
	}
}
