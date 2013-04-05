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

import org.apache.commons.io.FilenameUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class DirsTest {

  Runner runner = new SimpleRunner();
  Dirs dirs = new Dirs();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void should_init_default_task_work_dir() throws Exception {
    runner.setProperty("sonar.task", "views");
    dirs.init(runner);

    File workDir = new File(runner.property("sonar.working.directory", null));
    assertThat(workDir).isNotNull().isDirectory();
    assertThat(workDir.getCanonicalPath()).isEqualTo(new File(".").getCanonicalPath());
  }

  @Test
  public void should_use_parameterized_task_work_dir() throws Exception {
    runner.setProperty("sonar.task", "views");
    runner.setProperty("sonar.working.directory", "generated/reports");
    dirs.init(runner);

    File workDir = new File(runner.property("sonar.working.directory", null));
    assertThat(workDir).isNotNull();
    assertThat(FilenameUtils.separatorsToUnix(workDir.getCanonicalPath())).contains("generated/reports");
  }

  @Test
  public void should_init_default_project_dirs() throws Exception {
    runner.setProperty("sonar.task", "scan");
    dirs.init(runner);


    File projectDir = new File(runner.property("sonar.projectBaseDir", null));
    File workDir = new File(runner.property("sonar.working.directory", null));

    assertThat(projectDir).isNotNull().isDirectory();
    assertThat(workDir).isNotNull();

    assertThat(projectDir.getCanonicalPath()).isEqualTo(new File(".").getCanonicalPath());
    assertThat(workDir.getName()).isEqualTo(".sonar");
    assertThat(workDir.getParentFile()).isEqualTo(projectDir);
  }

  @Test
  public void should_set_relative_path_to_project_work_dir() throws Exception {
    File initialProjectDir = temp.newFolder();
    runner.setProperty("sonar.task", "scan");
    runner.setProperty("sonar.working.directory", "relative/path");
    runner.setProperty("sonar.projectBaseDir", initialProjectDir.getAbsolutePath());
    dirs.init(runner);


    File projectDir = new File(runner.property("sonar.projectBaseDir", null));
    File workDir = new File(runner.property("sonar.working.directory", null));

    assertThat(projectDir).isNotNull().isDirectory();
    assertThat(projectDir.getCanonicalPath()).isEqualTo(initialProjectDir.getCanonicalPath());

    assertThat(workDir).isNotNull();
    assertThat(workDir.getCanonicalPath()).isEqualTo(new File(projectDir, "relative/path").getCanonicalPath());
  }
}
