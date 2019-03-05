/*
 * SonarQube Scanner
 * Copyright (C) 2011-2019 SonarSource SA
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

import java.util.Arrays;
import java.util.List;

public interface ExpandPathProperties {
  String SONAR_SOURCES = "sonar.sources";
  String JACOCO_PATHS = "sonar.jacoco.reportPaths";
  String SCOVERAGE_PATHS = "sonar.scala.coverage.reportPaths";
  String JAVA_BINARIES = "sonar.java.binaries";
  List<String> AllValues = Arrays.asList(SONAR_SOURCES, JACOCO_PATHS, SCOVERAGE_PATHS, JAVA_BINARIES);
}
