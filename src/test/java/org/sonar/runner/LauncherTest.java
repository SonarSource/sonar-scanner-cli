/*
 * Sonar Standalone Runner
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
package org.sonar.runner;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Test;
import org.sonar.api.batch.bootstrap.ProjectDefinition;

import java.io.File;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

public class LauncherTest {

  @Test
  public void shouldFilterFiles() throws Exception {
    File baseDir = new File(getClass().getResource("/org/sonar/runner/LauncherTest/shouldFilterFiles/").toURI());
    assertThat(Launcher.getLibraries(baseDir, "in*.txt").length).isEqualTo(1);
    assertThat(Launcher.getLibraries(baseDir, "*.txt").length).isEqualTo(2);
    assertThat(Launcher.getLibraries(baseDir.getParentFile(), "shouldFilterFiles/in*.txt").length).isEqualTo(1);
    assertThat(Launcher.getLibraries(baseDir.getParentFile(), "shouldFilterFiles/*.txt").length).isEqualTo(2);
  }

  @Test
  public void shouldWorkWithAbsolutePath() throws Exception {
    File baseDir = new File("not-exists");
    String absolutePattern = new File(getClass().getResource("/org/sonar/runner/LauncherTest/shouldFilterFiles/").toURI()).getAbsolutePath() + "/in*.txt";
    assertThat(Launcher.getLibraries(baseDir.getParentFile(), absolutePattern).length).isEqualTo(1);
  }

  @Test
  public void shouldThrowExceptionWhenNoFilesMatchingPattern() throws Exception {
    File baseDir = new File(getClass().getResource("/org/sonar/runner/LauncherTest/shouldFilterFiles/").toURI());
    try {
      Launcher.getLibraries(baseDir, "*.jar");
      fail("Exception expected");
    } catch (RunnerException e) {
      assertThat(e.getMessage()).contains("No files matching pattern \"*.jar\" in directory");
    }
  }

  @Test
  public void shouldDefineProject() {
    Properties conf = new Properties();
    conf.setProperty("sources", "src/main/java");
    conf.setProperty("tests", "src/test/java");
    conf.setProperty("binaries", "target/classes");
    conf.setProperty("libraries", "./*.xml");
    Runner runner = Runner.create(conf);

    Launcher launcher = new Launcher(runner);
    ProjectDefinition projectDefinition = launcher.defineProject();
    assertThat(projectDefinition.getSourceDirs()).contains("src/main/java");
    assertThat(projectDefinition.getTestDirs()).contains("src/test/java");
    assertThat(projectDefinition.getBinaries()).contains("target/classes");
    assertThat(projectDefinition.getLibraries()).contains(new File("assembly.xml").getAbsolutePath(), new File("pom.xml").getAbsolutePath());
  }

  @Test
  public void testGetSqlLevel() throws Exception {
    Configuration conf = new BaseConfiguration();

    assertThat(Launcher.getSqlLevel(conf)).isEqualTo("WARN");

    conf.setProperty("sonar.showSql", "true");
    assertThat(Launcher.getSqlLevel(conf)).isEqualTo("DEBUG");

    conf.setProperty("sonar.showSql", "false");
    assertThat(Launcher.getSqlLevel(conf)).isEqualTo("WARN");
  }

  @Test
  public void testGetSqlResultsLevel() throws Exception {
    Configuration conf = new BaseConfiguration();

    assertThat(Launcher.getSqlResultsLevel(conf)).isEqualTo("WARN");

    conf.setProperty("sonar.showSqlResults", "true");
    assertThat(Launcher.getSqlResultsLevel(conf)).isEqualTo("DEBUG");

    conf.setProperty("sonar.showSqlResults", "false");
    assertThat(Launcher.getSqlResultsLevel(conf)).isEqualTo("WARN");
  }

}
