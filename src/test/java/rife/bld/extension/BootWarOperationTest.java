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
import rife.bld.testing.BlankSource;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static rife.bld.extension.BootOperationTest.*;

public class BootWarOperationTest {

    @Nested
    @DisplayName("Inf Lib Tests")
    class InfLibTest {

        private final File bar = new File(EXAMPLES_LIB_COMPILE + FOO_JAR);
        private final File foo = new File(EXAMPLES_LIB_COMPILE + SPRING_BOOT_JAR);
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
            op.infLibs(EXAMPLES_LIB_COMPILE + SPRING_BOOT_JAR, EXAMPLES_LIB_COMPILE + FOO_JAR);
            assertThat(op.infLibs()).as("String...").containsExactly(foo, bar);
        }

        @Test
        void infLibsAsStringList() {
            op.infLibs().clear();
            op.infLibsStrings(List.of(EXAMPLES_LIB_COMPILE + SPRING_BOOT_JAR, EXAMPLES_LIB_COMPILE + FOO_JAR));
            assertThat(op.infLibs()).as("List(String...)").containsExactly(foo, bar);

        }

        @Nested
        @DisplayName("Errors Tests")
        class ErrorsTests {

            @Test
            void destinationDirectoryMissing() {
                assertThatCode(() ->
                        new BootWarOperation()
                                .mainClass(MAIN_CLASS)
                                .launcherClass("org.springframework.boot.loader.launch.WarLauncher")
                                .launcherLibs(
                                        List.of(new File(EXAMPLES_LIB_STANDALONE + SPRING_BOOT_LOADER_JAR)))
                                .execute()
                )
                        .isInstanceOf(NullPointerException.class)
                        .hasMessage("Destination directory must not be null");
            }

            @Test
            void launcherClassMissing() {
                var bootWar =
                        new BootWarOperation()
                                .mainClass(MAIN_CLASS)
                                .launcherLibs(
                                        List.of(new File(EXAMPLES_LIB_STANDALONE + SPRING_BOOT_LOADER_JAR)));
                assertThatCode(bootWar::execute)
                        .isInstanceOf((NullPointerException.class))
                        .hasMessage("Spring Boot loader launcher class must not be null");
            }

            @Test
            void launcherLibMising() {
                assertThatCode(() ->
                        new BootWarOperation()
                                .mainClass(MAIN_CLASS)
                                .launcherClass("org.springframework.boot.loader.launch.WarLauncher")
                                .execute()
                )
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("Spring Boot loader launcher libraries must not be empty");
            }

            @ParameterizedTest
            @BlankSource
            void mainClassBlank(String value) {
                var bootWar = new BootWarOperation().mainClass(value);
                assertThatCode(bootWar::execute)
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("Project main class must not be blank");
            }

            @Test
            void mainClassMissing() {
                var bootWar = new BootWarOperation();
                assertThatCode(bootWar::execute).isInstanceOf(NullPointerException.class)
                        .hasMessage("Project main class must not be null");
            }

            @Test
            @SuppressWarnings("DataFlowIssue")
            void mainClassNull() {
                var bootWar = new BootWarOperation().mainClass(null);
                assertThatCode(bootWar::execute)
                        .isInstanceOf(NullPointerException.class)
                        .hasMessage("Project main class must not be null");
            }
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    @SuppressWarnings({"DataFlowIssue", "PMD.AvoidDuplicateLiterals"})
    class ValidationTests {

