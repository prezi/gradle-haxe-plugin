package com.prezi.haxe.gradle;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.gradle.api.Project;
import org.gradle.language.base.LanguageSourceSet;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HaxeCommandBuilder {
	private final HaxelibDependencyExtractor extractor;
	private final List<String> cmd;

	public HaxeCommandBuilder(Project project, String... cmd) {
		this.extractor = new HaxelibDependencyExtractor(project);
		this.cmd = Lists.newArrayList(Arrays.asList(cmd));
	}

	public List<String> build() {
		return cmd;
	}

	public void append(Object... what) {
		for (Object it : what) {
			cmd.add(String.valueOf(it));
		}
	}

	public HaxeCommandBuilder withMain(String main) {
		if (!Strings.isNullOrEmpty(main)) {
			append("-main", main);
		}

		return this;
	}

	public HaxeCommandBuilder withTarget(String target, File output) {
		append("-" + target, output);
		if (target.equals("swf")) {
			append("-swf-version", 11);
		}

		return this;
	}

	public HaxeCommandBuilder withIncludes(Iterable<?> inlcudes) {
		for (Object it : inlcudes) {
			append("--macro", "include(\'" + String.valueOf(it) + "\')");
		}

		return this;
	}

	public HaxeCommandBuilder withExcludes(Iterable<?> excludes) {
		for (Object it : excludes) {
			append("--macro", "exclude(\'" + String.valueOf(it) + "\')");
		}

		return this;
	}

	public HaxeCommandBuilder withMacros(Iterable<?> macros) {
		for (Object it : macros) {
			append("--macro", it);
		}

		return this;
	}

	private HaxeCommandBuilder withEmbeddedResources(Map<String, File> embeddedResources) {
		for (Map.Entry<String, File> entry : embeddedResources.entrySet()) {
			final String name = entry.getKey();
			File file = entry.getValue();
			final String filePath = file.getAbsolutePath();
			append("-resource", filePath + "@" + name);
		}

		return this;
	}

	public HaxeCommandBuilder withSources(Iterable<File> sources) {
		for (File it : sources) {
			append("-cp", it);
		}

		return this;
	}

	public HaxeCommandBuilder withSourceSets(Set<LanguageSourceSet> sources, Map<String, File> embeddedResources) {
		Set<File> sourcePath = Sets.newLinkedHashSet();
		Set<File> resourcePath = Sets.newLinkedHashSet();
		Map<String, File> allEmbeddedResources = Maps.newLinkedHashMap();
		allEmbeddedResources.putAll(embeddedResources);

		for (LanguageSourceSet source : sources) {
			if (source instanceof HaxeSourceSet) {
				extractor.extractDependenciesFrom(((HaxeSourceSet) source).getCompileClassPath(), sourcePath, resourcePath, allEmbeddedResources);
			}
		}

		withSources(sourcePath);
		withSources(resourcePath);
		withEmbeddedResources(allEmbeddedResources);
		return this;
	}

	public HaxeCommandBuilder withFlags(Iterable<String> flags) {
		for (String flag : flags) {
			for (String flagPart : flag.split(" ")) {
				append(flagPart);
			}
		}

		return this;
	}

	public HaxeCommandBuilder withDebugFlags(boolean debug) {
		if (debug) {
			withFlags(new ArrayList<String>(Arrays.asList("-D fdb", "-debug")));
		}

		return this;
	}
}
