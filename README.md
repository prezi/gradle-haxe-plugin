Gradle Haxe Plugin
==================

# How to try it

If you want to test it, you can try:

	# Let's build
	gradle clean install
	# This will run the test build
	gradle run

This will run the [test-project](tree/master/test/at). Once you've installed the plugin, you can also run the test project manually with `gradle build` in the `test/at` directory.


# Improvements over version 0.x


## Bundling sources and resources

With the 0.x version of the plugin, it is hard to follow which source folders from which dependency participate in a build.

The 1.0 version of the `CompileHaxe` task builds two output artifacts:

* the output of the Haxe compiler (i.e. your output JS or SWF file)
* a HAR (Haxe ARchive) containing the sources and the resources required to build the project

You can safely depend on the `.har` artifact in another project without having to worry about

* which source folders to include (the HAR has only one `sources` folder with only the sources to build one specific platform), or
* how to access resources (they are in the HAR file).

The `CompileHaxe` task handles HAR dependencies automatically, but it also accepts 0.x-style `hxsrc` archives and standard Haxelibs, too.


## Easier testing

The 0.x version only supported one MUnit test to be run, with tests contained in the `src/common` directory. Now you can have a test task for each of your builds with different tests for each platform:

	task buildServer(type: CompileHaxe) {
		configuration configurations.runtime
		source "src/main/haxe/common"
		source "src/main/haxe/js"
		resource "src/main/resources"
		targetPlatform "js"
	}
	
	task testServer(type: MUnit) {
		// The build to test
		test buildServer
		// Tests can be found in these directories
		testSource "src/test/haxe/common"
		testSource "src/test/haxe/js"
		// You can add resources for testing
		testResource "src/test/resources"
	}


## Dependencies between projects in a multi-project build

Suppose you have a build like this:

	root
	+- project-a
	|   +- build.gradle
	+- project-b
	|   +- build.gradle
	+- build.gradle
	+- settings.gradle
    
Then you can do this in `project-b/build.gradle`:

	dependencies {
		runtime project(path: ":project-a", configuration: "runtime")
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
