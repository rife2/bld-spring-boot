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
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builds and creates a Spring Boot executable Java archive (JAR).
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public class BootJarOperation extends AbstractBootOperation<BootJarOperation> {
    private static final Logger LOGGER = Logger.getLogger(BootJarOperation.class.getName());

    /**
     * Performs the BootJar operation.
     */
    @Override
    public void execute() throws Exception {
        verifyExecute();

        var stagingDir = Files.createTempDirectory("bootjar").toFile();

        try {
            var bootInfDir = executeCreateBootInfDirectory(stagingDir);
            executeCopyInfClassesFiles(bootInfDir);
            executeCopyInfLibs(bootInfDir);
            executeCopyBootLoader(stagingDir);

            var archive = executeCreateArchive(stagingDir);

            if (!silent() && LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info(String.format("The executable JAR was created: %s (%s)", archive.getAbsolutePath(),
                        BootUtils.fileSize(archive)));
            }
        } finally {
            FileUtils.deleteDirectory(stagingDir);
        }
    }

    /**
     * Part of the {@link #execute execute} operation, creates the {@code BOOT-INF} staging directory.
     *
     * @param stagingDirectory the staging directory
     * @return the {@code BOOT-INF} directory location
     * @throws IOException if an error occurs
     */
    protected File executeCreateBootInfDirectory(File stagingDirectory) throws IOException {
        var bootInf = new File(stagingDirectory, "BOOT-INF");
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
     *
     * @param project the project
     * @return this operation instance
     */
    @Override
    public BootJarOperation fromProject(Project project) throws IOException {
        return destinationDirectory(project.buildDistDirectory())
                .destinationFileName(project.archiveBaseName() + "-" + project.version() + "-boot.jar")
                .infLibs(project.compileClasspathJars())
                .infLibs(project.runtimeClasspathJars())
                .launcherClass(BootUtils.launcherClass(project, "JarLauncher"))
                .launcherLibs(project.standaloneClasspathJars())
                .mainClass(project.mainClass())
                .manifestAttributes(Map.of(
                        "Manifest-Version", "1.0",
                        "Main-Class", launcherClass(),
                        "Start-Class", mainClass()))
                .sourceDirectories(project.buildMainDirectory(), project.srcMainResourcesDirectory());
    }
}
