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

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import rife.bld.Project;
import rife.bld.extension.tools.CollectionTools;
import rife.bld.extension.tools.ObjectTools;
import rife.tools.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public class BootWarOperation extends AbstractBootOperation<BootWarOperation> {

    private static final String PROVIDED_LIBS = "providedLibs";
    private final List<File> providedLibs_ = new ArrayList<>();

    /**
     * Performs the BootWar operation.
     */
    @Override
    public void execute() throws Exception {
        verifyExecute();

        var stagingDir = Files.createTempDirectory("bootwar").toFile();

        try {
            var webInfDir = new File(stagingDir, "WEB-INF");
            BootUtils.mkDirs(webInfDir);

            executeCopyInfClassesFiles(webInfDir);
            executeCopyInfLibs(webInfDir);
            executeCopyWebInfProvidedLib(webInfDir);
            executeCopyBootLoader(stagingDir);

            var archive = executeCreateArchive(stagingDir);

            if (!silent() && logger.isLoggable(Level.INFO)) {
                logger.info("The executable WAR was created: %s (%s)".formatted(
                        archive.getAbsolutePath(), BootUtils.fileSize(archive)));
            }
        } finally {
            FileUtils.deleteDirectory(stagingDir);
        }
    }

    /**
     * Configures the operation from a {@link Project}.
     * <p>
     * Sets the following:
     * <ul>
     *     <li>The {@link #destinationFileName(String) destination file name} to
     *     {@link Project#archiveBaseName() archiveBaseName} and {@link Project#version() version}</li>
     *     <li>The {@link #infLibs(Collection) INF libs} to {@link Project#compileClasspathJars() compileClasspathJars},
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
     * <p>
     * The project {@link Project#mainClass() main class} and {@link Project#version() version} must be defined.
     *
     * @param project the project
     * @return this operation instance
     * @throws NullPointerException     if the {@code project}, {@link Project#version() version} or
     *                                  {@link Project#mainClass() main class} are {@code null}
     * @throws IllegalArgumentException if the {@link Project#mainClass() main class} is blank or empty.
     */
    @Override
    public BootWarOperation fromProject(@NonNull Project project) {
        ObjectTools.requireNonNull(project, "fromProject");
        ObjectTools.requireNotEmpty(project.mainClass(), "project main class");
        ObjectTools.requireNonNull(project.version(), "project version");

        // Resolve launcher and main class first to avoid referencing an unset state
        // when building the manifest attributes map.
        var launcher = BootUtils.launcherClass(project, "WarLauncher");
        var main = project.mainClass();

        destinationDirectory(project.buildDistDirectory());
        destinationFileName(project.archiveBaseName() + "-" + project.version() + "-boot.war");
        if (!project.compileClasspathJars().isEmpty()) {
            infLibs(project.compileClasspathJars());
        }
        if (!project.runtimeClasspathJars().isEmpty()) {
            infLibs(project.runtimeClasspathJars());
        }
        infLibs(project.buildDistDirectory());
        launcherClass(launcher);
        if (!project.standaloneClasspathJars().isEmpty()) {
            launcherLibs(project.standaloneClasspathJars());
        }
        mainClass(main);
        manifestAttributes(Map.of(
                "Manifest-Version", "1.0",
                "Main-Class", launcher,
                "Start-Class", main)
        );
        if (!project.providedClasspathJars().isEmpty()) {
            providedLibs(project.providedClasspathJars());
        }
        sourceDirectories(project.buildMainDirectory(), project.srcMainResourcesDirectory());

        return this;
    }

    /**
     * Provides libraries that will be used for the WAR creation in {@code /WEB-INF/lib-provided}.
     *
     * @param jars a collection of Java archive files
     * @return this operation instance
     * @throws NullPointerException     if {@code jars} is null
     * @throws IllegalArgumentException if {@code jars} is empty, or contains {@code null} or empty elements
     * @see #providedLibs(File...)
     */
    public final BootWarOperation providedLibs(@NonNull Collection<File> jars) {
        ObjectTools.requireNotEmpty(jars, PROVIDED_LIBS);
        providedLibs_.addAll(jars);
        return this;
    }

    /**
     * Provides the libraries that will be used for the WAR creation in {@code /WEB-INF/lib-provided}.
     *
     * @param jars one or more Java archive files
     * @return this operation instance
     * @throws NullPointerException     if {@code jars} is null
     * @throws IllegalArgumentException if {@code jars} is empty, or contains {@code null} or empty elements
     * @see #providedLibsStrings(Collection)
     */
    public BootWarOperation providedLibs(@NonNull String... jars) {
        ObjectTools.requireNotEmpty(jars, PROVIDED_LIBS);
        providedLibs_.addAll(CollectionTools.combineStringsToFiles(jars));
        return this;
    }

    /**
     * Provides the libraries that will be used for the WAR creation in {@code /WEB-INF/lib-provided}.
     *
     * @param jars one or more Java archive files
     * @return this operation instance
     * @throws NullPointerException     if {@code jars} is null
     * @throws IllegalArgumentException if {@code jars} is empty, or contains {@code null} or empty elements
     * @see #providedLibs(Collection)
     */
    public BootWarOperation providedLibs(@NonNull File... jars) {
        ObjectTools.requireNotEmpty(jars, PROVIDED_LIBS);
        providedLibs_.addAll(List.of(jars));
        return this;
    }

    /**
     * Provides the libraries that will be used for the WAR creation in {@code /WEB-INF/lib-provided}.
     *
     * @param jars one or more Java archive files
     * @return this operation instance
     * @throws NullPointerException     if {@code jars} is null
     * @throws IllegalArgumentException if {@code jars} is empty, or contains {@code null} or empty elements
     * @see #providedLibsPaths(Collection)
     */
    public BootWarOperation providedLibs(@NonNull Path... jars) {
        ObjectTools.requireNotEmpty(jars, PROVIDED_LIBS);
        providedLibs_.addAll(CollectionTools.combinePathsToFiles(jars));
        return this;
    }

    /**
     * Retrieves the libraries that will be used for the WAR creation in {@code /WEB-INF/lib-provided}.
     *
     * @return the list of Java archive files.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<File> providedLibs() {
        return providedLibs_;
    }

    /**
     * Provides the libraries that will be used for the WAR creation in {@code /WEB-INF/lib-provided}.
     *
     * @param jars one or more Java archive files
     * @return this operation instance
     * @throws NullPointerException     if {@code jars} is null
     * @throws IllegalArgumentException if {@code jars} is empty, or contains {@code null} or empty elements
     * @see #providedLibs(Path...)
     */
    public final BootWarOperation providedLibsPaths(@NonNull Collection<Path> jars) {
        ObjectTools.requireNotEmpty(jars, "providedLibsPaths");
        providedLibs_.addAll(CollectionTools.combinePathsToFiles(jars));
        return this;
    }

    /**
     * Provides the libraries that will be used for the WAR creation in {@code /WEB-INF/lib-provided}.
     *
     * @param jars one or more Java archive files
     * @return this operation instance
     * @throws NullPointerException     if {@code jars} is null
     * @throws IllegalArgumentException if {@code jars} is empty, or contains {@code null} or empty elements
     * @see #providedLibs(String...)
     */
    public final BootWarOperation providedLibsStrings(@NonNull Collection<String> jars) {
        ObjectTools.requireNotEmpty(jars, "providedLibsStrings");
        providedLibs_.addAll(CollectionTools.combineStringsToFiles(jars));
        return this;
    }

    /**
     * Part of the {@link #execute execute} operation, copies the {@code WEB-INF/lib-provided} libraries.
     *
     * @param stagingWebInfDirectory the staging {@code WEB-INF} directory
     * @throws IOException if an error occurs
     */
    protected void executeCopyWebInfProvidedLib(File stagingWebInfDirectory) throws IOException {
        var libProvidedPath = stagingWebInfDirectory.toPath().resolve("lib-provided");
        BootUtils.mkDirs(libProvidedPath.toFile());

        for (var lib : providedLibs_) {
            if (lib.exists()) {
                Files.copy(lib.toPath(), libProvidedPath.resolve(lib.getName()), StandardCopyOption.REPLACE_EXISTING);
            } else if (!silent() && logger.isLoggable(Level.WARNING)) {
                logger.warning("File not found: " + lib);
            }
        }
    }
}