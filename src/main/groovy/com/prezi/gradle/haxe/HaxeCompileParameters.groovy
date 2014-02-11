package com.prezi.gradle.haxe

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.IConventionAware

class HaxeCompileParameters {

	private final Project project

	public HaxeCompileParameters(Project project)
	{
		this.project = project
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

	List<String> flagList = []

	public void flag(String... flag)
	{
		flagList.addAll(flag)
	}

	boolean debug

	String spaghetti

	public spaghetti(String output) {
		if (!(output in ["module", "application"])) {
			throw new IllegalArgumentException("spaghetti argument must be either 'module' or 'application'")
		}
		this.spaghetti = output
	}

	protected HaxeCompileParameters merge(HaxeCompileParameters other) {
		def result = new HaxeCompileParameters(project)
		result.macros    = macros    +  other.macros
		result.includes  = includes  +  other.includes
		result.excludes  = excludes  +  other.excludes
		result.flagList  = flagList  +  other.flagList
		result.spaghetti = spaghetti ?: other.spaghetti
		result.debug     = debug     || other.debug
		return result
	}

	protected static void setConvention(IConventionAware task, HaxeCompileParameters... params)
	{
		task.conventionMapping.macros = { new ArrayList<>(params*.macros.flatten()) }
		task.conventionMapping.includes = { new LinkedHashSet<>(params*.includes.flatten()) }
		task.conventionMapping.excludes = { new LinkedHashSet<>(params*.excludes.flatten()) }
		task.conventionMapping.flagList = { new ArrayList<>(params*.flagList.flatten()) }
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
