/*
 * Copyright 2023-2024 the original author or authors.
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

import org.junit.jupiter.api.Test;
import rife.bld.Project;
import rife.bld.dependencies.VersionNumber;
import rife.tools.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class BootJarOperationTest {
    private static final String BLD = "bld-2.1.0.jar";
    private static final String BOOT_VERSION = "3.3.3";
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

    private StringBuilder readJarEntries(File jar) throws IOException {
        var jarEntries = new StringBuilder();
        try (var jarFile = new JarFile(jar)) {
            for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements(); ) {
                jarEntries.append(entries.nextElement().getName()).append('\n');
            }
        }
        return jarEntries;
    }

    @Test
    void testErrors() throws IOException {
        var bootWar = new BootWarOperation();
        assertThatCode(bootWar::execute).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mainClass");

        bootWar = bootWar.mainClass(MAIN_CLASS);
        assertThatCode(bootWar::execute)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("class required");

        assertThatCode(() -> new BootWarOperation().launcherLibs(new File("foo")))
                .as("foo")
                .isInstanceOf(IOException.class)
                .hasMessageContaining("not found");

        assertThatCode(() -> new BootWarOperation().launcherLibs("bar"))
                .as("bar")
                .isInstanceOf(IOException.class)
                .hasMessageContaining("not found");

        bootWar = bootWar.launcherLibs(List.of(new File(EXAMPLES_LIB_STANDALONE + SPRING_BOOT_LOADER)));
        assertThatCode(bootWar::execute)
                .isInstanceOf((IllegalArgumentException.class))
                .hasMessageContaining("class required");

        bootWar = bootWar.launcherClass("org.springframework.boot.loader.launch.WarLauncher");
        assertThat(bootWar.verifyExecute()).isTrue();
    }

    @Test
    void testInfLibs() {
        var op = new BootWarOperation();

        var foo = new File(EXAMPLES_LIB_COMPILE + SPRING_BOOT);
        var bar = new File(EXAMPLES_LIB_COMPILE + SPRING_BOOT_ACTUATOR);

        op.infLibs(EXAMPLES_LIB_COMPILE + SPRING_BOOT, EXAMPLES_LIB_COMPILE + SPRING_BOOT_ACTUATOR);
        assertThat(op.infLibs()).as("String...").containsExactly(foo, bar);
        op.infLibs().clear();

        op.infLibs(foo, bar);
        assertThat(op.infLibs()).as("File...").containsExactly(foo, bar);
        op.infLibs().clear();

        op.infLibs(foo.toPath(), bar.toPath());
        assertThat(op.infLibs()).as("Path...").containsExactly(foo, bar);
        op.infLibs().clear();

        op.infLibsStrings(List.of(EXAMPLES_LIB_COMPILE + SPRING_BOOT, EXAMPLES_LIB_COMPILE + SPRING_BOOT_ACTUATOR));
        assertThat(op.infLibs()).as("List(String...)").containsExactly(foo, bar);
        op.infLibs().clear();

        op.infLibs(List.of(foo, bar));
        assertThat(op.infLibs()).as("List(File...)").containsExactly(foo, bar);
        op.infLibs().clear();

        op.infLibsPaths(List.of(foo.toPath(), bar.toPath()));
        assertThat(op.infLibs()).as("List(Path...)").containsExactly(foo, bar);
        op.infLibs().clear();
    }

    @Test
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    void testJarExecute() throws Exception {
        var tmp_dir = Files.createTempDirectory("bootjartmp").toFile();
        var jar = "foo-1.1.1.jar";
        new BootJarOperation()
                .launcherClass("org.springframework.boot.loader.launch.JarLauncher")
                .launcherLibs(List.of(new File(EXAMPLES_LIB_STANDALONE + SPRING_BOOT_LOADER)))
                .destinationDirectory(tmp_dir)
                .destinationFileName(jar)
                .infLibs(new File(EXAMPLES_LIB_COMPILE + SPRING_BOOT),
                        new File(EXAMPLES_LIB_COMPILE + SPRING_BOOT_ACTUATOR))
                .mainClass(MAIN_CLASS)
                .sourceDirectories(new File("build/main"))
                .execute();

        var jarFile = new File(tmp_dir, jar);
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

        FileUtils.deleteDirectory(tmp_dir);
    }

    @Test
    void testJarProjectExecute() throws Exception {
        var tmp_dir = Files.createTempDirectory("bootwartmp").toFile();
        new BootJarOperation()
                .fromProject(new CustomProject(new File(".")))
                .launcherLibs(List.of(new File(EXAMPLES_LIB_STANDALONE + SPRING_BOOT_LOADER)))
                .destinationDirectory(tmp_dir.getAbsolutePath())
                .infLibs(new File(EXAMPLES_LIB_COMPILE + SPRING_BOOT).getAbsolutePath(),
                        new File(EXAMPLES_LIB_COMPILE + SPRING_BOOT_ACTUATOR).getAbsolutePath())
                .execute();

        var jarFile = new File(tmp_dir, "test_project-0.0.1-boot.jar");
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

        FileUtils.deleteDirectory(tmp_dir);
    }

    @Test
    void testLauncherLibs() throws IOException {
        var op = new BootJarOperation();

        var launcher = new File(EXAMPLES_LIB_STANDALONE + SPRING_BOOT_LOADER);
        op = op.launcherLibs(EXAMPLES_LIB_STANDALONE + SPRING_BOOT_LOADER);
        assertThat(op.launcherLibs()).as("String...").containsExactly(launcher);
        op.launcherLibs().clear();

        op = op.launcherLibs(launcher);
        assertThat(op.launcherLibs()).as("File...").containsExactly(launcher);
        op.launcherLibs().clear();

        op = op.launcherLibs(launcher.toPath());
        assertThat(op.launcherLibs()).as("Path...").containsExactly(launcher);
        op.launcherLibs().clear();

        op = op.launcherLibsStrings(List.of(EXAMPLES_LIB_STANDALONE + SPRING_BOOT_LOADER));
        assertThat(op.launcherLibs()).as("List(String...)").containsExactly(launcher);
        op.launcherLibs().clear();

        op = op.launcherLibs(List.of(launcher));
        assertThat(op.launcherLibs()).as("List(File...)").containsExactly(launcher);
        op.launcherLibs().clear();

        op = op.launcherLibsPaths(List.of(launcher.toPath()));
        assertThat(op.launcherLibs()).as("List(Path...)").containsExactly(launcher);
        op.launcherLibs().clear();
    }

    @Test
    void testProject() throws IOException {
        var tmp_dir = Files.createTempDirectory("bootprjtmp").toFile();
        var project = new CustomProject(tmp_dir);
        var bootJar = new BootJarOperation().fromProject(project).sourceDirectories(SRC_MAIN_JAVA);

        assertThat(bootJar.mainClass()).as("mainClass").isEqualTo(MAIN_CLASS);
        assertThat(bootJar.sourceDirectories()).as("sourceDirectories.size").hasSize(3)
                .containsExactly(project.buildMainDirectory(), project.srcMainResourcesDirectory(),
                        new File(SRC_MAIN_JAVA));
        assertThat(bootJar.manifestAttributes()).as("manifestAttributes.size").hasSize(3);
        assertThat(bootJar.manifestAttributes().get("Manifest-Version")).as("Manifest-Version").isEqualTo("1.0");
        assertThat(bootJar.manifestAttributes().get("Main-Class")).as("Main-Class").endsWith("JarLauncher");
        assertThat(bootJar.manifestAttributes().get("Start-Class")).as("Start-Class").isEqualTo(MAIN_CLASS);
        assertThat(bootJar.manifestAttribute("Manifest-Test", "tsst")
                .manifestAttributes().get("Manifest-Test")).as("Manifest-Test").isEqualTo("tsst");
        assertThat(bootJar.destinationDirectory()).as("destinationDirectory").isDirectory();
        assertThat(bootJar.destinationDirectory()).isEqualTo(project.buildDistDirectory());
        assertThat(bootJar.infLibs()).as("infoLibs").isEmpty();
        assertThat(bootJar.launcherLibs()).as("launcherJars").isEmpty();
        assertThat(bootJar.destinationFileName()).isEqualTo("test_project-0.0.1-boot.jar");

        FileUtils.deleteDirectory(tmp_dir);
    }

    @Test
    void testSourceDirectories() {
        var op = new BootJarOperation();

        var src = new File(SRC_MAIN_JAVA);
        var test = new File(SRC_TEST_JAVA);
        op = op.sourceDirectories(SRC_MAIN_JAVA, SRC_TEST_JAVA);
        assertThat(op.sourceDirectories()).as("String...").containsExactly(src, test);
        op.sourceDirectories().clear();

        op = op.sourceDirectories(src, test);
        assertThat(op.sourceDirectories()).as("File...").containsExactly(src, test);
        op.sourceDirectories().clear();

        op = op.sourceDirectories(src.toPath(), test.toPath());
        assertThat(op.sourceDirectories()).as("Path...").containsExactly(src, test);
        op.sourceDirectories().clear();

        op.sourceDirectoriesStrings(List.of(SRC_MAIN_JAVA, SRC_TEST_JAVA));
        assertThat(op.sourceDirectories()).as("List(String...").containsExactly(src, test);
        op.sourceDirectories().clear();

        op.sourceDirectories(List.of(src, test));
        assertThat(op.sourceDirectories()).as("List(File...)").containsExactly(src, test);
        op.sourceDirectories().clear();

        op.sourceDirectoriesPaths(List.of(src.toPath(), test.toPath()));
        assertThat(op.sourceDirectories()).as("List(Path...)").containsExactly(src, test);
        op.sourceDirectories().clear();
    }

    @Test
    void testWarProjectExecute() throws Exception {
        var tmp_dir = Files.createTempDirectory("bootjartmp").toFile();
        var project = new CustomProject(new File("."));
        new BootWarOperation()
                .fromProject(project)
                .launcherLibs(List.of(new File(EXAMPLES_LIB_STANDALONE + SPRING_BOOT_LOADER)))
                .destinationDirectory(tmp_dir.toPath())
                .infLibs(Path.of(EXAMPLES_LIB_COMPILE + SPRING_BOOT),
                        Path.of(EXAMPLES_LIB_COMPILE + SPRING_BOOT_ACTUATOR))
                .providedLibs(new File(EXAMPLES_LIB_RUNTIME + PROVIDED_LIB))
                .execute();

        var warFile = new File(tmp_dir, project.name() + '-' + project.version().toString() + "-boot.war");
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

        FileUtils.deleteDirectory(tmp_dir);
    }

    @Test
    void testWarProvidedLibs() {
        var op = new BootWarOperation();

        var foo = new File(EXAMPLES_LIB_RUNTIME + PROVIDED_LIB);
        op = op.providedLibs(EXAMPLES_LIB_RUNTIME + PROVIDED_LIB);
        assertThat(op.providedLibs()).containsExactly(foo);
        op.providedLibs().clear();

        op = op.providedLibs(foo);
        assertThat(op.providedLibs()).containsExactly(foo);
        op.providedLibs().clear();

        op = op.providedLibs(foo.toPath());
        assertThat(op.providedLibs()).containsExactly(foo);
        op.providedLibs().clear();
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
}
