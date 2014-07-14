package com.prezi.haxe.gradle

import com.google.common.collect.Lists
import com.google.common.collect.Sets

/**
 * Created by lptr on 14/07/14.
 */
class DefaultHaxeCompilerParametersSupport implements HaxeCompilerParametersSupport {
	private String main;
	private List<String> macros = Lists.newArrayList();
	private Set<String> includes = Sets.newLinkedHashSet();
	private Set<String> excludes = Sets.newLinkedHashSet();
	private List<String> flagList = Lists.newArrayList();
	private boolean debug;

	@Override
	String getMain() {
		return main;
	}

	@Override
	void main(String main) {
		this.main = main;
	}

	@Override
	List<String> getMacros() {
		return macros;
	}

	@Override
	void macro(String macro) {
		macros.add(macro);
	}

	@Override
	Set<String> getIncludes() {
		return includes;
	}

	@Override
	void include(String include) {
		includes.add(include);
	}

	@Override
	Set<String> getExcludes() {
		return excludes;
	}

	@Override
	void exclude(String exclude) {
		excludes.add(exclude);
	}

	@Override
	List<String> getFlagList() {
		return flagList;
	}

	@Override
	void flag(String... flags) {
		flagList.addAll(flags);
	}

	@Override
	boolean isDebug() {
		return debug
	}

	@Override
	void debug(boolean debug) {
		this.debug = debug;
	}
}
