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

import rife.bld.Project;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.regex.Pattern;

/**
 * Collection of utility-type methods used by {@link AbstractBootOperation Spring Boot operations}.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public final class BootUtils {
    private static final String[] FILE_SIZE_UNITS = {"B", "KB", "MB", "GB", "TB"};
    private static final Pattern LOADER_JAR = Pattern.compile("spring-boot-loader-(\\d+).(\\d+).(\\d+).jar");

    private BootUtils() {
        // no-op
    }

    /**
     * Calculates the given file size in bytes, kilobytes, megabytes, gigabytes, or terabytes.
     *
     * @param file the file
     * @return the file size in B, KB, MB, GB, or TB.
     */
    public static String fileSize(File file) {
        var size = file.length();
        if (size <= 0) {
            return "0 B";
        }
        var digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups))
                + ' ' + FILE_SIZE_UNITS[digitGroups];
    }

    /**
     * Return the fully qualified name of the launcher class.
     *
     * @param project the project
     * @param name    the class name
     * @return the fully qualified class name
     */
    public static String launcherClass(Project project, String name) {
        for (var jar : project.standaloneClasspathJars()) {
            var matcher = LOADER_JAR.matcher(jar.getName());
            if (matcher.find()) {
                var major = Integer.parseInt(matcher.group(1));
                var minor = Integer.parseInt(matcher.group(2));
                if (major == 3 && minor >= 2 || major > 3) {
                    return "org.springframework.boot.loader.launch." + name;
                }
            }
        }

        return "org.springframework.boot.loader." + name;
    }

    /**
     * Makes a directory for the given path, including any necessary but nonexistent parent directories.
     *
     * @param path the directory path
     * @throws IOException if an error occurs
     */
    public static void mkDirs(File path) throws IOException {
        if (!path.exists() && !path.mkdirs()) {
            throw new IOException("Unable to create: " + path.getAbsolutePath());
        }
    }
}
