package com.prezi.haxe.gradle;

import org.gradle.api.Named;

import java.io.Serializable;

public interface Flavor extends Named, HaxeCompilerParameters, Serializable {
}
