/*
 * SonarScanner CLI
 * Copyright (C) 2011-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfTest {

  private final Map<String, String> env = new HashMap<>();
  private final Properties args = new Properties();
  private final Cli cli = mock(Cli.class);
  private final Conf conf = new Conf(cli, env);

  @BeforeEach
  void initConf() {
    env.clear();
    when(cli.properties()).thenReturn(args);
  }

  @Test
  void should_load_global_settings_by_home() throws Exception {
    Path home = Paths.get(getClass().getResource("ConfTest/shouldLoadRunnerSettingsByHome/").toURI());
    args.setProperty("scanner.home", home.toAbsolutePath().toString());

    Properties properties = conf.properties();
    assertThat(properties).containsEntry("sonar.prop", "value");
  }

  @Test
  void should_not_fail_if_no_home() {
    assertThat(conf.properties()).isNotEmpty();
    // worst case, use current path
    assertThat(conf.properties().getProperty("sonar.projectBaseDir")).isEqualTo(Paths.get("").toAbsolutePath().toString());
  }

  @Test
  void should_set_bootstrap_time_only_once() {
    Properties properties = conf.properties();

    assertThat(properties).containsKey("sonar.scanner.bootstrapStartTime");
    String value = properties.getProperty("sonar.scanner.bootstrapStartTime");

    assertThat(conf.properties())
      .containsEntry("sonar.scanner.bootstrapStartTime", value);
  }

  @Test
  void base_dir_can_be_relative() throws URISyntaxException {
    Path projectHome = Paths.get(getClass().getResource("ConfTest/shouldLoadModuleConfiguration/project").toURI());
    args.setProperty("project.home", projectHome.getParent().toAbsolutePath().toString());
    args.setProperty("sonar.projectBaseDir", "project");

    Properties properties = conf.properties();

    assertThat(properties.getProperty("module1.sonar.projectName")).isEqualTo("Module 1");
    assertThat(properties.getProperty("module2.sonar.projectName")).isEqualTo("Module 2");
    assertThat(properties.getProperty("sonar.projectBaseDir")).isEqualTo(projectHome.toString());
  }

  @Test
  void should_load_conf_by_direct_path() throws Exception {
    Path settings = Paths.get(getClass().getResource("ConfTest/shouldLoadRunnerSettingsByDirectPath/other-conf.properties").toURI());
    args.setProperty("scanner.settings", settings.toAbsolutePath().toString());

    assertThat(conf.properties()).containsEntry("sonar.prop", "otherValue");
  }

  @Test
  void shouldLoadCompleteConfiguration() throws Exception {
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
  void shouldLoadModuleConfiguration() throws Exception {
    Path projectHome = Paths.get(getClass().getResource("ConfTest/shouldLoadModuleConfiguration/project").toURI());
    args.setProperty("project.home", projectHome.toAbsolutePath().toString());

    Properties properties = conf.properties();

    assertThat(properties.getProperty("module1.sonar.projectName")).isEqualTo("Module 1");
    assertThat(properties.getProperty("module2.sonar.projectName")).isEqualTo("Module 2");
    assertThat(properties.getProperty("sonar.projectBaseDir")).isEqualTo(projectHome.toString());
  }

  @Test
  void shouldSupportDeepModuleConfigurationInRoot() throws Exception {
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
  void shouldLoadModuleConfigurationOverrideBasedir() throws Exception {
    Path projectHome = Paths.get(getClass().getResource("ConfTest/shouldLoadModuleConfigurationOverrideBasedir/project").toURI());
    args.setProperty("project.home", projectHome.toAbsolutePath().toString());

    Properties properties = conf.properties();

    assertThat(properties.getProperty("module1.sonar.projectName")).isEqualTo("Module 1");
    assertThat(properties.getProperty("module2.sonar.projectName")).isEqualTo("Module 2");
    assertThat(properties.getProperty("module3.sonar.projectName")).isEqualTo("Module 3");
    assertThat(properties.getProperty("sonar.projectBaseDir")).isEqualTo(projectHome.toString());
  }

  @Test
  void shouldCliOverrideSettingFiles() throws Exception {
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
  void shouldUseCliToDiscoverModules() throws Exception {
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
  void shouldLoadModuleConfigurationWithoutRootConf() throws Exception {
    Path projectHome = Paths.get(getClass().getResource("ConfTest/shouldLoadModuleConfigurationWithoutRootConf/project").toURI());
    args.setProperty("project.home", projectHome.toAbsolutePath().toString());

    args.put("sonar.modules", "module1,module2");
    Properties properties = conf.properties();

    assertThat(properties.getProperty("module1.sonar.projectName")).isEqualTo("Module 1");
    assertThat(properties.getProperty("module2.sonar.projectName")).isEqualTo("Module 2");
    assertThat(properties.getProperty("sonar.projectBaseDir")).isEqualTo(projectHome.toString());
  }

  @Test
  void failModuleBaseDirDoesNotExist() {
    args.setProperty("sonar.modules", "module1");
    args.setProperty("module1.sonar.projectBaseDir", "invalid");

    assertThatIllegalStateException()
      .isThrownBy(conf::properties)
      .withMessageStartingWith("The base directory of the module 'module1' does not exist");
  }

  @Test
  void failModulePropertyFileDoesNotExist() {
    args.setProperty("sonar.modules", "module1");
    args.setProperty("module1.sonar.projectConfigFile", "invalid");

    assertThatIllegalStateException()
      .isThrownBy(conf::properties)
      .withMessageStartingWith("The properties file of the module 'module1' does not exist");
  }

  @Test
  void shouldSupportSettingBaseDirFromCli(@TempDir Path projectHome) throws Exception {
    Path projectBaseDir = Paths.get(getClass().getResource("ConfTest/shouldLoadModuleConfiguration/project").toURI());
    args.setProperty("project.home", projectHome.toString());
    args.setProperty("sonar.projectBaseDir", projectBaseDir.toAbsolutePath().toString());

    Properties properties = conf.properties();

    assertThat(properties.getProperty("module1.sonar.projectName")).isEqualTo("Module 1");
    assertThat(properties.getProperty("module2.sonar.projectName")).isEqualTo("Module 2");
    assertThat(properties.getProperty("sonar.projectBaseDir")).isEqualTo(projectBaseDir.toString());
  }

  @Test
  void ignoreEmptyModule(@TempDir Path projectHome) throws Exception {
    Path projectBaseDir = Paths.get(getClass().getResource("ConfTest/emptyModules/project").toURI());
    args.setProperty("project.home", projectHome.toString());
    args.setProperty("sonar.projectBaseDir", projectBaseDir.toAbsolutePath().toString());

    assertThatCode(conf::properties)
      .doesNotThrowAnyException();
  }

  @Test
  void shouldGetList() {
    Properties props = new Properties();

    props.put("prop", "  foo  ,,  bar  , \n\ntoto,tutu");
    assertThat(Conf.getListFromProperty(props, "prop")).containsOnly("foo", "bar", "toto", "tutu");
  }

  @Test
  void shouldNotResolveSymlinks(@TempDir Path root) throws IOException, URISyntaxException {
    assumeTrue(SystemUtils.IS_OS_UNIX);
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
      Files.delete(linkProjectHome);
    }
  }

  // SQSCANNER-24
  @Test
  void should_load_project_settings_using_property() throws Exception {
    Path home = Paths.get(getClass().getResource("ConfTest/shouldOverrideProjectSettingsPath/").toURI());
    args.setProperty("project.home", home.toAbsolutePath().toString());

    Properties properties = conf.properties();
    assertThat(properties).containsEntry("sonar.prop", "default");

    args.setProperty("project.settings", home.resolve("conf/sq-project.properties").toAbsolutePath().toString());

    properties = conf.properties();
    assertThat(properties).containsEntry("sonar.prop", "expected");
  }

}
