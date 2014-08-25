package com.prezi.haxe.gradle;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public class CheckHaxeVersion extends ConventionTask {

	private Set<Object> compilerVersions = Sets.newLinkedHashSet();

	@Input
	public Set<Object> getCompilerVersions() {
		return compilerVersions;
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

	@TaskAction
	public void check() throws IOException, InterruptedException {
		Set<Object> compilerVersions = getCompilerVersions();
		if (!compilerVersions.isEmpty()) {
			final AtomicReference<String> versionRef = new AtomicReference<String>();
			CommandExecutor.execute(Arrays.asList("haxe", "-version"), null, new ExecutionResultHandler() {
				@Override
				public void handleResult(int exitValue, String output) {
					if (exitValue != 0) {
						throw new RuntimeException("Could not get Haxe version:\n" + output);
					}
					versionRef.set(output.trim());
				}
			});
			final String version = versionRef.get();
			boolean matches = checkVersion(compilerVersions, version);
			if (!matches) {
				String validVersions = Joiner.on(", ").join(Collections2.transform(compilerVersions, new Function<Object, String>() {
					@Override
					public String apply(Object compilerVersion) {
						if (compilerVersion instanceof Pattern) {
							return ((Pattern) compilerVersion).pattern();
						} else {
							return String.valueOf(compilerVersion);
						}
					}
				}));
				if (compilerVersions.size() > 1) {
					validVersions = "either of " + validVersions;
				}
				throw new RuntimeException("Invalid Haxe version: " + version + ", should match " + validVersions);
			}
		} else {
			getLogger().debug("Not checking compiler version as no requirement is specified");
		}
	}

	static boolean checkVersion(Set<Object> compilerVersions, final String version) {
		return Iterables.any(Collections2.transform(compilerVersions, new Function<Object, Pattern>() {
			@Override
			public Pattern apply(Object compilerVersion) {
				if (compilerVersion instanceof Pattern) {
					return (Pattern) compilerVersion;
				} else if (compilerVersion instanceof String) {
					return Pattern.compile(Pattern.quote((String) compilerVersion));
				} else {
					return Pattern.compile(Pattern.quote(String.valueOf(compilerVersion)));
				}
			}
		}), new Predicate<Pattern>() {
			@Override
			public boolean apply(Pattern pattern) {
				return pattern.matcher(version).matches();
			}
		});
	}
}
