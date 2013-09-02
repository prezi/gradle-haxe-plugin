Gradle Haxe Plugin
==================

# Improvements over version 0.x

## Bundling sources and resources

The `CompileHaxe` task builds two output artifacts:

* the output of the Haxe compiler (i.e. your output JS or SWF file)
* a HAR (Haxe ARchive) containing the sources and the resources required to build the project

The advantage is that you can depend on the `.har` artifact in another project without having to worry about accessing resources.

## Dependencies between projects in a multi-project build

Suppose you have a build like this:

	root
	+- project-a
	|   +- build.gradle
	+- project-b
	|   +- build.gradle
	+- build.gradle
	+- settings.gradle
    
Then you can do this in `project-b`:

	dependencies {
		runtime project(path: "project-a", configuration: "runtime")
	}

If you build `project-b` only, it will run the required tasks from `project-a` as well.

## Easier publishing

You can publish via the `ivy-publish` plugin like this:

	apply plugin: "ivy-publish"
	
	task buildServer(type: CompileHaxe) {
		configuration configurations.runtime
		source "src/main/haxe/common"
		source "src/main/haxe/js"
		resource "src/main/resources"
		resource "build.gradle"
		targetPlatform "js"
	}
	
	publishing {
		repositories {
			ivy {
				// Define Ivy repository
			}
		}
		publications {
			ivy(IvyPublication) {
				// This is the built JS file
				artifact(buildServer.artifact)
				// This is the HAR archive of sources and resources
				artifact(buildServer.sources)
			}
		}
	}

# Planned improvements

## "Prezi" plugin

The Haxe plugin does not include the definition of default Prezi repositories and publishing rules. There should be a separate "Prezi" plugin for this. It's in the works.

## Bundling Haxe "headers" with built output

It is already possible to use the HAR artifact as a self-contained dependency that can be used to build your sources togther with the dependency sources (and resources). This is like static linking in C.

It would be nice to have something resembling dynamic linking a well: you take the built JS/SWC output of a module, and use that in another Haxe module. To do this, the dependent module must know about the structure of the JS/SWC file.

The Haxe plugin could generate another artifact that includes the built JS/SWC output of the build, plus "header" Haxe files, i.e. Haxe classes that are only `extern`s of the original classes.

This should be fairly simple to implement wiht the plugin.
