# [b<span style="color:orange">l</span>d](https://rife2.com/bld) Extension to Help Create [Spring Boot](https://spring.io/projects/spring-boot) Web Applications


[![License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/java-17%2B-blue)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![bld](https://img.shields.io/badge/1.7.3-FA9052?label=bld&labelColor=2392FF)](https://rife2.com/bld)
[![Release](https://flat.badgen.net/maven/v/metadata-url/repo.rife2.com/releases/com/uwyn/rife2/bld-spring-boot/maven-metadata.xml?color=blue)](https://repo.rife2.com/#/releases/com/uwyn/rife2/bld-spring-boot)
[![Snapshot](https://flat.badgen.net/maven/v/metadata-url/repo.rife2.com/snapshots/com/uwyn/rife2/bld-spring-boot/maven-metadata.xml?label=snapshot)](https://repo.rife2.com/#/snapshots/com/uwyn/rife2/bld-spring-boot)
[![GitHub CI](https://github.com/rife2/bld-spring-boot/actions/workflows/bld.yml/badge.svg)](https://github.com/rife2/bld-spring-boot/actions/workflows/bld.yml)

To install, please refer to the [extensions documentation](https://github.com/rife2/bld/wiki/Extensions).

To create a [Spring Boot executable Java Archive](https://docs.spring.io/spring-boot/docs/current/reference/html/executable-jar.html)
(JAR) from the current project:

```java
@@BuildCommand(summary = "Creates an executable JAR for the project")
public void bootjar() throws Exception {
    new BootJarOperation()
            .fromProject(this)
            .execute();
}
```

```text
./bld compile bootjar
```

To create a [Spring Boot executable Web Archive](https://docs.spring.io/spring-boot/docs/current/reference/html/executable-jar.html#appendix.executable-jar.nested-jars.war-structure)
(WAR) from the current project:

```java
@BuildCommand(summary = "Create an executable WAR for the project")
public void bootwar() throws Exception {
    new BootWarOperation()
            .fromProject(this)
            .execute();
}
```

```text
./bld compile bootwar
```

Please check the [BootJarOperation documentation](https://rife2.github.io/bld-spring-boot/rife/bld/extension/BootJarOperation.html#method-summary)
or [BootWarOperation documentation](https://rife2.github.io/bld-spring-boot/rife/bld/extension/BootWarOperation.html#method-summary)
for all available configuration options.

You might also want to have a look at the [Spring Boot Web Application Example for bld](https://github.com/rife2/spring-boot-bld).
