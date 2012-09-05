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

import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;

public class LauncherTest {

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

  @Test
  public void shouldDetermineVerboseMode() {
    Properties properties = new Properties();
    Launcher launcher = new Launcher(properties);
    assertThat(launcher.isDebug()).isFalse();
    properties.setProperty(Runner.PROPERTY_VERBOSE, "true");
    assertThat(launcher.isDebug()).isTrue();
  }

  @Test
  public void shouldSupportDeprecatedDebugProperty() {
    Properties properties = new Properties();
    Launcher launcher = new Launcher(properties);
    properties.setProperty(Runner.PROPERTY_OLD_DEBUG_MODE, "true");
    assertThat(launcher.isDebug()).isTrue();
  }

}
