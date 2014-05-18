package com.prezi.haxe.gradle

import org.gradle.api.DomainObjectSet
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.DefaultDomainObjectSet
import org.gradle.language.base.LanguageSourceSet
import org.gradle.language.base.internal.AbstractBuildableModelElement
import org.gradle.language.base.internal.BinaryInternal
import org.gradle.language.base.internal.BinaryNamingScheme

/**
 * Created by lptr on 09/02/14.
 */
abstract class AbstractHaxeBinary extends AbstractBuildableModelElement implements HaxeBinaryBase, BinaryInternal {
	private final source = new DefaultDomainObjectSet<>(LanguageSourceSet.class)
	private final String name
	private final Configuration configuration
	private final BinaryNamingScheme namingScheme
	private final TargetPlatform targetPlatform
	private final Flavor flavor

	HaxeCompile compileTask
	Har sourceHarTask

	protected AbstractHaxeBinary(String parentName, Configuration configuration, TargetPlatform targetPlatform, Flavor flavor) {
		this.namingScheme = new HaxeBinaryNamingScheme(parentName)
		this.name = namingScheme.getLifecycleTaskName()
		this.configuration = configuration
		this.targetPlatform = targetPlatform
		this.flavor = flavor
	}

	@Override
	String getName() {
		return name
	}

	@Override
	String getDisplayName() {
		return namingScheme.description
	}

	@Override
	BinaryNamingScheme getNamingScheme() {
		return namingScheme
	}

	@Override
	public String toString() {
		return namingScheme.description
	}

	@Override
	Configuration getConfiguration() {
		return configuration
	}

	@Override
	DomainObjectSet<LanguageSourceSet> getSource() {
		return source
	}

	@Override
	TargetPlatform getTargetPlatform() {
		return targetPlatform
	}

	@Override
	Flavor getFlavor() {
		return flavor
	}
}
