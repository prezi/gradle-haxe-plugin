package com.prezi.haxe.gradle.nodetest;

import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.prezi.haxe.gradle.HaxeTestCompile;

import java.io.File;
import java.io.IOException;

public class HaxeNodeTestCompile extends HaxeTestCompile {

    @Override
    protected void postProcessGeneratedSources(File testDir) throws IOException {
		super.postProcessGeneratedSources(testDir);
        File jsRunnerTemplate = new File(testDir, "TestMain.hx");
		Resources.asByteSource(this.getClass().getResource("/TestMain.hx")).copyTo(Files.asByteSink(jsRunnerTemplate));
	}
}
