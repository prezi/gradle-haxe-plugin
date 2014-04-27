package com.prezi.gradle.haxe

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

	static void setConventionMapping(IConventionAware task, Iterable<HaxeCompileParameters> params)
	{
		task.conventionMapping.main = { params*.main.find { it } }
		task.conventionMapping.macros = { new ArrayList<>(params*.macros.flatten()) }
		task.conventionMapping.includes = { new LinkedHashSet<>(params*.includes.flatten()) }
		task.conventionMapping.excludes = { new LinkedHashSet<>(params*.excludes.flatten()) }
		task.conventionMapping.flagList = { new LinkedHashSet<>(params*.flagList.flatten()) }
		task.conventionMapping.debug = { params*.debug.find { it } != null }
	}

	@Override
	String toString()
	{
		def s = new StringBuilder()
		def separator = "\n\t"
		s.append "Haxe compiler config"
		s.append separator
		s.append "Main: ${main}"
		s.append separator
		s.append "Macros: ${macros}"
		s.append separator
		s.append "Includes: ${includes}"
		s.append separator
		s.append "Excludes: ${excludes}"
		s.append separator
		s.append "Flags: ${flagList}"
		s.append separator
		s.append "Debug: ${debug}"
		return s.toString()
	}
}
