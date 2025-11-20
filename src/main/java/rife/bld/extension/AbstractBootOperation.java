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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import rife.bld.Project;
import rife.bld.operations.AbstractOperation;
import rife.bld.operations.exceptions.ExitStatusException;
import rife.tools.FileUtils;
import rife.tools.exceptions.FileUtilsErrorException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.spi.ToolProvider;

/**
 * Implements common methods used by Spring Boot operations, such as {@link BootJarOperation} and
 * {@link BootWarOperation}.
 *
 * @param <T> the type parameter
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public abstract class AbstractBootOperation<T extends AbstractBootOperation<T>>
        extends AbstractOperation<AbstractBootOperation<T>> {
    protected static final Logger LOGGER = Logger.getLogger("BootOperation");
    private final List<File> infLibs_ = new ArrayList<>();
    private final List<File> launcherLibs_ = new ArrayList<>();
    private final Map<String, String> manifestAttributes_ = new ConcurrentHashMap<>();
    private final List<File> sourceDirectories_ = new ArrayList<>();
    private File destinationDirectory_;
    private String destinationFileName_;
    private String launcherClass_;
    private String mainClass_;

    /**
     * Provides the destination directory in which the archive will be created.
     *
     * @param directory the destination directory
     * @return this operation instance
     * @throws IOException if an error occurs
     */
    public T destinationDirectory(String directory) throws IOException {
        return destinationDirectory(new File(directory));
    }

    /**
     * Provides the destination directory in which the archive will be created.
     *
     * @param directory the destination directory
     * @return this operation instance
     * @throws IOException if an error occurs
     */
    public T destinationDirectory(File directory) throws IOException {
        destinationDirectory_ = directory;
        BootUtils.mkDirs(destinationDirectory_);
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Provides the destination directory in which the archive will be created.
     *
     * @param directory the destination directory
     * @return this operation instance
     * @throws IOException if an error occurs
     */
    public T destinationDirectory(Path directory) throws IOException {
        return destinationDirectory(directory.toFile());
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
     * Configures the operation from a {@link Project}.
     *
     * @param project the project
     * @return this operation instance
     * @throws IOException if an error occurs
     */
    public abstract T fromProject(Project project) throws IOException;

    /**
     * Provides the libraries that will be stored in {@code BOOT-INF} or {@code WEB-INF}.
     *
     * @param jars one or more Java archive files
     * @return this operation instance
     * @see #infLibs(Collection)
     */
    public T infLibs(File... jars) {
        return infLibs(List.of(jars));
    }

    /**
     * Provides the libraries that will be stored in {@code BOOT-INF} or {@code WEB-INF}.
     *
     * @param jars a collection of Java archive files
     * @return this operation instance
     * @see #infLibs(File...)
     */
    public T infLibs(Collection<File> jars) {
        infLibs_.addAll(jars);
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Provides the libraries that will be stored in {@code BOOT-INF} or {@code WEB-INF}.
     *
     * @param jars one or more Java archive files
     * @return this operation instance
     * @see #infLibsPaths(Collection)
     */
    public T infLibs(Path... jars) {
        return infLibsPaths(List.of(jars));
    }

    /**
     * Provides the libraries that will be stored in {@code BOOT-INF} or {@code WEB-INF}.
     *
     * @param jars one or more Java archive files
     * @return this operation instance
     * @see #infLibs(Path...)
     */
    public T infLibsPaths(Collection<Path> jars) {
        return infLibs(jars.stream().map(Path::toFile).toList());
    }

    /**
     * Provides the libraries that will be stored in {@code BOOT-INF} or {@code WEB-INF}.
     *
     * @param jars one or more Java archive files
     * @return this operation instance
     * @see #infLibsStrings(Collection)
     */
    public T infLibs(String... jars) {
        return infLibsStrings(List.of(jars));
    }

    /**
     * Provides the libraries that will be stored in {@code BOOT-INF} or {@code WEB-INF}.
     *
     * @param jars one or more Java archive files
     * @return this operation instance
     * @see #infLibs(String...)
     */
    public T infLibsStrings(Collection<String> jars) {
        return infLibs(jars.stream().map(File::new).toList());
    }

    /**
     * Retrieves the libraries in {@code BOOT-INF} or {@code WEB-INF}.
     *
     * @return the Java archives
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<File> infLibs() {
        return infLibs_;
    }

    /**
     * Provides the Spring Boot loader launcher fully qualified class name.
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
     * Provides the libraries for the Spring Boot loader launcher.
     *
     * @param jars one or more Java archives
     * @return this operation instance
     * @throws IOException if a JAR could not be found
     * @see #infLibs(Collection)
     */
    public T launcherLibs(File... jars) throws IOException {
        return launcherLibs(List.of(jars));
    }

    /**
     * Provides the libraries for the Spring Boot loader launcher.
     *
     * @param jars a collection of Java archives
     * @return this operation instance
     * @throws IOException if a JAR could not be found
     * @see #infLibs(File...)
     */
    public T launcherLibs(Collection<File> jars) throws IOException {
        for (var j : jars) {
            if (j.exists()) {
                launcherLibs_.add(j);
            } else {
                throw new IOException("Spring Boot loader launcher library not found: " + j);
            }
        }
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Provides the libraries for the Spring Boot loader launcher.
     *
     * @param jars one or more Java archives
     * @return this operation instance
     * @throws IOException if a JAR could not be found
     * @see #launcherLibsStrings(Collection)
     */
    public T launcherLibs(String... jars) throws IOException {
        return launcherLibsStrings(List.of(jars));
    }

    /**
     * Provides the libraries for the Spring Boot loader launcher.
     *
     * @param jars one or more Java archives
     * @return this operation instance
     * @throws IOException if a JAR could not be found
     * @see #launcherLibs(String...)
     */
    public T launcherLibsStrings(Collection<String> jars) throws IOException {
        return launcherLibs(jars.stream().map(File::new).toList());
    }

    /**
     * Provides the libraries for the Spring Boot loader launcher.
     *
     * @param jars one or more Java archives
     * @return this operation instance
     * @throws IOException if a JAR could not be found
     * @see #launcherLibsPaths(Collection)
     */
    public T launcherLibs(Path... jars) throws IOException {
        return launcherLibsPaths(List.of(jars));
    }

    /**
     * Provides the libraries for the Spring Boot loader launcher.
     *
     * @param jars one or more Java archives
     * @return this operation instance
     * @throws IOException if a JAR could not be found
     * @see #launcherLibs(Path...)
     */
    public T launcherLibsPaths(Collection<Path> jars) throws IOException {
        return launcherLibs(jars.stream().map(Path::toFile).toList());
    }

    /**
     * Provides an attribute to put in the archive manifest.
     *
     * @param name  the attribute name
     * @param value the attribute value
     * @return this operation instance
     */
    public T manifestAttribute(String name, String value) {
        manifestAttributes_.put(name, value);
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Retrieves the attributes that will be put in the archive manifest.
     *
     * @return the manifest attributes
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Map<String, String> manifestAttributes() {
        return manifestAttributes_;
    }

    /**
     * Provides a map of attributes to put in the archive manifest.
     *
     * @param attributes the manifest attributes
     * @return this operation instance
     * @see #manifestAttribute(String, String)
     */
    public T manifestAttributes(Map<String, String> attributes) {
        manifestAttributes_.putAll(attributes);
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Provides source directories that will be used for the archive creation.
     *
     * @param directories one or more source directories
     * @return this operation instance
     * @see #sourceDirectories(Collection)
     */
    public T sourceDirectories(File... directories) {
        return sourceDirectories(List.of(directories));
    }

    /**
     * Provides source directories that will be used for the archive creation.
     *
     * @param directories one or more source directories
     * @return this operation instance
     * @see #sourceDirectories(File...)
     */
    public T sourceDirectories(Collection<File> directories) {
        sourceDirectories_.addAll(directories);
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Provides source directories that will be used for the archive creation.
     *
     * @param directories one or more source directories
     * @return this operation instance
     * @see #sourceDirectoriesStrings(Collection)
     */
    public T sourceDirectories(String... directories) {
        return sourceDirectoriesStrings(List.of(directories));
    }

    /**
     * Provides source directories that will be used for the archive creation.
     *
     * @param directories one or more source directories
     * @return this operation instance
     * @see #sourceDirectories(String...)
     */
    public T sourceDirectoriesStrings(Collection<String> directories) {
        return sourceDirectories(directories.stream().map(File::new).toList());
    }

    /**
     * Provides source directories that will be used for the archive creation.
     *
     * @param directories one or more source directories
     * @return this operation instance
     * @see #sourceDirectoriesPaths(Collection)
     */
    public T sourceDirectories(Path... directories) {
        return sourceDirectoriesPaths(List.of(directories));
    }

    /**
     * Provides source directories that will be used for the archive creation.
     *
     * @param directories one or more source directories
     * @return this operation instance
     * @see #sourceDirectories(Path...)
     */
    public T sourceDirectoriesPaths(Collection<Path> directories) {
        return sourceDirectories(directories.stream().map(Path::toFile).toList());
    }

    /**
     * Retrieves the source directories that will be used for the archive creation.
     *
     * @return the source directories
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<File> sourceDirectories() {
        return sourceDirectories_;
    }

    /**
     * Part of the {@link #execute execute} operation, copies the Spring Boot loader launcher archive content to the
     * staging directory.
     *
     * @param stagingDirectory the staging directory
     * @throws FileUtilsErrorException if an error occurs
     */
    protected void executeCopyBootLoader(File stagingDirectory) throws FileUtilsErrorException, ExitStatusException {
        if (launcherLibs_.isEmpty()) {
            if (LOGGER.isLoggable(Level.SEVERE) && !silent()) {
                LOGGER.severe("Spring Boot loader launcher required.");
            }
            throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
        } else {
            var metaInfDir = new File(stagingDirectory, "META-INF");
            for (var jar : launcherLibs()) {
                if (jar.exists()) {
                    FileUtils.unzipFile(jar, stagingDirectory);
                    if (metaInfDir.exists()) {
                        FileUtils.deleteDirectory(metaInfDir);
                    }
                } else if (LOGGER.isLoggable(Level.WARNING) && !silent()) {
                    LOGGER.warning("File not found: " + jar.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Retrieves the Spring Boot loader launcher libraries.
     *
     * @return the Java archives
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<File> launcherLibs() {
        return launcherLibs_;
    }

    /**
     * Part of the {@link #execute execute} operation, copies the {@code BOOT-INF} or {@code WEB-INF} classes.
     *
     * @param stagingInfDirectory Tte staging {@code INF} directory
     * @throws IOException if an error occurs
     */
    protected void executeCopyInfClassesFiles(File stagingInfDirectory) throws IOException {
        var infClassesDir = new File(stagingInfDirectory, "classes");
        BootUtils.mkDirs(infClassesDir);

        for (var dir : sourceDirectories_) {
            if (dir.exists()) {
                FileUtils.copyDirectory(dir, infClassesDir);
            } else if (LOGGER.isLoggable(Level.WARNING) && !silent()) {
                LOGGER.warning("Directory not found: " + dir.getAbsolutePath());
            }
        }
    }

    /**
     * Part of the {@link #execute execute} operation, copies the {@code BOOT-INF} or (@code WEB-INF) libs.
     *
     * @param stagingInfDirectory the staging {@code INF} directory
     * @throws IOException if an error occurs
     */
    protected void executeCopyInfLibs(File stagingInfDirectory) throws IOException {
        var infLibDir = stagingInfDirectory.toPath().resolve("lib");
        BootUtils.mkDirs(infLibDir.toFile());

        for (var jar : infLibs_) {
            if (jar.exists()) {
                Files.copy(jar.toPath(), infLibDir.resolve(jar.getName()));
            } else if (LOGGER.isLoggable(Level.WARNING) && !silent()) {
                LOGGER.warning("File not found: " + jar.getAbsolutePath());
            }
        }
    }

    /**
     * Part of the {@link #execute execute} operation, creates the archive from the staging directory.
     *
     * @param stagingDirectory the staging directory
     * @return the archive
     * @throws IOException if an error occurs
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
        if (LOGGER.isLoggable(Level.FINER) && !silent()) {
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
     * @throws IOException if an error occurs
     */
    protected void executeCreateManifest(File stagingDirectory) throws IOException {
        var metaInfDir = new File(stagingDirectory, "META-INF");
        BootUtils.mkDirs(metaInfDir);

        var manifest = new File(metaInfDir, "MANIFEST.MF").toPath();

        try (var fileWriter = Files.newBufferedWriter(manifest)) {
            for (var set : manifestAttributes_.entrySet()) {
                fileWriter.write(set.getKey() + ": " + set.getValue() + System.lineSeparator());
            }
        }
    }

    /**
     * Retrieves the destination directory in which the archive will be created.
     *
     * @return the destination directory
     */
    public File destinationDirectory() {
        return destinationDirectory_;
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
     * Retrieves the Spring Boot loader launcher fully qualified class name.
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
     * Provides the fully qualified main class name.
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
     * Verifies that all the elements ({@link #mainClass() mainClass}, {@link #launcherClass() launcherClass} and
     * {@link #launcherLibs() launcherLibs}) required to create the archive have been provided, throws an
     * {@link IllegalArgumentException} otherwise.
     *
     * @return {@code true} or an {@link IllegalArgumentException}
     * @throws IllegalArgumentException if an error occurs
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

    /**
     * Retrieves the main class name.
     *
     * @return the class name
     */
    public String mainClass() {
        return mainClass_;
    }
}
