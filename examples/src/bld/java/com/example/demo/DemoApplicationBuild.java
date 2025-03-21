package com.example.demo;

import rife.bld.BuildCommand;
import rife.bld.WebProject;
import rife.bld.extension.BootJarOperation;
import rife.bld.extension.BootWarOperation;

import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static rife.bld.dependencies.Repository.MAVEN_CENTRAL;
import static rife.bld.dependencies.Scope.*;

public class DemoApplicationBuild extends WebProject {
    public DemoApplicationBuild() {
        pkg = "com.example.demo";
        name = "DemoApplication";
        mainClass = "com.example.demo.DemoApplication";
        version = version(0, 1, 0);

        javaRelease = 17;

        autoDownloadPurge = true;

        repositories = List.of(MAVEN_CENTRAL);

        var boot = version(3, 4, 4);
        scope(compile)
                .include(dependency("org.springframework.boot", "spring-boot-starter", boot))
                .include(dependency("org.springframework.boot", "spring-boot-starter-actuator", boot))
                .include(dependency("org.springframework.boot", "spring-boot-starter-web", boot))
                .include(dependency("org.mockito:mockito-core:5.16.1"));
        scope(test)
                .include(dependency("org.springframework.boot", "spring-boot-starter-test", boot))
                .include(dependency("org.junit.jupiter", "junit-jupiter", version(5, 12, 1)))
                .include(dependency("org.junit.platform", "junit-platform-console-standalone", version(1, 12, 1)));
        scope(standalone)
                .include(dependency("org.springframework.boot", "spring-boot-loader", boot));

        testOperation().javaOptions(List.of("-XX:+EnableDynamicAgentLoading"))
                .javaOptions().enableNativeAccess("ALL-UNNAMED");
    }

    public static void main(String[] args) {
        var level = Level.ALL;
        var logger = Logger.getLogger("rife.bld.extension");
        var consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(level);
        logger.addHandler(consoleHandler);
        logger.setLevel(level);
        logger.setUseParentHandlers(false);

        new DemoApplicationBuild().start(args);
    }

    @BuildCommand(summary = "Creates an executable JAR for the project")
    public void bootjar() throws Exception {
        new BootJarOperation()
                .fromProject(this)
                .execute();
    }

    @BuildCommand(summary = "Creates an executable WAR for the project")
    public void bootwar() throws Exception {
        new BootWarOperation()
                .fromProject(this)
                .execute();
    }
}
