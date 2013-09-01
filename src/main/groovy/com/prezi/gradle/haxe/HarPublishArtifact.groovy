package com.prezi.gradle.haxe

import org.gradle.api.internal.artifacts.publish.AbstractPublishArtifact

public class HarPublishArtifact extends AbstractPublishArtifact {

	public static final DEFAULT_TYPE = "har"

	private final HaxeTask task
	private final File archiveFile

	HarPublishArtifact(HaxeTask task, File archiveFile)
	{
		super(task)
		this.task = task
		this.archiveFile = archiveFile
	}

	@Override
	String getName()
	{
		return task.getBaseName()
	}

	@Override
	String getExtension()
	{
		return HarCopyAction.DEFAULT_EXTENSION
	}

	@Override
	String getType()
	{
		return DEFAULT_TYPE
	}

	@Override
	String getClassifier()
	{
		return task.getClassifier()
	}

	@Override
	File getFile()
	{
		return archiveFile
	}

	@Override
	Date getDate()
	{
		return new Date(archiveFile.lastModified())
	}
}
