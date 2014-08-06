package com.prezi.haxe.gradle;

import com.prezi.haxe.gradle.incubating.Binary;
import com.prezi.haxe.gradle.incubating.LanguageSourceSet;
import org.gradle.api.DomainObjectSet;
import org.gradle.api.artifacts.Configuration;

public interface HaxeBinaryBase<T extends HaxeCompile> extends Binary {
	DomainObjectSet<LanguageSourceSet> getSource();
	Configuration getConfiguration();
	TargetPlatform getTargetPlatform();
	Flavor getFlavor();
	T getCompileTask();
	void setCompileTask(T compileTask);
	Har getSourceHarTask();
	void setSourceHarTask(Har compileTask);
}
