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

import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import rife.bld.Project;
import rife.bld.dependencies.VersionNumber;
import rife.bld.extension.testing.LoggingExtension;
import rife.bld.extension.testing.TestLogHandler;
import rife.tools.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(LoggingExtension.class)
@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.SignatureDeclareThrowsException"})
class BootOperationTests {
    private static final String BLD = "bld-2.3.0.jar";
    private static final String BOOT_VERSION = "3.5.4";
    private static final String EXAMPLES_LIB_COMPILE = "examples/lib/compile/";
    private static final String EXAMPLES_LIB_RUNTIME = "examples/lib/runtime/";
    private static final String EXAMPLES_LIB_STANDALONE = "examples/lib/standalone/";
    private static final String LAUNCHER_JARS = """
            org/
            org/springframework/
            org/springframework/boot/
            org/springframework/boot/loader/
            org/springframework/boot/loader/jar/
            org/springframework/boot/loader/jar/JarEntriesStream$InputStreamSupplier.class
            org/springframework/boot/loader/jar/JarEntriesStream.class
            org/springframework/boot/loader/jar/ManifestInfo.class
            org/springframework/boot/loader/jar/MetaInfVersionsInfo.class
            org/springframework/boot/loader/jar/NestedJarFile$JarEntriesEnumeration.class
            org/springframework/boot/loader/jar/NestedJarFile$JarEntryInflaterInputStream.class
            org/springframework/boot/loader/jar/NestedJarFile$JarEntryInputStream.class
            org/springframework/boot/loader/jar/NestedJarFile$NestedJarEntry.class
            org/springframework/boot/loader/jar/NestedJarFile$RawZipDataInputStream.class
            org/springframework/boot/loader/jar/NestedJarFile$ZipContentEntriesSpliterator.class
            org/springframework/boot/loader/jar/NestedJarFile.class
            org/springframework/boot/loader/jar/NestedJarFileResources.class
            org/springframework/boot/loader/jar/SecurityInfo.class
            org/springframework/boot/loader/jar/ZipInflaterInputStream.class
            org/springframework/boot/loader/jarmode/
            org/springframework/boot/loader/jarmode/JarMode.class
            org/springframework/boot/loader/jarmode/JarModeErrorException.class
            org/springframework/boot/loader/launch/
            org/springframework/boot/loader/launch/Archive$Entry.class
            org/springframework/boot/loader/launch/Archive.class
            org/springframework/boot/loader/launch/ClassPathIndexFile.class
            org/springframework/boot/loader/launch/ExecutableArchiveLauncher.class
            org/springframework/boot/loader/launch/ExplodedArchive$FileArchiveEntry.class
            org/springframework/boot/loader/launch/ExplodedArchive.class
            org/springframework/boot/loader/launch/JarFileArchive$JarArchiveEntry.class
            org/springframework/boot/loader/launch/JarFileArchive.class
            org/springframework/boot/loader/launch/JarLauncher.class
            org/springframework/boot/loader/launch/JarModeRunner.class
            org/springframework/boot/loader/launch/LaunchedClassLoader$DefinePackageCallType.class
            org/springframework/boot/loader/launch/LaunchedClassLoader.class
            org/springframework/boot/loader/launch/Launcher.class
            org/springframework/boot/loader/launch/PropertiesLauncher$Instantiator$Using.class
            org/springframework/boot/loader/launch/PropertiesLauncher$Instantiator.class
            org/springframework/boot/loader/launch/PropertiesLauncher.class
            org/springframework/boot/loader/launch/SystemPropertyUtils.class
            org/springframework/boot/loader/launch/WarLauncher.class
            org/springframework/boot/loader/log/
            org/springframework/boot/loader/log/DebugLogger$DisabledDebugLogger.class
            org/springframework/boot/loader/log/DebugLogger$SystemErrDebugLogger.class
            org/springframework/boot/loader/log/DebugLogger.class
            org/springframework/boot/loader/net/
            org/springframework/boot/loader/net/protocol/
            org/springframework/boot/loader/net/protocol/Handlers.class
            org/springframework/boot/loader/net/protocol/jar/
            org/springframework/boot/loader/net/protocol/jar/Canonicalizer.class
            org/springframework/boot/loader/net/protocol/jar/Handler.class
            org/springframework/boot/loader/net/protocol/jar/JarFileUrlKey.class
            org/springframework/boot/loader/net/protocol/jar/JarUrl.class
            org/springframework/boot/loader/net/protocol/jar/JarUrlClassLoader$OptimizedEnumeration.class
            org/springframework/boot/loader/net/protocol/jar/JarUrlClassLoader.class
            org/springframework/boot/loader/net/protocol/jar/JarUrlConnection$ConnectionInputStream.class
            org/springframework/boot/loader/net/protocol/jar/JarUrlConnection$EmptyUrlStreamHandler.class
            org/springframework/boot/loader/net/protocol/jar/JarUrlConnection.class
            org/springframework/boot/loader/net/protocol/jar/LazyDelegatingInputStream.class
            org/springframework/boot/loader/net/protocol/jar/Optimizations.class
            org/springframework/boot/loader/net/protocol/jar/UrlJarEntry.class
            org/springframework/boot/loader/net/protocol/jar/UrlJarFile.class
            org/springframework/boot/loader/net/protocol/jar/UrlJarFileFactory.class
            org/springframework/boot/loader/net/protocol/jar/UrlJarFiles$Cache.class
            org/springframework/boot/loader/net/protocol/jar/UrlJarFiles.class
            org/springframework/boot/loader/net/protocol/jar/UrlJarManifest$ManifestSupplier.class
            org/springframework/boot/loader/net/protocol/jar/UrlJarManifest.class
            org/springframework/boot/loader/net/protocol/jar/UrlNestedJarFile.class
            org/springframework/boot/loader/net/protocol/nested/
            org/springframework/boot/loader/net/protocol/nested/Handler.class
            org/springframework/boot/loader/net/protocol/nested/NestedLocation.class
            org/springframework/boot/loader/net/protocol/nested/NestedUrlConnection$ConnectionInputStream.class
            org/springframework/boot/loader/net/protocol/nested/NestedUrlConnection.class
            org/springframework/boot/loader/net/protocol/nested/NestedUrlConnectionResources.class
            org/springframework/boot/loader/net/util/
            org/springframework/boot/loader/net/util/UrlDecoder.class
            org/springframework/boot/loader/nio/
            org/springframework/boot/loader/nio/file/
            org/springframework/boot/loader/nio/file/NestedByteChannel$Resources.class
            org/springframework/boot/loader/nio/file/NestedByteChannel.class
            org/springframework/boot/loader/nio/file/NestedFileStore.class
            org/springframework/boot/loader/nio/file/NestedFileSystem.class
            org/springframework/boot/loader/nio/file/NestedFileSystemProvider.class
            org/springframework/boot/loader/nio/file/NestedPath.class
            org/springframework/boot/loader/nio/file/UriPathEncoder.class
            org/springframework/boot/loader/ref/
            org/springframework/boot/loader/ref/Cleaner.class
            org/springframework/boot/loader/ref/DefaultCleaner.class
            org/springframework/boot/loader/zip/
            org/springframework/boot/loader/zip/ByteArrayDataBlock.class
            org/springframework/boot/loader/zip/CloseableDataBlock.class
            org/springframework/boot/loader/zip/DataBlock.class
            org/springframework/boot/loader/zip/DataBlockInputStream.class
            org/springframework/boot/loader/zip/FileDataBlock$FileAccess.class
            org/springframework/boot/loader/zip/FileDataBlock$Tracker$1.class
            org/springframework/boot/loader/zip/FileDataBlock$Tracker.class
            org/springframework/boot/loader/zip/FileDataBlock.class
            org/springframework/boot/loader/zip/NameOffsetLookups.class
            org/springframework/boot/loader/zip/VirtualDataBlock.class
            org/springframework/boot/loader/zip/VirtualZipDataBlock$DataPart.class
            org/springframework/boot/loader/zip/VirtualZipDataBlock.class
            org/springframework/boot/loader/zip/Zip64EndOfCentralDirectoryLocator.class
            org/springframework/boot/loader/zip/Zip64EndOfCentralDirectoryRecord.class
            org/springframework/boot/loader/zip/ZipCentralDirectoryFileHeaderRecord.class
            org/springframework/boot/loader/zip/ZipContent$Entry.class
            org/springframework/boot/loader/zip/ZipContent$Kind.class
            org/springframework/boot/loader/zip/ZipContent$Loader.class
            org/springframework/boot/loader/zip/ZipContent$Source.class
            org/springframework/boot/loader/zip/ZipContent.class
            org/springframework/boot/loader/zip/ZipDataDescriptorRecord.class
            org/springframework/boot/loader/zip/ZipEndOfCentralDirectoryRecord$Located.class
            org/springframework/boot/loader/zip/ZipEndOfCentralDirectoryRecord.class
            org/springframework/boot/loader/zip/ZipLocalFileHeaderRecord.class
            org/springframework/boot/loader/zip/ZipString$CompareType.class
            org/springframework/boot/loader/zip/ZipString.class
            """;
    private static final String MAIN_CLASS = "com.example.Foo";
    private static final String PROVIDED_LIB = "LatencyUtils-2.0.3.jar";
    private static final String SPRING_BOOT = "spring-boot-" + BOOT_VERSION + ".jar";
    private static final String SPRING_BOOT_ACTUATOR = "spring-boot-actuator-" + BOOT_VERSION + ".jar";
    private static final String SPRING_BOOT_LOADER = "spring-boot-loader-" + BOOT_VERSION + ".jar";
    private static final String SRC_MAIN_JAVA = "src/main/java";
    private static final String SRC_TEST_JAVA = "src/test/java";

