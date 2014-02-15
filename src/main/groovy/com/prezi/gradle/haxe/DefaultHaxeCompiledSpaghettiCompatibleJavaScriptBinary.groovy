package com.prezi.gradle.haxe
/**
 * Created by lptr on 09/02/14.
 */
class DefaultHaxeCompiledSpaghettiCompatibleJavaScriptBinary
		extends DefaultHaxeCompiledBinary
		implements HaxeCompiledSpaghettiCompatibleJavaScriptBinary {
	public DefaultHaxeCompiledSpaghettiCompatibleJavaScriptBinary(String parentName, TargetPlatform targetPlatform, Flavor flavor) {
		super(parentName, targetPlatform, flavor)
	}

	@Override
	File getJavaScriptFile() {
		return getCompileTask().getOutputFile()
	}

	@Override
	File getSourceMapFile() {
		def outputFile = getCompileTask().getOutputFile()
		def sourceMapFile = new File(outputFile.parentFile, outputFile.name + ".map")
		return sourceMapFile.exists() ? sourceMapFile : null
	}
}
