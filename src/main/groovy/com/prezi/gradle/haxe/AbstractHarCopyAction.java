package com.prezi.gradle.haxe;

import groovy.lang.Closure;
import org.gradle.api.file.CopySpec;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.file.archive.ZipCopyAction;
import org.gradle.api.internal.file.archive.ZipCopySpecVisitor;
import org.gradle.api.internal.file.copy.CopyActionImpl;
import org.gradle.api.internal.file.copy.ZipCompressor;
import org.gradle.api.internal.file.copy.ZipDeflatedCompressor;
import org.gradle.internal.reflect.Instantiator;

import java.io.File;

public class AbstractHarCopyAction extends CopyActionImpl implements ZipCopyAction {
	private final File archivePath;

	public AbstractHarCopyAction(Instantiator instantiator, FileResolver resolver, File archivePath)
	{
		super(instantiator, resolver, new ZipCopySpecVisitor());
		this.archivePath = archivePath;
	}

	@Override
	public File getArchivePath()
	{
		return archivePath;
	}

	@Override
	public ZipCompressor getCompressor()
	{
		return ZipDeflatedCompressor.INSTANCE;
	}

	// These are needed because Groovy complains otherwise
	@Override
	public CopySpec from(Object... sourcePaths)
	{
		return super.from(sourcePaths);
	}

	@Override
	public CopySpec from(Object sourcePath, Closure c)
	{
		return super.from(sourcePath, c);
	}
}
