package com.prezi.gradle

import org.gradle.api.internal.artifacts.publish.AbstractPublishArtifact

public class HarPublishArtifact extends AbstractPublishArtifact {

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
		return "har"
	}

	@Override
	String getType()
	{
		return "har"
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
