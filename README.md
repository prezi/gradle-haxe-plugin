Gradle Haxe Plugin
==================

The Haxe plugin allows you to build, test and package Haxe projects from Gradle.

[![Build Status](https://travis-ci.org/prezi/gradle-haxe-plugin.svg?branch=master)](https://travis-ci.org/prezi/gradle-haxe-plugin)

Gradle 2.0 required. With earlier versions you will get an error that `org.gradle.runtime.base.BinaryContainer` is missing.

The plugin requires [Haxe](http://haxe.org/download) to be installed and available on the path. For testing it also requires [Neko](http://nekovm.org/download) and the [MUnit haxelib](http://lib.haxe.org/p/munit) to be installed.

## Standard project layout

As most Gradle plugins, the Haxe plugin tries to be clever and figure out what you want by looking at what you have. it acts as the middle-man between you and Gradle: you give it the high-level detail, and it will create all the low-level stuff like tasks and source directories. In most cases you need little configuration to describe what you want it to do, but full customization is also possible.

### Target platforms and Configurations

Haxe is an inherently multi-platform programming language that can build software for JavaScript, Flash and Java from the same source. Each of your builds must specify which platforms you are targetting:

```groovy
haxe {
	targetPlatforms {
		js
		swf
	}
}
```

### Configurations

For each target platform the plugin creates two configurations:

* one to contain the dependencies and resulting artifacts for the production code for that platform
* and another for all the test code

There are also two shared configurations called `main` and `test` that hold the shared production/test dependencies for every platform. The platform configurations extend their respective shared configurations.

### Source directories

When compiling and packaging your project, the plugin will check these directories:

* `src/main` -- shared production sources and resources for all platforms
* `src/test` -- shared test sources and resources for all platforms
* `src/<platform>` -- production sources and resources for a specific platform (e.g. `src/js`)
* `src/<platform>Test` -- production sources and resources for a specific platform (e.g. `src/jsTest`)

Each of these directories can contain the following sub-directories (among others):

* `haxe` -- Haxe code to compile
* `resources` -- resources and assets (images, data etc.) -- these will be made available on the compiler's classpath

This makes things easy to find for both human and plugin. For example if you are looking for the JavaScript specific Haxe code, you don't need to look no further than `src/js/haxe`.

See below about how you can embed resources in your Haxe programs.

### Tasks

For each target platform the plugin creates the following tasks:

* `compile<platform>` -- calls the Haxe compiler (e.g. `compileJs`)
* `compile<platform>Test` -- calls the Haxe compiler to compile tests (e.g. `compileJsTest`)
* `run<platform>Test` -- runs the tests via MUnit (e.g. `runJsTest`)
* `source<platform>` -- creates the [`.har` archive](#published-artifacts) from the sources used to compile the platform (e.g. `sourceJs`)
* `source<platform>Test` -- same for tests

There is also a `compile` and a `test` task created that run all compile- and test tasks, respectively. There is a `check` task that calls `test` (so that you can hook in other tests, like integration and acceptance tests).

### Published artifacts

The source code is published to the target platform's configuration as a Haxe ARchive (.har).

The `.har` archive includes every source code and resource needed to build your project:

	+- META-INF
	|   +- MANIFEST.MF  -- manifest describing the archive
	+- sources          -- all source files copied into one folder
	+- resources        -- all resources copied into one folder
	+- embedded			-- all embedded resources

Each source task builds a `.har` artifact for one specific platform. This means if you want to build a project for JS and AS3 as well, you will end up with one archive for each platform.


## Customizations

While the plugin tries to be as clever as possible, sometimes you have to give it a helping hand.

### Compiler options

You can set compiler options for all your code, or for just a specific platform. For compiler options see [Compilation](#Compilation).

```groovy
haxe {
	// Set compiler options here for all platforms
	inlcude "com.example.test.haxe"

	targetPlatforms {
		js {
			// Set compiler options here for only the JavaScript target
			inlcude "com.example.test.haxe.javascript"
			flag "-D js-enabled"
		}
		swc {
			// Set compiler options here for only the Flash target
			flag "-D only-for-flash"
		}
	}
}
```

### Compiling generated code

Say you have a nice task that generates some Haxe code, and you'd like to add it to the compiler's classpath:

```groovy
task generateSomething(type: SomeGenerator) {
	inputFile = file("...")
	outputDirectory = file("${buildDir}/geneated-haxe")
}
```

Here's how you can add it for every platform:

```groovy
sources {
	main {
		haxe {
			// This tells the plugin where to find the code to compile
			source.srcDir generateSomething.outputDirectory
			// This tells it which task to run when it needs the generated source
			builtBy generateSomething
		}
	}
}
```

If you wanted to only add it for the `js` platform, you'd have to add it to the `js` source set:

```groovy
// Notice that here we say 'js' instead of 'main':
sources.js.haxe {
	source.srcDir generateSomething.outputDirectory
	builtBy generateSomething
}
```

### Embedding resources

TBD

### MUnit command-line parameters

The default runner for the MUnit tests is a custom Node.js based runner. To use custom node modules create a task that copies them to `${buildDir}/munit/node_modules`. To use the default browser-based testing use the `-Pmunit.usebrowser` flag.

You can pass some command-line parameters to Gradle so that it will pass some to MUnit:

* `-Pmunit.browser=<browser>` -- adds `-browser <browser>`
* `-Pmunit.debug` -- turns on `-debug`
* `-Pmunit.kill-browser` -- adds `-kill-browser`
* `-Pmunit.platform=<platform>` -- adds `-<platform>`

A typical server-side example:

	$ gradle clean uploadArchives -Pmunit.browser=chromium -Pmunit.kill-browser -Pmunit.usebrowser`

### Tasks

#### Compilation

Syntax:

	task compile(type: com.prezi.haxe.gradle.HaxeCompile) {
		main "<main-class>"
		source <directory>
		targetPlatform "<js|swf|as3>"
	
		// Optional parameters
		configuration <configuration>
		debug <true|false>
		embed <file> ["<name>"]
		embedAll <directory>
		exclude "<package|class>"
		flag "<flag>"
		include "<package|class>"
		macro "<macro>"
		outputDirectory <directory>
		outputFile <file>
		resource <directory>
	}

Parameters:

* `configuration` -- the Gradle configuration to bind the resulting artifacts to, and to search for dependencies from.
* `debug` -- enables `-debug` and `-D fdb`.
* `embed` -- embeds the file (with the given name) as a built-in [Haxe resource](http://haxe.org/doc/advanced/resources).
* `embedAll` -- embeds all files in the directory.
* `exclude` -- adds `--macro exclude('…')` to the build command.
* `flag` -- add flag on Haxe command path, such as `"-D node"` or `"--js-modern"`.
* `include` -- adds `--macro include('…')` to the build command.
* `macro` -- adds `--macro "…"` on the build command.
* `main` -- specifies main class.
* `resource` -- specify a resource directory. Repeat `resource` clause for multiple resource directories.
* `output(File|Directory)` -- For JS and SWF use `outputFile`, for AS3 use `outputDirectory`. If not specified, defaults to `${project.name}-${classifier}.{targetPlatform}`.
* `source` -- specify a source directory. Repeat `source` clause for multiple source directories.
* `targetPlatform` -- specify the target platform.


#### Testing

The `HaxeTestCompile` task generates an MUnit test suite in addition to compiling your tests. Additional parameter to `HaxeCompile`:

* `workingDirectory` -- where the tests will be copied

The `MUnit` task tests the results of a compilation task with [MassiveUnit](https://github.com/massiveinteractive/MassiveUnit). It works with the same configuration as the compile task above.

	task munit(type: com.prezi.haxe.gradle.MUnit) {
		targetPlatform "<js|swf|as3>"
		inputFile <file>
		workingDirectory <dir>
	}

* `inputFile` -- the tests compiled by a `HaxeTestCompile` task
* `targetPlatform` -- specify the target platform.
* `workingDirectory` -- the directory where the tests will be executed in


You can supply MUnit-specific parameters.

## Example

You have a project with the following source folders for separate platforms:

	/src
		/main
			/haxe		-- shared code for all platforms
		/as
			/haxe		-- ActionScript-specific stuff
		/js
			/haxe		-- JS-specific code (i.e. for Node.JS and browser)
		/jsBrowser
			/haxe		-- code when used from a web browser
		/jsNode
			/haxe		-- code used only from Node.JS

You can set up three builds for Node, web browser and AS as follows:

	haxe {
		main "com.example.test.Main"

		targetPlatforms {
			js {
				browser {}
				node {}
			}
			swf
		}
	}

You will be able to run several commands to build your code. For example `compileJsNode` will run the Haxe compiler on the following directories:

* `/src/main/haxe`
* `/src/js/haxe`
* `/src/jsNode/haxe`

Each source task will give you a source `.har` archive. E.g. `sourceJs` will zip you the following:

* `src/main/haxe`
* `src/js/haxe`

## To develop the plugin

* Generate IDEA project by `gradle idea`
* Make sure you have Groovy plugin installed in IDEA
* Open the generated `gradle-haxe-plugin-ng.ipr` file and import the generated module `gradle-haxe-plugin-ng.iml`
