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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import rife.bld.Project;
import rife.bld.blueprints.BaseProjectBlueprint;
import rife.tools.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals"})
class BootUtilsTests {
    @Nested
    @DisplayName("FileSize Tests")
    class FileSizeTests {
        private static final String TMP_FILE_PREFIX = "filesize-tmp-";

        @Test
        void fileSizeInBytes() throws IOException {
            var file = File.createTempFile(TMP_FILE_PREFIX, null);
            file.deleteOnExit();

            Files.write(file.toPath(), "1234".getBytes());
            assertEquals("4 B", BootUtils.fileSize(file));
        }

        @Test
        void fileSizeInGigabytes() {
            // This test simulates a 3 GB file by mocking the length method
            var file = new File("testFile") {
                @Override
                public long length() {
                    return 3L * 1024 * 1024 * 1024; // 3 GB
                }
            };
            assertEquals("3 GB", BootUtils.fileSize(file));
        }

        @Test
        void fileSizeInKilobytes() throws IOException {
            var file = File.createTempFile(TMP_FILE_PREFIX, null);
            file.deleteOnExit();

            var data = new byte[1500]; // ~1.46 KB
            Files.write(file.toPath(), data);

            assertEquals("1.5 KB", BootUtils.fileSize(file));
        }

        @Test
        void fileSizeInMegabytes() throws IOException {
            var file = File.createTempFile(TMP_FILE_PREFIX, null);
            file.deleteOnExit();

            var data = new byte[5 * 1024 * 1024]; // 5 MB
            Files.write(file.toPath(), data);

            assertEquals("5 MB", BootUtils.fileSize(file));
        }

        @Test
        void fileSizeZeroBytes() throws IOException {
            var file = File.createTempFile(TMP_FILE_PREFIX, null);
            file.deleteOnExit();
            assertEquals("0 B", BootUtils.fileSize(file));
        }
    }

    @Nested
    @DisplayName("LauncherClass Tests")
    class LauncherClassTests {
        private final File examplesProjectDir = new File("examples");

        @Test
        void jarLauncher() {
            var launcher = BootUtils.launcherClass(new BaseProjectBlueprint(
                    examplesProjectDir, "com.example", "examples", "Examples"), "JarLauncher");
            assertEquals("org.springframework.boot.loader.JarLauncher", launcher);
        }

        @Test
        void warLauncher() {
            var launcher = BootUtils.launcherClass(new BaseProjectBlueprint(
                    examplesProjectDir, "com.example", "examples", "Examples"), "WarLauncher");
            assertEquals("org.springframework.boot.loader.WarLauncher", launcher);
        }
    }

    @Nested
    @DisplayName("LauncherClass Version Tests")
    @SuppressWarnings("PMD.MethodNamingConventions")
    class LauncherClassVersionTests {
        @Test
        void launcherClassWithVersion3_2OrHigher() {
            var project = new Project() {
                @Override
                public List<File> standaloneClasspathJars() {
                    return List.of(new File("spring-boot-loader-3.2.0.jar"));
                }
            };

            assertEquals("org.springframework.boot.loader.launch.JarLauncher",
                    BootUtils.launcherClass(project, "JarLauncher"));
        }

        @Test
        void launcherClassWithVersion4() {
            var project = new Project() {
                @Override
                public List<File> standaloneClasspathJars() {
                    return List.of(new File("spring-boot-loader-4.0.0.jar"));
                }
            };

            assertEquals("org.springframework.boot.loader.launch.JarLauncher",
                    BootUtils.launcherClass(project, "JarLauncher"));
        }

        @Test
        void launcherClassWithVersionLowerThan3_2() {
            var project = new Project() {
                @Override
                public List<File> standaloneClasspathJars() {
                    return List.of(new File("spring-boot-loader-2.0.0.jar"));
                }
            };

            assertEquals("org.springframework.boot.loader.JarLauncher",
                    BootUtils.launcherClass(project, "JarLauncher"));
        }

        @Test
        void launcherClassWithoutMatchingJar() {
            var project = new Project() {
                @Override
                public List<File> standaloneClasspathJars() {
                    return List.of(new File("foo-1.0.0.jar"));
                }
            };

            assertEquals("org.springframework.boot.loader.JarLauncher",
                    BootUtils.launcherClass(project, "JarLauncher"));
        }


    }

    @Nested
    @DisplayName("MkDirs Tests")
    class MkDirsTests {
        @TempDir
        private File tmpDir;

        @Test
        void mkDirsCreatesDirectories() throws IOException {
            FileUtils.deleteDirectory(tmpDir);
            assertFalse(tmpDir.exists());

            BootUtils.mkDirs(tmpDir);
            assertTrue(tmpDir.exists());
        }

        @Test
        @DisabledOnOs(OS.WINDOWS)
        void mkDirsThrowsIOException() {
            var tmpFile = new File("/foo/bar.txt");
            tmpFile.deleteOnExit();

            var exception = assertThrows(IOException.class, () -> BootUtils.mkDirs(tmpFile));
            assertEquals("Unable to create: " + tmpFile.getAbsolutePath(), exception.getMessage());
        }
    }
}