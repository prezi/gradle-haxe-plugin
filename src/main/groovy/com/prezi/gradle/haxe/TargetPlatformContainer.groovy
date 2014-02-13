package com.prezi.gradle.haxe

import org.gradle.api.NamedDomainObjectSet

/**
 * Created by lptr on 09/02/14.
 */
public interface TargetPlatformContainer extends NamedDomainObjectSet<TargetPlatform> {
	HaxeCompileParameters getParams()
}
