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
import rife.bld.Project;
import rife.bld.extension.tools.IOTools;
import rife.bld.extension.tools.ObjectTools;
import rife.tools.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;

/**
 * Builds and creates a Spring Boot executable Java archive (JAR).
 *
 * <h4>Usage:</h4>
 *
 * <blockquote><pre>
 * &#64;BuildCommand(summary = "Creates an executable JAR for the project")
 * public void bootjar() throws Exception {
 *     new BootJarOperation()
 *             .fromProject(this)
 *             .execute();
 * }
 * </pre></blockquote>
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public class BootJarOperation extends AbstractBootOperation<BootJarOperation> {

    /**
     * Performs the BootJar operation.
     */
    @Override
    @SuppressWarnings("RedundantThrows")
    public void execute() throws Exception {
        verifyExecute();

        var stagingDir = Files.createTempDirectory("bootjar").toFile();

        try {
            var bootInfDir = new File(stagingDir, "BOOT-INF");
            IOTools.createDirs(bootInfDir);

            executeCopyInfClassesFiles(bootInfDir);
            executeCopyInfLibs(bootInfDir);
            executeCopyBootLoader(stagingDir);

            var archive = executeCreateArchive(stagingDir);

            if (!silent() && logger.isLoggable(Level.INFO)) {
                logger.info("The executable JAR was created: %s (%s)".formatted(
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
     *     <li>The {@link #infLibs(File...) INF libs} to {@link Project#compileClasspathJars() compileClasspathJars}
     *     and {@link Project#runtimeClasspathJars() runtimeClasspathJars}</li>
     *     <li>The {@link #launcherClass(String) launcher class} to {@code JarLauncher}</li>
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
     */
    @Override
    public BootJarOperation fromProject(@NonNull Project project) {
        ObjectTools.requireNonNull(project, "fromProject");
        ObjectTools.requireNotEmpty(project.mainClass(), "project main class");
        ObjectTools.requireNonNull(project.version(), "project version");

        // Resolve launcher and main class first to avoid referencing an unset state
        // when building the manifest attributes map.
        var launcher = BootUtils.launcherClass(project, "JarLauncher");
        var main = project.mainClass();

        destinationDirectory(project.buildDistDirectory());
        destinationFileName(project.archiveBaseName() + "-" + project.version() + "-boot.jar");
        if (!project.compileClasspathJars().isEmpty()) {
            infLibs(project.compileClasspathJars());
        }
        if (!project.runtimeClasspathJars().isEmpty()) {
            infLibs(project.runtimeClasspathJars());
        }
        launcherClass(launcher);
        if (!project.standaloneClasspathJars().isEmpty()) {
            launcherLibs(project.standaloneClasspathJars());
        }
        mainClass(main);
        manifestAttributes(Map.of(
                "Manifest-Version", "1.0",
                "Main-Class", launcher,
                "Start-Class", main));
        sourceDirectories(project.buildMainDirectory(), project.srcMainResourcesDirectory());

        return this;
    }
}