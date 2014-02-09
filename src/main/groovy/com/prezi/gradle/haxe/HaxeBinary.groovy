package com.prezi.gradle.haxe

import org.gradle.api.DomainObjectCollection
import org.gradle.language.base.Binary
import org.gradle.language.base.LanguageSourceSet

/**
 * Created by lptr on 08/02/14.
 */
public interface HaxeBinary extends Binary {
	DomainObjectCollection<LanguageSourceSet> getSource()
	TargetPlatform getTargetPlatform()
}
