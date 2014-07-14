package com.prezi.haxe.gradle;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.util.List;
import java.util.Set;

public class DefaultHaxeCompilerParameters implements HaxeCompilerParameters {

	private String main;
	private List<String> macros = Lists.newArrayList();
	private Set<String> includes = Sets.newLinkedHashSet();
	private Set<String> excludes = Sets.newLinkedHashSet();
	private List<String> flagList = Lists.newArrayList();
	private boolean debug;

	@Override
	public String getMain() {
		return main;
	}

	@Override
	public void main(String main) {
		this.main = main;
	}

	@Override
	public List<String> getMacros() {
		return macros;
	}

	@Override
	public void macro(String macro) {
		macros.add(macro);
	}

	@Override
	public Set<String> getIncludes() {
		return includes;
	}

	@Override
	public void include(String include) {
		includes.add(include);
	}

	@Override
	public Set<String> getExcludes() {
		return excludes;
	}

	@Override
	public void exclude(String exclude) {
		excludes.add(exclude);
	}

	@Override
	public List<String> getFlagList() {
		return flagList;
	}

	@Override
	public void flag(String... flags) {
		DefaultGroovyMethods.addAll(flagList, flags);
	}

	@Override
	public boolean isDebug() {
		return debug;
	}

	@Override
	public void debug(boolean debug) {
		this.debug = debug;
	}
}
