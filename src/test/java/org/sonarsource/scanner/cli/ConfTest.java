/*
 * SonarQube Scanner
 * Copyright (C) 2011-2020 SonarSource SA
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
import org.apache.commons.lang.SystemUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonarsource.scanner.api.internal.shaded.minimaljson.Json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private Map<String, String> env = new HashMap<>();
  private Properties args = new Properties();
  private Logs logs = new Logs(System.out, System.err);
  private Cli cli = mock(Cli.class);
  private Conf conf = new Conf(cli, logs, env);

  @Before
  public void initConf() {
    env.clear();
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
  public void should_not_fail_if_no_home() {
    assertThat(conf.properties()).isNotEmpty();
    // worst case, use current path
    assertThat(conf.properties().getProperty("sonar.projectBaseDir")).isEqualTo(Paths.get("").toAbsolutePath().toString());
  }

  @Test
  public void base_dir_can_be_relative() throws URISyntaxException {
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
  public void shouldLoadEnvironmentProperties() {
    env.put("SONARQUBE_SCANNER_PARAMS", "{\"sonar.key1\" : \"v1\", \"sonar.key2\" : \"v2\"}");
    args.put("sonar.key2", "v3");

    Properties props = conf.properties();

    assertThat(props.getProperty("sonar.key1")).isEqualTo("v1");
    assertThat(props.getProperty("sonar.key2")).isEqualTo("v3");
  }

  @Test
  public void shouldFailWithInvalidEnvironmentProperties() {
    env.put("SONARQUBE_SCANNER_PARAMS", "{sonar.key1: \"v1\", \"sonar.key2\" : \"v2\"}");
    exception.expect(IllegalStateException.class);
    exception.expectMessage("JSON");
    conf.properties();
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
  public void failModuleBaseDirDoesNotExist() {
    args.setProperty("sonar.modules", "module1");
    args.setProperty("module1.sonar.projectBaseDir", "invalid");

    exception.expect(IllegalStateException.class);
    exception.expectMessage("The base directory of the module 'module1' does not exist");
    conf.properties();
  }

  @Test
  public void failModulePropertyFileDoesNotExist() {
    args.setProperty("sonar.modules", "module1");
    args.setProperty("module1.sonar.projectConfigFile", "invalid");

    exception.expect(IllegalStateException.class);
    exception.expectMessage("The properties file of the module 'module1' does not exist");
    conf.properties();
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
    assumeTrue(SystemUtils.IS_OS_UNIX);
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
      Files.delete(linkProjectHome);
    }
  }

  // SQSCANNER-24
  @Test
  public void should_load_project_settings_using_property() throws Exception {
    Path home = Paths.get(getClass().getResource("ConfTest/shouldOverrideProjectSettingsPath/").toURI());
    args.setProperty("project.home", home.toAbsolutePath().toString());

    Properties properties = conf.properties();
    assertThat(properties.get("sonar.prop")).isEqualTo("default");

    args.setProperty("project.settings", home.resolve("conf/sq-project.properties").toAbsolutePath().toString());

    properties = conf.properties();
    assertThat(properties.get("sonar.prop")).isEqualTo("expected");
  }

  // SQSCANNER-61
  @Test
  public void should_load_project_settings_using_env() throws Exception {
    Path home = Paths.get(getClass().getResource("ConfTest/shouldOverrideProjectSettingsPath/").toURI());
    args.setProperty("project.home", home.toAbsolutePath().toString());

    Properties properties = conf.properties();
    assertThat(properties.get("sonar.prop")).isEqualTo("default");

    String jsonString = Json.object()
      .add("project.settings", home.resolve("conf/sq-project.properties").toAbsolutePath().toString())
      .toString();

    env.put("SONARQUBE_SCANNER_PARAMS", jsonString);

    properties = conf.properties();
    assertThat(properties.get("sonar.prop")).isEqualTo("expected");
  }

  // SQSCANNER-57
  @Test
  public void should_return_true_is_sonar_cloud() {

    args.setProperty("sonar.host.url", "https://sonarcloud.io");

    conf.properties();

    assertThat(conf.isSonarCloud(null)).isTrue();
  }

  // SQSCANNER-57
  @Test
  public void should_return_false_is_sonar_cloud() {
    args.setProperty("sonar.host.url", "https://mysonarqube.com:9000/");

    //Still returns false, sonarcloud not detected in the content of the url
    Properties properties = conf.properties();

    assertThat(properties.getProperty("sonar.host.url")).isEqualTo("https://mysonarqube.com:9000/");

    assertThat(conf.isSonarCloud(null)).isFalse();
  }

  // SQSCANNER-57
  @Test
  public void should_return_false_is_sonar_cloud_host_is_null() {

    Properties emptyProperties = new Properties();

    assertThat(emptyProperties.getProperty("sonar.host.url")).isNull();

    assertThat(conf.isSonarCloud(emptyProperties)).isFalse();
  }
}
