/*
 * SonarScanner CLI
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.scanner.cli;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class GradleJavaModel {

  private GradleJavaModel() {
  }

  static final class GeneratedProperties {
    private final LinkedHashMap<String, String> properties;
    private final List<String> warnings;

    GeneratedProperties(LinkedHashMap<String, String> properties, List<String> warnings) {
      this.properties = new LinkedHashMap<>(properties);
      this.warnings = List.copyOf(warnings);
    }

    Map<String, String> getProperties() {
      return Collections.unmodifiableMap(properties);
    }

    List<String> getWarnings() {
      return warnings;
    }
  }

  static final class Project {
    final Path rootProjectDir;
    final List<Module> modules;

    Project(Path rootProjectDir, List<Module> modules) {
      this.rootProjectDir = rootProjectDir;
      this.modules = List.copyOf(modules);
    }
  }

  static final class Module {
    final String name;
    final String gradleProjectPath;
    final Path projectDir;
    final List<Path> sourceDirs;
    final List<Path> testDirs;
    final Path mainOutputDir;
    final Path testOutputDir;
    final List<Path> mainLibraries;
    final List<Path> testLibraries;
    final List<String> moduleDependencies;
    final String sourceVersion;
    final Path jdkHome;

    Module(String name, String gradleProjectPath, Path projectDir, List<Path> sourceDirs, List<Path> testDirs,
      Path mainOutputDir, Path testOutputDir, List<Path> mainLibraries, List<Path> testLibraries,
      List<String> moduleDependencies, String sourceVersion, Path jdkHome) {
      this.name = name;
      this.gradleProjectPath = gradleProjectPath;
      this.projectDir = projectDir;
      this.sourceDirs = List.copyOf(sourceDirs);
      this.testDirs = List.copyOf(testDirs);
      this.mainOutputDir = mainOutputDir;
      this.testOutputDir = testOutputDir;
      this.mainLibraries = List.copyOf(mainLibraries);
      this.testLibraries = List.copyOf(testLibraries);
      this.moduleDependencies = List.copyOf(moduleDependencies);
      this.sourceVersion = sourceVersion;
      this.jdkHome = jdkHome;
    }

    boolean hasJavaMetadata() {
      return !sourceDirs.isEmpty()
        || !testDirs.isEmpty()
        || mainOutputDir != null
        || testOutputDir != null
        || !mainLibraries.isEmpty()
        || !testLibraries.isEmpty()
        || sourceVersion != null
        || jdkHome != null;
    }
  }
}
