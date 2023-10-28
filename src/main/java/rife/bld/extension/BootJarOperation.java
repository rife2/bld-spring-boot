/*
 * Copyright 2023 the original author or authors.
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

import rife.bld.Project;
import rife.tools.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builds and creates a Spring Boot executable Java archive (JAR).
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public class BootJarOperation extends AbstractBootOperation {
    private static final Logger LOGGER = Logger.getLogger(BootJarOperation.class.getName());

    /**
     * Provides the destination file name that will be used for the JAR creation.
     *
     * @param name the JAR destination file name
     * @return this operation instance
     */
    @Override
    public BootJarOperation destinationArchiveFileName(String name) {
        return (BootJarOperation) super.destinationArchiveFileName(name);
    }

    /**
     * Provides the destination directory in which the JAR will be created.
     *
     * @param directory the JAR destination directory
     * @return this operation instance
     */
    @Override
    public BootJarOperation destinationDirectory(File directory) throws IOException {
        return (BootJarOperation) super.destinationDirectory(directory);
    }

    /**
     * Provides JAR libraries that will be used for the JAR creation.
     *
     * @param jars Java archive files
     * @return this operation instance
     */
    @Override
    public BootJarOperation infLibs(List<File> jars) {
        return (BootJarOperation) super.infLibs(jars);
    }

    /**
     * Provides JAR libraries that will be used for the JAR creation.
     *
     * @param jar Java archive file
     * @return this operation instance
     */
    @Override
    public AbstractBootOperation infLibs(File... jar) {
        return super.infLibs(jar);
    }

    /**
     * Part of the {@link #execute} operation, configure the JAR launcher ({@code spring-boot-loader}) class.
     */
    @Override
    public BootJarOperation launcherClass(String className) {
        return (BootJarOperation) super.launcherClass(className);
    }

    /**
     * Part of the {@link #execute} operation, configure the launcher ({@code spring-boot-loader}) JAR(s).
     */
    public BootJarOperation launcherJars(List<File> jars) throws IOException {
        return (BootJarOperation) super.launcherJars(jars);
    }

    /**
     * Provides the fully-qualified main class name.
     */
    public BootJarOperation mainClass(String className) {
        return (BootJarOperation) super.mainClass(className);
    }

    /**
     * Provides an attribute to put in the JAR manifest.
     *
     * @param name  the attribute name to put in the manifest
     * @param value the attribute value to put in the manifest
     * @return this operation instance
     */
    @Override
    public BootJarOperation manifestAttribute(String name, String value) {
        return (BootJarOperation) super.manifestAttribute(name, value);
    }

    /**
     * Provides a map of attributes to put in the jar manifest.
     * <p>
     * A copy will be created to allow this map to be independently modifiable.
     *
     * @param attributes the attributes to put in the manifest
     * @return this operation instance
     */
    @Override
    public BootJarOperation manifestAttributes(Collection<BootManifestAttribute> attributes) {
        return (BootJarOperation) super.manifestAttributes(attributes);
    }

    /**
     * Provides the bld project.
     */
    @Override
    public BootJarOperation project(Project project) {
        return (BootJarOperation) super.project(project);
    }

    /**
     * Provides source directories that will be used for the jar archive creation.
     *
     * @param directories source directories
     * @return this operation instance
     */
    @Override
    public BootJarOperation sourceDirectories(File... directories) {
        return (BootJarOperation) super.sourceDirectories(directories);
    }

    /**
     * Performs the BootJar operation.
     */
    @Override
    public void execute() throws Exception {
        verifyExecute();

        var staging_dir = Files.createTempDirectory("bootjar").toFile();

        try {
            var boot_inf_dir = executeCreateBootInfDirectory(staging_dir);
            executeCopyInfClassesFiles(boot_inf_dir);
            executeCopyInfLibs(boot_inf_dir);
            executeCopyBootLoader(staging_dir);

            executeCreateArchive(staging_dir, LOGGER);

            if (!silent() && LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info(String.format("The executable JAR (%s) was created in: %s%n", destinationArchiveFileName(),
                        destinationDirectory()));
            }
        } finally {
            FileUtils.deleteDirectory(staging_dir);
        }
    }


    /**
     * Part of the {@link #execute} operation, creates the {@code BOOT-INF} staging directory.
     */
    protected File executeCreateBootInfDirectory(File stagingDirectory) throws IOException {
        var boot_inf = new File(stagingDirectory, "BOOT-INF");
        mkDirs(boot_inf);
        return boot_inf;
    }

    /**
     * Configures the operation from a {@link Project}.
     */
    public BootJarOperation fromProject(Project project) throws IOException {
        project(project);
        mainClass(project.mainClass());

        return destinationDirectory(project.buildDistDirectory())
                .destinationArchiveFileName(project.archiveBaseName() + "-" + project.version() + "-boot.jar")
                .infLibs(project.compileClasspathJars())
                .infLibs(project.runtimeClasspathJars())
                .launcherClass("org.springframework.boot.loader.JarLauncher")
                .launcherJars(project.standaloneClasspathJars())
                .manifestAttributes(
                        List.of(
                                new BootManifestAttribute("Manifest-Version", "1.0"),
                                new BootManifestAttribute("Main-Class", launcherClass()),
                                new BootManifestAttribute("Start-Class", mainClass()))
                )
                .sourceDirectories(project.buildMainDirectory(), project.srcMainResourcesDirectory());
    }
}