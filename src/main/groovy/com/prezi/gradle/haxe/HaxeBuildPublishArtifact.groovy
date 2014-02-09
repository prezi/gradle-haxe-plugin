package com.prezi.gradle.haxe

import org.gradle.api.internal.artifacts.publish.AbstractPublishArtifact

class HaxeBuildPublishArtifact extends AbstractPublishArtifact {

	private final HaxeCompile task

	HaxeBuildPublishArtifact(HaxeCompile task)
	{
		super(task)
		this.task = task
	}

	@Override
	String getName()
	{
		return task.baseName
	}

	@Override
	String getExtension()
	{
		def fileName = task.outputFile.name
		def extension = fileName.substring(fileName.lastIndexOf('.') + 1)
		return extension
	}

	@Override
	String getType()
	{
		return getExtension()
	}

	@Override
	String getClassifier()
	{
		return task.classifier
	}

	@Override
	File getFile()
	{
		return task.outputFile
	}

	@Override
	Date getDate()
	{
		return new Date(task.outputFile.lastModified())
	}
}
