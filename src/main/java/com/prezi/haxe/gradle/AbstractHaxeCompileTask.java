package com.prezi.haxe.gradle;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.prezi.haxe.gradle.incubating.LanguageSourceSet;
import com.prezi.haxe.gradle.incubating.SourceSetNotationParser;
import com.prezi.haxe.gradle.incubating.typeconversion.NotationParser;
import org.gradle.api.DomainObjectSet;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.internal.DefaultDomainObjectSet;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

public abstract class AbstractHaxeCompileTask extends ConventionTask implements HaxeCompilerParameters {

	protected static final NotationParser<Object, Set<LanguageSourceSet>> notationParser = SourceSetNotationParser.parser();
	protected final HaxeCompilerParameters params = new DefaultHaxeCompilerParameters();
	private Set<Object> sources = Sets.newLinkedHashSet();
	private Map<String, File> embeddedResources = Maps.newLinkedHashMap();
	private String targetPlatform;

	public void source(Object... sources) {
		this.sources.addAll(Arrays.asList(sources));
	}

	@Internal
	protected DomainObjectSet<LanguageSourceSet> getSourceSets() {
		DomainObjectSet<LanguageSourceSet> result = new DefaultDomainObjectSet<LanguageSourceSet>(LanguageSourceSet.class);
		for (Object source : sources) {
			result.addAll(notationParser.parseNotation(source));
		}
		return result;
	}

	protected static Set<File> getAllSourceDirectories(Set<LanguageSourceSet> sources) {
		Set<File> result = Sets.newLinkedHashSet();
		for (LanguageSourceSet sourceSet : sources) {
			result.addAll(sourceSet.getSource().getSrcDirs());
		}
		return result;
	}

	@InputFiles
	public Set<File> getInputFiles() {
		return Sets.newLinkedHashSet(Iterables.concat(getAllSourceDirectories(getSourceSets()), getEmbeddedResources().values()));
	}

	public void setConventionMapping(final HaxeCompilerParameters... params) {
		final Iterable<HaxeCompilerParameters> nonNullParams = Iterables.filter(Arrays.asList(params), new Predicate<HaxeCompilerParameters>() {
			@Override
			public boolean apply(HaxeCompilerParameters param) {
				return param != null;
			}
		});
		getConventionMapping().map("main", new Callable<String>() {
			@Override
			public String call() throws Exception {
				for (HaxeCompilerParameters param : nonNullParams) {
					if (!Strings.isNullOrEmpty(param.getMain())) {
						return param.getMain();
					}
				}
				return null;
			}
		});
		getConventionMapping().map("macros", new Callable<List<String>>() {
			@Override
			public List<String> call() throws Exception {
				List<String> result = Lists.newArrayList();
				for (HaxeCompilerParameters param : nonNullParams) {
					result.addAll(param.getMacros());
				}
				return result;
			}
		});
		getConventionMapping().map("includes", new Callable<Set<String>>() {
			@Override
			public Set<String> call() throws Exception {
				Set<String> result = Sets.newLinkedHashSet();
				for (HaxeCompilerParameters param : nonNullParams) {
					result.addAll(param.getIncludes());
				}
				return result;
			}
		});
		getConventionMapping().map("excludes", new Callable<Set<String>>() {
			@Override
			public Set<String> call() throws Exception {
				Set<String> result = Sets.newLinkedHashSet();
				for (HaxeCompilerParameters param : nonNullParams) {
					result.addAll(param.getExcludes());
				}
				return result;
			}
		});
		getConventionMapping().map("flagList", new Callable<List<String>>() {
			@Override
			public List<String> call() throws Exception {
				List<String> result = Lists.newArrayList();
				for (HaxeCompilerParameters param : nonNullParams) {
					result.addAll(param.getFlagList());
				}
				return result;
			}
		});
		getConventionMapping().map("debug", new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				boolean debug = false;
				for (HaxeCompilerParameters param : nonNullParams) {
					if (param.isDebug()) {
						debug = true;
						break;
					}
				}
				return debug;
			}
		});
	}

	@Input
	@Optional
	@Override
	public String getMain() {
		return params.getMain();
	}

	@Override
	public void main(String main) {
		params.main(main);
	}

	@Input
	@Override
	public List<String> getMacros() {
		return params.getMacros();
	}

	@Override
	public void macro(String macro) {
		params.macro(macro);
	}

	@Input
	@Override
	public Set<String> getIncludes() {
		return params.getIncludes();
	}

	@Override
	public void include(String include) {
		params.include(include);
	}

	@Input
	@Override
	public Set<String> getExcludes() {
		return params.getExcludes();
	}

	@Override
	public void exclude(String exclude) {
		params.exclude(exclude);
	}

	@Input
	@Override
	public List<String> getFlagList() {
		return params.getFlagList();
	}

	@Override
	public void flag(String... flag) {
		params.flag(flag);
	}

	@Input
	@Override
	public boolean isDebug() {
		return params.isDebug();
	}
	@Override
	public void debug(boolean debug) {
		params.debug(debug);
	}

	@Internal
	public Set<Object> getSources() {
		return sources;
	}
	public void setSources(Set<Object> sources) {
		this.sources = sources;
	}

	@Input
	public Map<String, File> getEmbeddedResources() {
		return embeddedResources;
	}
	public void setEmbeddedResources(Map<String, File> embeddedResources) {
		this.embeddedResources = embeddedResources;
	}

	@Input
	public String getTargetPlatform() {
		return targetPlatform;
	}
	public void setTargetPlatform(String targetPlatform) {
		this.targetPlatform = targetPlatform;
	}
	public void targetPlatform(String targetPlatform) {
		this.targetPlatform = targetPlatform;
	}
}
