package com.prezi.gradle.haxe

import org.gradle.api.DomainObjectCollection
import org.gradle.api.internal.DefaultDomainObjectSet
import org.gradle.language.base.LanguageSourceSet
import org.gradle.language.base.internal.AbstractBuildableModelElement
import org.gradle.language.base.internal.BinaryInternal
import org.gradle.language.base.internal.BinaryNamingScheme

/**
 * Created by lptr on 08/02/14.
 */
class DefaultHaxeBinary extends AbstractBuildableModelElement implements HaxeBinary, BinaryInternal {
	private final source = new DefaultDomainObjectSet<>(LanguageSourceSet.class)
	private final String name
	private final BinaryNamingScheme namingScheme
	private final TargetPlatform targetPlatform

	public DefaultHaxeBinary(String name, TargetPlatform targetPlatform) {
		this.name = name
		this.targetPlatform = targetPlatform
		this.namingScheme = new HaxeBinaryNamingScheme(name)
	}

	@Override
	String getName() {
		return name
	}

	@Override
	DomainObjectCollection<LanguageSourceSet> getSource() {
		return source
	}

	@Override
	TargetPlatform getTargetPlatform() {
		return targetPlatform
	}

	@Override
	BinaryNamingScheme getNamingScheme() {
		return namingScheme
	}

	@Override
	public String toString() {
		return namingScheme.getDescription();
	}
}
