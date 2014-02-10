package com.prezi.gradle.haxe

import org.gradle.api.DefaultTask
import org.gradle.api.DomainObjectSet
import org.gradle.api.internal.DefaultDomainObjectSet
import org.gradle.api.tasks.InputFiles
import org.gradle.language.base.LanguageSourceSet
import org.gradle.nativebinaries.internal.SourceSetNotationParser

/**
 * Created by lptr on 10/02/14.
 */
abstract class AbstractHaxeCompileTask extends DefaultTask {

	protected static final notationParser = SourceSetNotationParser.parser()
	protected final HaxelibDependencyExtractor extractor = new HaxelibDependencyExtractor(project)

	final DomainObjectSet<LanguageSourceSet> sources = new DefaultDomainObjectSet<>(LanguageSourceSet)
	LinkedHashMap<String, File> embeddedResources = [:]
	TargetPlatform targetPlatform

	public source(Object... sources) {
		sources.each { source ->
			this.sources.addAll(notationParser.parseNotation(source))
		}
	}

	protected void withSourceSets(HaxeCommandBuilder builder, DomainObjectSet<LanguageSourceSet> sources) {
		LinkedHashSet<File> sourcePath = []
		LinkedHashSet<File> resourcePath = []
		LinkedHashMap<String, File> allEmbeddedResources = [:]
		sources.withType(HaxeSourceSet) { source ->
			extractor.extractDependenciesFrom(source.compileClassPath, sourcePath, resourcePath, allEmbeddedResources)
		}

		builder.withSources(sourcePath)
		builder.withSources(resourcePath)
		builder.withEmbeddedResources(allEmbeddedResources)
	}

	protected static Set<File> getAllSourceDirectories(Set<LanguageSourceSet> sources) {
		return (sources*.source*.srcDirs).flatten()
	}

	@InputFiles
	public Set<File> getInputFiles()
	{
		return getAllSourceDirectories(sources) + getEmbeddedResources().values()
	}
}
