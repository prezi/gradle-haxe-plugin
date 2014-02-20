package com.prezi.gradle.haxe

import org.gradle.api.internal.IConventionAware

class HaxeCompileParameters {

	String main
	public main(String main) {
		this.main = main
	}

	List<String> macros = []
	public macro(String m)
	{
		macros.add(m)
	}

	LinkedHashSet<String> includes = []
	public include(String thing)
	{
		includes.add(thing)
	}

	LinkedHashSet<String> excludes = []
	public exclude(String thing)
	{
		excludes.add(thing)
	}

	LinkedHashSet<String> flagList = []
	public void flag(String... flag)
	{
		flagList.addAll(flag)
	}

	boolean debug
	public debug(boolean debug) {
		this.debug = debug
	}

	String spaghetti
	public spaghetti(String output) {
		if (!(output in ["module", "application"])) {
			throw new IllegalArgumentException("spaghetti argument must be either 'module' or 'application'")
		}
		this.spaghetti = output
	}

	static void setConvention(IConventionAware task, Iterable<HaxeCompileParameters> params)
	{
		task.conventionMapping.main = { params*.main.find { it } }
		task.conventionMapping.macros = { new ArrayList<>(params*.macros.flatten()) }
		task.conventionMapping.includes = { new LinkedHashSet<>(params*.includes.flatten()) }
		task.conventionMapping.excludes = { new LinkedHashSet<>(params*.excludes.flatten()) }
		task.conventionMapping.flagList = { new LinkedHashSet<>(params*.flagList.flatten()) }
		task.conventionMapping.spaghetti = { params*.spaghetti.find { it } }
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