        @ParameterizedTest
        @EmptySource
        void destinationDirectoryWithEmpty(String arg) {
            assertThatThrownBy(() -> new BootWarOperation().destinationDirectory(arg))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void destinationDirectoryWithNull() {
            assertThatThrownBy(() -> new BootWarOperation().destinationDirectory((File) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootWarOperation().destinationDirectory((Path) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @EmptySource
        void destinationFileNameWithEmpty(String arg) {
            assertThatThrownBy(() -> new BootWarOperation().destinationFileName(arg))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void destinationFileNameWithNull() {
            assertThatThrownBy(() -> new BootWarOperation().destinationFileName(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void fromProjectWithNull() {
            assertThatThrownBy(() -> new BootWarOperation().fromProject(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @EmptySource
        void infLibsWithEmpty(String arg) {
            assertThatThrownBy(() -> new BootWarOperation().infLibs(arg))
                    .as("varargs with empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootWarOperation().infLibs("foo", arg))
                    .as("array has empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootWarOperation().infLibsStrings(List.of("foo", arg)))
                    .as("list has empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootWarOperation().infLibsStrings(List.of()))
                    .as("list is empty").isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @NullSource
        void infLibsWithNull(String arg) {
            assertThatThrownBy(() -> new BootWarOperation().infLibs(arg))
                    .as("varargs with null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootWarOperation().infLibs("foo", arg))
                    .as("array has null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootWarOperation().infLibsStrings(List.of("foo", arg)))
                    .as("list has null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootWarOperation().infLibs((File[]) null))
                    .as("array is null").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootWarOperation().infLibs((Collection<File>) null))
                    .as("collection is null").isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @EmptySource
        void launcherClassWithEmpty(String arg) {
            assertThatThrownBy(() -> new BootWarOperation().launcherClass(arg))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void launcherClassWithNull() {
            assertThatThrownBy(() -> new BootWarOperation().launcherClass(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @EmptySource
        void launcherLibsWithEmpty(String arg) {
            assertThatThrownBy(() -> new BootWarOperation().launcherLibs(arg))
                    .as("varargs with empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootWarOperation().launcherLibs("foo", arg))
                    .as("array has empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootWarOperation().launcherLibsStrings(List.of("foo", arg)))
                    .as("list has empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootWarOperation().launcherLibsStrings(List.of()))
                    .as("list is empty").isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @NullSource
        void launcherLibsWithNull(String arg) {
            assertThatThrownBy(() -> new BootWarOperation().launcherLibs(arg))
                    .as("varargs with null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootWarOperation().launcherLibs("foo", arg))
                    .as("array has null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootWarOperation().launcherLibsStrings(List.of("foo", arg)))
                    .as("list has null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootWarOperation().launcherLibs((File[]) null))
                    .as("array is null").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootWarOperation().launcherLibs((Collection<File>) null))
                    .as("collection is null").isInstanceOf(NullPointerException.class);
        }

        @Test
        void manifestAttributeWithNullOrEmpty() {
            assertThatThrownBy(() -> new BootWarOperation().manifestAttribute(null, "val"))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootWarOperation().manifestAttribute("", "val"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootWarOperation().manifestAttribute("name", null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootWarOperation().manifestAttribute("name", ""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void manifestAttributesWithNullOrEmpty() {
            assertThatThrownBy(() -> new BootWarOperation().manifestAttributes(null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootWarOperation().manifestAttributes(Map.of()))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootWarOperation().manifestAttributes(Map.of("", "val")))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootWarOperation().manifestAttributes(Map.of("name", "")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @EmptySource
        void providedLibsWithEmpty(String arg) {
            assertThatThrownBy(() -> new BootWarOperation().providedLibs(arg))
                    .as("varargs with empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootWarOperation().providedLibs("foo", arg))
                    .as("array has empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootWarOperation().providedLibsStrings(List.of("foo", arg)))
                    .as("list has empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootWarOperation().providedLibsStrings(List.of()))
                    .as("list is empty").isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @NullSource
        void providedLibsWithNull(String arg) {
            assertThatThrownBy(() -> new BootWarOperation().providedLibs(arg))
                    .as("varargs with null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootWarOperation().providedLibs("foo", arg))
                    .as("array has null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootWarOperation().providedLibsStrings(List.of("foo", arg)))
                    .as("list has null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootWarOperation().providedLibs((File[]) null))
                    .as("array is null").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootWarOperation().providedLibs((Collection<File>) null))
                    .as("collection is null").isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @EmptySource
        void sourceDirectoriesWithEmpty(String arg) {
            assertThatThrownBy(() -> new BootWarOperation().sourceDirectories(arg))
                    .as("varargs with empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootWarOperation().sourceDirectories("foo", arg))
                    .as("array has empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootWarOperation().sourceDirectoriesStrings(List.of("foo", arg)))
                    .as("list has empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BootWarOperation().sourceDirectoriesStrings(List.of()))
                    .as("list is empty").isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @NullSource
        void sourceDirectoriesWithNull(String arg) {
            assertThatThrownBy(() -> new BootWarOperation().sourceDirectories(arg))
                    .as("varargs with null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootWarOperation().sourceDirectories("foo", arg))
                    .as("array has null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootWarOperation().sourceDirectoriesStrings(List.of("foo", arg)))
                    .as("list has null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootWarOperation().sourceDirectories((File[]) null))
                    .as("array is null").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BootWarOperation().sourceDirectories((Collection<File>) null))
                    .as("collection is null").isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("War Libs Tests")
    class WarLibTest {

        private final File foo = new File(EXAMPLES_LIB_RUNTIME + "foo.jar");
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
            var tmp = op.providedLibs(foo.toPath());
            assertThat(tmp).isEqualTo(op);
            assertThat(op.providedLibs()).containsExactly(foo);
        }

        @Test
        void warProvidedLibsAsPaths() {
            op.providedLibs().clear();
            var tmp = op.providedLibsPaths(List.of(foo.toPath()));
            assertThat(tmp).isEqualTo(op);
            assertThat(op.providedLibs()).containsExactly(foo);
        }

        @Test
        void warProvidedLibsAsString() {
            op.providedLibs().clear();
            var tmp = op.providedLibs(EXAMPLES_LIB_RUNTIME + "foo.jar");
            assertThat(tmp).isEqualTo(op);
            assertThat(op.providedLibs()).containsExactly(foo);
        }

        @Test
        void warProvidedLibsAsStrings() {
            op.providedLibs().clear();
            var tmp = op.providedLibsStrings(List.of(EXAMPLES_LIB_RUNTIME + "foo.jar"));
            assertThat(tmp).isEqualTo(op);
            assertThat(op.providedLibs()).containsExactly(foo);
        }
    }
}
