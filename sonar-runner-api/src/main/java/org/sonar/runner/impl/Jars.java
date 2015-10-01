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

import org.sonar.runner.cache.FileCache;
import org.sonar.runner.cache.FileCacheBuilder;
import org.sonar.runner.cache.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

class Jars {
  private static final String BOOTSTRAP_INDEX_PATH = "/batch_bootstrap/index";
  static final String BATCH_PATH = "/batch/";

  private final FileCache fileCache;
  private final ServerConnection connection;
  private final JarExtractor jarExtractor;
  private final Logger logger;

  Jars(ServerConnection conn, JarExtractor jarExtractor, Logger logger, Properties props) {
    this.logger = logger;
    this.fileCache = new FileCacheBuilder(logger)
      .setUserHome(props.getProperty("sonar.userHome"))
      .build();
    this.connection = conn;
    this.jarExtractor = jarExtractor;
  }

  /**
   * For unit tests
   */
  Jars(FileCache fileCache, ServerConnection conn, JarExtractor jarExtractor, Logger logger) {
    this.logger = logger;
    this.fileCache = fileCache;
    this.connection = conn;
    this.jarExtractor = jarExtractor;
  }

  /**
   * For unit tests
   */
  FileCache getFileCache() {
    return fileCache;
  }

  List<File> download() {
    List<File> files = new ArrayList<File>();
    logger.debug("Extract sonar-runner-batch in temp...");
    files.add(jarExtractor.extractToTemp("sonar-runner-batch"));
    files.addAll(dowloadFiles());
    return files;
  }

  private List<File> dowloadFiles() {
    try {
      List<File> files = new ArrayList<File>();
      logger.debug("Get bootstrap index...");
      String libs = connection.loadString(BOOTSTRAP_INDEX_PATH);
      logger.debug("Get bootstrap completed");
      String[] lines = libs.split("[\r\n]+");
      BatchFileDownloader batchFileDownloader = new BatchFileDownloader(connection);
      for (String line : lines) {
        line = line.trim();
        if (!"".equals(line)) {
          String[] libAndHash = line.split("\\|");
          String filename = libAndHash[0];
          String hash = libAndHash.length > 0 ? libAndHash[1] : "";
          files.add(fileCache.get(filename, hash, batchFileDownloader));
        }
      }
      return files;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to download libraries from server", e);
    }
  }

  static class BatchFileDownloader implements FileCache.Downloader {
    private final ServerConnection connection;

    BatchFileDownloader(ServerConnection conn) {
      this.connection = conn;
    }

    @Override
    public void download(String filename, File toFile) throws IOException {
      connection.download(BATCH_PATH + filename, toFile);
    }
  }
}
