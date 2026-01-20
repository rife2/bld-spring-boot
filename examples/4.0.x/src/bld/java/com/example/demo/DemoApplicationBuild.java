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

        repositories = List.of(MAVEN_CENTRAL, RIFE2_RELEASES);

        var boot = version(4, 0, 1);
        var junit = version(6, 0, 2);
        scope(compile)
                .include(dependency("org.springframework.boot", "spring-boot-starter", boot))
                .include(dependency("org.springframework.boot", "spring-boot-starter-web", boot));
        scope(test)
                .include(dependency("org.springframework.boot", "spring-boot-starter-test", boot))
                .include(dependency("org.springframework.boot", "spring-boot-webmvc-test", boot))
                .include(dependency("org.junit.jupiter", "junit-jupiter", junit))
                .include(dependency("org.junit.platform", "junit-platform-console-standalone", junit))
                .include(dependency("org.mockito:mockito-core:5.21.0"));
        scope(standalone)
                .include(dependency("org.springframework.boot", "spring-boot-loader", boot));

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
