package com.prezi.haxe.gradle

import org.gradle.api.DomainObjectSet
import org.gradle.api.artifacts.Configuration
import org.gradle.language.base.Binary
import org.gradle.language.base.LanguageSourceSet

/**
 * Created by lptr on 08/02/14.
 */
public interface HaxeBinary extends Binary {
	DomainObjectSet<LanguageSourceSet> getSource()
	DomainObjectSet<LanguageSourceSet> getTestSource()
	Configuration getConfiguration()
	Configuration getTestConfiguration()
	TargetPlatform getTargetPlatform()
	Flavor getFlavor()
	HaxeCompile getCompileTask()
	void setCompileTask(HaxeCompile compileTask)
	Har getSourceHarTask()
	void setSourceHarTask(Har compileTask)
}
