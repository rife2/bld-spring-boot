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
import java.util.List;
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

        var staging_dir = Files.createTempDirectory("bootjar").toFile();

        try {
            var boot_inf_dir = executeCreateBootInfDirectory(staging_dir);
            executeCopyInfClassesFiles(boot_inf_dir);
            executeCopyInfLibs(boot_inf_dir);
            executeCopyBootLoader(staging_dir);

            var archive = executeCreateArchive(staging_dir);

            if (!silent() && LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info("The executable JAR was created: " + archive.getAbsolutePath());
            }
        } finally {
            FileUtils.deleteDirectory(staging_dir);
        }
    }

    /**
     * Part of the {@link #execute} operation, creates the {@code BOOT-INF} staging directory.
     *
     * @param stagingDirectory the staging directory
     * @return the {@code BOOT-INF} directory location
     */
    protected File executeCreateBootInfDirectory(File stagingDirectory) throws IOException {
        var boot_inf = new File(stagingDirectory, "BOOT-INF");
        mkDirs(boot_inf);
        return boot_inf;
    }

    /**
     * Configures the operation from a {@link Project}.
     *
     * @param project the project
     * @return this operation instance
     */
    public BootJarOperation fromProject(Project project) throws IOException {
        mainClass(project.mainClass());

        return destinationDirectory(project.buildDistDirectory())
                .destinationFileName(project.archiveBaseName() + "-" + project.version() + "-boot.jar")
                .infLibs(project.compileClasspathJars())
                .infLibs(project.runtimeClasspathJars())
                .launcherClass("org.springframework.boot.loader.JarLauncher")
                .launcherLibs(project.standaloneClasspathJars())
                .manifestAttributes(
                        List.of(
                                new BootManifestAttribute("Manifest-Version", "1.0"),
                                new BootManifestAttribute("Main-Class", launcherClass()),
                                new BootManifestAttribute("Start-Class", mainClass()))
                )
                .sourceDirectories(project.buildMainDirectory(), project.srcMainResourcesDirectory());
    }
}