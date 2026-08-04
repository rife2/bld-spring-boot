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

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import rife.bld.Project;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class AbstractBootOperationTest {

    @Nested
    @DisplayName("Validation Tests")
    @SuppressWarnings("DataFlowIssue")
    class ValidationTests {

        @ParameterizedTest
        @EmptySource
        void destinationDirectoryWithEmpty(String arg) {
            assertThatThrownBy(() -> new ConcreteBootOperation().destinationDirectory(arg))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void destinationDirectoryWithNull() {
            assertThatThrownBy(() -> new ConcreteBootOperation().destinationDirectory((File) null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().destinationDirectory((Path) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @EmptySource
        void destinationFileNameWithEmpty(String arg) {
            assertThatThrownBy(() -> new ConcreteBootOperation().destinationFileName(arg))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void destinationFileNameWithNull() {
            assertThatThrownBy(() -> new ConcreteBootOperation().destinationFileName(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @EmptySource
        void infLibsWithEmpty(String arg) {
            assertThatThrownBy(() -> new ConcreteBootOperation().infLibs(arg))
                    .as("varargs with empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().infLibs("foo", arg))
                    .as("array has empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().infLibsStrings(List.of("foo", arg)))
                    .as("list has empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().infLibsStrings(List.of()))
                    .as("list is empty").isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @NullSource
        void infLibsWithNull(String arg) {
            assertThatThrownBy(() -> new ConcreteBootOperation().infLibs(arg))
                    .as("varargs with null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().infLibs("foo", arg))
                    .as("array has null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().infLibsStrings(List.of("foo", arg)))
                    .as("list has null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().infLibs((File[]) null))
                    .as("array is null").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().infLibs((Collection<File>) null))
                    .as("collection is null").isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @EmptySource
        void launcherClassWithEmpty(String arg) {
            assertThatThrownBy(() -> new ConcreteBootOperation().launcherClass(arg))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void launcherClassWithNull() {
            assertThatThrownBy(() -> new ConcreteBootOperation().launcherClass(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @EmptySource
        void launcherLibsWithEmpty(String arg) {
            assertThatThrownBy(() -> new ConcreteBootOperation().launcherLibs(arg))
                    .as("varargs with empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().launcherLibs("foo", arg))
                    .as("array has empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().launcherLibsStrings(List.of("foo", arg)))
                    .as("list has empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().launcherLibsStrings(List.of()))
                    .as("list is empty").isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @NullSource
        void launcherLibsWithNull(String arg) {
            assertThatThrownBy(() -> new ConcreteBootOperation().launcherLibs(arg))
                    .as("varargs with null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().launcherLibs("foo", arg))
                    .as("array has null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().launcherLibsStrings(List.of("foo", arg)))
                    .as("list has null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().launcherLibs((File[]) null))
                    .as("array is null").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().launcherLibs((Collection<File>) null))
                    .as("collection is null").isInstanceOf(NullPointerException.class);
        }

        @Test
        void manifestAttributeWithNullOrEmpty() {
            assertThatThrownBy(() -> new ConcreteBootOperation().manifestAttribute(null, "val"))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().manifestAttribute("", "val"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().manifestAttribute("name", null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().manifestAttribute("name", ""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void manifestAttributesWithNullOrEmpty() {
            assertThatThrownBy(() -> new ConcreteBootOperation().manifestAttributes(null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().manifestAttributes(Map.of()))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().manifestAttributes(Map.of("", "val")))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().manifestAttributes(Map.of("name", "")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @EmptySource
        void sourceDirectoriesWithEmpty(String arg) {
            assertThatThrownBy(() -> new ConcreteBootOperation().sourceDirectories(arg))
                    .as("varargs with empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().sourceDirectories("foo", arg))
                    .as("array has empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().sourceDirectoriesStrings(List.of("foo", arg)))
                    .as("list has empty element").isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().sourceDirectoriesStrings(List.of()))
                    .as("list is empty").isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @NullSource
        void sourceDirectoriesWithNull(String arg) {
            assertThatThrownBy(() -> new ConcreteBootOperation().sourceDirectories(arg))
                    .as("varargs with null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().sourceDirectories("foo", arg))
                    .as("array has null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().sourceDirectoriesStrings(List.of("foo", arg)))
                    .as("list has null element").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().sourceDirectories((File[]) null))
                    .as("array is null").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ConcreteBootOperation().sourceDirectories((Collection<File>) null))
                    .as("collection is null").isInstanceOf(NullPointerException.class);
        }

        static class ConcreteBootOperation extends AbstractBootOperation<ConcreteBootOperation> {

            @Override
            public void execute() {
                // no=op
            }

            @Override
            public ConcreteBootOperation self() {
                return this;
            }

            @Override
            public ConcreteBootOperation fromProject(@NonNull Project project) {
                return this;
            }
        }
    }
}
