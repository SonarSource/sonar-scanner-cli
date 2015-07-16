/*
 * SonarQube Runner - API
 * Copyright (C) 2011 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.runner.impl;

public class VersionUtils {
  private VersionUtils() {
    // only util static methods
  }

  public static boolean isAtLeast52(String version) {
    // it can be snapshot (5.2-SNAPSHOT)
    if (version == null) {
      return false;
    }

    int endIndex = Math.min(3, version.length());
    try {
      return Double.parseDouble(version.substring(0, endIndex)) >= 5.2;
    } catch (NumberFormatException e) {
      return false;
    }
  }

}
