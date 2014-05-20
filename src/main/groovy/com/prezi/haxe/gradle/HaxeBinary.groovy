package com.prezi.haxe.gradle

import org.gradle.api.DomainObjectSet
import org.gradle.api.artifacts.Configuration
import org.gradle.language.base.Binary
import org.gradle.language.base.LanguageSourceSet

/**
 * Created by lptr on 08/02/14.
 */
public class HaxeBinary extends AbstractHaxeBinary<HaxeCompile> {
	protected HaxeBinary(String parentName, Configuration configuration, TargetPlatform targetPlatform, Flavor flavor) {
		super(parentName, configuration, targetPlatform, flavor)
	}
}
