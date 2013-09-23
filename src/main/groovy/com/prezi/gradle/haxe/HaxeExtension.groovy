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

	LinkedHashSet<String> includePackages = []
	public includePackage(String pkg)
	{
		includePackages.add(pkg)
	}

	LinkedHashSet<String> excludePackages = []
	public excludePackage(String pkg)
	{
		excludePackages.add(pkg)
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

	String targetPlatform
	public targetPlatform(String targetPlatform)
	{
		this.targetPlatform = targetPlatform
	}

	void mapTo(CompileHaxe compileTask)
	{
		compileTask.main = main
		compileTask.macros = new ArrayList<>(macros)
		compileTask.includePackages = new LinkedHashSet<>(includePackages)
		compileTask.excludePackages = new LinkedHashSet<>(excludePackages)
		compileTask.resourcePaths = resourcePaths
		compileTask.flagList = new LinkedHashSet<>(flagList)
		compileTask.debug = debug
		compileTask.configuration = configuration
		compileTask.targetPlatform = targetPlatform
	}
}
