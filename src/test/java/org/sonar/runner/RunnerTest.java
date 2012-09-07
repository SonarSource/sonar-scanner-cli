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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.runner.bootstrapper.BootstrapException;
import org.sonar.runner.bootstrapper.Bootstrapper;

import java.io.File;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RunnerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void shouldHaveDefaultEnvironmentInformationValues() {
    Runner runner = Runner.create(new Properties());
    assertThat(runner.getProperties().getProperty(Runner.PROPERTY_ENVIRONMENT_INFORMATION_KEY)).isEqualTo("Runner");
    assertThat(runner.getProperties().getProperty(Runner.PROPERTY_ENVIRONMENT_INFORMATION_VERSION)).contains(".");
    assertThat(runner.getProperties().getProperty(Runner.PROPERTY_ENVIRONMENT_INFORMATION_VERSION)).doesNotContain("$");
  }

  @Test
  public void shouldOverwriteDefaultEnvironmentInformationValues() {
    Runner runner = Runner.create(new Properties());
    runner.setEnvironmentInformation("Ant", "1.2.3");
    assertThat(runner.getProperties().getProperty(Runner.PROPERTY_ENVIRONMENT_INFORMATION_KEY)).isEqualTo("Ant");
    assertThat(runner.getProperties().getProperty(Runner.PROPERTY_ENVIRONMENT_INFORMATION_VERSION)).isEqualTo("1.2.3");
  }

  @Test
  public void shouldCheckVersion() {
    assertThat(Runner.isUnsupportedVersion("1.0")).isTrue();
    assertThat(Runner.isUnsupportedVersion("2.0")).isTrue();
    assertThat(Runner.isUnsupportedVersion("2.1")).isTrue();
    assertThat(Runner.isUnsupportedVersion("2.2")).isTrue();
    assertThat(Runner.isUnsupportedVersion("2.3")).isTrue();
    assertThat(Runner.isUnsupportedVersion("2.4")).isTrue();
    assertThat(Runner.isUnsupportedVersion("2.4.1")).isTrue();
    assertThat(Runner.isUnsupportedVersion("2.5")).isTrue();
    assertThat(Runner.isUnsupportedVersion("2.11")).isFalse();
    assertThat(Runner.isUnsupportedVersion("3.0")).isFalse();
  }

  @Test
  public void shouldGetServerUrl() {
    Properties properties = new Properties();
    Runner runner = Runner.create(properties);
    assertThat(runner.getSonarServerURL()).isEqualTo("http://localhost:9000");
    properties.setProperty("sonar.host.url", "foo");
    assertThat(runner.getSonarServerURL()).isEqualTo("foo");
  }

  @Test
  public void shouldInitDirs() throws Exception {
    Properties props = new Properties();
    File home = new File(getClass().getResource("/org/sonar/runner/RunnerTest/shouldInitDirs/").toURI());
    props.setProperty("project.home", home.getCanonicalPath());
    Runner runner = Runner.create(props);
    assertThat(runner.getProperties().get("project.home")).isEqualTo(home.getCanonicalPath());

    assertThat(runner.getProjectDir()).isEqualTo(home);
    assertThat(runner.getWorkDir()).isEqualTo(new File(home, ".sonar"));
  }

  @Test
  public void shouldInitProjectDirWithCurrentDir() throws Exception {
    Runner runner = Runner.create(new Properties());

    assertThat(runner.getProjectDir().isDirectory()).isTrue();
    assertThat(runner.getProjectDir().exists()).isTrue();
  }

  @Test
  public void shouldSpecifyWorkingDirectory() {
    Properties properties = new Properties();
    Runner runner = Runner.create(properties);
    assertThat(runner.getWorkDir()).isEqualTo(new File(".", ".sonar"));

    // empty string
    properties.setProperty(Runner.PROPERTY_WORK_DIRECTORY, "    ");
    runner = Runner.create(properties);
    assertThat(runner.getWorkDir()).isEqualTo(new File(".", ".sonar"));

    // real relative path
    properties.setProperty(Runner.PROPERTY_WORK_DIRECTORY, "temp-dir");
    runner = Runner.create(properties);
    assertThat(runner.getWorkDir()).isEqualTo(new File(".", "temp-dir"));

    // real asbolute path
    properties.setProperty(Runner.PROPERTY_WORK_DIRECTORY, new File("target").getAbsolutePath());
    runner = Runner.create(properties);
    assertThat(runner.getWorkDir()).isEqualTo(new File("target").getAbsoluteFile());
  }

  @Test
  public void shouldCheckSonarVersion() {
    Properties properties = new Properties();
    Runner runner = Runner.create(properties);
    Bootstrapper bootstrapper = mock(Bootstrapper.class);

    // nothing happens, OK
    when(bootstrapper.getServerVersion()).thenReturn("3.1");
    runner.checkSonarVersion(bootstrapper);

    // but fails with older versions
    when(bootstrapper.getServerVersion()).thenReturn("2.1");
    thrown.expect(BootstrapException.class);
    thrown.expectMessage("Sonar 2.1 does not support Standalone Runner. Please upgrade Sonar to version 2.6 or more.");
    runner.checkSonarVersion(bootstrapper);
  }

}
