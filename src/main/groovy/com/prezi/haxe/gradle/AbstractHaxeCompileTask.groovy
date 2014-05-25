package com.prezi.haxe.gradle

import org.gradle.api.DomainObjectSet
import org.gradle.api.internal.ConventionTask
import org.gradle.api.internal.DefaultDomainObjectSet
import org.gradle.api.tasks.InputFiles
import org.gradle.internal.typeconversion.NotationParser
import org.gradle.language.base.LanguageSourceSet
import org.gradle.nativebinaries.internal.SourceSetNotationParser

/**
 * Created by lptr on 10/02/14.
 */
abstract class AbstractHaxeCompileTask extends ConventionTask {

	protected static final NotationParser<Object, Set<LanguageSourceSet>> notationParser = SourceSetNotationParser.parser()

	@Delegate(deprecated = true)
	protected final HaxeCompileParameters params = new HaxeCompileParameters()

	LinkedHashSet<Object> sources = []
	LinkedHashMap<String, File> embeddedResources = [:]
	TargetPlatform targetPlatform
	void targetPlatform(String targetPlatform) {
		this.targetPlatform = project.extensions.getByType(HaxeExtension).targetPlatforms.maybeCreate(targetPlatform)
	}

	public source(Object... sources) {
		this.sources.addAll(sources)
	}

	protected DomainObjectSet<LanguageSourceSet> getSourceSets() {
		def sourceSets = sources.collectMany { notationParser.parseNotation(it) }
		return new DefaultDomainObjectSet<LanguageSourceSet>(LanguageSourceSet, sourceSets)
	}

	protected static LinkedHashSet<File> getAllSourceDirectories(Set<LanguageSourceSet> sources) {
		return (sources*.source*.srcDirs).flatten()
	}

	@InputFiles
	public Set<File> getInputFiles()
	{
		return getAllSourceDirectories(getSourceSets()) + getEmbeddedResources().values()
	}
}
