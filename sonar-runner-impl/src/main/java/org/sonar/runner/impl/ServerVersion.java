/*
 * SonarQube Runner - Implementation
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
package org.sonar.runner.impl;

import java.io.IOException;

class ServerVersion {

  private static final String[] LESS_THAN_3_0 = {"0", "1", "2"};
  private static final String[] LESS_THAN_3_5 = {"0", "1", "2", "3.0", "3.1", "3.2", "3.3", "3.4"};

  private final ServerConnection serverConnection;
  private String version;

  ServerVersion(ServerConnection serverConnection) {
    this.serverConnection = serverConnection;
  }

  String version() {
    // Guava Suppliers#memoize() would be great here :D
    if (version == null) {
      version = downloadVersion();
      Logs.info("SonarQube Server " + version);
    }
    return version;
  }

  private String downloadVersion() {
    String result;
    try {
      result = serverConnection.downloadString("/api/server/version");
    } catch (IOException e) {
      throw new IllegalStateException("Fail to request server version", e);
    }
    if (result == null || "".equals(result.trim())) {
      throw new IllegalStateException("Server version is not set");
    }
    return result;
  }

  boolean is30Compatible() {
    return !inVersions(version(), LESS_THAN_3_0);
  }

  boolean is35Compatible() {
    return !inVersions(version(), LESS_THAN_3_5);
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
