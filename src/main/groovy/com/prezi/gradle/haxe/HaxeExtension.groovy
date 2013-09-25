package com.prezi.gradle.haxe

import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.UnionFileCollection

class HaxeExtension {

	String main = ""
	public main(String main)
	{
		this.main = main
	}

	boolean debug = false
	public debug(boolean value)
	{
		this.debug = value
	}

	List<String> macros = []
	public macro(String m)
	{
		macros.add(m)
	}

	LinkedHashSet<String> includes = []
	public include(String pkg)
	{
		includes.add(pkg)
	}

	LinkedHashSet<String> excludes = []
	public exclude(String pkg)
	{
		excludes.add(pkg)
	}

	LinkedHashSet<String> flagList = []
	public flag(String flag)
	{
		flagList.add(flag)
	}

	List<Object> resourcePaths = []
	public resource(path)
	{
		resourcePaths.add(path)
	}

	Configuration configuration
	public configuration(Configuration configuration)
	{
		this.configuration = configuration
	}

	void mapTo(AbstractCompileHaxe compileTask)
	{
		compileTask.main = main
		compileTask.macros = new ArrayList<>(macros)
		compileTask.includes = new LinkedHashSet<>(includes)
		compileTask.excludes = new LinkedHashSet<>(excludes)
		compileTask.resourcePaths = new ArrayList<>(resourcePaths)
		compileTask.flagList = new LinkedHashSet<>(flagList)
		compileTask.debug = debug
		compileTask.configuration = configuration
	}
}
