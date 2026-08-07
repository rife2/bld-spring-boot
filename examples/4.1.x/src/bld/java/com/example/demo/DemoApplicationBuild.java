package com.example.demo;

import rife.bld.BuildCommand;
import rife.bld.WebProject;
import rife.bld.extension.BootJarOperation;
import rife.bld.extension.BootWarOperation;

import java.util.List;

import static rife.bld.dependencies.Repository.*;
import static rife.bld.dependencies.Scope.*;

public class DemoApplicationBuild extends WebProject {
    public DemoApplicationBuild() {
        pkg = "com.example.demo";
        name = "demo";
        mainClass = "com.example.demo.DemoApplication";
        version = version(0, 1, 0);

        autoDownloadPurge = true;
        downloadSources = true;

        repositories = List.of(MAVEN_CENTRAL, RIFE2_RELEASES, RIFE2_SNAPSHOTS);

        var boot = bom("org.springframework.boot", "spring-boot-dependencies", version(4, 1, 0));
        var junit = bom("org.junit", "junit-bom", version(6, 1, 3));
        scope(compile)
                .include(boot)
                .include(dependency("org.springframework.boot", "spring-boot-starter"))
                .include(dependency("org.springframework.boot", "spring-boot-starter-web"));
        scope(standalone)
                .include(boot)
                .include(dependency("org.springframework.boot", "spring-boot-loader"));
        scope(test)
                .include(junit)
                .include(dependency("org.springframework.boot", "spring-boot-starter-test"))
                .include(dependency("org.springframework.boot", "spring-boot-webmvc-test"))
                .include(dependency("org.junit.jupiter", "junit-jupiter"))
                .include(dependency("org.junit.platform", "junit-platform-console-standalone", junit.version()))
                .include(dependency("org.mockito:mockito-core:5.23.0"));

        testOperation().javaOptions(List.of("-XX:+EnableDynamicAgentLoading"))
                .javaOptions().enableNativeAccess("ALL-UNNAMED");
    }

    public static void main(String[] args) {
        new DemoApplicationBuild().start(args);
    }

    @BuildCommand(summary = "Creates an executable JAR for the project")
    public void bootjar() throws Exception {
        new BootJarOperation()
                .fromProject(this)
                .execute();
    }

    @BuildCommand(summary = "Create an executable WAR for the project")
    public void bootwar() throws Exception {
        new BootWarOperation()
                .fromProject(this)
                .execute();
    }
}
