/*
 * Sonar Runner
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

import org.junit.Test;

import java.io.File;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;

public class MainTest {

  @Test
  public void shouldParseEmptyArguments() {
    Properties props = new Main().parseArguments(new String[] {});
    assertThat(props).isEmpty();
  }

  @Test
  public void shouldParseArguments() {
    Properties props = new Main().parseArguments(new String[] {"-D", "foo=bar", "--define", "hello=world", "-Dboolean"});
    assertThat(props).hasSize(3);
    assertThat(props.getProperty("foo")).isEqualTo("bar");
    assertThat(props.getProperty("hello")).isEqualTo("world");
    assertThat(props.getProperty("boolean")).isEqualTo("true");
  }

  @Test
  public void shouldEnableDebugMode() {
    Properties props = new Main().parseArguments(new String[] {"-X"});
    assertThat(props.getProperty(Runner.PROPERTY_VERBOSE)).isEqualTo("true");
  }

  @Test
  public void shouldDisableDebugModeByDefault() {
    Properties props = new Main().parseArguments(new String[] {});
    assertThat(props.getProperty(Runner.PROPERTY_VERBOSE)).isNull();
  }

  @Test
  public void shouldLoadRunnerSettingsByHome() throws Exception {
    File home = new File(getClass().getResource("/org/sonar/runner/MainTest/shouldLoadRunnerSettingsByHome/").toURI());
    Properties args = new Properties();
    args.setProperty("runner.home", home.getCanonicalPath());

    Properties props = new Main().loadRunnerConfiguration(args);

    assertThat(props.getProperty("sonar.host.url")).isEqualTo("http://moon/sonar");
  }

  @Test
  public void shouldNotFailIfNoHome() throws Exception {
    Properties args = new Properties();
    Properties props = new Main().loadRunnerConfiguration(args);

    assertThat(props).isEmpty();
  }

  @Test
  public void shouldLoadRunnerSettingsByDirectPath() throws Exception {
    File settings = new File(getClass().getResource("/org/sonar/runner/MainTest/shouldLoadRunnerSettingsByDirectPath/other-conf.properties").toURI());
    Properties args = new Properties();
    args.setProperty("runner.settings", settings.getCanonicalPath());
    Properties props = new Main().loadRunnerConfiguration(args);

    assertThat(props.getProperty("sonar.host.url")).isEqualTo("http://other/sonar");
  }

  @Test
  public void shouldLoadCompleteConfiguration() throws Exception {
    File runnerHome = new File(getClass().getResource("/org/sonar/runner/MainTest/shouldLoadCompleteConfiguration/runner").toURI());
    File projectHome = new File(getClass().getResource("/org/sonar/runner/MainTest/shouldLoadCompleteConfiguration/project").toURI());
    Main main = new Main();
    Properties args = main.parseArguments(new String[] {
      "-D", "runner.home=" + runnerHome.getCanonicalPath(),
      "-D", "project.home=" + projectHome.getCanonicalPath()
    });
    main.loadProperties(args);

    assertThat(main.projectProperties.getProperty("project.prop")).isEqualTo("foo");
    assertThat(main.projectProperties.getProperty("overridden.prop")).isEqualTo("project scope");
    assertThat(main.globalProperties.getProperty("global.prop")).isEqualTo("jdbc:mysql:localhost/sonar");
  }

}
