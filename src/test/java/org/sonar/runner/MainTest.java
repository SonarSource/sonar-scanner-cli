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

import org.junit.Test;

import java.io.File;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class MainTest {

  @Test
  public void shouldParseEmptyArguments() {
    Properties props = Main.parseArguments(new String[] {});
    assertThat(props.isEmpty(), is(true));
  }

  @Test
  public void shouldParseArguments() {
    Properties props = Main.parseArguments(new String[] { "-D", "foo=bar", "--define", "hello=world", "-Dboolean" });
    assertThat(props.size(), is(3));
    assertThat(props.getProperty("foo"), is("bar"));
    assertThat(props.getProperty("hello"), is("world"));
    assertThat(props.getProperty("boolean"), is("true"));
  }

  @Test
  public void shouldEnableDebugMode() {
    Properties props = Main.parseArguments(new String[] { "-X" });
    assertThat(props.getProperty(Runner.VERBOSE), is("true"));
  }

  @Test
  public void shouldDisableDebugModeByDefault() {
    Properties props = Main.parseArguments(new String[] {});
    assertThat(props.getProperty(Runner.VERBOSE), nullValue());
  }

  @Test
  public void shouldLoadRunnerSettingsByHome() throws Exception {
    File home = new File(getClass().getResource("/org/sonar/runner/MainTest/shouldLoadRunnerSettingsByHome/").toURI());
    Properties args = new Properties();
    args.setProperty("runner.home", home.getCanonicalPath());

    Properties props = Main.loadRunnerProperties(args);

    assertThat(props.getProperty("sonar.host.url"), is("http://moon/sonar"));
  }

  @Test
  public void shouldNotFailIfNoHome() throws Exception {
    Properties args = new Properties();
    Properties props = Main.loadRunnerProperties(args);

    assertThat(props.isEmpty(), is(true));
  }

  @Test
  public void shouldLoadRunnerSettingsByDirectPath() throws Exception {
    File settings = new File(getClass().getResource("/org/sonar/runner/MainTest/shouldLoadRunnerSettingsByDirectPath/other-conf.properties").toURI());
    Properties args = new Properties();
    args.setProperty("runner.settings", settings.getCanonicalPath());
    Properties props = Main.loadRunnerProperties(args);

    assertThat(props.getProperty("sonar.host.url"), is("http://other/sonar"));
  }

  @Test
  public void shouldLoadCompleteConfiguration() throws Exception {
    File runnerHome = new File(getClass().getResource("/org/sonar/runner/MainTest/shouldLoadCompleteConfiguration/runner").toURI());
    File projectHome = new File(getClass().getResource("/org/sonar/runner/MainTest/shouldLoadCompleteConfiguration/project").toURI());
    Properties props = Main.loadProperties(new String[] {
      "-D", "runner.home=" + runnerHome.getCanonicalPath(),
      "-D", "project.home=" + projectHome.getCanonicalPath()
    });

    assertThat(props.getProperty("project.prop"), is("foo"));
    assertThat(props.getProperty("overridden.prop"), is("project scope"));
    assertThat(props.getProperty("global.prop"), is("jdbc:mysql:localhost/sonar"));
  }

  @Test
  public void shouldFormatTime() {
    assertThat(Main.formatTime(1 * 60 * 60 * 1000 + 2 * 60 * 1000 + 3 * 1000 + 400), is("1:02:03.400s"));
    assertThat(Main.formatTime(2 * 60 * 1000 + 3 * 1000 + 400), is("2:03.400s"));
    assertThat(Main.formatTime(3 * 1000 + 400), is("3.400s"));
    assertThat(Main.formatTime(400), is("0.400s"));
  }
}
