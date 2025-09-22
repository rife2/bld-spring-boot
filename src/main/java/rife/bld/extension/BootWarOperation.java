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

import rife.bld.Project;
import rife.tools.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Builds and creates a Spring Boot executable web archive (WAR).
 *
 * <h4>Usage:</h4>
 *
 * <blockquote><pre>
 * &#64;BuildCommand(summary = "Creates an executable WAR for the project")
 * public void bootwar() throws Exception {
 *     new BootWarOperation()
 *             .fromProject(this)
 *             .execute();
 * }
 * </pre></blockquote>
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a> @since 1.0
 */
public class BootWarOperation extends AbstractBootOperation<BootWarOperation> {
    private final List<File> providedLibs_ = new ArrayList<>();

    /**
     * Performs the BootJar operation.
     */
    @Override
    public void execute() throws Exception {
        verifyExecute();

        var stagingDir = Files.createTempDirectory("bootwar").toFile();

        try {
            var webInfDir = executeCreateWebInfDirectory(stagingDir);
            executeCopyInfClassesFiles(webInfDir);
            executeCopyInfLibs(webInfDir);
            executeCopyWebInfProvidedLib(webInfDir);
            executeCopyBootLoader(stagingDir);

            var archive = executeCreateArchive(stagingDir);

            if (!silent() && LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info(String.format("The executable WAR was created: %s (%s)", archive.getAbsolutePath(),
                        BootUtils.fileSize(archive)));
            }
        } finally {
            FileUtils.deleteDirectory(stagingDir);
        }
    }

    /**
     * Part of the {@link #execute execute} operation, copies the {@code WEB-INF/lib-provided} libraries.
     *
     * @param stagingWebInfDirectory the staging {@code WEB-INF/lib-provided} directory
     * @throws IOException if an error occurs
     */
    protected void executeCopyWebInfProvidedLib(File stagingWebInfDirectory) throws IOException {
        var libProvidedDir = new File(stagingWebInfDirectory, "lib-provided");
        BootUtils.mkDirs(libProvidedDir);

        for (var jar : providedLibs_) {
            if (jar.exists()) {
                Files.copy(jar.toPath(), libProvidedDir.toPath().resolve(jar.getName()));
            } else if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning("File not found: " + jar);
            }
        }
    }

    /**
     * Part of the {@link #execute execute} operation, creates the {@code WEB-INF} staging directory.
     *
     * @param stagingDirectory the staging directory
     * @return the {@code WEB-INF} directory location
     * @throws IOException if an error occurs
     */
    protected File executeCreateWebInfDirectory(File stagingDirectory) throws IOException {
        var bootInf = new File(stagingDirectory, "WEB-INF");
        BootUtils.mkDirs(bootInf);
        return bootInf;
    }

    /**
     * Configures the operation from a {@link Project}.
     * <p>
     * Sets the following:
     * <ul>
     *     <li>The {@link #destinationFileName(String) destination file name} to
     *     {@link Project#archiveBaseName() archiveBaseName} and {@link Project#version() version}</li>
     *     <li>The {@link #infLibs(File...) INF libs} to {@link Project#compileClasspathJars() compileClasspathJars},
     *     {@link Project#runtimeClasspathJars() runtimeClasspathJars} and
     *     {@link Project#buildDistDirectory() buildDistDirectory}</li>
     *     <li>The {@link #launcherClass(String) launcher class} to {@code WarLauncher}</li>
     *     <li>The {@link #launcherLibs(Collection) launcher libs} to
     *     {@link Project#standaloneClasspathJars() standaloneClasspathJars}</li>
     *     <li>The {@link #mainClass(String) main class} to {@link Project#mainClass() mainClass}</li>
     *     <li>The {@code Manifest-Version}, {@code Main-Class} and {@code Start-Class}
     *     {@link #manifestAttributes() manifest attributes}</li>
     *     <li>The {@link #sourceDirectories(File...) source directories} to
     *     {@link Project#buildMainDirectory() buildMainDirectory} and
     *     {@link Project#srcMainResourcesDirectory() srcMainResourcesDirectory}</li>
     * </ul>
     *
     * @param project the project
     * @return this operation instance
     */
    @Override
    public BootWarOperation fromProject(Project project) throws IOException {
        return destinationDirectory(project.buildDistDirectory())
                .destinationFileName(project.archiveBaseName() + "-" + project.version() + "-boot.war")
                .infLibs(project.compileClasspathJars())
                .infLibs(project.runtimeClasspathJars())
                .infLibs(project.buildDistDirectory())
                .launcherClass(BootUtils.launcherClass(project, "WarLauncher"))
                .launcherLibs(project.standaloneClasspathJars())
                .mainClass(project.mainClass())
                .manifestAttributes(Map.of(
                        "Manifest-Version", "1.0",
                        "Main-Class", launcherClass(),
                        "Start-Class", mainClass()))
                .providedLibs(project.providedClasspathJars())
                .sourceDirectories(project.buildMainDirectory(), project.srcMainResourcesDirectory());
    }

    /**
     * Provides libraries that will be used for the WAR creation in {@code /WEB-INF/lib-provided}.
     *
     * @param jars a collection of Java archive files
     * @return this operation instance
     * @see #providedLibs(File...)
     */
    public BootWarOperation providedLibs(Collection<File> jars) {
        providedLibs_.addAll(jars);
        return this;
    }

    /**
     * Provides the libraries that will be used for the WAR creation in {@code /WEB-INF/lib-provided}.
     *
     * @param jars one or more Java archive files
     * @return this operation instance
     * @see #providedLibsStrings(Collection)
     */
    public BootWarOperation providedLibs(String... jars) {
        return providedLibsStrings(List.of(jars));
    }

    /**
     * Provides the libraries that will be used for the WAR creation in {@code /WEB-INF/lib-provided}.
     *
     * @param jars one or more Java archive files
     * @return this operation instance
     * @see #providedLibs(Collection)
     */
    public BootWarOperation providedLibs(File... jars) {
        return providedLibs(List.of(jars));
    }

    /**
     * Provides the libraries that will be used for the WAR creation in {@code /WEB-INF/lib-provided}.
     *
     * @param jars one or more Java archive files
     * @return this operation instance
     * @see #providedLibsPaths(Collection)
     */
    public BootWarOperation providedLibs(Path... jars) {
        return providedLibsPaths(List.of(jars));
    }

    /**
     * Retrieves the libraries that will be used for the WAR creation in {@code /WEB-INF/lib-provided}.
     *
     * @return the list of Java archive files.
     */
    public List<File> providedLibs() {
        return providedLibs_;
    }

    /**
     * Provides the libraries that will be used for the WAR creation in {@code /WEB-INF/lib-provided}.
     *
     * @param jars one or more Java archive files
     * @return this operation instance
     * @see #providedLibs(Path...)
     */
    public BootWarOperation providedLibsPaths(Collection<Path> jars) {
        return providedLibs(jars.stream().map(Path::toFile).toList());
    }

    /**
     * Provides the libraries that will be used for the WAR creation in {@code /WEB-INF/lib-provided}.
     *
     * @param jars one or more Java archive files
     * @return this operation instance
     * @see #providedLibs(String...)
     */
    public BootWarOperation providedLibsStrings(Collection<String> jars) {
        return providedLibs(jars.stream().map(File::new).toList());
    }
}
