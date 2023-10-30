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
 * Implements common methods used by Spring Boot operations, such as {@link BootJarOperation} and
 * {@link BootWarOperation}.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public abstract class AbstractBootOperation<T extends AbstractBootOperation<T>>
        extends AbstractOperation<AbstractBootOperation<T>> {
    private final List<File> infLibs_ = new ArrayList<>();
    private final List<File> launcherLibs_ = new ArrayList<>();
    private final List<BootManifestAttribute> manifestAttributes_ = new ArrayList<>();
    private final List<File> sourceDirectories_ = new ArrayList<>();
    private File destinationDirectory_;
    private String destinationFileName_;
    private String launcherClass_;
    private String mainClass_;

    /**
     * Deletes the given directory.
     *
     * @param directory the directory to delete
     */
    public void deleteDirectories(File... directory) throws FileUtilsErrorException {
        for (var d : directory) {
            if (d.exists()) {
                FileUtils.deleteDirectory(d);
            }
        }
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
    public T destinationDirectory(File directory) throws IOException {
        destinationDirectory_ = directory;
        mkDirs(destinationDirectory_);
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Provides the file name that will be used for the archive creation.
     *
     * @param name the archive file name
     * @return this operation instance
     */
    public T destinationFileName(String name) {
        destinationFileName_ = name;
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Retrieves the file name that will be used for the archive creation.
     *
     * @return the archive file name
     */
    public String destinationFileName() {
        return destinationFileName_;
    }

    /**
     * Part of the {@link #execute} operation, copy the {@code spring-boot-loader} archive content to the staging
     * directory.
     *
     * @param stagingDirectory the staging directory
     */
    protected void executeCopyBootLoader(File stagingDirectory) throws FileUtilsErrorException {
        if (launcherLibs_.isEmpty()) {
            throw new IllegalArgumentException("ERROR: Spring Boot Loader required.");
        } else {
            var meta_inf_dir = new File(stagingDirectory, "META-INF");
            for (var jar : launcherLibs()) {
                FileUtils.unzipFile(jar, stagingDirectory);
                if (meta_inf_dir.exists()) {
                    FileUtils.deleteDirectory(meta_inf_dir);
                }
            }
        }
    }

    /**
     * Part of the {@link #execute} operation, copy the {@code BOOT-INF} or {@code WEB-INF} classes.
     *
     * @param stagingInfDirectory Tte staging {@code INF} directory
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
     *
     * @param stagingInfDirectory the staging {@code INF} directory
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
     *
     * @param stagingDirectory the staging directory
     * @param logger           the logger instance
     */
    protected void executeCreateArchive(File stagingDirectory, Logger logger)
            throws IOException {
        executeCreateManifest(stagingDirectory);
        if (logger.isLoggable(Level.FINE) && (!silent())) {
            logger.fine(MessageFormat.format("Staging Directory:     {0} (exists:{1})", stagingDirectory,
                    stagingDirectory.exists()));
            logger.fine(MessageFormat.format("Destination Directory: {0} (exists:{1})", destinationDirectory(),
                    destinationDirectory().exists()));
            logger.fine(MessageFormat.format("Destination Archive:   {0}", destinationFileName()));
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
                new File(destinationDirectory(), destinationFileName()).getAbsolutePath(),
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
     *
     * @param stagingDirectory the staging directory
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
     * @param jars a list of Java archive files
     * @return this operation instance
     */
    public T infLibs(List<File> jars) {
        infLibs_.addAll(jars);
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Provides JAR libraries that will be stored in {@code BOOT-INF} or {@code WEB-INF}.
     *
     * @param jar one or more Java archive file
     * @return this operation instance
     */
    public T infLibs(File... jar) {
        infLibs_.addAll(List.of(jar));
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Retrieves the JAR libraries in {@code BOOT-INF} or {@code WEB-INF}.
     *
     * @return a list of Java archives
     */
    public List<File> infLibs() {
        return infLibs_;
    }

    /**
     * Provides the JAR launcher ({@code spring-boot-loader}) fully-qualified class name.
     *
     * @param className the launcher class name
     * @return this operation instance
     */
    public T launcherClass(String className) {
        launcherClass_ = className;
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Retrieves the JAR launcher ({@code spring-boot-loader}) fully-qualified class name.
     *
     * @return the launcher class name
     */
    protected String launcherClass() {
        if (launcherClass_ == null) {
            throw new IllegalArgumentException("ERROR: Spring boot launcher (spring-boot-loader) class " +
                    "required.");
        }
        return launcherClass_;
    }

    /**
     * Retrieves the launcher ({@code spring-boot-loader}) JAR libraries.
     *
     * @return a list of Java archives
     */
    public List<File> launcherLibs() {
        return launcherLibs_;
    }

    /**
     * Provides the JAR libraries for the launcher ({@code spring-boot-loader}).
     *
     * @param jars a list of a Java archives
     * @return this operation instance
     */
    public T launcherLibs(List<File> jars) throws IOException {
        if (!jars.isEmpty()) {
            for (var j : jars) {
                if (!j.exists()) {
                    throw new IOException("ERROR: launcher (spring-boot-loader) JAR(s) not found: " + j);
                }
            }
            launcherLibs_.addAll(jars);
        }
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Provides the fully-qualified main class name.
     *
     * @param className the class name
     * @return this operation instance
     */
    protected T mainClass(String className) {
        mainClass_ = className;
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Retrieves the main class name.
     *
     * @return the class name
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
    public T manifestAttribute(String name, String value) {
        manifestAttributes_.add(new BootManifestAttribute(name, value));
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Retrieves the list of attributes that will be put in the jar manifest.
     *
     * @return a list of manifest attributes
     */
    public List<BootManifestAttribute> manifestAttributes() {
        return manifestAttributes_;
    }

    /**
     * Provides a map of attributes to put in the jar manifest.
     *
     * @param attributes the attributes to put in the manifest
     * @return this operation instance
     */
    public T manifestAttributes(Collection<BootManifestAttribute> attributes) {
        manifestAttributes_.addAll(attributes);
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Makes a directory for the given path, including any necessary but nonexistent parent directories.
     *
     * @param path the directory path
     */
    protected void mkDirs(File path) throws IOException {
        if (!path.exists() && !path.mkdirs()) {
            throw new IOException("ERROR: unable to create: " + path.getAbsolutePath());
        }
    }

    /**
     * Provides source directories that will be used for the jar archive creation.
     *
     * @param directories one or more source directory
     * @return this operation instance
     */
    public T sourceDirectories(File... directories) {
        sourceDirectories_.addAll(List.of(directories));
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Retrieves the source directories that will be used for the jar archive creation.
     *
     * @return a list of directories
     */
    public List<File> sourceDirectories() {
        return sourceDirectories_;
    }

    /**
     * Verifies that all the elements required to create the archived have been provided, throws an
     * {@link IllegalArgumentException} otherwise.
     *
     * @return {@code true} or an {@link IllegalArgumentException}
     */
    protected boolean verifyExecute() throws IllegalArgumentException {
        if (mainClass() == null) {
            throw new IllegalArgumentException("ERROR: project mainClass required.");
        } else if (launcherClass().isEmpty()) {
            throw new IllegalArgumentException(("ERROR: launcher (spring-boot-loader) class required"));
        } else if (launcherLibs().isEmpty()) {
            throw new IllegalArgumentException(("ERROR: launcher (spring-boot-loader) JAR(s) required"));
        }
        return true;
    }
}
