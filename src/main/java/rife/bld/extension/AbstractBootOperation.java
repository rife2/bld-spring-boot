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
import rife.bld.operations.AbstractOperation;
import rife.tools.FileUtils;
import rife.tools.exceptions.FileUtilsErrorException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.spi.ToolProvider;

/**
 * Implements commons methods used by Spring Boot operations, such as {@link BootJarOperation} and
 * {@link BootWarOperation}.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public abstract class AbstractBootOperation extends AbstractOperation<AbstractBootOperation> {
    private final List<File> infLibs_ = new ArrayList<>();
    private final List<File> launcherJars_ = new ArrayList<>();
    private final List<BootManifestAttribute> manifestAttributes_ = new ArrayList<>();
    private final List<File> sourceDirectories_ = new ArrayList<>();
    private String destinationArchiveFileName;
    private File destinationDirectory_;
    private String launcherClass_;
    private String mainClass_;
    private Project project_;

    public void deleteDirectories(File... directory) throws FileUtilsErrorException {
        for (var d : directory) {
            if (d.exists()) {
                FileUtils.deleteDirectory(d);
            }
        }
    }

    /**
     * Provides the file name that will be used for the archive creation.
     *
     * @param name the archive file name
     * @return this operation instance
     */
    public AbstractBootOperation destinationArchiveFileName(String name) {
        destinationArchiveFileName = name;
        return this;
    }

    /**
     * Retrieves the file name that will be used for the archive creation.
     *
     * @return the archive file name
     */
    public String destinationArchiveFileName() {
        return destinationArchiveFileName;
    }

    /**
     * Retrieves the destination directory in which the JAR will be created.
     *
     * @return the destination directory
     */
    public File destinationDirectory() {
        return destinationDirectory_;
    }

    /**
     * Provides the destination directory in which the archive will be created.
     *
     * @param directory the destination directory
     * @return this operation instance
     */
    public AbstractBootOperation destinationDirectory(File directory) throws IOException {
        destinationDirectory_ = directory;
        mkDirs(destinationDirectory_);
        return this;
    }

    /**
     * Part of the {@link #execute} operation, copy the {@code spring-boot-loader} archive content to the staging
     * directory.
     */
    protected void executeCopyBootLoader(File stagingDirectory) throws FileUtilsErrorException {
        if (launcherJars_.isEmpty()) {
            throw new IllegalArgumentException("ERROR: Spring Boot Loader required.");
        } else {
            var meta_inf_dir = new File(stagingDirectory, "META-INF");
            for (var jar : launcherJars()) {
                FileUtils.unzipFile(jar, stagingDirectory);
                if (meta_inf_dir.exists()) {
                    FileUtils.deleteDirectory(meta_inf_dir);
                }
            }
        }
    }

    /**
     * Part of the {@link #execute} operation, copy the {@code BOOT-INF} or {@code WEB-INF} classes.
     */
    protected void executeCopyInfClassesFiles(File stagingInfDirectory) throws IOException {
        var inf_classes_dir = new File(stagingInfDirectory, "classes");
        mkDirs(inf_classes_dir);

        for (var dir : sourceDirectories()) {
            FileUtils.copyDirectory(dir, inf_classes_dir);
        }

        deleteDirectories(new File(inf_classes_dir, "resources"), new File(inf_classes_dir, "templates"));
    }

    /**
     * Part of the {@link #execute} operation, copy the {@code BOOT-INF} or (@code WEB-INF) libs.
     */
    protected void executeCopyInfLibs(File stagingInfDirectory) throws IOException {
        var inf_lib_dir = new File(stagingInfDirectory, "lib");
        mkDirs(inf_lib_dir);

        for (var jar : infLibs_) {
            Files.copy(jar.toPath(), inf_lib_dir.toPath().resolve(jar.getName()));
        }
    }

    /**
     * Part of the {@link #execute} operation, create the archive from the staging directory.
     */
    protected void executeCreateArchive(File stagingDirectory, Logger logger)
            throws IOException {
        executeCreateManifest(stagingDirectory);
        if (logger.isLoggable(Level.FINE) && (!silent())) {
            logger.fine(MessageFormat.format("Staging Directory:     {0} (exists:{1})", stagingDirectory,
                    stagingDirectory.exists()));
            logger.fine(MessageFormat.format("Destination Directory: {0} (exists:{1})", destinationDirectory(),
                    destinationDirectory().exists()));
            logger.fine(MessageFormat.format("Destination Archive:   {0}", destinationArchiveFileName()));
        }

        var out = new StringWriter();
        var stdout = new PrintWriter(out);
        var err = new StringWriter();
        var stderr = new PrintWriter(err);
        var jarTool = ToolProvider.findFirst("jar").orElseThrow();

        String args;
        if (logger.isLoggable(Level.FINER)) {
            args = "-0cMvf";
        } else {
            args = "-0cMf";
        }

        jarTool.run(stdout, stderr, args,
                new File(destinationDirectory(), destinationArchiveFileName()).getAbsolutePath(),
                "-C", stagingDirectory.getAbsolutePath(), ".");

        var errBuff = err.getBuffer();
        if (!errBuff.isEmpty()) {
            throw new IOException(errBuff.toString());
        } else {
            var outBuff = out.getBuffer();
            if (!outBuff.isEmpty() && logger.isLoggable(Level.INFO) && !silent()) {
                logger.info(outBuff.toString());
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

        try (var fileWriter = Files.newBufferedWriter(manifest.toPath())) {
            for (var manifestAttribute : manifestAttributes()) {
                fileWriter.write(manifestAttribute.name() + ": " + manifestAttribute.value() + System.lineSeparator());
            }
        }
    }

    /**
     * Provides JAR libraries that will be stored in {@code BOOT-INF} or {@code WEB-INF}.
     *
     * @param jars Java archive files
     * @return this operation instance
     */
    public AbstractBootOperation infLibs(List<File> jars) {
        infLibs_.addAll(jars);
        return this;
    }

    /**
     * Provides JAR libraries that will be stored in {@code BOOT-INF} or {@code WEB-INF}.
     *
     * @param jar Java archive file
     * @return this operation instance
     */
    public AbstractBootOperation infLibs(File... jar) {
        infLibs_.addAll(List.of(jar));
        return this;
    }

    /**
     * Retrieves the JAR libraries in {@code BOOT-INF} or {@code WEB-INF}.
     */
    public List<File> infLibs() {
        return infLibs_;
    }

    /**
     * Part of the {@link #execute} operation, configure the JAR launcher ({@code spring-boot-loader}) class name.
     */
    public AbstractBootOperation launcherClass(String className) {
        launcherClass_ = className;
        return this;
    }

    /**
     * Retrieves the JAR launcher ({@code spring-boot-loader}) class fully-qualified name.
     */
    protected String launcherClass() {
        if (launcherClass_ == null) {
            throw new IllegalArgumentException("ERROR: Spring boot launcher (spring-boot-loader) class " +
                    "required.");
        }
        return launcherClass_;
    }

    /**
     * Retrieves the launcher ({@code spring-boot-loader}) JARs.
     */
    public List<File> launcherJars() {
        return launcherJars_;
    }

    /**
     * Part of the {@link #execute} operation, configure the launcher ({@code spring-boot-loader}) JAR location.
     */
    public AbstractBootOperation launcherJars(List<File> jars) throws IOException {
        if (!jars.isEmpty()) {
            for (var j : jars) {
                if (!j.exists()) {
                    throw new IOException("ERROR: launcher (spring-boot-loader) JAR(s) not found: " + j);
                }
            }
            launcherJars_.addAll(jars);
        }
        return this;
    }

    /**
     * Provides the fully-qualified main class name.
     */
    protected AbstractBootOperation mainClass(String className) {
        mainClass_ = className;
        return this;
    }

    /**
     * Retrieves the main class name.
     */
    public String mainClass() {
        return mainClass_;
    }

    /**
     * Provides an attribute to put in the JAR manifest.
     *
     * @param name  the attribute name to put in the manifest
     * @param value the attribute value to put in the manifest
     * @return this operation instance
     */
    public AbstractBootOperation manifestAttribute(String name, String value) {
        manifestAttributes_.add(new BootManifestAttribute(name, value));
        return this;
    }

    /**
     * Retrieves the list of attributes that will be put in the jar manifest.
     */
    public List<BootManifestAttribute> manifestAttributes() {
        return manifestAttributes_;
    }

    /**
     * Provides a map of attributes to put in the jar manifest.
     * <p>
     * A copy will be created to allow this map to be independently modifiable.
     *
     * @param attributes the attributes to put in the manifest
     * @return this operation instance
     */
    public AbstractBootOperation manifestAttributes(Collection<BootManifestAttribute> attributes) {
        manifestAttributes_.addAll(attributes);
        return this;
    }

    /**
     * Creates a directory for the given file path, including any necessary but nonexistent parent directories.
     */
    protected void mkDirs(File file) throws IOException {
        if (!file.exists() && !file.mkdirs()) {
            throw new IOException("ERROR: unable to create: " + file.getAbsolutePath());
        }
    }

    /**
     * Provides the bld project.
     */
    public AbstractBootOperation project(Project project) {
        project_ = project;
        return this;
    }

    /**
     * Retrieves the bld project.
     */
    public Project project() {
        return project_;
    }

    /**
     * Provides source directories that will be used for the jar archive creation.
     *
     * @param directories source directories
     * @return this operation instance
     */
    public AbstractBootOperation sourceDirectories(File... directories) {
        sourceDirectories_.addAll(List.of(directories));
        return this;
    }

    /**
     * Retrieves the source directories that will be used for the jar archive creation.
     */
    public List<File> sourceDirectories() {
        return sourceDirectories_;
    }

    /**
     * Verifies that all the elements required to create the archived have been provided, throws an
     * {@link IllegalArgumentException} otherwise.
     */
    protected boolean verifyExecute() throws IllegalArgumentException {
        if (mainClass() == null) {
            throw new IllegalArgumentException("ERROR: project mainClass required.");
        } else if (launcherClass().isEmpty()) {
            throw new IllegalArgumentException(("ERROR: launcher (spring-boot-loader) class required"));
        } else if (launcherJars().isEmpty()) {
            throw new IllegalArgumentException(("ERROR: launcher (spring-boot-loader) JAR(s) required"));
        }
        return true;
    }
}
