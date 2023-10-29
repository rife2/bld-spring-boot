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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builds and creates a Spring Boot executable web archive (WAR).
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public class BootWarOperation extends AbstractBootOperation {
    private static final Logger LOGGER = Logger.getLogger(BootWarOperation.class.getName());
    private final List<File> webInfProvidedLibs_ = new ArrayList<>();

    /**
     * Provides the destination file name that will be used for the WAR creation.
     *
     * @param name the wAR archive destination file name
     * @return this operation instance
     */
    @Override
    public BootWarOperation destinationArchiveFileName(String name) {
        return (BootWarOperation) super.destinationArchiveFileName(name);
    }

    /**
     * Provides the destination directory in which the WAR will be created.
     *
     * @param directory the WAR destination directory
     * @return this operation instance
     */
    @Override
    public BootWarOperation destinationDirectory(File directory) throws IOException {
        return (BootWarOperation) super.destinationDirectory(directory);
    }

    /**
     * Provides JAR libraries that will be used for the WAR creation.
     *
     * @param jars Java archive files
     * @return this operation instance
     */
    @Override
    public BootWarOperation infLibs(List<File> jars) {
        return (BootWarOperation) super.infLibs(jars);
    }

    /**
     * Provides JAR libraries that will be used for the WAR creation.
     *
     * @param jar Java archive file
     * @return this operation instance
     */
    @Override
    public BootWarOperation infLibs(File... jar) {
        return (BootWarOperation) super.infLibs(jar);
    }

    /**
     * Part of the {@link #execute} operation, configure the JAR launcher ({@code spring-boot-loader}) class.
     */
    @Override
    public BootWarOperation launcherClass(String className) {
        return (BootWarOperation) super.launcherClass(className);
    }

    /**
     * Part of the {@link #execute} operation, configure the launcher ({@code spring-boot-loader}) JAR(s).
     */
    @Override
    public BootWarOperation launcherJars(List<File> jars) throws IOException {
        return (BootWarOperation) super.launcherJars(jars);
    }

    /**
     * Provides the fully-qualified main class name.
     */
    @Override
    public BootWarOperation mainClass(String className) {
        return (BootWarOperation) super.mainClass(className);
    }

    /**
     * Provides an attribute to put in the JAR manifest.
     *
     * @param name  the attribute name to put in the manifest
     * @param value the attribute value to put in the manifest
     * @return this operation instance
     */
    @Override
    public BootWarOperation manifestAttribute(String name, String value) {
        return (BootWarOperation) super.manifestAttribute(name, value);
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
    public BootWarOperation manifestAttributes(Collection<BootManifestAttribute> attributes) {
        return (BootWarOperation) super.manifestAttributes(attributes);
    }

    /**
     * Provides the bld project.
     */
    @Override
    public BootWarOperation project(Project project) {
        return (BootWarOperation) super.project(project);
    }

    /**
     * Provides source directories that will be used for the jar archive creation.
     *
     * @param directories source directories
     * @return this operation instance
     */
    @Override
    public BootWarOperation sourceDirectories(File... directories) {
        return (BootWarOperation) super.sourceDirectories(directories);
    }

    /**
     * Performs the BootJar operation.
     */
    @Override
    public void execute() throws Exception {
        verifyExecute();

        var staging_dir = Files.createTempDirectory("bootwar").toFile();

        try {
            var web_inf_dir = executeCreateWebInfDirectory(staging_dir);
            executeCopyInfClassesFiles(web_inf_dir);
            executeCopyInfLibs(web_inf_dir);
            executeCopyWebInfProvidedLib(web_inf_dir);
            executeCopyBootLoader(staging_dir);

            executeCreateArchive(staging_dir, LOGGER);

            if (!silent() && LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info(String.format("The executable WAR (%s) was created in: %s%n", destinationArchiveFileName(),
                        destinationDirectory()));
            }
        } finally {
            FileUtils.deleteDirectory(staging_dir);
        }
    }

    /**
     * Part of the {@link #execute} operation, copy the {@code BOOT-INF} libs.
     */
    protected void executeCopyWebInfProvidedLib(File stagingBootInfDirectory) throws IOException {
        var boot_inf_lib_dir = new File(stagingBootInfDirectory, "lib");
        mkDirs(boot_inf_lib_dir);

        for (var jar : webInfProvidedLibs_) {
            Files.copy(jar.toPath(), boot_inf_lib_dir.toPath().resolve(jar.getName()));
        }
    }

    /**
     * Part of the {@link #execute} operation, creates the {@code WEB-INF} staging directory.
     */
    protected File executeCreateWebInfDirectory(File stagingDirectory) throws IOException {
        var boot_inf = new File(stagingDirectory, "WEB-INF");
        mkDirs(boot_inf);
        return boot_inf;
    }

    /**
     * Configures the operation from a {@link Project}.
     *
     * @param project the project to configure the operation from
     */
    public BootWarOperation fromProject(Project project) throws IOException {
        project(project);
        mainClass(project.mainClass());

        return destinationDirectory(project.buildDistDirectory())
                .destinationArchiveFileName(project.archiveBaseName() + "-" + project.version() + "-boot.war")
                .infLibs(project.compileClasspathJars())
                .infLibs(project.runtimeClasspathJars())
                .infLibs(project.buildDistDirectory())
                // TODO add provided libs
                .launcherClass("org.springframework.boot.loader.WarLauncher")
                .launcherJars(project.standaloneClasspathJars())
                .manifestAttributes(
                        List.of(
                                new BootManifestAttribute("Manifest-Version", "1.0"),
                                new BootManifestAttribute("Main-Class", launcherClass()),
                                new BootManifestAttribute("Start-Class", mainClass())
                        ))
                .sourceDirectories(project.buildMainDirectory(), project.srcMainResourcesDirectory());
    }


    /**
     * Provides JAR libraries that will be used for the WAR creation in {@code /WEB-INF/lib-provided}.
     *
     * @param jars Java archive files
     * @return this operation instance
     */
    public BootWarOperation webInfProvidedLibs(Collection<File> jars) {
        webInfProvidedLibs_.addAll(jars);
        return this;
    }

    /**
     * Provides the JAR libraries that will be used for the WAR creation in {@code /WEB-INF/lib-provided}.
     *
     * @param jar Java archive file
     * @return this operation instance
     */
    public BootWarOperation webInfProvidedLibs(File... jar) {
        webInfProvidedLibs_.addAll(List.of(jar));
        return this;
    }
}