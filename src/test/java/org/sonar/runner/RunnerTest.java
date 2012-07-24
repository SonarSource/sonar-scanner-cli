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
import org.sonar.batch.bootstrapper.BootstrapException;
import org.sonar.batch.bootstrapper.Bootstrapper;

import java.io.File;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RunnerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void shouldThrowExceptionIfMandatoryPropertyNotSpecified() {
    try {
      Runner.create(new Properties()).checkMandatoryProperties();
      fail("Exception expected");
    } catch (RunnerException e) {
      assertThat(e.getMessage(), is("You must define mandatory properties: sonar.projectKey, sonar.projectName, sonar.projectVersion, sources"));
    }
  }

  @Test
  public void shouldNotThrowExceptionIfAllMandatoryPropertiesSpecified() {
    Properties properties = new Properties();
    properties.setProperty("sonar.projectKey", "foo");
    properties.setProperty("sonar.projectName", "bar");
    properties.setProperty("sonar.projectVersion", "1.0");
    properties.setProperty("sources", "src");
    Runner.create(properties).checkMandatoryProperties();
  }

  @Test
  public void shouldCheckVersion() {
    assertThat(Runner.isUnsupportedVersion("1.0"), is(true));
    assertThat(Runner.isUnsupportedVersion("2.0"), is(true));
    assertThat(Runner.isUnsupportedVersion("2.1"), is(true));
    assertThat(Runner.isUnsupportedVersion("2.2"), is(true));
    assertThat(Runner.isUnsupportedVersion("2.3"), is(true));
    assertThat(Runner.isUnsupportedVersion("2.4"), is(true));
    assertThat(Runner.isUnsupportedVersion("2.4.1"), is(true));
    assertThat(Runner.isUnsupportedVersion("2.5"), is(true));
    assertThat(Runner.isUnsupportedVersion("2.6"), is(false));
  }

  /**
   * Simon: This test can only be executed by Maven, not by IDE
   * Godin: This test can be executed by Eclipse
   */
  @Test
  public void shouldGetVersion() {
    String version = Runner.create(new Properties()).getRunnerVersion();
    assertThat(version.length(), greaterThanOrEqualTo(3));
    assertThat(version, containsString("."));

    // test that version is set by Maven build
    assertThat(version, not(containsString("$")));
  }

  @Test
  public void shouldGetServerUrl() {
    Properties properties = new Properties();
    Runner runner = Runner.create(properties);
    assertThat("Default value", runner.getServerURL(), is("http://localhost:9000"));
    properties.setProperty("sonar.host.url", "foo");
    assertThat(runner.getServerURL(), is("foo"));
  }

  @Test
  public void shouldDetermineVerboseMode() {
    Properties properties = new Properties();
    Runner runner = Runner.create(properties);
    assertThat("Default value", runner.isDebug(), is(false));
    properties.setProperty(Runner.PROPERTY_VERBOSE, "true");
    assertThat(runner.isDebug(), is(true));
  }

  @Test
  public void shouldSupportDeprecatedDebugProperty() {
    Properties properties = new Properties();
    Runner runner = Runner.create(properties);
    properties.setProperty(Runner.DEBUG_MODE, "true");
    assertThat(runner.isDebug(), is(true));
  }

  @Test
  public void shouldInitDirs() throws Exception {
    Properties props = new Properties();
    File home = new File(getClass().getResource("/org/sonar/runner/RunnerTest/shouldInitDirs/").toURI());
    props.setProperty("project.home", home.getCanonicalPath());
    Runner runner = Runner.create(props);
    assertThat(runner.getProperties().get("project.home")).isEqualTo(home.getCanonicalPath());

    assertThat(runner.getProjectDir(), is(home));
    assertThat(runner.getWorkDir(), is(new File(home, ".sonar")));
  }

  @Test
  public void shouldFailInitDirsIfNotExist() throws Exception {
    Properties props = new Properties();

    props.setProperty("project.home", new File("target/foo/").getCanonicalPath());
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Project home must be an existing directory: ");
    Runner.create(props);
  }

  @Test
  public void shouldInitProjectDirWithCurrentDir() throws Exception {
    Runner runner = Runner.create(new Properties());

    assertThat(runner.getProjectDir().isDirectory(), is(true));
    assertThat(runner.getProjectDir().exists(), is(true));
  }

  @Test
  public void shouldSpecifyWorkingDirectory() {
    Properties properties = new Properties();
    Runner runner = Runner.create(properties);
    assertThat(runner.getWorkDir()).isEqualTo(new File(".", ".sonar"));

    properties.setProperty(Runner.PROPERTY_WORK_DIRECTORY, "temp-dir");
    runner = Runner.create(properties);
    assertThat(runner.getWorkDir()).isEqualTo(new File(".", "temp-dir"));
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
