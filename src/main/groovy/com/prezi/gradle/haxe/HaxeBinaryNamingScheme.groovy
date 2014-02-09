package com.prezi.gradle.haxe

import org.gradle.api.Nullable
import org.gradle.language.base.internal.BinaryNamingScheme
import org.gradle.util.GUtil

/**
 * Copied from {@link org.gradle.language.jvm.internal.ClassDirectoryBinaryNamingScheme}.
 */
class HaxeBinaryNamingScheme implements BinaryNamingScheme {
    private final String baseName;
    private final String collapsedName;

    public HaxeBinaryNamingScheme(String baseName) {
        this.baseName = baseName;
        this.collapsedName = collapseMain(this.baseName);
    }

    private static String collapseMain(String name) {
        return name.equals("main") ? "" : name;
    }

    public String getDescription() {
        return String.format("classes '%s'", baseName);
    }

    public String getLifecycleTaskName() {
        return getTaskName(null, "classes");
    }

    public String getTaskName(@Nullable String verb) {
        return getTaskName(verb, null);
    }

    public String getTaskName(@Nullable String verb, @Nullable String target) {
        String name = baseName;
        if (target != null) {
            name = collapsedName;
        }
        return GUtil.toLowerCamelCase(String.format("%s %s %s", nullToEmpty(verb), name, nullToEmpty(target)));
    }

    private static String nullToEmpty(String input) {
        return input == null ? "" : input;
    }

    public String getOutputDirectoryBase() {
        return baseName;
    }
}
