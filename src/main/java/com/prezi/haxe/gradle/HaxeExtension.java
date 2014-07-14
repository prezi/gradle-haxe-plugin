package com.prezi.haxe.gradle;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.Project;

import javax.inject.Inject;
import java.io.Serializable;

public class HaxeExtension extends DefaultHaxeCompilerParameters implements Serializable {
	private final NamedDomainObjectContainer<TargetPlatform> targetPlatforms;

	@Inject
	public HaxeExtension(final Project project) {
		this.targetPlatforms = project.container(TargetPlatform.class, new TargetPlatformNamedDomainObjectFactory(project));
	}

	public NamedDomainObjectContainer<TargetPlatform> getTargetPlatforms() {
		return targetPlatforms;
	}

	public void targetPlatforms(Action<? super NamedDomainObjectContainer<TargetPlatform>> action) {
		action.execute(targetPlatforms);
	}

	private static class TargetPlatformNamedDomainObjectFactory implements NamedDomainObjectFactory<TargetPlatform>, Serializable {
		private final Project project;

		public TargetPlatformNamedDomainObjectFactory(Project project) {
			this.project = project;
		}

		@Override
		public TargetPlatform create(String platformName) {
			return new DefaultTargetPlatform(platformName, project);
		}
	}
}
