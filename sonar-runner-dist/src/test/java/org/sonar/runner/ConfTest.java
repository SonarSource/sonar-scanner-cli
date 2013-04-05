/*
 * Sonar Runner - Distribution
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

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfTest {

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

    assertThat(conf.properties().get("sonar.host.url")).isEqualTo("http://moon/sonar");
  }

  @Test
  public void should_not_fail_if_no_home() throws Exception {
    assertThat(conf.properties()).isNotEmpty();
  }

  @Test
  public void should_load_conf_by_direct_path() throws Exception {
    File settings = new File(getClass().getResource("/org/sonar/runner/ConfTest/shouldLoadRunnerSettingsByDirectPath/other-conf.properties").toURI());
    args.setProperty("runner.settings", settings.getCanonicalPath());

    assertThat(conf.properties().get("sonar.host.url")).isEqualTo("http://other/sonar");
  }

//  @Test
//  public void shouldLoadCompleteConfiguration() throws Exception {
//    File runnerHome = new File(getClass().getResource("/org/sonar/runner/MainTest/shouldLoadCompleteConfiguration/runner").toURI());
//    File projectHome = new File(getClass().getResource("/org/sonar/runner/MainTest/shouldLoadCompleteConfiguration/project").toURI());
//    Main main = new Main();
//    Properties args = main.parseArguments(new String[] {
//      "-D", "runner.home=" + runnerHome.getCanonicalPath(),
//      "-D", "project.home=" + projectHome.getCanonicalPath()
//    });
//    main.loadProperties(args);
//
//    assertThat(main.projectProperties.getProperty("project.prop")).isEqualTo("foo");
//    assertThat(main.projectProperties.getProperty("overridden.prop")).isEqualTo("project scope");
//    assertThat(main.globalProperties.getProperty("global.prop")).isEqualTo("jdbc:mysql:localhost/sonar");
//  }

}
