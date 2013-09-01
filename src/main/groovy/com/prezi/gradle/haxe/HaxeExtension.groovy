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

	LinkedHashSet<String> flags = []
	public flag(String flag)
	{
		flags.add(flag)
	}

	FileCollection resourceTree = new UnionFileCollection()
	public resource(paths)
	{
		resourceTree.add(project.files(paths))
	}

	Configuration configuration
	public configuration(Configuration configuration)
	{
		this.configuration = configuration
	}

	void mapTo(CompileHaxe compileTask)
	{
		compileTask.main = main
		compileTask.macros = macros
		compileTask.includePackages = includePackages
		compileTask.excludePackages = excludePackages
		compileTask.resourceTree = resourceTree
		compileTask.flags = flags
		compileTask.debug = debug
		compileTask.configuration = configuration
	}
}