    private final Logger logger = Logger.getLogger("BootOperation");
    private TestLogHandler logHandler;

    @AfterEach
    void afterEach() {
        if (logHandler != null) {
            logger.removeHandler(logHandler);
        }
    }

    @BeforeEach
    void beforeEach() {
        logHandler = new TestLogHandler();
        logger.addHandler(logHandler);
        logger.setLevel(Level.ALL);
    }

    private StringBuilder readJarEntries(File jar) throws IOException {
        var jarEntries = new StringBuilder();
        try (var jarFile = new JarFile(jar)) {
            for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements(); ) {
                jarEntries.append(entries.nextElement().getName()).append('\n');
            }
        }
        return jarEntries;
    }

    static class CustomProject extends Project {
        CustomProject(File tmp) {
            super();
            workDirectory = tmp;
            pkg = "com.example";
            name = "test_project";
            version = new VersionNumber(0, 0, 1);
            mainClass = MAIN_CLASS;
        }
    }

    @Nested
    @DisplayName("Errors Tests")
    class ErrorsTests {
        @Test
        void invalidMainClass() {
            var bootWar = new BootWarOperation().mainClass(MAIN_CLASS);
            assertThatCode(bootWar::execute)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("class required");
        }

        @Test
        void launcherClass() throws IOException {
            var bootWar = new BootJarOperation().mainClass(MAIN_CLASS)
                    .launcherClass("org.springframework.boot.loader.launch.WarLauncher")
                    .launcherLibs(List.of(new File(EXAMPLES_LIB_STANDALONE + SPRING_BOOT_LOADER)));
            assertThat(bootWar.verifyExecute()).isTrue();
        }

        @Test
        void misingLauncherLibs() {
            assertThatCode(() -> new BootWarOperation().launcherLibs("bar"))
                    .as("bar")
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        void misingLauncherLibsAsFile() {
            assertThatCode(() -> new BootWarOperation().launcherLibs(new File("foo")))
                    .as("foo")
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        void missingLauncherClass() throws IOException {
            var bootWar = new BootWarOperation().mainClass(MAIN_CLASS)
                    .launcherLibs(List.of(new File(EXAMPLES_LIB_STANDALONE + SPRING_BOOT_LOADER)));
            assertThatCode(bootWar::execute)
                    .isInstanceOf((IllegalArgumentException.class))
                    .hasMessageContaining("class required");
        }

        @Test
        void missingMainClass() {
            var bootWar = new BootWarOperation();
            assertThatCode(bootWar::execute).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("mainClass");
        }
    }

    @Nested
    @DisplayName("Libs Tests")
    class LibsTests {
        @Nested
        @DisplayName("Inf Lib Tests")
        class InfLibTest {
            private final File bar = new File(EXAMPLES_LIB_COMPILE + SPRING_BOOT_ACTUATOR);
            private final File foo = new File(EXAMPLES_LIB_COMPILE + SPRING_BOOT);
            private final BootWarOperation op = new BootWarOperation();

            @Test
            void infLibsAsFileArray() {
                op.infLibs().clear();
                op.infLibs(foo, bar);
                assertThat(op.infLibs()).as("File...").containsExactly(foo, bar);
            }

            @Test
            void infLibsAsFileList() {
                op.infLibs().clear();
                op.infLibs(List.of(foo, bar));
                assertThat(op.infLibs()).as("List(File...)").containsExactly(foo, bar);

            }

            @Test
            void infLibsAsPathArray() {
                op.infLibs().clear();
                op.infLibs(foo.toPath(), bar.toPath());
                assertThat(op.infLibs()).as("Path...").containsExactly(foo, bar);
            }

            @Test
            void infLibsAsPathList() {
                op.infLibs().clear();
                op.infLibsPaths(List.of(foo.toPath(), bar.toPath()));
                assertThat(op.infLibs()).as("List(Path...)").containsExactly(foo, bar);
            }

            @Test
            void infLibsAsStringArray() {
                op.infLibs().clear();
                op.infLibs(EXAMPLES_LIB_COMPILE + SPRING_BOOT, EXAMPLES_LIB_COMPILE + SPRING_BOOT_ACTUATOR);
                assertThat(op.infLibs()).as("String...").containsExactly(foo, bar);
            }

            @Test
            void infLibsAsStringList() {
                op.infLibs().clear();
                op.infLibsStrings(List.of(EXAMPLES_LIB_COMPILE + SPRING_BOOT, EXAMPLES_LIB_COMPILE + SPRING_BOOT_ACTUATOR));
                assertThat(op.infLibs()).as("List(String...)").containsExactly(foo, bar);

            }
        }

        @Nested
        @DisplayName("Launcher Libs Tests")
        class LauncherLibTests {
            private final File launcher = new File(EXAMPLES_LIB_STANDALONE + SPRING_BOOT_LOADER);

            @Test
            void launcherLibsAsFileArray() throws IOException {
                var op = new BootJarOperation().launcherLibs(launcher);
                assertThat(op.launcherLibs()).as("File...").containsExactly(launcher);
            }

            @Test
            void launcherLibsAsFileList() throws IOException {
                var op = new BootJarOperation().launcherLibs(List.of(launcher));
                assertThat(op.launcherLibs()).as("List(File...)").containsExactly(launcher);
            }

            @Test
            void launcherLibsAsPathArray() throws IOException {
                var op = new BootJarOperation().launcherLibs(launcher.toPath());
                assertThat(op.launcherLibs()).as("Path...").containsExactly(launcher);
            }

            @Test
            void launcherLibsAsPathList() throws IOException {
                var op = new BootJarOperation().launcherLibsPaths(List.of(launcher.toPath()));
                assertThat(op.launcherLibs()).as("List(Path...)").containsExactly(launcher);
            }

            @Test
            void launcherLibsAsStringArray() throws IOException {
                var op = new BootJarOperation().launcherLibs(EXAMPLES_LIB_STANDALONE + SPRING_BOOT_LOADER);
                assertThat(op.launcherLibs()).as("String...").containsExactly(launcher);
            }

            @Test
            void launcherLibsAsStringList() throws IOException {
                var op = new BootJarOperation().launcherLibsStrings(List.of(EXAMPLES_LIB_STANDALONE + SPRING_BOOT_LOADER));
                assertThat(op.launcherLibs()).as("List(String...)").containsExactly(launcher);
            }
        }

        @Nested
        @DisplayName("War Libs Tests")
        class WarLibTest {
            private final File foo = new File(EXAMPLES_LIB_RUNTIME + PROVIDED_LIB);
            private final BootWarOperation op = new BootWarOperation();

            @Test
            void warProvidedLibs() {
                op.providedLibs().clear();
                op.providedLibs(foo);
                assertThat(op.providedLibs()).containsExactly(foo);
            }

            @Test
            void warProvidedLibsAsPath() {
                op.providedLibs().clear();
                op.providedLibs(foo.toPath());
                assertThat(op.providedLibs()).containsExactly(foo);
            }

            @Test
            void warProvidedLibsAsString() {
                op.providedLibs().clear();
                op.providedLibs(EXAMPLES_LIB_RUNTIME + PROVIDED_LIB);
                assertThat(op.providedLibs()).containsExactly(foo);
            }
        }
    }

    @Nested
    @DisplayName("Project Tests")
    class ProjectTests {
        @TempDir
        private File tmpDir;

        @Test
        void customProject() throws IOException {
            var project = new CustomProject(tmpDir);
            var bootJar = new BootJarOperation().fromProject(project).sourceDirectories(SRC_MAIN_JAVA);

            try (var softly = new AutoCloseableSoftAssertions()) {
                softly.assertThat(bootJar.mainClass()).as("mainClass").isEqualTo(MAIN_CLASS);
                softly.assertThat(bootJar.sourceDirectories()).as("sourceDirectories.size").hasSize(3)
                        .containsExactly(project.buildMainDirectory(), project.srcMainResourcesDirectory(),
                                new File(SRC_MAIN_JAVA));
                softly.assertThat(bootJar.manifestAttributes()).as("manifestAttributes.size").hasSize(3);
                softly.assertThat(bootJar.manifestAttributes().get("Manifest-Version")).as("Manifest-Version")
                        .isEqualTo("1.0");
                softly.assertThat(bootJar.manifestAttributes().get("Main-Class")).as("Main-Class").endsWith("JarLauncher");
                softly.assertThat(bootJar.manifestAttributes().get("Start-Class")).as("Start-Class").isEqualTo(MAIN_CLASS);
                softly.assertThat(bootJar.manifestAttribute("Manifest-Test", "tsst")
                        .manifestAttributes().get("Manifest-Test")).as("Manifest-Test").isEqualTo("tsst");
                softly.assertThat(bootJar.destinationDirectory()).as("destinationDirectory").isDirectory();
                softly.assertThat(bootJar.destinationDirectory()).isEqualTo(project.buildDistDirectory());
                softly.assertThat(bootJar.infLibs()).as("infoLibs").isEmpty();
                softly.assertThat(bootJar.launcherLibs()).as("launcherJars").isEmpty();
                softly.assertThat(bootJar.destinationFileName()).isEqualTo("test_project-0.0.1-boot.jar");
            }
        }

        @Test
        @SuppressWarnings("PMD.AvoidDuplicateLiterals")
        void jarExecute() throws Exception {
            var jar = "foo-1.1.1.jar";
            new BootJarOperation()
                    .launcherClass("org.springframework.boot.loader.launch.JarLauncher")
                    .launcherLibs(List.of(new File(EXAMPLES_LIB_STANDALONE + SPRING_BOOT_LOADER)))
                    .destinationDirectory(tmpDir)
                    .destinationFileName(jar)
                    .infLibs(new File(EXAMPLES_LIB_COMPILE + SPRING_BOOT),
                            new File(EXAMPLES_LIB_COMPILE + SPRING_BOOT_ACTUATOR))
                    .mainClass(MAIN_CLASS)
                    .sourceDirectories(new File("build/main"))
                    .execute();

            var jarFile = new File(tmpDir, jar);
            assertThat(jarFile).exists();

            var jarEntries = readJarEntries(jarFile);
            assertThat(jarEntries).isEqualToIgnoringNewLines(
                    "BOOT-INF/\n" +
                            "BOOT-INF/classes/\n" +
                            "BOOT-INF/classes/rife/\n" +
                            "BOOT-INF/classes/rife/bld/\n" +
                            "BOOT-INF/classes/rife/bld/extension/\n" +
                            "BOOT-INF/classes/rife/bld/extension/AbstractBootOperation.class\n" +
                            "BOOT-INF/classes/rife/bld/extension/BootJarOperation.class\n" +
                            "BOOT-INF/classes/rife/bld/extension/BootUtils.class\n" +
                            "BOOT-INF/classes/rife/bld/extension/BootWarOperation.class\n" +
                            "BOOT-INF/lib/\n" +
                            "BOOT-INF/lib/" + SPRING_BOOT + '\n' +
                            "BOOT-INF/lib/" + SPRING_BOOT_ACTUATOR + '\n' +
                            "META-INF/\n" +
                            "META-INF/MANIFEST.MF\n" + LAUNCHER_JARS);
        }

        @Test
        void jarProjectExecute() throws Exception {
            new BootJarOperation()
                    .fromProject(new CustomProject(new File(".")))
                    .launcherLibs(List.of(new File(EXAMPLES_LIB_STANDALONE + SPRING_BOOT_LOADER)))
                    .destinationDirectory(tmpDir.getAbsolutePath())
                    .infLibs(new File(EXAMPLES_LIB_COMPILE + SPRING_BOOT).getAbsolutePath(),
                            new File(EXAMPLES_LIB_COMPILE + SPRING_BOOT_ACTUATOR).getAbsolutePath())
                    .execute();

            var jarFile = new File(tmpDir, "test_project-0.0.1-boot.jar");
            assertThat(jarFile).exists();

            var jarEntries = readJarEntries(jarFile);
            assertThat(jarEntries).isEqualToIgnoringNewLines(
                    "BOOT-INF/\n" +
                            "BOOT-INF/classes/\n" +
                            "BOOT-INF/classes/rife/\n" +
                            "BOOT-INF/classes/rife/bld/\n" +
                            "BOOT-INF/classes/rife/bld/extension/\n" +
                            "BOOT-INF/classes/rife/bld/extension/AbstractBootOperation.class\n" +
                            "BOOT-INF/classes/rife/bld/extension/BootJarOperation.class\n" +
                            "BOOT-INF/classes/rife/bld/extension/BootUtils.class\n" +
                            "BOOT-INF/classes/rife/bld/extension/BootWarOperation.class\n" +
                            "BOOT-INF/lib/\n" +
                            "BOOT-INF/lib/" + BLD + '\n' +
                            "BOOT-INF/lib/" + SPRING_BOOT + '\n' +
                            "BOOT-INF/lib/" + SPRING_BOOT_ACTUATOR + '\n' +
                            "META-INF/\n" +
                            "META-INF/MANIFEST.MF\n" + LAUNCHER_JARS);
        }

        @Test
        void jarProjectExecuteWithLoggingDisabled() throws Exception {
            logger.setLevel(Level.OFF);
            jarProjectExecute();
            assertThat(logHandler.getLogMessages()).isEmpty();
        }

        @Test
        void jarProjectExecuteWithSilent() throws Exception {
            new BootJarOperation()
                    .fromProject(new CustomProject(new File(".")))
                    .silent(true)
                    .launcherLibs(List.of(new File(EXAMPLES_LIB_STANDALONE + SPRING_BOOT_LOADER)))
                    .destinationDirectory(tmpDir.getAbsolutePath())
                    .infLibs(new File(EXAMPLES_LIB_COMPILE + SPRING_BOOT).getAbsolutePath(),
                            new File(EXAMPLES_LIB_COMPILE + SPRING_BOOT_ACTUATOR).getAbsolutePath())
                    .execute();
            assertThat(logHandler.getLogMessages()).isEmpty();
        }

        @Test
        void warProjectExecute() throws Exception {
            var project = new CustomProject(new File("."));
            new BootWarOperation()
                    .fromProject(project)
                    .launcherLibs(List.of(new File(EXAMPLES_LIB_STANDALONE + SPRING_BOOT_LOADER)))
                    .destinationDirectory(tmpDir.toPath())
                    .infLibs(Path.of(EXAMPLES_LIB_COMPILE + SPRING_BOOT),
                            Path.of(EXAMPLES_LIB_COMPILE + SPRING_BOOT_ACTUATOR))
                    .providedLibs(new File(EXAMPLES_LIB_RUNTIME + PROVIDED_LIB))
                    .execute();

            var warFile = new File(tmpDir, project.name() + '-' + project.version().toString() + "-boot.war");
            assertThat(warFile).exists();

            var jarEntries = readJarEntries(warFile);
            assertThat(jarEntries).isEqualToIgnoringNewLines(
                    "META-INF/\n" +
                            "META-INF/MANIFEST.MF\n" +
                            "WEB-INF/\n" +
                            "WEB-INF/classes/\n" +
                            "WEB-INF/classes/rife/\n" +
                            "WEB-INF/classes/rife/bld/\n" +
                            "WEB-INF/classes/rife/bld/extension/\n" +
                            "WEB-INF/classes/rife/bld/extension/AbstractBootOperation.class\n" +
                            "WEB-INF/classes/rife/bld/extension/BootJarOperation.class\n" +
                            "WEB-INF/classes/rife/bld/extension/BootUtils.class\n" +
                            "WEB-INF/classes/rife/bld/extension/BootWarOperation.class\n" +
                            "WEB-INF/lib/\n" +
                            "WEB-INF/lib/" + BLD + '\n' +
                            "WEB-INF/lib/dist/\n" +
                            "WEB-INF/lib/" + SPRING_BOOT + '\n' +
                            "WEB-INF/lib/" + SPRING_BOOT_ACTUATOR + '\n' +
                            "WEB-INF/lib-provided/\n" +
                            "WEB-INF/lib-provided/" + PROVIDED_LIB + '\n' + LAUNCHER_JARS);

            FileUtils.deleteDirectory(tmpDir);
        }

        @Test
        void warProjectExecuteWithLoggingDisabled() throws Exception {
            logger.setLevel(Level.OFF);
            warProjectExecute();
            assertThat(logHandler.getLogMessages()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Source Directories Tests")
    class SourceDirectoriesTests {
        public final BootJarOperation op = new BootJarOperation();
        public final File src = new File(SRC_MAIN_JAVA);
        public final File test = new File(SRC_TEST_JAVA);

        @Test
        void sourceDirectoriesAsFileArray() {
            op.sourceDirectories().clear();
            op.sourceDirectories(src, test);
            assertThat(op.sourceDirectories()).as("File...").containsExactly(src, test);
        }

        @Test
        void sourceDirectoriesAsFileList() {
            op.sourceDirectories().clear();
            op.sourceDirectories(List.of(src, test));
            assertThat(op.sourceDirectories()).as("List(File...)").containsExactly(src, test);
        }

        @Test
        void sourceDirectoriesAsPathArray() {
            op.sourceDirectories().clear();
            op.sourceDirectories(src.toPath(), test.toPath());
            assertThat(op.sourceDirectories()).as("Path...").containsExactly(src, test);
        }

        @Test
        void sourceDirectoriesAsPathList() {
            op.sourceDirectories().clear();
            op.sourceDirectoriesPaths(List.of(src.toPath(), test.toPath()));
            assertThat(op.sourceDirectories()).as("List(Path...)").containsExactly(src, test);
        }

        @Test
        void sourceDirectoriesAsStringArray() {
            op.sourceDirectories().clear();
            op.sourceDirectories(SRC_MAIN_JAVA, SRC_TEST_JAVA);
            assertThat(op.sourceDirectories()).as("String...").containsExactly(src, test);
        }

        @Test
        void sourceDirectoriesAsStringList() {
            op.sourceDirectories().clear();
            op.sourceDirectoriesStrings(List.of(SRC_MAIN_JAVA, SRC_TEST_JAVA));
            assertThat(op.sourceDirectories()).as("List(String...").containsExactly(src, test);
        }
    }
}
