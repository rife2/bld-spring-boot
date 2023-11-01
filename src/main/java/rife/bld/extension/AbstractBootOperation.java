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
 * Implements common methods used by Spring Boot operations, such as {@link BootJarOperation} and
 * {@link BootWarOperation}.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public abstract class AbstractBootOperation<T extends AbstractBootOperation<T>>
        extends AbstractOperation<AbstractBootOperation<T>> {
    private static final Logger LOGGER = Logger.getLogger(AbstractBootOperation.class.getName());
    private final List<File> infLibs_ = new ArrayList<>();
    private final List<File> launcherLibs_ = new ArrayList<>();
    private final List<BootManifestAttribute> manifestAttributes_ = new ArrayList<>();
    private final List<File> sourceDirectories_ = new ArrayList<>();
    private File destinationDirectory_;
    private String destinationFileName_;
    private String launcherClass_;
    private String mainClass_;

    /**
     * Retrieves the destination directory in which the archive will be created.
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
        BootUtils.mkDirs(destinationDirectory_);
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
     * Part of the {@link #execute execute} operation, copies the Spring Boot loader launcher archive content to the
     * staging directory.
     *
     * @param stagingDirectory the staging directory
     */
    protected void executeCopyBootLoader(File stagingDirectory) throws FileUtilsErrorException {
        if (launcherLibs_.isEmpty()) {
            throw new IllegalArgumentException("Spring Boot loader launcher required.");
        } else {
            var meta_inf_dir = new File(stagingDirectory, "META-INF");
            for (var jar : launcherLibs()) {
                if (jar.exists()) {
                    FileUtils.unzipFile(jar, stagingDirectory);
                    if (meta_inf_dir.exists()) {
                        FileUtils.deleteDirectory(meta_inf_dir);
                    }
                } else if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.warning("File not found: " + jar.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Part of the {@link #execute execute} operation, copies the {@code BOOT-INF} or {@code WEB-INF} classes.
     *
     * @param stagingInfDirectory Tte staging {@code INF} directory
     */
    protected void executeCopyInfClassesFiles(File stagingInfDirectory) throws IOException {
        var inf_classes_dir = new File(stagingInfDirectory, "classes");
        BootUtils.mkDirs(inf_classes_dir);

        for (var dir : sourceDirectories()) {
            if (dir.exists()) {
                FileUtils.copyDirectory(dir, inf_classes_dir);
            } else if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning("Directory not found: " + dir.getAbsolutePath());
            }
        }

        BootUtils.deleteDirectories(new File(inf_classes_dir, "resources"), new File(inf_classes_dir, "templates"));
    }

    /**
     * Part of the {@link #execute execute} operation, copies the {@code BOOT-INF} or (@code WEB-INF) libs.
     *
     * @param stagingInfDirectory the staging {@code INF} directory
     */
    protected void executeCopyInfLibs(File stagingInfDirectory) throws IOException {
        var inf_lib_dir = new File(stagingInfDirectory, "lib");
        BootUtils.mkDirs(inf_lib_dir);

        for (var jar : infLibs_) {
            if (jar.exists()) {
                Files.copy(jar.toPath(), inf_lib_dir.toPath().resolve(jar.getName()));
            } else if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning("File not found: " + jar.getAbsolutePath());
            }
        }
    }

    /**
     * Part of the {@link #execute execute} operation, creates the archive from the staging directory.
     *
     * @param stagingDirectory the staging directory
     * @return the archive
     */
    protected File executeCreateArchive(File stagingDirectory) throws IOException {
        executeCreateManifest(stagingDirectory);

        if (LOGGER.isLoggable(Level.FINE) && (!silent())) {
            LOGGER.fine(MessageFormat.format("""
                            \tStaging     -> {0} [exists={1}]
                            \tDestination -> {2} [exists={3}]
                            \tArchive     -> {4}
                            \tLauncher    -> {5}""",
                    stagingDirectory, stagingDirectory.exists(),
                    destinationDirectory(), destinationDirectory().exists(),
                    destinationFileName(),
                    launcherClass()));
        }

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

        var archive = new File(destinationDirectory(), destinationFileName());

        jarTool.run(stdout, stderr, args, archive.getAbsolutePath(), "-C", stagingDirectory.getAbsolutePath(), ".");

        var errBuff = err.getBuffer();
        if (!errBuff.isEmpty()) {
            throw new IOException(errBuff.toString());
        } else {
            var outBuff = out.getBuffer();
            if (!outBuff.isEmpty() && LOGGER.isLoggable(Level.INFO) && !silent()) {
                LOGGER.info(outBuff.toString());
            }
        }

        return archive;
    }

    /**
     * Part of the {@link #execute execute} operation, creates the manifest for the archive.
     *
     * @param stagingDirectory the staging directory
     */
    protected void executeCreateManifest(File stagingDirectory) throws IOException {
        var meta_inf_dir = new File(stagingDirectory, "META-INF");
        BootUtils.mkDirs(meta_inf_dir);

        var manifest = new File(meta_inf_dir, "MANIFEST.MF").toPath();

        try (var fileWriter = Files.newBufferedWriter(manifest)) {
            for (var manifestAttribute : manifestAttributes()) {
                fileWriter.write(manifestAttribute.name() + ": " + manifestAttribute.value() + System.lineSeparator());
            }
        }
    }

    /**
     * Configures the operation from a {@link Project}.
     *
     * @param project the project
     * @return this operation instance
     */
    public abstract T fromProject(Project project) throws IOException;

    /**
     * Provides the libraries that will be stored in {@code BOOT-INF} or {@code WEB-INF}.
     *
     * @param jars a collection of Java archive files
     * @return this operation instance
     */
    public T infLibs(Collection<File> jars) {
        infLibs_.addAll(jars);
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Provides the libraries that will be stored in {@code BOOT-INF} or {@code WEB-INF}.
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
     * Retrieves the libraries in {@code BOOT-INF} or {@code WEB-INF}.
     *
     * @return a list of Java archives
     */
    public List<File> infLibs() {
        return infLibs_;
    }

    /**
     * Provides the Spring Boot loader launcher fully-qualified class name.
     * <p>
     * For examples:
     * <ul>
     *     <li>{@code org.springframework.boot.loader.JarLauncher}</li>
     *     <li>{@code org.springframework.boot.loader.PropertiesLauncher}</li>
     *     <li>{@code org.springframework.boot.loader.WarLauncher}</li>
     * </ul>
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
     * Retrieves the Spring Boot loader launcher fully-qualified class name.
     *
     * @return the launcher class name
     */
    protected String launcherClass() {
        if (launcherClass_ == null) {
            throw new IllegalArgumentException("Spring boot loader launcher class required.");
        }
        return launcherClass_;
    }

    /**
     * Retrieves the Spring Boot loader launcher libraries.
     *
     * @return a list of Java archives
     */
    public List<File> launcherLibs() {
        return launcherLibs_;
    }

    /**
     * Provides the libraries for the Spring Boot loader launcher.
     *
     * @param jars a collection of Java archives
     * @return this operation instance
     */
    public T launcherLibs(Collection<File> jars) throws IOException {
        if (!jars.isEmpty()) {
            for (var j : jars) {
                if (j.exists()) {
                    launcherLibs_.add(j);
                } else {
                    throw new IOException("Spring Boot loader launcher library not found: " + j);
                }
            }
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
     * Provides an attribute to put in the archive manifest.
     *
     * @param name  the attribute name
     * @param value the attribute value
     * @return this operation instance
     */
    public T manifestAttribute(String name, String value) {
        manifestAttributes_.add(new BootManifestAttribute(name, value));
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Retrieves the list of attributes that will be put in the archive manifest.
     *
     * @return a list of manifest attributes
     */
    public List<BootManifestAttribute> manifestAttributes() {
        return manifestAttributes_;
    }

    /**
     * Provides a map of attributes to put in the archive manifest.
     *
     * @param attributes the manifest attributes
     * @return this operation instance
     */
    public T manifestAttributes(Collection<BootManifestAttribute> attributes) {
        manifestAttributes_.addAll(attributes);
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Provides source directories that will be used for the archive creation.
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
     * Retrieves the source directories that will be used for the archive creation.
     *
     * @return a list of directories
     */
    public List<File> sourceDirectories() {
        return sourceDirectories_;
    }

    /**
     * Verifies that all the elements ({@link #mainClass() mainClass}, {@link #launcherClass() launcherClass} and
     * {@link #launcherLibs() launcherLibs}) required to create the archive have been provided, throws an
     * {@link IllegalArgumentException} otherwise.
     *
     * @return {@code true} or an {@link IllegalArgumentException}
     */
    @SuppressWarnings("SameReturnValue")
    protected boolean verifyExecute() throws IllegalArgumentException {
        if (mainClass() == null) {
            throw new IllegalArgumentException("Project mainClass required.");
        } else if (launcherClass().isEmpty()) {
            throw new IllegalArgumentException(("Spring Boot loader launcher class required."));
        } else if (launcherLibs().isEmpty()) {
            throw new IllegalArgumentException(("Spring Boot loader launcher libraries required."));
        }
        return true;
    }
}
