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

import java.io.File;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.home.cache.Logger;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DirsTest {

  Properties p = new Properties();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void should_init_default_task_work_dir() throws Exception {
    p.setProperty("sonar.task", "views");
    new Dirs(mock(Logger.class)).init(p);

    File workDir = new File(p.getProperty(RunnerProperties.WORK_DIR, null));
    assertThat(workDir).isNotNull().isDirectory();
    assertThat(workDir.getCanonicalPath()).isEqualTo(new File(".").getCanonicalPath());
  }

  @Test
  public void should_use_parameterized_task_work_dir() throws Exception {
    p.setProperty("sonar.task", "views");
    p.setProperty(RunnerProperties.WORK_DIR, "generated/reports");
    new Dirs(mock(Logger.class)).init(p);

    File workDir = new File(p.getProperty(RunnerProperties.WORK_DIR, null));
    assertThat(workDir).isNotNull();
    // separators from windows to unix
    assertThat(workDir.getCanonicalPath().replace("\\", "/")).contains("generated/reports");
  }

  @Test
  public void should_init_default_project_dirs() throws Exception {
    p.setProperty("sonar.task", "scan");
    new Dirs(mock(Logger.class)).init(p);

    File projectDir = new File(p.getProperty(ScanProperties.PROJECT_BASEDIR, null));
    File workDir = new File(p.getProperty(RunnerProperties.WORK_DIR, null));

    assertThat(projectDir).isNotNull().isDirectory();
    assertThat(workDir).isNotNull();

    assertThat(projectDir.getCanonicalPath()).isEqualTo(new File(".").getCanonicalPath());
    assertThat(workDir.getName()).isEqualTo(".sonar");
    assertThat(workDir.getParentFile()).isEqualTo(projectDir);
  }

  @Test
  public void should_set_relative_path_to_project_work_dir() throws Exception {
    File initialProjectDir = temp.getRoot();
    p.setProperty("sonar.task", "scan");
    p.setProperty(RunnerProperties.WORK_DIR, "relative/path");
    p.setProperty(ScanProperties.PROJECT_BASEDIR, initialProjectDir.getAbsolutePath());
    new Dirs(mock(Logger.class)).init(p);

    File projectDir = new File(p.getProperty(ScanProperties.PROJECT_BASEDIR, null));
    File workDir = new File(p.getProperty(RunnerProperties.WORK_DIR, null));

    assertThat(projectDir).isNotNull().isDirectory();
    assertThat(projectDir.getCanonicalPath()).isEqualTo(initialProjectDir.getCanonicalPath());

    assertThat(workDir).isNotNull();
    assertThat(workDir.getCanonicalPath()).isEqualTo(new File(projectDir, "relative/path").getCanonicalPath());
  }
}
