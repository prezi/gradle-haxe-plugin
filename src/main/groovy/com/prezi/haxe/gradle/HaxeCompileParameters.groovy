package com.prezi.haxe.gradle

import org.gradle.api.internal.IConventionAware
import org.gradle.api.tasks.Input

class HaxeCompileParameters {
	@Input
	String main
	public main(String main) {
		this.main = main
	}

	@Input
	List<String> macros = []
	public macro(String m)
	{
		macros.add(m)
	}

	@Input
	LinkedHashSet<String> includes = []
	public include(String thing)
	{
		includes.add(thing)
	}

	@Input
	LinkedHashSet<String> excludes = []
	public exclude(String thing)
	{
		excludes.add(thing)
	}

	@Input
	LinkedHashSet<String> flagList = []
	public void flag(String... flag)
	{
		flagList.addAll(flag)
	}

	@Input
	boolean debug
	public debug(boolean debug) {
		this.debug = debug
	}
}
