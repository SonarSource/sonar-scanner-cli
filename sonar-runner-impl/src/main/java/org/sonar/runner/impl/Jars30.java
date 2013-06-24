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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class Jars30 {
  private static final String BATCH_PATH = "/batch/";
  private final ServerConnection connection;

  Jars30(ServerConnection conn) {
    this.connection = conn;
  }

  List<File> download(File workDir, JarExtractor jarExtractor) {
    List<File> files = new ArrayList<File>();
    files.add(jarExtractor.extractToTemp("sonar-runner-batch"));
    files.addAll(downloadFiles(workDir));
    return files;
  }

  private List<File> downloadFiles(File workDir) {
    try {
      List<File> files = new ArrayList<File>();
      String libs = connection.downloadString(BATCH_PATH);
      File dir = new File(workDir, "batch");
      dir.mkdirs();
      for (String lib : libs.split(",")) {
        File file = new File(dir, lib);
        connection.download(BATCH_PATH + lib, file);
        files.add(file);
      }
      return files;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to download libraries from server", e);
    }
  }


}
