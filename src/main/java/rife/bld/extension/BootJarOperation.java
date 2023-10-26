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
import rife.bld.WebProject;
import rife.bld.operations.AbstractOperation;
import rife.tools.FileUtils;
import rife.tools.exceptions.FileUtilsErrorException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.spi.ToolProvider;
import java.util.zip.CRC32;

public class BootJarOperation extends AbstractOperation<BootJarOperation> {
    private static final Logger LOGGER = Logger.getLogger(BootJarOperation.class.getName());
    private final CRC32 crc32_ = new CRC32();
    private final List<File> libJars_ = new ArrayList<>();
    private final List<ManifestAttribute> manifestAttributes_ = new ArrayList<>();
    private final List<File> sourceDirectories_ = new ArrayList<>();
    private File destinationDirectory_;
    private String destinationJarFileName_;
    private String jarLauncherClass_ = "org.springframework.boot.loader.JarLauncher";
    private WebProject project_;

    private long computeCrc32Checksum(Path filePath) throws IOException {
        crc32_.reset();
        crc32_.update(Files.readAllBytes(filePath));

        return crc32_.getValue();
    }

    /**
     * Retrieves the destination directory in which the JAR will be created.
     *
     * @return the JAR's destination directory
     */
    public File destinationDirectory() {
        return destinationDirectory_;
    }

    /**
     * Provides the destination directory in which the JAR will be created.
     *
     * @param directory the war destination directory
     * @return this operation instance
     * @since 1.5
     */
    public BootJarOperation destinationDirectory(File directory) throws IOException {
        destinationDirectory_ = directory;
        mkDirs(destinationDirectory_);
        return this;
    }

    /**
     * Provides the destination file name that will be used for the JAR creation.
     *
     * @param name the war archive destination file name
     * @return this operation instance
     */
    public BootJarOperation destinationJarFileName(String name) {
        destinationJarFileName_ = name;
        return this;
    }

    /**
     * Retrieves the destination file name that will be used for the JAR creation.
     *
     * @return the war Jar's destination file name
     */
    public String destinationJarFileName() {
        return destinationJarFileName_;
    }

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

        var staging_dir = Files.createTempDirectory("bootjar").toFile();

