# [b<span style="color:orange">l</span>d](https://rife2.com/bld) Extension to Help Create [Spring Boot](https://spring.io/projects/spring-boot) Web Applications

[![License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/java-17%2B-blue)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![bld](https://img.shields.io/badge/2.3.0-FA9052?label=bld&labelColor=2392FF)](https://rife2.com/bld)
[![Release](https://flat.badgen.net/maven/v/metadata-url/repo.rife2.com/releases/com/uwyn/rife2/bld-spring-boot/maven-metadata.xml?color=blue)](https://repo.rife2.com/#/releases/com/uwyn/rife2/bld-spring-boot)
[![Snapshot](https://flat.badgen.net/maven/v/metadata-url/repo.rife2.com/snapshots/com/uwyn/rife2/bld-spring-boot/maven-metadata.xml?label=snapshot)](https://repo.rife2.com/#/snapshots/com/uwyn/rife2/bld-spring-boot)
[![GitHub CI](https://github.com/rife2/bld-spring-boot/actions/workflows/bld.yml/badge.svg)](https://github.com/rife2/bld-spring-boot/actions/workflows/bld.yml)

To install the latest version, add the following to the `lib/bld/bld-wrapper.properties` file:

```properties
bld.extension-spring-boot=com.uwyn.rife2:bld-spring-boot
```

For more information, please refer to the [extensions](https://github.com/rife2/bld/wiki/Extensions) documentation.

## Create an Executable JAR

To create a [Spring Boot executable Java Archive](https://docs.spring.io/spring-boot/docs/current/reference/html/executable-jar.html)
(JAR) from the current project:

```java
@BuildCommand(summary = "Creates an executable JAR for the project")
public void bootjar() throws Exception {
    new BootJarOperation()
            .fromProject(this)
            .execute();
}
```

```console
./bld compile bootjar
```

- [View Examples Project](https://github.com/rife2/bld-spring-boot/tree/main/examples)

## Create an Executable WAR

To create a [Spring Boot executable Web Archive](https://docs.spring.io/spring-boot/docs/current/reference/html/executable-jar.html#appendix.executable-jar.nested-jars.war-structure)
(WAR) from the current project:

```java
@BuildCommand(summary = "Creates an executable WAR for the project")
public void bootwar() throws Exception {
    new BootWarOperation()
            .fromProject(this)
            .execute();
}
```

```console
./bld compile bootwar
```

- [View Examples Project](https://github.com/rife2/bld-spring-boot/tree/main/examples)

## Required Dependency

Don't forget to include the _Spring Boot Loader_ dependency to your project:

```java
scope(standalone)
    .include(dependency("org.springframework.boot:spring-boot-loader:3.5.4"));
```

Please check the [BootJarOperation documentation](https://rife2.github.io/bld-spring-boot/rife/bld/extension/BootJarOperation.html#method-summary)
or [BootWarOperation documentation](https://rife2.github.io/bld-spring-boot/rife/bld/extension/BootWarOperation.html#method-summary)
for all available configuration options.

You may also want to have a look at the [Spring Boot Web Application Example for bld](https://github.com/rife2/spring-boot-bld-example) template.
