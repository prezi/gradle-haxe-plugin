package com.prezi.gradle.haxe

import org.gradle.api.Nullable
import org.gradle.language.base.internal.BinaryNamingScheme

/**
 * Copied from {@link org.gradle.language.jvm.internal.ClassDirectoryBinaryNamingScheme}.
 */
class HaxeBinaryNamingScheme implements BinaryNamingScheme {
	private final String parentName;
	private final String collapsedName;

	public HaxeBinaryNamingScheme(String parentName) {
		this.parentName = parentName;
		this.collapsedName = collapseMain(this.parentName);
	}

	private static String collapseMain(String name) {
		return name.equals("main") ? "" : name;
	}

	public String getDescription() {
		return String.format("%s binary", parentName);
	}

	public String getLifecycleTaskName() {
		return getTaskName(null, null);
	}

	public String getTaskName(@Nullable String verb) {
		return getTaskName(verb, null);
	}

	public String getTaskName(@Nullable String verb, @Nullable String target) {
		return makeName(verb, collapsedName, target);
	}

	private static String makeName(String... words) {
		StringBuilder builder = new StringBuilder();
		for (String word : words) {
			if (word == null || word.length() == 0) {
				continue;
			}
			if (builder.length() == 0) {
				appendUncapitalized(builder, word);
			} else {
				appendCapitalized(builder, word);
			}
		}
		return builder.toString();
	}

	private static void appendCapitalized(StringBuilder builder, String word) {
		builder.append(Character.toTitleCase(word.charAt(0))).append(word.substring(1));
	}

	private static void appendUncapitalized(StringBuilder builder, String word) {
		builder.append(Character.toLowerCase(word.charAt(0))).append(word.substring(1));
	}


	private static String nullToEmpty(String input) {
		return input == null ? "" : input;
	}

	public String getOutputDirectoryBase() {
		return parentName
	}
}
