/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.prezi.gradle

import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.Usage

/**
 * A SoftwareComponent representing a Haxe library.
 */
public class HaxeLibrary implements SoftwareComponentInternal {
	private final Usage runtimeUsage = new RuntimeUsage()
	private final Set<PublishArtifact> artifacts = new LinkedHashSet<PublishArtifact>()
	private final DependencySet runtimeDependencies

	public HaxeLibrary(DependencySet runtimeDependencies)
	{
		this.runtimeDependencies = runtimeDependencies
	}

	public String getName()
	{
		return "haxe"
	}

	public void addArtifact(PublishArtifact artifact)
	{
		artifacts.add(artifact);
	}

	public Set<Usage> getUsages()
	{
		return Collections.singleton(runtimeUsage)
	}

	private class RuntimeUsage implements Usage {
		public String getName()
		{
			return "runtime"
		}

		public Set<PublishArtifact> getArtifacts()
		{
			return artifacts
		}

		public Set<ModuleDependency> getDependencies()
		{
			return runtimeDependencies.withType(ModuleDependency.class)
		}
	}
}
