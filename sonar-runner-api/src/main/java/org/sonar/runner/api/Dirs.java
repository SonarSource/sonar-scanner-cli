/*
 * Sonar Runner - API
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
package org.sonar.runner.api;

import org.apache.commons.io.FileUtils;

import java.io.File;

class Dirs {

  void init(Runner runner) {
    boolean onProject = Utils.taskRequiresProject(runner.properties());
    if (onProject) {
      initProjectDirs(runner);
    } else {
      initTaskDirs(runner);
    }
  }

  private void initProjectDirs(Runner runner) {
    String path = runner.property(ScanProperties.PROJECT_BASEDIR, ".");
    File projectDir = new File(path);
    if (!projectDir.isDirectory()) {
      throw new IllegalStateException("Project home must be an existing directory: " + path);
    }
    runner.setProperty(ScanProperties.PROJECT_BASEDIR, projectDir.getAbsolutePath());

    File workDir;
    path = runner.property(RunnerProperties.WORK_DIR, "");
    if ("".equals(path.trim())) {
      workDir = new File(projectDir, ".sonar");

    } else {
      workDir = new File(path);
      if (!workDir.isAbsolute()) {
        workDir = new File(projectDir, path);
      }
    }
    FileUtils.deleteQuietly(workDir);
    runner.setProperty(RunnerProperties.WORK_DIR, workDir.getAbsolutePath());
  }

  /**
   * Non-scan task
   */
  private void initTaskDirs(Runner runner) {
    String path = runner.property(RunnerProperties.WORK_DIR, ".");
    File workDir = new File(path);
    runner.setProperty(RunnerProperties.WORK_DIR, workDir.getAbsolutePath());
  }
}
