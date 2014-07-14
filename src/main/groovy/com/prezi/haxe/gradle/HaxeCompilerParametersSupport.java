package com.prezi.haxe.gradle;

import java.util.List;
import java.util.Set;

public interface HaxeCompilerParametersSupport {
	String getMain();
	void main(String main);
	List<String> getMacros();
	void macro(String macro);
	Set<String> getIncludes();
	void include(String include);
	Set<String> getExcludes();
	List<String> getFlagList();
	void flag(String... flag);
	void exclude(String exclude);
	boolean isDebug();
	void debug(boolean debug);
}
