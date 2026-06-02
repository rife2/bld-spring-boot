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
import rife.bld.extension.tools.IOTools;
import rife.bld.extension.tools.ObjectTools;
import rife.bld.extension.tools.TextTools;
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
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
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
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "Builder pattern intentionally exposes mutable collections; callers may add to them directly"
)
public abstract class AbstractBootOperation<T extends AbstractBootOperation<T>>
        extends AbstractOperation<AbstractBootOperation<T>> {

    private static final String INF_LIBS = "infLibs";
    private static final String LAUNCHER_LIBS = "launcherLibs";
    private static final String SOURCE_DIRECTORIES = "sourceDirectories";
    protected final Logger logger = Logger.getLogger(getClass().getName());
    private final List<File> infLibs_ = new ArrayList<>();
    private final List<File> launcherLibs_ = new ArrayList<>();
    private final Map<String, String> manifestAttributes_ = new HashMap<>();
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
     * @throws NullPointerException     if {@code directory} is null
     * @throws IllegalArgumentException if {@code directory} is empty
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public T destinationDirectory(@NonNull String directory) {
        ObjectTools.requireNotEmpty(directory, "destinationDirectory");
        destinationDirectory_ = new File(directory);
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Provides the destination directory in which the archive will be created.
     *
     * @param directory the destination directory
     * @return this operation instance
     * @throws NullPointerException if {@code directory} is null
     */
    public T destinationDirectory(@NonNull File directory) {
        destinationDirectory_ = ObjectTools.requireNonNull(directory, "destinationDirectory");
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Provides the destination directory in which the archive will be created.
     *
     * @param directory the destination directory
     * @return this operation instance
     * @throws NullPointerException if {@code directory} is null
     */
    public T destinationDirectory(@NonNull Path directory) {
        ObjectTools.requireNonNull(directory, "destinationDirectory");
        destinationDirectory_ = directory.toFile();
        //noinspection unchecked
        return (T) this;
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
     * Provides the file name that will be used for the archive creation.
     *
     * @param name the archive file name
     * @return this operation instance
     * @throws NullPointerException     if {@code name} is null
     * @throws IllegalArgumentException if {@code name} is empty
     */
    public T destinationFileName(@NonNull String name) {
        destinationFileName_ = ObjectTools.requireNotEmpty(name, "destinationFileName");
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
     * Configures the operation from a {@link Project}.
     *
     * @param project the project
     * @return this operation instance
     */
    public abstract T fromProject(Project project);

    /**
     * Provides the libraries that will be stored in {@code BOOT-INF} or {@code WEB-INF}.
     *
     * @param jars one or more Java archive files
     * @return this operation instance
     * @throws NullPointerException     if {@code jars} is {@code null} or contains {@code null} elements
     * @throws IllegalArgumentException if {@code jars} is empty
     * @see #infLibs(Collection)
     */
    public T infLibs(@NonNull File... jars) {
        ObjectTools.requireNotEmpty(jars, INF_LIBS);
        infLibs_.addAll(List.of(jars));
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Provides the libraries that will be stored in {@code BOOT-INF} or {@code WEB-INF}.
     *
     * @param jars a collection of Java archive files
     * @return this operation instance
     * @throws NullPointerException     if {@code jars} is {@code null} or contains {@code null} elements
     * @throws IllegalArgumentException if {@code jars} is empty
     * @see #infLibs(File...)
     */
    public final T infLibs(@NonNull Collection<File> jars) {
        ObjectTools.requireNotEmpty(jars, INF_LIBS);
        infLibs_.addAll(jars);
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Provides the libraries that will be stored in {@code BOOT-INF} or {@code WEB-INF}.
     *
     * @param jars one or more Java archive files
     * @return this operation instance
     * @throws NullPointerException     if {@code jars} is {@code null} or contains {@code null} elements
     * @throws IllegalArgumentException if {@code jars} is empty
     * @see #infLibsPaths(Collection)
     */
    public T infLibs(@NonNull Path... jars) {
        ObjectTools.requireNotEmpty(jars, INF_LIBS);
        infLibs_.addAll(CollectionTools.combinePathsToFiles(jars));
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Provides the libraries that will be stored in {@code BOOT-INF} or {@code WEB-INF}.
     *
     * @param jars one or more Java archive files
     * @return this operation instance
     * @throws NullPointerException     if {@code jars} is {@code null} or contains {@code null} elements
     * @throws IllegalArgumentException if {@code jars} is empty, or contains empty elements
     * @see #infLibsStrings(Collection)
     */
    public T infLibs(@NonNull String... jars) {
        ObjectTools.requireNotEmpty(jars, INF_LIBS);
        infLibs_.addAll(CollectionTools.combineStringsToFiles(jars));
        //noinspection unchecked
        return (T) this;
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
     * Provides the libraries that will be stored in {@code BOOT-INF} or {@code WEB-INF}.
     *
     * @param jars one or more Java archive files
     * @return this operation instance
     * @throws NullPointerException     if {@code jars} is {@code null} or contains {@code null} elements
     * @throws IllegalArgumentException if {@code jars} is empty
     * @see #infLibs(Path...)
     */
    public final T infLibsPaths(@NonNull Collection<Path> jars) {
        ObjectTools.requireNotEmpty(jars, "infLibsPaths");
        infLibs_.addAll(CollectionTools.combinePathsToFiles(jars));
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Provides the libraries that will be stored in {@code BOOT-INF} or {@code WEB-INF}.
     *
     * @param jars one or more Java archive files
     * @return this operation instance
     * @throws NullPointerException     if {@code jars} is {@code null} or contains {@code null} elements
     * @throws IllegalArgumentException if {@code jars} is empty, or contains empty elements
     * @see #infLibs(String...)
     */
    public final T infLibsStrings(@NonNull Collection<String> jars) {
        ObjectTools.requireNotEmpty(jars, "infLibsStrings");
        infLibs_.addAll(CollectionTools.combineStringsToFiles(jars));
        //noinspection unchecked
        return (T) this;
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
     * @throws NullPointerException     if {@code className} is null
     * @throws IllegalArgumentException if {@code className} is empty
     */
    public T launcherClass(@NonNull String className) {
        launcherClass_ = ObjectTools.requireNotEmpty(className, "launcherClass");
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Provides the libraries for the Spring Boot loader launcher.
     *
     * @param jars one or more Java archives
     * @return this operation instance
     * @throws NullPointerException     if {@code jars} is {@code null} or contains {@code null} elements
     * @throws IllegalArgumentException if {@code jars} is empty
     * @see #infLibs(Collection)
     */
    public T launcherLibs(@NonNull File... jars) {
        ObjectTools.requireNotEmpty(jars, LAUNCHER_LIBS);
        launcherLibs_.addAll(List.of(jars));
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Provides the libraries for the Spring Boot loader launcher.
     *
     * @param jars a collection of Java archives
     * @return this operation instance
     * @throws NullPointerException     if {@code jars} is {@code null} or contains {@code null} elements
     * @throws IllegalArgumentException if {@code jars} is empty
     * @see #infLibs(File...)
     */
    public final T launcherLibs(@NonNull Collection<File> jars) {
        ObjectTools.requireNotEmpty(jars, LAUNCHER_LIBS);
        launcherLibs_.addAll(jars);
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Provides the libraries for the Spring Boot loader launcher.
     *
     * @param jars one or more Java archives
     * @return this operation instance
     * @throws NullPointerException     if {@code jars} is {@code null} or contains {@code null} elements
     * @throws IllegalArgumentException if {@code jars} is empty, or contains empty elements
     * @see #launcherLibsStrings(Collection)
     */
    public T launcherLibs(@NonNull String... jars) {
        ObjectTools.requireNotEmpty(jars, LAUNCHER_LIBS);
        launcherLibs_.addAll(CollectionTools.combineStringsToFiles(jars));
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Provides the libraries for the Spring Boot loader launcher.
     *
     * @param jars one or more Java archives
     * @return this operation instance
     * @throws NullPointerException     if {@code jars} is {@code null} or contains {@code null} elements
     * @throws IllegalArgumentException if {@code jars} is empty
     * @see #launcherLibsPaths(Collection)
     */
    public T launcherLibs(@NonNull Path... jars) {
        ObjectTools.requireNotEmpty(jars, LAUNCHER_LIBS);
        launcherLibs_.addAll(CollectionTools.combinePathsToFiles(jars));
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Retrieves the Spring Boot loader launcher libraries.
     *
     * @return the Java archives
     */
    public List<File> launcherLibs() {
        return launcherLibs_;
    }

    /**
     * Provides the libraries for the Spring Boot loader launcher.
     *
     * @param jars one or more Java archives
     * @return this operation instance
     * @throws NullPointerException     if {@code jars} is {@code null} or contains {@code null} elements
     * @throws IllegalArgumentException if {@code jars} is empty
     * @see #launcherLibs(Path...)
     */
    public final T launcherLibsPaths(@NonNull Collection<Path> jars) {
        ObjectTools.requireNotEmpty(jars, "launcherLibsPaths");
        launcherLibs_.addAll(CollectionTools.combinePathsToFiles(jars));
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Provides the libraries for the Spring Boot loader launcher.
     *
     * @param jars one or more Java archives
     * @return this operation instance
     * @throws NullPointerException     if {@code jars} is {@code null} or contains {@code null} elements
     * @throws IllegalArgumentException if {@code jars} is empty, or contains empty elements
     * @see #launcherLibs(String...)
     */
    public final T launcherLibsStrings(@NonNull Collection<String> jars) {
        ObjectTools.requireNotEmpty(jars, "launcherLibsStrings");
        launcherLibs_.addAll(CollectionTools.combineStringsToFiles(jars));
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
     * @throws NullPointerException     if {@code name} or {@code value} is null
     * @throws IllegalArgumentException if {@code name} or {@code value} is empty
     */
    public T manifestAttribute(@NonNull String name, @NonNull String value) {
        ObjectTools.requireNotEmpty(name, "manifestAttribute name");
        ObjectTools.requireNotEmpty(value, "manifestAttribute value");
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
     * @throws NullPointerException     if {@code attributes} is {@code null}
     * @throws IllegalArgumentException if {@code attributes} is empty, or contains {@code null} or empty elements
     * @see #manifestAttribute(String, String)
     */
    public T manifestAttributes(@NonNull Map<String, String> attributes) {
        ObjectTools.requireNotEmpty(attributes, "manifestAttributes");
        manifestAttributes_.putAll(attributes);
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Provides source directories that will be used for the archive creation.
     *
     * @param directories one or more source directories
     * @return this operation instance
     * @throws NullPointerException     if {@code directories} is {@code null} or contains {@code null} elements
     * @throws IllegalArgumentException if {@code directories} is empty
     * @see #sourceDirectories(Collection)
     */
    public T sourceDirectories(@NonNull File... directories) {
        ObjectTools.requireNotEmpty(directories, SOURCE_DIRECTORIES);
        sourceDirectories_.addAll(List.of(directories));
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Provides source directories that will be used for the archive creation.
     *
     * @param directories one or more source directories
     * @return this operation instance
     * @throws NullPointerException     if {@code directories} is {@code null} or contains {@code null} elements
     * @throws IllegalArgumentException if {@code directories} is empty
     * @see #sourceDirectories(File...)
     */
    public final T sourceDirectories(@NonNull Collection<File> directories) {
        ObjectTools.requireNotEmpty(directories, SOURCE_DIRECTORIES);
        sourceDirectories_.addAll(directories);
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Provides source directories that will be used for the archive creation.
     *
     * @param directories one or more source directories
     * @return this operation instance
     * @throws NullPointerException     if {@code directories} is {@code null} or contains {@code null} elements
     * @throws IllegalArgumentException if {@code directories} is empty, or contains empty elements
     * @see #sourceDirectoriesStrings(Collection)
     */
    public T sourceDirectories(@NonNull String... directories) {
        ObjectTools.requireNotEmpty(directories, SOURCE_DIRECTORIES);
        sourceDirectories_.addAll(CollectionTools.combineStringsToFiles(directories));
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Provides source directories that will be used for the archive creation.
     *
     * @param directories one or more source directories
     * @return this operation instance
     * @throws NullPointerException     if {@code directories} is {@code null} or contains {@code null} elements
     * @throws IllegalArgumentException if {@code directories} is empty
     * @see #sourceDirectoriesPaths(Collection)
     */
    public T sourceDirectories(@NonNull Path... directories) {
        ObjectTools.requireNotEmpty(directories, SOURCE_DIRECTORIES);
        return sourceDirectoriesPaths(List.of(directories));
    }

    /**
     * Retrieves the source directories that will be used for the archive creation.
     *
     * @return the source directories
     */
    public List<File> sourceDirectories() {
        return sourceDirectories_;
    }

    /**
     * Provides source directories that will be used for the archive creation.
     *
     * @param directories one or more source directories
     * @return this operation instance
     * @throws NullPointerException     if {@code directories} is {@code null} or contains {@code null} elements
     * @throws IllegalArgumentException if {@code directories} is empty
     * @see #sourceDirectories(Path...)
     */
    public final T sourceDirectoriesPaths(@NonNull Collection<Path> directories) {
        ObjectTools.requireNotEmpty(directories, "sourceDirectoriesPaths");
        sourceDirectories_.addAll(CollectionTools.combinePathsToFiles(directories));
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Provides source directories that will be used for the archive creation.
     *
     * @param directories one or more source directories
     * @return this operation instance
     * @throws NullPointerException     if {@code directories} is {@code null} or contains {@code null} elements
     * @throws IllegalArgumentException if {@code directories} is empty, or contains empty elements
     * @see #sourceDirectories(String...)
     */
    public final T sourceDirectoriesStrings(@NonNull Collection<String> directories) {
        ObjectTools.requireNotEmpty(directories, "sourceDirectoriesStrings");
        sourceDirectories_.addAll(CollectionTools.combineStringsToFiles(directories));
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Part of the {@link #execute execute} operation, copies the Spring Boot loader launcher archive content to the
     * staging directory.
     *
     * @param stagingDirectory the staging directory
     * @throws FileUtilsErrorException if an error occurs
     * @throws ExitStatusException     if the launcher libraries are missing
     */
    protected void executeCopyBootLoader(File stagingDirectory) throws FileUtilsErrorException, ExitStatusException {
        if (launcherLibs_.isEmpty()) {
            if (logger.isLoggable(Level.SEVERE) && !silent()) {
                logger.severe("Spring Boot loader launcher required.");
            }
            throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
        } else {
            var metaInfDir = new File(stagingDirectory, "META-INF");
            for (var jar : launcherLibs()) {
                if (jar.exists()) {
                    FileUtils.unzipFile(jar, stagingDirectory);
                } else if (logger.isLoggable(Level.WARNING) && !silent()) {
                    logger.warning("File not found: " + jar.getAbsolutePath());
                }
            }
            // Remove the launcher's own META-INF so it doesn't end up in the archive;
            // the correct manifest is written separately by executeCreateManifest.
            if (metaInfDir.exists()) {
                FileUtils.deleteDirectory(metaInfDir);
            }
        }
    }

    /**
     * Part of the {@link #execute execute} operation, copies the {@code BOOT-INF} or {@code WEB-INF} classes.
     *
     * @param stagingInfDirectory the staging {@code INF} directory
     * @throws IOException if an error occurs
     */
    protected void executeCopyInfClassesFiles(File stagingInfDirectory) throws IOException {
        var infClassesDir = new File(stagingInfDirectory, "classes");
        IOTools.createDirs(infClassesDir);

        for (var dir : sourceDirectories_) {
            if (dir.exists()) {
                FileUtils.copyDirectory(dir, infClassesDir);
            } else if (logger.isLoggable(Level.WARNING) && !silent()) {
                logger.warning("Directory not found: " + dir.getAbsolutePath());
            }
        }
    }

    /**
     * Part of the {@link #execute execute} operation, copies the {@code BOOT-INF} or {@code WEB-INF} libs.
     *
     * @param stagingInfDirectory the staging {@code INF} directory
     * @throws IOException if an error occurs
     */
    protected void executeCopyInfLibs(File stagingInfDirectory) throws IOException {
        var infLibDir = stagingInfDirectory.toPath().resolve("lib");
        IOTools.createDirs(infLibDir.toFile());

        for (var jar : infLibs_) {
            if (jar.exists()) {
                Files.copy(jar.toPath(), infLibDir.resolve(jar.getName()), StandardCopyOption.REPLACE_EXISTING);
            } else if (logger.isLoggable(Level.WARNING) && !silent()) {
                logger.warning("File not found: " + jar.getAbsolutePath());
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
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    protected File executeCreateArchive(File stagingDirectory) throws IOException {
        executeCreateManifest(stagingDirectory);

        if (logger.isLoggable(Level.FINE) && (!silent())) {
            logger.fine(MessageFormat.format("""
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
        var err = new StringWriter();
        var jarTool = ToolProvider.findFirst("jar")
                .orElseThrow(() -> new IllegalArgumentException("JDK jar tool not found on this platform."));

        String args;
        if (logger.isLoggable(Level.FINER) && !silent()) {
            args = "-0cMvf";
        } else {
            args = "-0cMf";
        }

        var archive = new File(destinationDirectory(), destinationFileName());

        try (var stdout = new PrintWriter(out);
             var stderr = new PrintWriter(err)) {
            jarTool.run(stdout, stderr, args, archive.getAbsolutePath(),
                    "-C", stagingDirectory.getAbsolutePath(), ".");
        }

        var errBuff = err.getBuffer();
        if (!errBuff.isEmpty()) {
            throw new IOException(errBuff.toString());
        } else {
            var outBuff = out.getBuffer();
            if (!outBuff.isEmpty() && logger.isLoggable(Level.INFO) && !silent()) {
                logger.info(outBuff.toString());
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
        var metaInfDir = stagingDirectory.toPath().resolve("META-INF");
        IOTools.createDirs(metaInfDir.toFile());

        var manifestFile = metaInfDir.resolve("MANIFEST.MF");

        var manifest = new Manifest();
        var mainAttributes = manifest.getMainAttributes();

        // Manifest-Version is required and must be first
        mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");

        // Add all user-provided attributes
        for (var entry : manifestAttributes_.entrySet()) {
            // Attributes.Name is preferred for standard names, but String works for custom ones
            mainAttributes.putValue(entry.getKey(), entry.getValue());
        }

        // Write it out. Manifest.write() handles line wrapping and final blank line.
        try (var out = Files.newOutputStream(manifestFile)) {
            manifest.write(out);
        }
    }

    /**
     * Retrieves the Spring Boot loader launcher fully qualified class name.
     *
     * @return the launcher class name, or {@code null} if not set
     */
    protected String launcherClass() {
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
     * @throws IOException              if the {@link #destinationDirectory() destination directory} could not be created
     * @throws IllegalArgumentException if a required element is missing or invalid
     */
    @SuppressFBWarnings("DRE_DECLARED_RUNTIME_EXCEPTION")
    protected void verifyExecute() throws IllegalArgumentException, IOException {
        if (TextTools.isBlank(mainClass_)) {
            throw new IllegalArgumentException("Project mainClass required.");
        } else if (TextTools.isBlank(launcherClass_)) {
            throw new IllegalArgumentException(("Spring Boot loader launcher class required."));
        } else if (ObjectTools.isEmpty(launcherLibs_)) {
            throw new IllegalArgumentException(("Spring Boot loader launcher libraries required."));
        } else if (IOTools.notExists(destinationDirectory_)) {
            if (destinationDirectory_ == null) {
                throw new IllegalArgumentException("Destination directory required.");
            }
            try {
                IOTools.createDirs(destinationDirectory_);
            } catch (IOException e) {
                throw new IOException("Cannot create destination directory: " + destinationDirectory_, e);
            }
        }
        for (var jar : launcherLibs_) {
            if (IOTools.notExists(jar)) {
                throw new IllegalArgumentException("Spring Boot loader launcher library not found: " + jar);
            }
        }
    }
}