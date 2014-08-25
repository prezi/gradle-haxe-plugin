package com.prezi.haxe.gradle;

import com.google.common.collect.Sets;
import com.prezi.haxe.gradle.incubating.BinaryContainer;
import com.prezi.haxe.gradle.incubating.BinaryInternal;
import com.prezi.haxe.gradle.incubating.DefaultBinaryContainer;
import com.prezi.haxe.gradle.incubating.DefaultProjectSourceSet;
import com.prezi.haxe.gradle.incubating.ProjectSourceSet;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.internal.reflect.Instantiator;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Set;

public class HaxeExtension extends DefaultHaxeCompilerParameters implements Serializable {
	private final NamedDomainObjectContainer<TargetPlatform> targetPlatforms;

	private final ProjectSourceSet sources;
	private final BinaryContainer binaries;
	private final Set<Object> compilerVersions = Sets.newLinkedHashSet();

	public HaxeExtension(final Project project, Instantiator instantiator) {
		this.sources = instantiator.newInstance(DefaultProjectSourceSet.class, instantiator);
		this.binaries = instantiator.newInstance(DefaultBinaryContainer.class, instantiator);
		this.targetPlatforms = project.container(TargetPlatform.class, new TargetPlatformNamedDomainObjectFactory(project));

		binaries.withType(BinaryInternal.class).all(new Action<BinaryInternal>() {
			public void execute(BinaryInternal binary) {
				Task binaryLifecycleTask = project.task(binary.getNamingScheme().getLifecycleTaskName());
				binaryLifecycleTask.setGroup("build");
				binaryLifecycleTask.setDescription(String.format("Assembles %s.", binary));
				binary.setBuildTask(binaryLifecycleTask);
			}
		});
	}

	public ProjectSourceSet getSources() {
		return sources;
	}

	public void sources(Action<ProjectSourceSet> action) {
		action.execute(sources);
	}

	public BinaryContainer getBinaries() {
		return binaries;
	}

	public void binaries(Action<BinaryContainer> action) {
		action.execute(binaries);
	}

	public NamedDomainObjectContainer<TargetPlatform> getTargetPlatforms() {
		return targetPlatforms;
	}

	public void targetPlatforms(Action<? super NamedDomainObjectContainer<TargetPlatform>> action) {
		action.execute(targetPlatforms);
	}

	public void setCompilerVersions(Object... versions) {
		compilerVersions.addAll(Arrays.asList(versions));
	}

	public void setCompilerVersion(Object... versions) {
		setCompilerVersions(versions);
	}

	public void compilerVersions(Object... versions) {
		setCompilerVersions(versions);
	}

	public void compilerVersion(Object... versions) {
		setCompilerVersions(versions);
	}

	public Set<Object> getCompilerVersions() {
		return compilerVersions;
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
