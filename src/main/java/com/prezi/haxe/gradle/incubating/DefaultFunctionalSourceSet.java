package com.prezi.haxe.gradle.incubating;

import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.internal.DefaultPolymorphicDomainObjectContainer;
import org.gradle.internal.reflect.Instantiator;

public class DefaultFunctionalSourceSet extends DefaultPolymorphicDomainObjectContainer<LanguageSourceSet> implements FunctionalSourceSet {
	private final String name;

    public DefaultFunctionalSourceSet(String name, Instantiator instantiator, CollectionCallbackActionDecorator callbackDecorator) {
        super(LanguageSourceSet.class, instantiator, callbackDecorator);
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("source set '%s'", name);
    }

    public String getName() {
        return name;
    }
}
