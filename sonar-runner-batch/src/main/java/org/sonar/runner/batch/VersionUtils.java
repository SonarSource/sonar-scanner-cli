/*
 * SonarQube Runner - Batch
 * Copyright (C) 2011 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.runner.batch;

class VersionUtils {

  private static final String[] LESS_THAN_3_7 = {"0", "1", "2", "3.0", "3.1", "3.2", "3.3", "3.4", "3.5", "3.6"};

  static boolean isLessThan37(String version) {
    return inVersions(version, LESS_THAN_3_7);
  }

  private static boolean inVersions(String version, String[] versions) {
    for (String s : versions) {
      if (isVersion(version, s)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isVersion(String version, String prefix) {
    return version.startsWith(prefix + ".") || version.equals(prefix);
  }
}
