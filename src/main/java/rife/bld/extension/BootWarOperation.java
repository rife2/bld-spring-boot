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
import java.util.logging.Logger;

/**
 * Builds and creates a Sprint Boot web archive (WAR).
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public class BootWarOperation extends AbstractBootOperation {
    private static final Logger LOGGER = Logger.getLogger(BootWarOperation.class.getName());
    private final List<File> webInfLibs_ = new ArrayList<>();
    private final List<File> webInfProvidedLibs_ = new ArrayList<>();

    /**
     * Performs the BootJar operation.
     */
    @Override
    public void execute() throws Exception {
        if (project_ == null) {
            throw new IllegalArgumentException("ERROR: project required.");
        } else if (project_.mainClass() == null) {
            throw new IllegalArgumentException("ERROR: project mainClass required.");
        }

        var staging_dir = Files.createTempDirectory("bootwar").toFile();

        try {
            var boot_web_inf_dir = executeCreateWebInfDirectory(staging_dir);
            executeCopyBootInfClassesFiles(boot_web_inf_dir);
            executeCopyWebInfLib(boot_web_inf_dir);
            executeCopyWebInfProvidedLib(boot_web_inf_dir);
            executeCopyBootLoader(staging_dir);

            executeCreateArchive(staging_dir, LOGGER);

            if (!silent()) {
                System.out.printf("The WAR (%s) was created in: %s%n", destinationArchiveFileName(),
                        destinationDirectory());
            }

        } finally {
            FileUtils.deleteDirectory(staging_dir);
        }
    }

    /**
     * Part of the {@link #execute} operation, copy the {@code BOOT-INF} libs.
     */
    protected void executeCopyWebInfLib(File stagingBootInfDirectory) throws IOException {
        var boot_inf_lib_dir = new File(stagingBootInfDirectory, "lib");
        mkDirs(boot_inf_lib_dir);

        for (var jar : webInfLibs_) {
            Files.copy(jar.toPath(), boot_inf_lib_dir.toPath().resolve(jar.getName()));
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
     * Configures the operation from a {@link Project}.
     *
     * @param project the project to configure the operation from
     */
    public AbstractBootOperation fromProject(Project project) throws IOException {
        project_ = project;
        return webInfLibs(project.compileClasspathJars())
                .webInfLibs(project.runtimeClasspathJars())
                .webInfLibs(project.buildDistDirectory())
                // TODO add provided libs
                .launcherClass("org.springframework.boot.loader.WarLauncher")
                .manifestAttributes(
                        List.of(
                                new BootManifestAttribute("Manifest-Version", "1.0"),
                                new BootManifestAttribute("Main-Class", launcherClass()),
                                new BootManifestAttribute("Start-Class", project.mainClass())
                        ))
                .destinationDirectory(project.buildDistDirectory())
                .destinationArchiveFileName(project.archiveBaseName() + "-" + project.version() + "-boot.war")
                .sourceDirectories(project.buildMainDirectory(), project.srcMainResourcesDirectory());
    }

    /**
     * Provides library JARs that will be used for the WAR creation.
     *
     * @param jars Java archive files
     * @return this operation instance
     */
    public BootWarOperation webInfLibs(Collection<File> jars) {
        webInfLibs_.addAll(jars);
        return this;
    }

    /**
     * Provides library JARs that will be used for the WAR creation.
     *
     * @param jar Java archive file
     * @return this operation instance
     */
    public BootWarOperation webInfLibs(File... jar) {
        webInfLibs_.addAll(List.of(jar));
        return this;
    }

    /**
     * Provides library JARs that will be used for the WAR creation in {@code /WEB-INF/lib-provided}.
     *
     * @param jars Java archive files
     * @return this operation instance
     */
    public BootWarOperation webInfProvidedLibs(Collection<File> jars) {
        webInfProvidedLibs_.addAll(jars);
        return this;
    }

    /**
     * Provides the library JARs that will be used for the WAR creation in {@code /WEB-INF/lib-provided}.
     *
     * @param jar Java archive file
     * @return this operation instance
     */
    public BootWarOperation webInfProvidedLibs(File... jar) {
        webInfProvidedLibs_.addAll(List.of(jar));
        return this;
    }
}