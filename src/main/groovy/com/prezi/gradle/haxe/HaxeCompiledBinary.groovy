package com.prezi.gradle.haxe
/**
 * Created by lptr on 08/02/14.
 */
public interface HaxeCompiledBinary extends HaxeBinary {
	HaxeCompile getCompileTask()
	void setCompileTask(HaxeCompile compileTask)
}
