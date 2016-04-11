/*
 * SonarQube Scanner
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.scanner.cli;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  Properties args = new Properties();
  Logs logs = new Logs(System.out, System.err);
  Cli cli = mock(Cli.class);
  Conf conf = new Conf(cli, logs);

  @Before
  public void initConf() {
    when(cli.properties()).thenReturn(args);
  }

  @Test
  public void should_load_global_settings_by_home() throws Exception {
    Path home = Paths.get(getClass().getResource("ConfTest/shouldLoadRunnerSettingsByHome/").toURI());
    args.setProperty("scanner.home", home.toAbsolutePath().toString());

    Properties properties = conf.properties();
    assertThat(properties.get("sonar.prop")).isEqualTo("value");
  }

  @Test
  public void should_not_fail_if_no_home() throws Exception {
    assertThat(conf.properties()).isNotEmpty();
    // worst case, use current path
    assertThat(conf.properties().getProperty("sonar.projectBaseDir")).isEqualTo(Paths.get("").toAbsolutePath().toString());
  }

  @Test
  public void base_dir_can_be_relative() throws URISyntaxException, IOException {
    Path projectHome = Paths.get(getClass().getResource("ConfTest/shouldLoadModuleConfiguration/project").toURI());
    args.setProperty("project.home", projectHome.getParent().toAbsolutePath().toString());
    args.setProperty("sonar.projectBaseDir", "project");

    Properties properties = conf.properties();

    assertThat(properties.getProperty("module1.sonar.projectName")).isEqualTo("Module 1");
    assertThat(properties.getProperty("module2.sonar.projectName")).isEqualTo("Module 2");
    assertThat(properties.getProperty("sonar.projectBaseDir")).isEqualTo(projectHome.toString());
  }

  @Test
  public void should_load_conf_by_direct_path() throws Exception {
    Path settings = Paths.get(getClass().getResource("ConfTest/shouldLoadRunnerSettingsByDirectPath/other-conf.properties").toURI());
    args.setProperty("scanner.settings", settings.toAbsolutePath().toString());

    assertThat(conf.properties().get("sonar.prop")).isEqualTo("otherValue");
  }

  @Test
  public void shouldLoadCompleteConfiguration() throws Exception {
    Path runnerHome = Paths.get(getClass().getResource("ConfTest/shouldLoadCompleteConfiguration/runner").toURI());
    Path projectHome = Paths.get(getClass().getResource("ConfTest/shouldLoadCompleteConfiguration/project").toURI());
    args.setProperty("scanner.home", runnerHome.toAbsolutePath().toString());
    args.setProperty("project.home", projectHome.toAbsolutePath().toString());

    Properties properties = conf.properties();

    assertThat(properties.getProperty("project.prop")).isEqualTo("foo");
    assertThat(properties.getProperty("overridden.prop")).isEqualTo("project scope");
    assertThat(properties.getProperty("global.prop")).isEqualTo("jdbc:mysql:localhost/sonar");
    assertThat(properties.getProperty("sonar.projectBaseDir")).isEqualTo(projectHome.toString());
  }

  @Test
  public void shouldLoadModuleConfiguration() throws Exception {
    Path projectHome = Paths.get(getClass().getResource("ConfTest/shouldLoadModuleConfiguration/project").toURI());
    args.setProperty("project.home", projectHome.toAbsolutePath().toString());

    Properties properties = conf.properties();

    assertThat(properties.getProperty("module1.sonar.projectName")).isEqualTo("Module 1");
    assertThat(properties.getProperty("module2.sonar.projectName")).isEqualTo("Module 2");
    assertThat(properties.getProperty("sonar.projectBaseDir")).isEqualTo(projectHome.toString());
  }

  @Test
  public void shouldSupportDeepModuleConfigurationInRoot() throws Exception {
    Path projectHome = Paths.get(getClass().getResource("ConfTest/shouldSupportDeepModuleConfigurationInRoot/project").toURI());
    args.setProperty("project.home", projectHome.toAbsolutePath().toString());

    Properties properties = conf.properties();

    assertThat(properties.getProperty("1.sonar.projectName")).isEqualTo("Module 1");
    assertThat(properties.getProperty("1.11.sonar.projectName")).isEqualTo("Module 11");
    assertThat(properties.getProperty("1.11.111.sonar.projectName")).isEqualTo("Module 111");
    assertThat(properties.getProperty("1.12.sonar.projectName")).isEqualTo("Module 12");
    assertThat(properties.getProperty("2.sonar.projectName")).isEqualTo("Module 2");

    // SONARUNNER-125
    assertThat(properties.getProperty("11.111.sonar.projectName")).isNull();
    assertThat(properties.getProperty("sonar.projectBaseDir")).isEqualTo(projectHome.toString());
  }

  @Test
  public void shouldLoadModuleConfigurationOverrideBasedir() throws Exception {
    Path projectHome = Paths.get(getClass().getResource("ConfTest/shouldLoadModuleConfigurationOverrideBasedir/project").toURI());
    args.setProperty("project.home", projectHome.toAbsolutePath().toString());

    Properties properties = conf.properties();

    assertThat(properties.getProperty("module1.sonar.projectName")).isEqualTo("Module 1");
    assertThat(properties.getProperty("module2.sonar.projectName")).isEqualTo("Module 2");
    assertThat(properties.getProperty("module3.sonar.projectName")).isEqualTo("Module 3");
    assertThat(properties.getProperty("sonar.projectBaseDir")).isEqualTo(projectHome.toString());
  }

  @Test
  public void shouldCliOverrideSettingFiles() throws Exception {
    Path projectHome = Paths.get(getClass().getResource("ConfTest/shouldLoadModuleConfigurationOverrideBasedir/project").toURI());
    args.setProperty("project.home", projectHome.toAbsolutePath().toString());
    args.setProperty("module1.sonar.projectName", "mod1");
    args.setProperty("module2.sonar.projectName", "mod2");
    args.setProperty("module3.sonar.projectName", "mod3");

    Properties properties = conf.properties();

    assertThat(properties.getProperty("module1.sonar.projectName")).isEqualTo("mod1");
    assertThat(properties.getProperty("module2.sonar.projectName")).isEqualTo("mod2");
    assertThat(properties.getProperty("module3.sonar.projectName")).isEqualTo("mod3");
    assertThat(properties.getProperty("sonar.projectBaseDir")).isEqualTo(projectHome.toString());
  }

  @Test
  public void shouldUseCliToDiscoverModules() throws Exception {
    Path projectHome = Paths.get(getClass().getResource("ConfTest/shouldLoadModuleConfigurationOverrideBasedir/project").toURI());
    args.setProperty("project.home", projectHome.toAbsolutePath().toString());
    args.setProperty("sonar.modules", "module1");
    args.setProperty("module1.sonar.projectBaseDir", "module_3");

    Properties properties = conf.properties();

    assertThat(properties.getProperty("module1.sonar.projectName")).isEqualTo("Module 3");
    assertThat(properties.getProperty("module2.sonar.projectName")).isNull();
    assertThat(properties.getProperty("module3.sonar.projectName")).isNull();
    assertThat(properties.getProperty("sonar.projectBaseDir")).isEqualTo(projectHome.toString());
  }

  @Test
  public void shouldNotUseCurrentDir() throws Exception {
    Path projectHome = Paths.get(getClass().getResource("ConfTest/shouldLoadModuleConfigurationOverrideBasedir/project").toURI());
    args.setProperty("project.home", projectHome.toAbsolutePath().toString());

    Properties properties = conf.properties();

    assertThat(properties.getProperty("module1.sonar.projectName")).isEqualTo("Module 1");
    assertThat(properties.getProperty("module2.sonar.projectName")).isEqualTo("Module 2");
    assertThat(properties.getProperty("module3.sonar.projectName")).isEqualTo("Module 3");
    assertThat(properties.getProperty("sonar.projectBaseDir")).isEqualTo(projectHome.toString());
  }

  @Test
  public void shouldLoadModuleConfigurationWithoutRootConf() throws Exception {
    Path projectHome = Paths.get(getClass().getResource("ConfTest/shouldLoadModuleConfigurationWithoutRootConf/project").toURI());
    args.setProperty("project.home", projectHome.toAbsolutePath().toString());

    args.put("sonar.modules", "module1,module2");
    Properties properties = conf.properties();

    assertThat(properties.getProperty("module1.sonar.projectName")).isEqualTo("Module 1");
    assertThat(properties.getProperty("module2.sonar.projectName")).isEqualTo("Module 2");
    assertThat(properties.getProperty("sonar.projectBaseDir")).isEqualTo(projectHome.toString());
  }

  @Test
  public void shouldSupportSettingBaseDirFromCli() throws Exception {
    Path projectHome = Paths.get(getClass().getResource("ConfTest/shouldLoadModuleConfiguration/project").toURI());
    args.setProperty("project.home", temp.newFolder().getCanonicalPath());
    args.setProperty("sonar.projectBaseDir", projectHome.toAbsolutePath().toString());

    Properties properties = conf.properties();

    assertThat(properties.getProperty("module1.sonar.projectName")).isEqualTo("Module 1");
    assertThat(properties.getProperty("module2.sonar.projectName")).isEqualTo("Module 2");
    assertThat(properties.getProperty("sonar.projectBaseDir")).isEqualTo(projectHome.toString());
  }

  @Test
  public void ignoreEmptyModule() throws Exception {
    Path projectHome = Paths.get(getClass().getResource("ConfTest/emptyModules/project").toURI());
    args.setProperty("project.home", temp.newFolder().getCanonicalPath());
    args.setProperty("sonar.projectBaseDir", projectHome.toAbsolutePath().toString());

    conf.properties();
  }

  @Test
  public void shouldGetList() {
    Properties props = new Properties();

    props.put("prop", "  foo  ,,  bar  , \n\ntoto,tutu");
    assertThat(Conf.getListFromProperty(props, "prop")).containsOnly("foo", "bar", "toto", "tutu");
  }

  @Test
  public void shouldNotResolveSymlinks() throws IOException, URISyntaxException {
    Path root = temp.getRoot().toPath();
    Path realProjectHome = Paths.get(getClass().getResource("ConfTest/shouldLoadModuleConfiguration/project").toURI());
    Path linkProjectHome = root.resolve("link");
    try {
      Files.createSymbolicLink(linkProjectHome, realProjectHome);

      args.setProperty("project.home", linkProjectHome.toAbsolutePath().toString());

      Properties properties = conf.properties();
      assertThat(properties.getProperty("module1.sonar.projectName")).isEqualTo("Module 1");
      assertThat(properties.getProperty("module2.sonar.projectName")).isEqualTo("Module 2");
      assertThat(properties.getProperty("sonar.projectBaseDir")).isEqualTo(linkProjectHome.toString());
      assertThat(properties.getProperty("module1.sonar.projectBaseDir")).isEqualTo(linkProjectHome.resolve("module1").toString());
      assertThat(properties.getProperty("module2.sonar.projectBaseDir")).isEqualTo(linkProjectHome.resolve("module2").toString());
    } finally {
      if (linkProjectHome != null) {
        Files.delete(linkProjectHome);
      }
    }
  }
}
