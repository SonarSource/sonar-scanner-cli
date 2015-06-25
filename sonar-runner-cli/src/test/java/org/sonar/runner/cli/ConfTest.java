/*
 * SonarQube Runner - CLI - Distribution
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
package org.sonar.runner.cli;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.runner.cli.Cli;
import org.sonar.runner.cli.Conf;
import java.io.File;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  Properties args = new Properties();
  Cli cli = mock(Cli.class);
  Conf conf = new Conf(cli);

  @Before
  public void initConf() {
    when(cli.properties()).thenReturn(args);
  }

  @Test
  public void should_load_global_settings_by_home() throws Exception {
    File home = new File(getClass().getResource("/org/sonar/runner/ConfTest/shouldLoadRunnerSettingsByHome/").toURI());
    args.setProperty("runner.home", home.getCanonicalPath());

    assertThat(conf.properties().get("sonar.prop")).isEqualTo("value");
  }

  @Test
  public void should_not_fail_if_no_home() throws Exception {
    assertThat(conf.properties()).isNotEmpty();
  }

  @Test
  public void should_load_conf_by_direct_path() throws Exception {
    File settings = new File(getClass().getResource("/org/sonar/runner/ConfTest/shouldLoadRunnerSettingsByDirectPath/other-conf.properties").toURI());
    args.setProperty("runner.settings", settings.getCanonicalPath());

    assertThat(conf.properties().get("sonar.prop")).isEqualTo("otherValue");
  }

  @Test
  public void shouldLoadCompleteConfiguration() throws Exception {
    File runnerHome = new File(getClass().getResource("/org/sonar/runner/ConfTest/shouldLoadCompleteConfiguration/runner").toURI());
    File projectHome = new File(getClass().getResource("/org/sonar/runner/ConfTest/shouldLoadCompleteConfiguration/project").toURI());
    args.setProperty("runner.home", runnerHome.getCanonicalPath());
    args.setProperty("project.home", projectHome.getCanonicalPath());

    Properties properties = conf.properties();

    assertThat(properties.getProperty("project.prop")).isEqualTo("foo");
    assertThat(properties.getProperty("overridden.prop")).isEqualTo("project scope");
    assertThat(properties.getProperty("global.prop")).isEqualTo("jdbc:mysql:localhost/sonar");
  }

  @Test
  public void shouldLoadModuleConfiguration() throws Exception {
    File projectHome = new File(getClass().getResource("/org/sonar/runner/ConfTest/shouldLoadModuleConfiguration/project").toURI());
    args.setProperty("project.home", projectHome.getCanonicalPath());

    Properties properties = conf.properties();

    assertThat(properties.getProperty("module1.sonar.projectName")).isEqualTo("Module 1");
    assertThat(properties.getProperty("module2.sonar.projectName")).isEqualTo("Module 2");
  }

  @Test
  public void shouldSupportDeepModuleConfigurationInRoot() throws Exception {
    File projectHome = new File(getClass().getResource("/org/sonar/runner/ConfTest/shouldSupportDeepModuleConfigurationInRoot/project").toURI());
    args.setProperty("project.home", projectHome.getCanonicalPath());

    Properties properties = conf.properties();

    assertThat(properties.getProperty("1.sonar.projectName")).isEqualTo("Module 1");
    assertThat(properties.getProperty("1.11.sonar.projectName")).isEqualTo("Module 11");
    assertThat(properties.getProperty("1.11.111.sonar.projectName")).isEqualTo("Module 111");
    assertThat(properties.getProperty("1.12.sonar.projectName")).isEqualTo("Module 12");
    assertThat(properties.getProperty("2.sonar.projectName")).isEqualTo("Module 2");

    // SONARUNNER-125
    assertThat(properties.getProperty("11.111.sonar.projectName")).isNull();
  }

  @Test
  public void shouldLoadModuleConfigurationOverrideBasedir() throws Exception {
    File projectHome = new File(getClass().getResource("/org/sonar/runner/ConfTest/shouldLoadModuleConfigurationOverrideBasedir/project").toURI());
    args.setProperty("project.home", projectHome.getCanonicalPath());

    Properties properties = conf.properties();

    assertThat(properties.getProperty("module1.sonar.projectName")).isEqualTo("Module 1");
    assertThat(properties.getProperty("module2.sonar.projectName")).isEqualTo("Module 2");
    assertThat(properties.getProperty("module3.sonar.projectName")).isEqualTo("Module 3");
  }

  @Test
  public void shouldSupportSettingBaseDirFromCli() throws Exception {
    File projectHome = new File(getClass().getResource("/org/sonar/runner/ConfTest/shouldLoadModuleConfiguration/project").toURI());
    args.setProperty("project.home", temp.newFolder().getCanonicalPath());
    args.setProperty("sonar.projectBaseDir", projectHome.getCanonicalPath());

    Properties properties = conf.properties();

    assertThat(properties.getProperty("module1.sonar.projectName")).isEqualTo("Module 1");
    assertThat(properties.getProperty("module2.sonar.projectName")).isEqualTo("Module 2");
  }

}
