package com.prezi.gradle.haxe

import org.gradle.api.DomainObjectCollection
import org.gradle.api.artifacts.Configuration
import org.gradle.language.base.Binary
import org.gradle.language.base.LanguageSourceSet

/**
 * Created by lptr on 08/02/14.
 */
public interface HaxeBinary extends Binary {
	DomainObjectCollection<LanguageSourceSet> getSource()
	Configuration getConfiguration()
	TargetPlatform getTargetPlatform()
	Flavor getFlavor()
}
