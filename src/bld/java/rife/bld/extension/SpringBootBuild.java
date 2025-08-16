/*
 * Copyright 2023-2025 the original author or authors.
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
import rife.bld.publish.PublishDeveloper;
import rife.bld.publish.PublishLicense;
import rife.bld.publish.PublishScm;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static rife.bld.dependencies.Repository.*;
import static rife.bld.dependencies.Scope.compile;
import static rife.bld.dependencies.Scope.test;
import static rife.bld.operations.JavadocOptions.DocLinkOption.NO_MISSING;

public class SpringBootBuild extends Project {
    public SpringBootBuild() {
        pkg = "rife.bld.extension";
        name = "bld-spring-boot";
        version = version(1, 0, 4, "SNAPSHOT");

        javaRelease = 17;

        downloadSources = true;
        autoDownloadPurge = true;

        repositories = List.of(MAVEN_LOCAL, MAVEN_CENTRAL, RIFE2_RELEASES, RIFE2_SNAPSHOTS);

        scope(compile)
                .include(dependency("com.uwyn.rife2", "bld", version(2, 3, 0)));
        scope(test)
                .include(dependency("com.uwyn.rife2", "bld-extensions-testing-helpers",
                        version(0, 9, 0, "SNAPSHOT")))
                .include(dependency("org.junit.jupiter", "junit-jupiter",
                        version(5, 13, 4)))
                .include(dependency("org.junit.platform", "junit-platform-console-standalone",
                        version(1, 13, 4)))
                .include(dependency("org.assertj", "assertj-core",
                        version(3, 27, 4)));

        javadocOperation()
                .javadocOptions()
                .author()
                .docLint(NO_MISSING)
                .link("https://rife2.github.io/bld/");

        publishOperation()
                .repository(version.isSnapshot() ? repository("rife2-snapshot") : repository("rife2"))
                .repository(repository("github"))
                .info()
                .groupId("com.uwyn.rife2")
                .artifactId("bld-spring-boot")
                .description("bld Extension for Spring Boot")
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

    public static void main(String[] args) {
        new SpringBootBuild().start(args);
    }

    @BuildCommand(summary = "Runs PMD analysis")
    public void pmd() throws Exception {
        new PmdOperation()
                .fromProject(this)
                .failOnViolation(true)
                .ruleSets("config/pmd.xml")
                .execute();
    }

    @Override
    public void test() throws Exception {
        var testResultsDir = "build/test-results/test/";
        var op = testOperation().fromProject(this);
        op.testToolOptions().reportsDir(new File(testResultsDir));

        Exception ex = null;
        try {
            op.execute();
        } catch (Exception e) {
            ex = e;
        }

        var npmPackagesEnv = System.getenv("NPM_PACKAGES");
        if (npmPackagesEnv != null && !npmPackagesEnv.isEmpty()) {
            var xunitViewer = Path.of(npmPackagesEnv, "bin", "xunit-viewer").toFile();
            if (xunitViewer.exists() && xunitViewer.canExecute()) {
                var reportsDir = "build/reports/tests/test/";

                Files.createDirectories(Path.of(reportsDir));

                new ExecOperation()
                        .fromProject(this)
                        .command(xunitViewer.getPath(), "-r", testResultsDir, "-o", reportsDir + "index.html")
                        .execute();
            }
        }

        if (ex != null) {
            throw ex;
        }
    }
}
