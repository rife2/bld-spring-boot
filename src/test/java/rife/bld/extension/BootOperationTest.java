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

import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import rife.bld.Project;
import rife.bld.blueprints.AppProjectBlueprint;
import rife.bld.dependencies.VersionNumber;
import rife.bld.extension.testing.LoggingExtension;
import rife.bld.extension.testing.TestLogHandler;
import rife.tools.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(LoggingExtension.class)
@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.SignatureDeclareThrowsException",
        "PMD.AvoidDuplicateLiterals", "PMD.MoreThanOneLogger"})
class BootOperationTest {

    static final String BOOT_VERSION = "4.0.6";
    static final String EXAMPLES_DIR = "examples/4.0.x";
    static final String EXAMPLES_LIB_COMPILE = EXAMPLES_DIR + "/lib/compile/";
    static final String EXAMPLES_LIB_RUNTIME = EXAMPLES_DIR + "/lib/runtime/";
    static final String EXAMPLES_LIB_STANDALONE = EXAMPLES_DIR + "/lib/standalone/";
    static final String FOO_JAR = "foo.jar";
    static final String MAIN_CLASS = "com.example.Foo";
    static final String SPRING_BOOT_JAR = "spring-boot-" + BOOT_VERSION + ".jar";
    static final String SPRING_BOOT_LOADER_JAR = "spring-boot-loader-" + BOOT_VERSION + ".jar";
    static final String SRC_MAIN_JAVA = "src/main/java";
    static final String SRC_TEST_JAVA = "src/test/java";

    private static final String BLD_EXTENSIONS_TOOLS = "bld-extensions-tools";
    private static final String JSR305 = "jsr305";
    private static final String JSR305_VERSION = "3.0.2";
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
    private static final String SPOTBUGS_ANNOTATIONS = "spotbugs-annotations";
    private static final String SPOTBUGS_ANNOTATIONS_PATTERN = SPOTBUGS_ANNOTATIONS + "-\\d+\\.\\d+\\.\\d+\\.jar";
    private static final String WEB_INF_LIB_PROVIDED_SPOTBUGS_PATTERN = "WEB-INF/lib-provided/"
            + SPOTBUGS_ANNOTATIONS_PATTERN;
    private static final String VERSION_PATTERN = "-\\d+\\.\\d+\\.\\d+(-SNAPSHOT|-\\d{8}\\.\\d{6}-\\d+)?";
    private static final String BLD_EXTENSIONS_TOOLS_PATTERN = BLD_EXTENSIONS_TOOLS
            + VERSION_PATTERN + "\\.jar";
    private static final String BOOT_INF_LIB_BLD_EXTENSIONS_TOOLS_PATTERN = "BOOT-INF/lib/"
            + BLD_EXTENSIONS_TOOLS_PATTERN;
    private static final String WEB_INF_LIB_BLD_EXTENSIONS_TOOLS_PATTERN = "WEB-INF/lib/"
            + BLD_EXTENSIONS_TOOLS_PATTERN;
    private static final String BLD_PATTERN = "bld" + VERSION_PATTERN + "\\.jar";
    private static final String BOOT_INF_LIB_BLD_PATTERN = "BOOT-INF/lib/" + BLD_PATTERN;
    private static final String WEB_INF_LIB_BLD_PATTERN = "WEB-INF/lib/" + BLD_PATTERN;
    private static final String JSR305_PATTERN = JSR305 + VERSION_PATTERN + "\\.jar";
    private static final String WEB_INF_LIB_PROVIDED_JSR305_PATTERN = "WEB-INF/lib-provided/" + JSR305_PATTERN;

    @SuppressWarnings({"LoggerInitializedWithForeignClass"})
    private final Logger bootJarLogger = Logger.getLogger(BootJarOperation.class.getName());
    @SuppressWarnings({"LoggerInitializedWithForeignClass"})
    private final Logger bootWarLogger = Logger.getLogger(BootWarOperation.class.getName());

    private TestLogHandler jarLogHandler;
    private TestLogHandler warLogHandler;


    @AfterEach
    void afterEach() {
        if (jarLogHandler != null) {
            bootJarLogger.removeHandler(jarLogHandler);
        }
        if (warLogHandler != null) {
            bootWarLogger.removeHandler(warLogHandler);
        }
    }