        try {
            var boot_inf_dir = executeCreateBootInfDirectory(staging_dir);
            executeCopyBootInfClassesFiles(boot_inf_dir);
            executeCopyBootInfLibJars(boot_inf_dir);
            executeCopyBootLoader(staging_dir);

            executeCreateJar(staging_dir);

            if (!silent()) {
                System.out.printf("The executable JAR (%s) was created in: %s%n", destinationJarFileName(),
                        destinationDirectory());
            }

        } finally {
            FileUtils.deleteDirectory(staging_dir);
        }
    }

    /**
     * Part of the {@link #execute} operation, copy the {@code BOOT-INF} classes.
     */
    protected void executeCopyBootInfClassesFiles(File stagingBootInfDirectory) throws IOException {
        var boot_inf_classes_dir = new File(stagingBootInfDirectory, "classes");
        mkDirs(boot_inf_classes_dir);

        for (var dir : sourceDirectories_) {
            FileUtils.copyDirectory(dir, boot_inf_classes_dir);
        }

        var resources_dir = new File(boot_inf_classes_dir, "resources");
        if (resources_dir.exists()) {
            FileUtils.deleteDirectory(resources_dir);
        }
    }

    /**
     * Part of the {@link #execute} operation, copy the {@code BOOT-INF} libs.
     */
    protected void executeCopyBootInfLibJars(File stagingBootInfDirectory) throws IOException {
        var boot_inf_lib_dir = new File(stagingBootInfDirectory, "lib");
        mkDirs(boot_inf_lib_dir);

        for (var jar : libJars_) {
            Files.copy(jar.toPath(), boot_inf_lib_dir.toPath().resolve(jar.getName()));
        }
    }

    /**
     * Part of the {@link #execute} operation, copy the {@code spring-boot-loader} archive content to the staging directory.
     */
    protected void executeCopyBootLoader(File stagingDirectory) throws FileUtilsErrorException {
        if (project_.standaloneClasspathJars().isEmpty()) {
            throw new IllegalArgumentException("ERROR: Spring Boot Loader required.");
        } else {
            for (var jar : project_.standaloneClasspathJars()) {
                FileUtils.unzipFile(jar, stagingDirectory);
                var meta_inf_dir = new File(stagingDirectory, "META-INF");
                if (meta_inf_dir.exists()) {
                    FileUtils.deleteDirectory(meta_inf_dir);
                }
            }
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
     * Part of the {@link #execute} operation, create the executable JAR from the staging directory.
     */
    protected void executeCreateJar(File stagingDirectory)
            throws IOException {
        executeCreateManifest(stagingDirectory);
        if (LOGGER.isLoggable(Level.FINE) && (!silent())) {
            LOGGER.fine(MessageFormat.format("Staging Directory:     {0} (exists:{1})", stagingDirectory,
                    stagingDirectory.exists()));
            LOGGER.fine(MessageFormat.format("Destination Directory: {0} (exists:{1})", destinationDirectory(),
                    destinationDirectory().exists()));
            LOGGER.fine(MessageFormat.format("Destination JAR:       {0}", destinationJarFileName()));
        }

//        try (var zipOutputStream = new ZipOutputStream(new FileOutputStream(
//                new File(destinationDirectory(), destinationJarFileName())))) {
//            zipOutputStream.setLevel(ZipOutputStream.STORED);
//            zipOutputStream.setMethod(Deflater.NO_COMPRESSION);
//            var stagingPath = stagingDirectory.toPath();
//            Files.walkFileTree(stagingPath, new SimpleFileVisitor<>() {
//                public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
//                    var zipEntry = new ZipEntry(stagingPath.relativize(filePath).toString());
//                    zipEntry.setMethod(ZipEntry.STORED);
//                    zipEntry.setSize(filePath.toFile().length());
//                    zipEntry.setSize(filePath.toFile().length());
//                    zipEntry.setCrc(computeCrc32Checksum(filePath));
//                    zipOutputStream.putNextEntry(zipEntry);
//                    Files.copy(filePath, zipOutputStream);
//                    zipOutputStream.closeEntry();
//
//                    if (logger.isLoggable(Level.FINER) && !silent()) {
//                        logger.finer(MessageFormat.format("Added to JAR:         {0}", filePath));
//                    }
//
//                    return FileVisitResult.CONTINUE;
//                }
//            });
//        }

        var out = new StringWriter();
        var stdout = new PrintWriter(out);
        var err = new StringWriter();
        var stderr = new PrintWriter(err);
        var jarTool = ToolProvider.findFirst("jar").orElseThrow();

        String args;
        if (LOGGER.isLoggable(Level.FINER)) {
            args = "-0cMvf";
        } else {
            args = "-0cMf";
        }

        jarTool.run(stdout, stderr, args,
                new File(destinationDirectory(), destinationJarFileName()).getAbsolutePath(),
                "-C", stagingDirectory.getAbsolutePath(), ".");

        var errBuff = err.getBuffer();
        if (!errBuff.isEmpty()) {
            throw new IOException(errBuff.toString());
        } else {
            var outBuff = out.getBuffer();
            if (!outBuff.isEmpty()) {
                if (LOGGER.isLoggable(Level.INFO) && !silent()) {
                    LOGGER.info(outBuff.toString());
                }
            }
        }
    }

    /**
     * Part of the {@link #execute} operation, create the manifest for the jar archive.
     */
    protected void executeCreateManifest(File stagingDirectory) throws IOException {
        var meta_inf_dir = new File(stagingDirectory, "META-INF");
        mkDirs(meta_inf_dir);

        var manifest = new File(meta_inf_dir, "MANIFEST.MF");

        try (var fileWriter = new FileWriter(manifest)) {
            for (var manifestAttribute : manifestAttributes()) {
                fileWriter.write(manifestAttribute.name + ": " + manifestAttribute.value + System.lineSeparator());
            }
        }
    }

    /**
     * Configures the operation from a {@link Project}.
     *
     * @param project the project to configure the operation from
     */
    public BootJarOperation fromProject(WebProject project) throws IOException {
        project_ = project;
        return manifestAttributes(
                List.of(
                        new ManifestAttribute("Manifest-Version", "1.0"),
                        new ManifestAttribute("Main-Class", jarLauncherClass()),
                        new ManifestAttribute("Start-Class", project_.mainClass())
                ))
                .destinationDirectory(project.buildDistDirectory())
                .destinationJarFileName(project.jarFileName())
                .libJars(project_.compileClasspathJars())
                .libJars(project_.runtimeClasspathJars())
                .sourceDirectories(project_.buildMainDirectory(), project_.srcMainResourcesDirectory());
    }

    /**
     * Part of the {@link #execute} operation, configure the JAR launcher class.
     */
    protected BootJarOperation jarLauncherClass(String className) {
        jarLauncherClass_ = className;
        return this;
    }

    /**
     * Retrieves the JAR launcher class fully-qualified name.
     */
    protected String jarLauncherClass() {
        return jarLauncherClass_;
    }

    /**
     * Provides library JARs that will be used for the jar archive creation.
     *
     * @param jar Java archive file
     * @return this operation instance
     */
    public BootJarOperation libJars(File... jar) {
        libJars_.addAll(List.of(jar));
        return this;
    }

    /**
     * Provides library JARs that will be used for the jar archive creation.
     *
     * @param jars Java archive files
     * @return this operation instance
     */
    public BootJarOperation libJars(Collection<File> jars) {
        libJars_.addAll(jars);
        return this;
    }

    /**
     * Provides an attribute to put in the JAR manifest.
     *
     * @param name  the attribute name to put in the manifest
     * @param value the attribute value to put in the manifest
     * @return this operation instance
     */
    public BootJarOperation manifestAttribute(String name, String value) {
        manifestAttributes_.add(new ManifestAttribute(name, value));
        return this;
    }

    /**
     * Retrieves the list of attributes that will be put in the jar manifest.
     */
    public List<ManifestAttribute> manifestAttributes() {
        return manifestAttributes_;
    }

    /**
     * Provides a map of attributes to put in the jar manifest.
     * <p>
     * A copy will be created to allow this map to be independently modifiable.
     *
     * @param attributes the attributes to put in the manifest
     * @return this operation instance
     * \
     */
    public BootJarOperation manifestAttributes(Collection<ManifestAttribute> attributes) {
        manifestAttributes_.addAll(attributes);
        return this;
    }

    private void mkDirs(File file) throws IOException {
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new IOException("ERROR: unable to create: " + file.getAbsolutePath());
            }
        }
    }

    /**
     * Provides source directories that will be used for the jar archive creation.
     *
     * @param directories source directories
     * @return this operation instance
     */
    public BootJarOperation sourceDirectories(File... directories) {
        sourceDirectories_.addAll(List.of(directories));
        return this;
    }

    public record ManifestAttribute(String name, String value) {
    }
}