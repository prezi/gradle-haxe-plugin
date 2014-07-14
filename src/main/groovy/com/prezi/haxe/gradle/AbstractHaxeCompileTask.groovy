package com.prezi.haxe.gradle

import com.google.common.base.Predicate
import com.google.common.base.Strings
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import com.google.common.collect.Sets
import org.gradle.api.DomainObjectSet
import org.gradle.api.internal.ConventionTask
import org.gradle.api.internal.DefaultDomainObjectSet
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.internal.typeconversion.NotationParser
import org.gradle.language.base.LanguageSourceSet
import org.gradle.nativebinaries.internal.SourceSetNotationParser

import java.util.concurrent.Callable

/**
 * Created by lptr on 10/02/14.
 */
abstract class AbstractHaxeCompileTask extends ConventionTask implements HaxeCompilerParametersSupport {

	protected static final NotationParser<Object, Set<LanguageSourceSet>> notationParser = SourceSetNotationParser.parser()

	protected final HaxeCompilerParametersSupport params = new DefaultHaxeCompilerParametersSupport()

	LinkedHashSet<Object> sources = []
	LinkedHashMap<String, File> embeddedResources = [:]
	TargetPlatform targetPlatform
	void targetPlatform(String targetPlatform) {
		this.targetPlatform = project.extensions.getByType(HaxeExtension).targetPlatforms.maybeCreate(targetPlatform)
	}

	public source(Object... sources) {
		this.sources.addAll(sources)
	}

	protected DomainObjectSet<LanguageSourceSet> getSourceSets() {
		def sourceSets = sources.collectMany { notationParser.parseNotation(it) }
		return new DefaultDomainObjectSet<LanguageSourceSet>(LanguageSourceSet, sourceSets)
	}

	protected static LinkedHashSet<File> getAllSourceDirectories(Set<LanguageSourceSet> sources) {
		return (sources*.source*.srcDirs).flatten()
	}

	@InputFiles
	public Set<File> getInputFiles()
	{
		return getAllSourceDirectories(getSourceSets()) + getEmbeddedResources().values()
	}

	public void setConventionMapping(final HaxeCompilerParametersSupport... params)
	{
		final Iterable<HaxeCompilerParametersSupport> nonNullParams = Iterables.filter(Arrays.asList(params), new Predicate<HaxeCompilerParametersSupport>() {
			@Override
			boolean apply(HaxeCompilerParametersSupport param) {
				return param != null
			}
		});
		conventionMapping.map("main", new Callable<String>() {
			@Override
			String call() throws Exception {
				for (HaxeCompilerParametersSupport param : nonNullParams) {
					if (!Strings.isNullOrEmpty(param.getMain())) {
						return param.getMain();
					}
				}
				return null;
			}
		});
		conventionMapping.map("macros", new Callable<List<String>>() {
			@Override
			List<String> call() throws Exception {
				List<String> result = Lists.newArrayList();
				for (HaxeCompilerParametersSupport param : nonNullParams) {
					result.addAll(param.getMacros());
				}
				return result;
			}
		});
		conventionMapping.map("includes", new Callable<Set<String>>() {
			@Override
			Set<String> call() throws Exception {
				Set<String> result = Sets.newLinkedHashSet();
				for (HaxeCompilerParametersSupport param : nonNullParams) {
					result.addAll(param.getIncludes());
				}
				return result;
			}
		});
		conventionMapping.map("excludes", new Callable<Set<String>>() {
			@Override
			Set<String> call() throws Exception {
				Set<String> result = Sets.newLinkedHashSet();
				for (HaxeCompilerParametersSupport param : nonNullParams) {
					result.addAll(param.getExcludes());
				}
				return result;
			}
		});
		conventionMapping.map("flagList", new Callable<List<String>>() {
			@Override
			List<String> call() throws Exception {
				List<String> result = Lists.newArrayList();
				for (HaxeCompilerParametersSupport param : nonNullParams) {
					result.addAll(param.getFlagList());
				}
				return result;
			}
		});
		conventionMapping.map("debug", new Callable<Boolean>() {
			@Override
			Boolean call() throws Exception {
				boolean debug = false;
				for (HaxeCompilerParametersSupport param : nonNullParams) {
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
	String getMain() {
		return params.getMain()
	}

	@Override
	void main(String main) {
		params.main(main);
	}

	@Input
	@Override
	List<String> getMacros() {
		return params.getMacros();
	}

	@Override
	void macro(String macro) {
		params.macro(macro);
	}

	@Input
	@Override
	Set<String> getIncludes() {
		return params.getIncludes();
	}

	@Override
	void include(String include) {
		params.include(include);
	}

	@Input
	@Override
	Set<String> getExcludes() {
		return params.getExcludes();
	}

	@Override
	void exclude(String exclude) {
		params.exclude(exclude);
	}

	@Input
	@Override
	List<String> getFlagList() {
		return params.getFlagList();
	}

	@Override
	void flag(String... flag) {
		params.flag(flag);
	}

	@Input
	@Optional
	@Override
	boolean isDebug() {
		return params.isDebug();
	}

	@Override
	void debug(boolean debug) {
		params.debug(debug);
	}
}
