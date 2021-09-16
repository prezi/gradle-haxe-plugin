package com.prezi.haxe.gradle.incubating;

import javax.annotation.Nullable;
import java.util.List;

public interface BinaryNamingScheme {
	String getLifecycleTaskName();

    String getTaskName(@Nullable String verb);

    String getTaskName(@Nullable String verb, @Nullable String target);

    String getOutputDirectoryBase();

    String getDescription();

    List<String> getVariantDimensions();
}
