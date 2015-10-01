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
package org.sonar.runner.api;

import org.sonar.runner.cache.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;


class Dirs {

  private final Logger logger;

  Dirs(Logger logger) {
    this.logger = logger;
  }

  void init(Properties p) {
    boolean onProject = Utils.taskRequiresProject(p);
    if (onProject) {
      initProjectDirs(p);
    } else {
      initTaskDirs(p);
    }
  }

  private void initProjectDirs(Properties p) {
    String pathString = p.getProperty(ScanProperties.PROJECT_BASEDIR, "");
    Path absoluteProjectPath = Paths.get(pathString).toAbsolutePath().normalize();
    if (!Files.isDirectory(absoluteProjectPath)) {
      throw new IllegalStateException("Project home must be an existing directory: " + pathString);
    }
    p.setProperty(ScanProperties.PROJECT_BASEDIR, absoluteProjectPath.toString());

    Path workDirPath;
    pathString = p.getProperty(RunnerProperties.WORK_DIR, "");
    if ("".equals(pathString.trim())) {
      workDirPath = absoluteProjectPath.resolve(".sonar");
    } else {
      workDirPath = Paths.get(pathString);
      if (!workDirPath.isAbsolute()) {
        workDirPath = absoluteProjectPath.resolve(pathString);
      }
    }
    p.setProperty(RunnerProperties.WORK_DIR, workDirPath.normalize().toString());
    logger.debug("Work directory: " + workDirPath.normalize().toString());
  }

  /**
   * Non-scan task
   */
  private static void initTaskDirs(Properties p) {
    String path = p.getProperty(RunnerProperties.WORK_DIR, ".");
    File workDir = new File(path);
    p.setProperty(RunnerProperties.WORK_DIR, workDir.getAbsolutePath());
  }
}
