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
    private static final String BLD = "bld-1.7.5.jar";
    private static final String EXAMPLES_LIB_COMPILE = "examples/lib/compile/";
    private static final String EXAMPLES_LIB_RUNTIME = "examples/lib/runtime/";
    private static final String EXAMPLES_LIB_STANDALONE = "examples/lib/standalone/";
    private static final String LAUNCHER_JARS = """
            org/
            org/springframework/
            org/springframework/boot/
            org/springframework/boot/loader/
            org/springframework/boot/loader/ClassPathIndexFile.class
            org/springframework/boot/loader/ExecutableArchiveLauncher.class
            org/springframework/boot/loader/JarLauncher.class
            org/springframework/boot/loader/LaunchedURLClassLoader$DefinePackageCallType.class
            org/springframework/boot/loader/LaunchedURLClassLoader$UseFastConnectionExceptionsEnumeration.class
            org/springframework/boot/loader/LaunchedURLClassLoader.class
            org/springframework/boot/loader/Launcher.class
            org/springframework/boot/loader/MainMethodRunner.class
            org/springframework/boot/loader/PropertiesLauncher$ArchiveEntryFilter.class
            org/springframework/boot/loader/PropertiesLauncher$ClassPathArchives.class
            org/springframework/boot/loader/PropertiesLauncher$PrefixMatchingArchiveFilter.class
            org/springframework/boot/loader/PropertiesLauncher.class
            org/springframework/boot/loader/WarLauncher.class
            org/springframework/boot/loader/archive/
            org/springframework/boot/loader/archive/Archive$Entry.class
            org/springframework/boot/loader/archive/Archive$EntryFilter.class
            org/springframework/boot/loader/archive/Archive.class
            org/springframework/boot/loader/archive/ExplodedArchive$AbstractIterator.class
            org/springframework/boot/loader/archive/ExplodedArchive$ArchiveIterator.class
            org/springframework/boot/loader/archive/ExplodedArchive$EntryIterator.class
            org/springframework/boot/loader/archive/ExplodedArchive$FileEntry.class
            org/springframework/boot/loader/archive/ExplodedArchive$SimpleJarFileArchive.class
            org/springframework/boot/loader/archive/ExplodedArchive.class
            org/springframework/boot/loader/archive/JarFileArchive$AbstractIterator.class
            org/springframework/boot/loader/archive/JarFileArchive$EntryIterator.class
            org/springframework/boot/loader/archive/JarFileArchive$JarFileEntry.class
            org/springframework/boot/loader/archive/JarFileArchive$NestedArchiveIterator.class
            org/springframework/boot/loader/archive/JarFileArchive.class
            org/springframework/boot/loader/data/
            org/springframework/boot/loader/data/RandomAccessData.class
            org/springframework/boot/loader/data/RandomAccessDataFile$DataInputStream.class
            org/springframework/boot/loader/data/RandomAccessDataFile$FileAccess.class
            org/springframework/boot/loader/data/RandomAccessDataFile.class
            org/springframework/boot/loader/jar/
            org/springframework/boot/loader/jar/AbstractJarFile$JarFileType.class
            org/springframework/boot/loader/jar/AbstractJarFile.class
            org/springframework/boot/loader/jar/AsciiBytes.class
            org/springframework/boot/loader/jar/Bytes.class
            org/springframework/boot/loader/jar/CentralDirectoryEndRecord$Zip64End.class
            org/springframework/boot/loader/jar/CentralDirectoryEndRecord$Zip64Locator.class
            org/springframework/boot/loader/jar/CentralDirectoryEndRecord.class
            org/springframework/boot/loader/jar/CentralDirectoryFileHeader.class
            org/springframework/boot/loader/jar/CentralDirectoryParser.class
            org/springframework/boot/loader/jar/CentralDirectoryVisitor.class
            org/springframework/boot/loader/jar/FileHeader.class
            org/springframework/boot/loader/jar/Handler.class
            org/springframework/boot/loader/jar/JarEntry.class
            org/springframework/boot/loader/jar/JarEntryCertification.class
            org/springframework/boot/loader/jar/JarEntryFilter.class
            org/springframework/boot/loader/jar/JarFile$1.class
            org/springframework/boot/loader/jar/JarFile$JarEntryEnumeration.class
            org/springframework/boot/loader/jar/JarFile.class
            org/springframework/boot/loader/jar/JarFileEntries$1.class
            org/springframework/boot/loader/jar/JarFileEntries$EntryIterator.class
            org/springframework/boot/loader/jar/JarFileEntries$Offsets.class
            org/springframework/boot/loader/jar/JarFileEntries$Zip64Offsets.class
            org/springframework/boot/loader/jar/JarFileEntries$ZipOffsets.class
            org/springframework/boot/loader/jar/JarFileEntries.class
            org/springframework/boot/loader/jar/JarFileWrapper.class
            org/springframework/boot/loader/jar/JarURLConnection$1.class
            org/springframework/boot/loader/jar/JarURLConnection$JarEntryName.class
            org/springframework/boot/loader/jar/JarURLConnection.class
            org/springframework/boot/loader/jar/StringSequence.class
            org/springframework/boot/loader/jar/ZipInflaterInputStream.class
            org/springframework/boot/loader/jarmode/
            org/springframework/boot/loader/jarmode/JarMode.class
            org/springframework/boot/loader/jarmode/JarModeLauncher.class
            org/springframework/boot/loader/jarmode/TestJarMode.class
            org/springframework/boot/loader/util/
            org/springframework/boot/loader/util/SystemPropertyUtils.class
            """;
    private static final String MAIN_CLASS = "com.example.Foo";
    private static final String PROVIDED_LIB = "LatencyUtils-2.0.3.jar";
    private static final String SPRING_BOOT = "spring-boot-3.1.5.jar";
    private static final String SPRING_BOOT_ACTUATOR = "spring-boot-actuator-3.1.5.jar";
    private static final String SPRING_BOOT_LOADER = "spring-boot-loader-3.1.5.jar";

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
                .hasMessageContaining("spring-boot-loader");

        assertThatCode(() -> new BootWarOperation().launcherLibs(List.of(new File("foo"))))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("found");

        bootWar = bootWar.launcherLibs(List.of(new File(EXAMPLES_LIB_STANDALONE + SPRING_BOOT_LOADER)));
        assertThatCode(bootWar::execute)
                .isInstanceOf((IllegalArgumentException.class))
                .hasMessageContaining("class required").hasMessageContaining("spring-boot-loader");

        bootWar = bootWar.launcherClass("org.springframework.boot.loader.WarLauncher");
        assertThat(bootWar.verifyExecute()).isTrue();
    }

    @Test
    void testJarExecute() throws Exception {
        var tmp_dir = Files.createTempDirectory("bootjartmp").toFile();
        var jar = "foo-1.1.1.jar";
        new BootJarOperation()
                .launcherClass("org.springframework.boot.loader.JarLauncher")
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
                        "BOOT-INF/classes/rife/bld/extension/BootManifestAttribute.class\n" +
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
                .destinationDirectory(tmp_dir)
                .infLibs(new File(EXAMPLES_LIB_COMPILE + SPRING_BOOT),
                        new File(EXAMPLES_LIB_COMPILE + SPRING_BOOT_ACTUATOR))
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
                        "BOOT-INF/classes/rife/bld/extension/BootManifestAttribute.class\n" +
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
    void testProject() throws IOException {
        var tmp_dir = Files.createTempDirectory("bootprjtmp").toFile();
        var project = new CustomProject(tmp_dir);
        var bootJar = new BootJarOperation().fromProject(project);

        assertThat(bootJar.mainClass()).as("mainClass").isEqualTo(MAIN_CLASS);
        assertThat(bootJar.sourceDirectories()).as("sourceDirectories.size").hasSize(2);
        assertThat(bootJar.manifestAttributes()).as("manifestAttributes.size").hasSize(3);
        assertThat(bootJar.manifestAttributes().get(0)).as("Manifest-Version")
                .isEqualTo(new BootManifestAttribute("Manifest-Version", "1.0"));
        assertThat(bootJar.manifestAttributes().get(1).value()).as("Main-Class").endsWith("JarLauncher");
        assertThat(bootJar.manifestAttributes().get(2)).as("Start-Class")
                .isEqualTo(new BootManifestAttribute("Start-Class", MAIN_CLASS));
        assertThat(bootJar.manifestAttribute("Manifest-Test", "tsst")
                .manifestAttributes()).as("Manifest-Test").hasSize(4)
                .element(3).extracting(BootManifestAttribute::name).isEqualTo("Manifest-Test");
        assertThat(bootJar.destinationDirectory()).as("destinationDirectory").isDirectory();
        assertThat(bootJar.destinationDirectory().getAbsolutePath()).as("destinationDirectory")
                .isEqualTo(Path.of(tmp_dir.getPath(), "build", "dist").toString());
        assertThat(bootJar.infLibs()).as("infoLibs").isEmpty();
        assertThat(bootJar.launcherLibs()).as("launcherJars").isEmpty();
        assertThat(bootJar.destinationFileName()).isEqualTo("test_project-0.0.1-boot.jar");

        FileUtils.deleteDirectory(tmp_dir);
    }

    @Test
    void testWarProjectExecute() throws Exception {
        var tmp_dir = Files.createTempDirectory("bootjartmp").toFile();
        var project = new CustomProject(new File("."));
        new BootWarOperation()
                .fromProject(project)
                .launcherLibs(List.of(new File(EXAMPLES_LIB_STANDALONE + SPRING_BOOT_LOADER)))
                .destinationDirectory(tmp_dir)
                .infLibs(new File(EXAMPLES_LIB_COMPILE + SPRING_BOOT),
                        new File(EXAMPLES_LIB_COMPILE + SPRING_BOOT_ACTUATOR))
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
                        "WEB-INF/classes/rife/bld/extension/BootManifestAttribute.class\n" +
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