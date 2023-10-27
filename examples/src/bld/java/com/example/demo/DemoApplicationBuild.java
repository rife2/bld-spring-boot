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

        scope(compile)
                .include(dependency("org.springframework.boot:spring-boot-starter:3.1.5"))
                .include(dependency("org.springframework.boot:spring-boot-starter-actuator:3.1.5"))
                .include(dependency("org.springframework.boot:spring-boot-starter-web:3.1.5"));
        scope(test)
                .include(dependency("org.springframework.boot:spring-boot-starter-test:3.1.5"))
                .include(dependency("org.junit.jupiter:junit-jupiter:5.10.0"))
                .include(dependency("org.junit.platform:junit-platform-console-standalone:1.10.0"));
        scope(standalone)
                .include(dependency("org.springframework.boot:spring-boot-loader:3.1.5"));
    }

    public static void main(String[] args) {
        var level = Level.FINER;
        var consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(level);
        var logger = Logger.getLogger(BootJarOperation.class.getName());
        logger.addHandler(consoleHandler);
        logger.setLevel(level);

        new DemoApplicationBuild().start(args);
    }

    @BuildCommand(summary = "Creates an executable JAR for the project")
    public void bootjar() throws Exception {
        new BootJarOperation()
                .fromProject(this)
                .execute();
    }

    @BuildCommand(summary = "Creates an executable JAR for the project")
    public void uberjar() throws Exception {
        bootjar();
    }

    @BuildCommand(summary = "Creates WAR for the project")
    public void war() throws Exception {
        new BootWarOperation()
                .fromProject(this)
                .execute();
    }
}