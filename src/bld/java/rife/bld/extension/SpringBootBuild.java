/*
 * Copyright 2023-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rife.bld.extension;

import rife.bld.BuildCommand;
import rife.bld.Project;
import rife.bld.extension.tools.IOTools;
import rife.bld.publish.PublishDeveloper;
import rife.bld.publish.PublishLicense;
import rife.bld.publish.PublishScm;

import java.util.List;

import static rife.bld.dependencies.Repository.*;
import static rife.bld.dependencies.Scope.*;
import static rife.bld.operations.JavadocOptions.DocLinkOption.NO_MISSING;

public class SpringBootBuild extends Project {

    public SpringBootBuild() {
        pkg = "rife.bld.extension";
        name = "bld-spring-boot";
        version = version(1, 1, 0, "SNAPSHOT");

        javaRelease = 17;

        downloadSources = true;
        autoDownloadPurge = true;

        repositories = List.of(MAVEN_CENTRAL, CENTRAL_SNAPSHOTS, RIFE2_RELEASES, RIFE2_SNAPSHOTS);

        var junit = version(6, 1, 2);
        scope(compile)
                .include(dependency("com.uwyn.rife2", "bld-extensions-tools",
                        version(1, 3, 0, "SNAPSHOT")))
                .include(dependency("com.uwyn.rife2", "bld",
                        version(2, 3, 1, "SNAPSHOT")));
        scope(provided)
                .include(dependency("org.jspecify", "jspecify", "1.0.1"))
                .include(dependency("com.github.spotbugs", "spotbugs-annotations",
                        version(4, 10, 3)));
        scope(test)
                .include(dependency("com.uwyn.rife2", "bld-testing-helpers",
                        version(1, 1, 0, "SNAPSHOT")))
                .include(dependency("org.junit.jupiter", "junit-jupiter", junit))
                .include(dependency("org.junit.platform", "junit-platform-console-standalone", junit))
                .include(dependency("org.assertj", "assertj-core",
                        version(3, 27, 7)));

        javadocOperation()
                .javadocOptions()
                .author()
                .docLint(NO_MISSING)
                .link("https://rife2.github.io/bld/")
                .link("https://jspecify.dev/docs/api/");

        publishOperation()
                .repository(version.isSnapshot() ? repository("rife2-snapshot") : repository("rife2"))
                .repository(repository("github"))
                .info()
                .groupId("com.uwyn.rife2")
                .artifactId("bld-spring-boot")
                .description("Spring Boot Extension for bld")
                .url("https://github.com/rife2/bld-spring-boot")
                .developer(new PublishDeveloper()
                        .id("ethauvin")
                        .name("Erik C. Thauvin")
                        .email("erik@thauvin.net")
                        .url("https://erik.thauvin.net/")
                )
                .license(new PublishLicense()
                        .name("The Apache License, Version 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0.txt")
                )
                .scm(new PublishScm()
                        .connection("scm:git:https://github.com/rife2/bld-spring-boot.git")
                        .developerConnection("scm:git:git@github.com:rife2/bld-spring-boot.git")
                        .url("https://github.com/rife2/bld-spring-boot")
                )
                .signKey(property("sign.key"))
                .signPassphrase(property("sign.passphrase"));
    }

    @Override
    public void test() throws Exception {
        var op = testOperation().fromProject(this);
        op.testToolOptions().reportsDir(IOTools.resolveFile(buildDirectory(), "test-results", "test"));
        op.execute();
    }

    public static void main(String[] args) {
        new SpringBootBuild().start(args);
    }

    @BuildCommand(summary = "Runs PMD analysis")
    public void pmd() throws Exception {
        new PmdOperation()
                .failOnViolation(true)
                .ruleSets("config/pmd.xml")
                .fromProject(this)
                .execute();
    }

    @BuildCommand(summary = "Runs the JUnit reporter")
    public void reporter() throws Exception {
        new JUnitReporterOperation()
                .fromProject(this)
                .failOnSummary(true)
                .execute();
    }

    @BuildCommand(summary = "Runs SpotBugs on this project")
    public void spotbugs() throws Exception {
        new SpotBugsOperation()
                .fromProject(this)
                .home("/opt/spotbugs")
                .execute();
    }
}
