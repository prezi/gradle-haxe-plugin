package com.prezi.haxe.gradle

import org.gradle.api.DomainObjectSet
import org.gradle.api.artifacts.Configuration
import org.gradle.language.base.Binary
import org.gradle.language.base.LanguageSourceSet

/**
 * Created by lptr on 08/02/14.
 */
public interface HaxeBinaryBase extends Binary {
	DomainObjectSet<LanguageSourceSet> getSource()
	Configuration getConfiguration()
	TargetPlatform getTargetPlatform()
	Flavor getFlavor()
	HaxeCompile getCompileTask()
	void setCompileTask(HaxeCompile compileTask)
	Har getSourceHarTask()
	void setSourceHarTask(Har compileTask)
}
