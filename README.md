Gradle Haxe Plugin
==================

# Basics

The Haxe plugin consists of three parts:

* compilation and source bundling
* testing
* publishing

### Compilation

Syntax:

	task compile(type: com.prezi.gradle.haxe.CompileHaxe) {
		main "<main-class>"
		source <directory>
		targetPlatform "<js|swf|as3>"
	
		// Optional parameters
		classifier "<classifier>"
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

* `classifier` -- the classifier to use for the built artifacts.
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


### Testing

The `MUnit` task tests the results of a compilation task with [MassiveUnit](https://github.com/massiveinteractive/MassiveUnit). To set it up you need to provide two things:

	task munit(type: com.prezi.gradle.haxe.MUnit) {
		test <task>
		source <directory>

		// Optional parameters
		resource <directory>
		debug <true|false>
	}

Parameters:

* `debug` -- run tests tagged with `@TestDebug` only.
* `resource` -- directory with test resources. Repeat clause for multiple directories.
* `source` -- directory with test sources. Repeat clause for multiple directories.
* `test` -- the `CompileHaxe` task to test.


### Publishing

A `CompileHaxe` task will create two output artifacts:

* `artifact`: the compiled code, i.e. the `.js` or `.swf` file. You can use this output as is.
* `soruces`: the `.har` archive that you can use to link your module together with other projects on the source level.

The artifacts pick up configuration parameters (like `classifier` and `configuration`) from the compile task.

The `artifact` is automatically published via the `archives` configuration. The `sources` are published via the `archives` configuration and the configuration specified in the compiler task.

### To Develop the plugin

* Generate IDEA project by `gradle idea`
* Make sure you have Groovy plugin installed in IDEA
* Open the generated `gradle-haxe-plugin-ng.ipr` file and import the generated module `gradle-haxe-plugin-ng.iml`

# Examples

## Compilation: the `CompileHaxe` task

You have a project with the following source folders for separate platforms:

	/src/common  -- shared code for all platforms
	/src/js      -- JS-specific code (i.e. for Node.JS and browser)
	/src/node    -- code used only from Node.JS
	/src/browser -- code when used from a web browser
	/src/as      -- ActionScript-specific stuff

You can set up three builds for Node, web browser and AS as follows:

	configurations {
		as
		js
			browser { extendsFrom js }
			node { extendsFrom js }
	}

	task compileBrowser(type: CompileHaxe) {
		targetPlatform "js"
		classifier "browser"
		configuration configurations.browser
		source "src/common"
		source "src/js"
		source "src/browser"
	}

	task compileNode(type: CompileHaxe) {
		targetPlatform "js"
		classifier "node"
		configuration configurations.node
		source "src/common"
		source "src/js"
		source "src/node"
	}

	task compileAS(type: CompileHaxe) {
		targetPlatform "swf"
		classifier "as"
		configuration configurations.as
		source "src/common"
		source "src/as"
	}

Each task will give you two artifacts: the built code and the source `.har` archive:

* `compileBrowser`:
	* `project-browser.js`
	* `project-browser.har` -- including `src/common`, `src/js`, `src/browser`
* `compileNode`:
	* `project-node.js`
	* `project-node.har` -- including `src/common`, `src/js`, `src/node`
* `compileAS`:
	* `project-as.swf`	
	* `project-as.har` -- including `src/common`, `src/as`


### Resources

TBD

### The `.har` archive

The `.har` archive includes everything needed to build your project:

	+- META-INF
	|   +- MANIFEST.MF  -- manifest describing the archive
	+- sources          -- all source files copied into one folder
	+- resources        -- all resources copied into one folder
	+- embedded			-- all embedded resources

Each compile task builds a `.har` artifact for one specific platform. This means if you want to build a project for JS and AS3 as well, you will end up with two archives. See example below.

### The `compile` task

A task called `compile` is automatically generated by the Haxe plugin that automatically depends on all `CompileHaxe` tasks. So in order to compile everything in your project, you can simply issue

	gradle compile

## Testing: the `MUnit` task

If you want to test your code with MUnit, you will need to define some test tasks. Keeping with the example above, here's how you could test the browser artifact:

	task munitBrowser(type: MUnit) {
		test compileBrowser
		source "test/common"
		source "test/some-tests-for-browser-only"
	}

You can then run the tests via

	gradle munitBrowser

### The `test` task

Similar to the auto-generated `build` task, the Haxe plugin also creates a catch-all task for all tasks of type `MUnit` called `test`. So to run all MUnit tests on your project you can go:

	gradle test