    @BeforeEach
    void beforeEach() {
        jarLogHandler = new TestLogHandler();
        bootJarLogger.addHandler(jarLogHandler);
        bootJarLogger.setLevel(Level.ALL);

        warLogHandler = new TestLogHandler();
        bootWarLogger.addHandler(warLogHandler);
        bootWarLogger.setLevel(Level.ALL);
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
    @DisplayName("Project Tests")
    class ProjectTests {

        @TempDir
        private File tmpDir;

        @Test
        void boot3() {
            var project = new AppProjectBlueprint(
                    new File("examples/3.5.x"),
                    "com.example.demo",
                    "demo",
                    "DemoApplication");
            assertThat(project.execute(new String[]{"clean", "compile", "test"})).isEqualTo(0);
        }

        @Test
        void customProject() {
            var project = new CustomProject(tmpDir);
            var bootJar = new BootJarOperation().fromProject(project).sourceDirectories(SRC_MAIN_JAVA);
            var map = Map.of("foo", "bar", "baz", "foz");

            try (var softly = new AutoCloseableSoftAssertions()) {
                softly.assertThat(bootJar.mainClass()).as("mainClass").isEqualTo(MAIN_CLASS);
                softly.assertThat(bootJar.sourceDirectories()).as("sourceDirectories.size").hasSize(3)
                        .containsExactly(project.buildMainDirectory(), project.srcMainResourcesDirectory(),
                                new File(SRC_MAIN_JAVA));
                softly.assertThat(bootJar.manifestAttributes()).as("manifestAttributes.size")
                        .hasSize(3);
                softly.assertThat(bootJar.manifestAttributes().get("Manifest-Version")).as("Manifest-Version")
                        .isEqualTo("1.0");
                softly.assertThat(bootJar.manifestAttributes().get("Main-Class")).as("Main-Class")
                        .endsWith("JarLauncher");
                softly.assertThat(bootJar.manifestAttributes().get("Start-Class")).as("Start-Class")
                        .isEqualTo(MAIN_CLASS);
                softly.assertThat(bootJar.manifestAttribute("Manifest-Test", "test")
                                .manifestAttributes().get("Manifest-Test")).as("Manifest-Test")
                        .isEqualTo("test");
                softly.assertThat(bootJar.manifestAttributes(map).manifestAttributes().get("foo"))
                        .as("food").isEqualTo("bar");
                softly.assertThat(bootJar.manifestAttributes().get("baz")).as("baz").isEqualTo("foz");
                softly.assertThat(bootJar.destinationDirectory()).isEqualTo(project.buildDistDirectory());
                softly.assertThat(bootJar.infLibs()).as("infoLibs").isEmpty();
                softly.assertThat(bootJar.launcherLibs()).as("launcherJars").isEmpty();
                softly.assertThat(bootJar.destinationFileName()).isEqualTo("test_project-0.0.1-boot.jar");
            }
        }

        @Test
        void jarExecute() throws Exception {
            var jar = "foo-1.1.1.jar";
            new BootJarOperation()
                    .launcherClass("org.springframework.boot.loader.launch.JarLauncher")
                    .launcherLibs(List.of(new File(EXAMPLES_LIB_STANDALONE + SPRING_BOOT_LOADER_JAR)))
                    .destinationDirectory(tmpDir)
                    .destinationFileName(jar)
                    .infLibs(new File(EXAMPLES_LIB_COMPILE + SPRING_BOOT_JAR))
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
                            "BOOT-INF/lib/" + SPRING_BOOT_JAR + '\n' +
                            "META-INF/\n" +
                            "META-INF/MANIFEST.MF\n" + LAUNCHER_JARS);
        }

        @Test
        void jarProjectExecute() throws Exception {
            new BootJarOperation()
                    .fromProject(new CustomProject(new File(".")))
                    .launcherLibs(List.of(new File(EXAMPLES_LIB_STANDALONE + SPRING_BOOT_LOADER_JAR)))
                    .destinationDirectory(tmpDir.getAbsolutePath())
                    .infLibs(new File(EXAMPLES_LIB_COMPILE + SPRING_BOOT_JAR).getAbsolutePath(),
                            new File(EXAMPLES_LIB_COMPILE + FOO_JAR).getAbsolutePath())
                    .execute();

            var jarFile = new File(tmpDir, "test_project-0.0.1-boot.jar");
            assertThat(jarFile).exists();

            var jarEntries = readJarEntries(jarFile);
            assertThat(jarEntries.toString())
                    .contains("BOOT-INF/")
                    .contains("BOOT-INF/classes/")
                    .contains("BOOT-INF/classes/rife/")
                    .contains("BOOT-INF/classes/rife/bld/")
                    .contains("BOOT-INF/classes/rife/bld/extension/")
                    .contains("BOOT-INF/classes/rife/bld/extension/AbstractBootOperation.class")
                    .contains("BOOT-INF/classes/rife/bld/extension/BootJarOperation.class")
                    .contains("BOOT-INF/classes/rife/bld/extension/BootUtils.class")
                    .contains("BOOT-INF/classes/rife/bld/extension/BootWarOperation.class")
                    .contains("BOOT-INF/lib/")
                    .containsPattern(BOOT_INF_LIB_BLD_PATTERN)
                    .containsPattern(BOOT_INF_LIB_BLD_EXTENSIONS_TOOLS_PATTERN)
                    .contains("BOOT-INF/lib/" + SPRING_BOOT_JAR)
                    .contains("META-INF/")
                    .contains("META-INF/MANIFEST.MF")
                    .contains("org/springframework/boot/loader/");
        }

        @Test
        void jarProjectExecuteWithLogging() throws Exception {
            jarProjectExecute();
            assertThat(jarLogHandler.getLogMessages()).isNotEmpty();
            assertThat(jarLogHandler.containsMessage("The executable JAR was created"));
        }

        @Test
        void jarProjectExecuteWithLoggingDisabled() throws Exception {
            bootJarLogger.setLevel(Level.OFF);
            jarProjectExecute();
            assertThat(jarLogHandler.getLogMessages()).isEmpty();
        }

        @Test
        void jarProjectExecuteWithSilent() throws Exception {
            new BootJarOperation()
                    .fromProject(new CustomProject(new File(".")))
                    .silent(true)
                    .launcherLibs(List.of(new File(EXAMPLES_LIB_STANDALONE + SPRING_BOOT_LOADER_JAR)))
                    .destinationDirectory(tmpDir.getAbsolutePath())
                    .infLibs(new File(EXAMPLES_LIB_COMPILE + SPRING_BOOT_JAR).getAbsolutePath(),
                            new File(EXAMPLES_LIB_COMPILE + FOO_JAR).getAbsolutePath())
                    .execute();
            assertThat(jarLogHandler.getLogMessages()).isEmpty();
        }

        @Test
        void warProjectExecute() throws Exception {
            var project = new CustomProject(new File("."));
            new BootWarOperation()
                    .fromProject(project)
                    .launcherLibs(List.of(new File(EXAMPLES_LIB_STANDALONE + SPRING_BOOT_LOADER_JAR)))
                    .destinationDirectory(tmpDir.toPath())
                    .infLibs(Path.of(EXAMPLES_LIB_COMPILE + SPRING_BOOT_JAR),
                            Path.of(EXAMPLES_LIB_COMPILE + FOO_JAR))
                    .providedLibs(new File(EXAMPLES_LIB_RUNTIME + JSR305 + '-' + JSR305_VERSION + ".jar"))
                    .execute();

            var warFile = new File(tmpDir, project.name() + '-' + project.version().toString() + "-boot.war");
            assertThat(warFile).exists();

            var jarEntries = readJarEntries(warFile);
            assertThat(jarEntries.toString())
                    .contains("META-INF/")
                    .contains("META-INF/MANIFEST.MF")
                    .contains("WEB-INF/")
                    .contains("WEB-INF/classes/")
                    .contains("WEB-INF/classes/rife/")
                    .contains("WEB-INF/classes/rife/bld/")
                    .contains("WEB-INF/classes/rife/bld/extension/")
                    .contains("WEB-INF/classes/rife/bld/extension/AbstractBootOperation.class")
                    .contains("WEB-INF/classes/rife/bld/extension/BootJarOperation.class")
                    .contains("WEB-INF/classes/rife/bld/extension/BootUtils.class")
                    .contains("WEB-INF/classes/rife/bld/extension/BootWarOperation.class")
                    .contains("WEB-INF/lib/")
                    .containsPattern(WEB_INF_LIB_BLD_PATTERN)
                    .containsPattern(WEB_INF_LIB_BLD_EXTENSIONS_TOOLS_PATTERN)
                    .contains("WEB-INF/lib/dist/")
                    .contains("WEB-INF/lib/" + SPRING_BOOT_JAR)
                    .contains("WEB-INF/lib-provided/")
                    .containsPattern(WEB_INF_LIB_PROVIDED_JSR305_PATTERN)
                    .containsPattern(WEB_INF_LIB_PROVIDED_SPOTBUGS_PATTERN)
                    .contains("org/springframework/boot/loader/");

            FileUtils.deleteDirectory(tmpDir);
        }

        @Test
        void warProjectExecuteWithLogging() throws Exception {
            warProjectExecute();
            assertThat(warLogHandler.getLogMessages()).isNotEmpty();
            assertThat(warLogHandler.containsMessage("The executable WAR was created")).isTrue();
        }

        @Test
        void warProjectExecuteWithLoggingDisabled() throws Exception {
            bootWarLogger.setLevel(Level.OFF);
            warProjectExecute();
            bootWarLogger.setLevel(Level.ALL);
            assertThat(warLogHandler.getLogMessages()).isEmpty();
        }
    }
}
