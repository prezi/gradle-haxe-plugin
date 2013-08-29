package com.prezi.gradle

import org.gradle.api.Project
import org.gradle.api.file.FileCollection

class CmdBuilder
{

    String cmd = "haxe"

    String build()
    {
        cmd
    }

    CmdBuilder withMain(String main)
    {
        if (!main.isEmpty())
        {
            cmd += " -main $main"
        }

        this
    }

    CmdBuilder withTarget(String target, File output)
    {
        cmd += " -$target $output"
        this
    }

    CmdBuilder withIncludePackages(def packages)
    {
        packages.each { cmd += " --macro \"include('$it')\""}
        this
    }

    CmdBuilder withExcludePackages(def packages)
    {
        packages.each { cmd += " --macro \"exclude('$it')\""}
        this
    }

    CmdBuilder withMacros(def macros)
    {
        macros.each { cmd += " --macro \"${it.replaceAll('"', '\\"')}\""}
        this
    }

    CmdBuilder withResources(def resources)
    {
        resources.each {
			def fileName = it.name
			def filePath = it.getAbsolutePath()
			cmd += " -resource \"${filePath.replaceAll('\"', '\\\"')}@${fileName.replaceAll('\"', '\\\"')}\"" }
        this
    }

    CmdBuilder withSources(Iterable<File> sources)
    {
        sources.each { cmd += " -cp ${it}" }
        this
    }

    CmdBuilder withSources(File... sources)
    {
        withSources(sources.toList())
        this
    }

    CmdBuilder withFlags(String flags)
    {
        if (!flags.isEmpty())
        {
            cmd += " $flags"
        }
        this
    }

    CmdBuilder withDebugFlags(boolean debug)
    {
        if (debug)
        {
            cmd += " -D fdb -debug"
        }
        this
    }
}
