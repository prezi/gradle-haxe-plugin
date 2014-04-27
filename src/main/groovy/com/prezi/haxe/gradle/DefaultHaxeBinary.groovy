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
class DefaultHaxeBinary extends AbstractBuildableModelElement implements HaxeBinary, BinaryInternal {
	private final source = new DefaultDomainObjectSet<>(LanguageSourceSet.class)
	private final testSource = new DefaultDomainObjectSet<>(LanguageSourceSet.class)
	private final String name
	private final Configuration configuration
	private final Configuration testConfiguration
	private final BinaryNamingScheme namingScheme
	private final TargetPlatform targetPlatform
	private final Flavor flavor

	HaxeCompile compileTask
	Har sourceHarTask

	protected DefaultHaxeBinary(String parentName, Configuration configuration, Configuration testConfiguration, TargetPlatform targetPlatform, Flavor flavor) {
		this.namingScheme = new HaxeBinaryNamingScheme(parentName)
		this.name = namingScheme.getLifecycleTaskName()
		this.configuration = configuration
		this.testConfiguration = testConfiguration
		this.targetPlatform = targetPlatform
		this.flavor = flavor
	}

	@Override
	String getName() {
		return name
	}

	@Override
	Configuration getConfiguration() {
		return configuration
	}

	@Override
	Configuration getTestConfiguration() {
		return testConfiguration
	}

	@Override
	String getDisplayName() {
		return namingScheme.description
	}

	@Override
	DomainObjectSet<LanguageSourceSet> getSource() {
		return source
	}

	@Override
	DomainObjectSet<LanguageSourceSet> getTestSource() {
		return testSource
	}

	@Override
	TargetPlatform getTargetPlatform() {
		return targetPlatform
	}

	@Override
	Flavor getFlavor() {
		return flavor
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
