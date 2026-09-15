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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static rife.bld.extension.BootOperationTest.*;

@SuppressWarnings({"DataFlowIssue", "PMD.AvoidDuplicateLiterals"})
class BootJarOperationTest {

    @Nested
    @DisplayName("Errors Tests")
    class ErrorsTests {

        @Test
        void launcherClass() {
            var bootWar = new BootJarOperation()
                    .fromProject(new BootOperationTest.CustomProject(new File(".")))
                    .mainClass(MAIN_CLASS)
                    .launcherClass("org.springframework.boot.loader.launch.WarLauncher")
                    .launcherLibs(List.of(new File(EXAMPLES_LIB_STANDALONE + SPRING_BOOT_LOADER_JAR)));

            assertThatCode(bootWar::verifyExecute).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Libs Tests")
    class LibsTests {

        @Nested
        @DisplayName("Launcher Libs Tests")
        class LauncherLibTests {

            private final File launcher = new File(EXAMPLES_LIB_STANDALONE + SPRING_BOOT_LOADER_JAR);

            @Test
            void launcherLibsAsFileArray() {
                var op = new BootJarOperation().launcherLibs(launcher);
                assertThat(op.launcherLibs()).as("File...").containsExactly(launcher);
            }

            @Test
            void launcherLibsAsFileList() {
                var op = new BootJarOperation().launcherLibs(List.of(launcher));
                assertThat(op.launcherLibs()).as("List(File...)").containsExactly(launcher);
            }

            @Test
            void launcherLibsAsPathArray() {
                var op = new BootJarOperation().launcherLibs(launcher.toPath());
                assertThat(op.launcherLibs()).as("Path...").containsExactly(launcher);
            }

            @Test
            void launcherLibsAsPathList() {
                var op = new BootJarOperation().launcherLibsPaths(List.of(launcher.toPath()));
                assertThat(op.launcherLibs()).as("List(Path...)").containsExactly(launcher);
            }

            @Test
            void launcherLibsAsStringArray() {
                var op = new BootJarOperation().launcherLibs(EXAMPLES_LIB_STANDALONE
                        + SPRING_BOOT_LOADER_JAR);
                assertThat(op.launcherLibs()).as("String...").containsExactly(launcher);
            }

            @Test
            void launcherLibsAsStringList() {
                var op = new BootJarOperation().launcherLibsStrings(List.of(EXAMPLES_LIB_STANDALONE
                        + SPRING_BOOT_LOADER_JAR));
                assertThat(op.launcherLibs()).as("List(String...)").containsExactly(launcher);
            }
        }
    }

    @Nested
    @DisplayName("Source Directories Tests")
    class SourceDirectoriesTests {

        final BootJarOperation op = new BootJarOperation();
        final File src = new File(SRC_MAIN_JAVA);
        final File test = new File(SRC_TEST_JAVA);

        @Test
        void sourceDirectoriesAsFileArray() {
            op.sourceDirectories().clear();
            op.sourceDirectories(src, test);
            assertThat(op.sourceDirectories()).as("File...").containsExactly(src, test);
        }

        @Test
        void sourceDirectoriesAsFileList() {
            op.sourceDirectories().clear();
            var tmp = op.sourceDirectories(List.of(src, test));
            assertThat(tmp).isEqualTo(op);
            assertThat(op.sourceDirectories()).as("List(File...)").containsExactly(src, test);
        }

        @Test
        void sourceDirectoriesAsPathArray() {
            op.sourceDirectories().clear();
            var tmp = op.sourceDirectories(src.toPath(), test.toPath());
            assertThat(tmp).isEqualTo(op);
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

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @ParameterizedTest
        @EmptySource
        void destinationDirectoryWithEmpty(String arg) {
            assertThatThrownBy(() -> new BootJarOperation().destinationDirectory(arg))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void destinationDirectoryWithNull() {
            assertThatThrownBy(() -> new BootJarOperation().destinationDirectory((File) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootJarOperation().destinationDirectory((Path) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @EmptySource
        void destinationFileNameWithEmpty(String arg) {
            assertThatThrownBy(() -> new BootJarOperation().destinationFileName(arg))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void destinationFileNameWithNull() {
            assertThatThrownBy(() -> new BootJarOperation().destinationFileName(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void fromProjectWithNull() {
            assertThatThrownBy(() -> new BootJarOperation().fromProject(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @EmptySource
        void infLibsWithEmpty(String arg) {
            assertThatThrownBy(() -> new BootJarOperation().infLibs(arg))
                    .as("varargs with empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootJarOperation().infLibs("foo", arg))
                    .as("array has empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootJarOperation().infLibsStrings(List.of("foo", arg)))
                    .as("list has empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootJarOperation().infLibsStrings(List.of()))
                    .as("list is empty").isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @NullSource
        void infLibsWithNull(String arg) {
            assertThatThrownBy(() -> new BootJarOperation().infLibs(arg))
                    .as("varargs with null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootJarOperation().infLibs("foo", arg))
                    .as("array has null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootJarOperation().infLibsStrings(List.of("foo", arg)))
                    .as("list has null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootJarOperation().infLibs((File[]) null))
                    .as("array is null").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootJarOperation().infLibs((Collection<File>) null))
                    .as("collection is null").isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @EmptySource
        void launcherClassWithEmpty(String arg) {
            assertThatThrownBy(() -> new BootJarOperation().launcherClass(arg))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void launcherClassWithNull() {
            assertThatThrownBy(() -> new BootJarOperation().launcherClass(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @EmptySource
        void launcherLibsWithEmpty(String arg) {
            assertThatThrownBy(() -> new BootJarOperation().launcherLibs(arg))
                    .as("varargs with empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootJarOperation().launcherLibs("foo", arg))
                    .as("array has empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootJarOperation().launcherLibsStrings(List.of("foo", arg)))
                    .as("list has empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootJarOperation().launcherLibsStrings(List.of()))
                    .as("list is empty").isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @NullSource
        void launcherLibsWithNull(String arg) {
            assertThatThrownBy(() -> new BootJarOperation().launcherLibs(arg))
                    .as("varargs with null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootJarOperation().launcherLibs("foo", arg))
                    .as("array has null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootJarOperation().launcherLibsStrings(List.of("foo", arg)))
                    .as("list has null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootJarOperation().launcherLibs((File[]) null))
                    .as("array is null").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootJarOperation().launcherLibs((Collection<File>) null))
                    .as("collection is null").isInstanceOf(NullPointerException.class);
        }

        @Test
        void manifestAttributeWithNullOrEmpty() {
            assertThatThrownBy(() -> new BootJarOperation().manifestAttribute(null, "val"))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootJarOperation().manifestAttribute("", "val"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootJarOperation().manifestAttribute("name", null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootJarOperation().manifestAttribute("name", ""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void manifestAttributesWithNullOrEmpty() {
            assertThatThrownBy(() -> new BootJarOperation().manifestAttributes(null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootJarOperation().manifestAttributes(Map.of()))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootJarOperation().manifestAttributes(Map.of("", "val")))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootJarOperation().manifestAttributes(Map.of("name", "")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @EmptySource
        void sourceDirectoriesWithEmpty(String arg) {
            assertThatThrownBy(() -> new BootJarOperation().sourceDirectories(arg))
                    .as("varargs with empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootJarOperation().sourceDirectories("foo", arg))
                    .as("array has empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootJarOperation().sourceDirectoriesStrings(List.of("foo", arg)))
                    .as("list has empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootJarOperation().sourceDirectoriesStrings(List.of()))
                    .as("list is empty").isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @NullSource
        void sourceDirectoriesWithNull(String arg) {
            assertThatThrownBy(() -> new BootJarOperation().sourceDirectories(arg))
                    .as("varargs with null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootJarOperation().sourceDirectories("foo", arg))
                    .as("array has null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootWarOperation().sourceDirectoriesStrings(List.of("foo", arg)))
                    .as("list has null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootJarOperation().sourceDirectories((File[]) null))
                    .as("array is null").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootJarOperation().sourceDirectories((Collection<File>) null))
                    .as("collection is null").isInstanceOf(NullPointerException.class);
        }
    }
}
