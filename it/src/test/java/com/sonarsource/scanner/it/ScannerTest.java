/*
 * SonarSource :: IT :: SonarQube Scanner
 * Copyright (C) 2009-2020 SonarSource SA
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
package com.sonarsource.scanner.it;

import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.BuildRunner;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringEscapeUtils;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarqube.ws.Measures.Measure;

import static java.lang.Integer.parseInt;
import static org.assertj.core.api.Assertions.assertThat;

public class ScannerTest extends ScannerTestCase {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @After
  public void cleanup() {
    orchestrator.resetData();
  }

  @Test
  public void basedir_contains_sources() {
    SonarScanner build = newScanner(new File("projects/basedir-with-source"));
    orchestrator.executeBuild(build);

    Map<String, Measure> projectMeasures = getMeasures(
      "java:basedir-with-source", "files", "ncloc");
    assertThat(parseInt(projectMeasures.get("files").getValue())).isEqualTo(1);
    assertThat(parseInt(projectMeasures.get("ncloc").getValue()))
      .isGreaterThan(1);
  }

  /**
   * Replace the maven format groupId:artifactId by a single key
   */
  @Test
  public void should_support_simple_project_keys() {
    SonarScanner build = newScanner(new File("projects/simple-sample"))
      .setProjectKey("SAMPLE");
    orchestrator.executeBuild(build);

    Map<String, Measure> projectMeasures = getMeasures("SAMPLE", "files",
      "ncloc");
    assertThat(parseInt(projectMeasures.get("files").getValue())).isEqualTo(2);
    assertThat(parseInt(projectMeasures.get("ncloc").getValue()))
      .isGreaterThan(1);
  }

  /**
   * SONARPLUGINS-1230
   */
  @Test
  public void should_override_working_dir_with_relative_path() {
    SonarScanner build = newScanner(new File("projects/override-working-dir"))
      .setProperty("sonar.working.directory", ".overridden-relative-sonar");
    orchestrator.executeBuild(build);

    assertThat(new File("projects/override-working-dir/.sonar")).doesNotExist();
    assertThat(
      new File("projects/override-working-dir/.overridden-relative-sonar"))
      .exists().isDirectory();
  }

  /**
   * SONARPLUGINS-1230
   */
  @Test
  public void should_override_working_dir_with_absolute_path() {
    File projectHome = new File("projects/override-working-dir");
    SonarScanner build = newScanner(projectHome)
      .setProperty("sonar.working.directory",
        new File(projectHome, ".overridden-absolute-sonar").getAbsolutePath());
    orchestrator.executeBuild(build);

    assertThat(new File("projects/override-working-dir/.sonar")).doesNotExist();
    assertThat(
      new File("projects/override-working-dir/.overridden-absolute-sonar"))
      .exists().isDirectory();
  }

  /**
   * SONARPLUGINS-1856
   */
  @Test
  public void should_fail_if_source_dir_does_not_exist() {
    SonarScanner build = newScanner(new File("projects/bad-source-dirs"));

    BuildResult result = orchestrator.executeBuildQuietly(build);
    assertThat(result.getStatus()).isNotEqualTo(0);
    // with the following message
    assertThat(result.getLogs())
      .contains("Invalid value of sonar.sources for bad-source-dirs");
  }

  /**
   * SONARPLUGINS-2256
   */
  @Test
  public void should_warn_when_analysis_is_platform_dependent() {
    SonarScanner build = newScanner(new File("projects/simple-sample"))
      // ORCH-243
      .setSourceEncoding("");
    String log = orchestrator.executeBuild(build).getLogs();

    // Note: we can't really check the locale value and the charset because the ones used during the Sonar analysis may not be the ones
    // used to launch the tests. But we can check that the analysis is platform dependent (i.e. "sonar.sourceEncoding" hasn't been set).
    assertThat(log).contains("Default locale:");
    assertThat(log).contains(", source code encoding:");
    assertThat(log).contains("(analysis is platform dependent)");
  }

  /**
   * SONARUNNER-153
   */
  @Test
  public void should_enable_verbose() {
    // this line should appear in all versions (LTS-DEV) in debug only
    String expectedLog = "Available languages:";
    SonarScanner build = newScanner(new File("projects/simple-sample"))
      .setProperty("sonar.verbose", "true");
    String logs = orchestrator.executeBuild(build).getLogs();
    assertThat(logs).contains(expectedLog);
  }

  @Test
  public void should_use_json_environment_props() {
    SonarScanner build = newScanner(
      new File("projects/simple-sample-no-properties"))
      .setEnvironmentVariable("SONARQUBE_SCANNER_PARAMS", "{"
        + "\"sonar.projectKey\" : \"sample\"," +
        "\"sonar.projectName\" : \"Sample, with comma\"," +
        "\"sonar.projectDescription\" : \"This is a sample\"," +
        "\"sonar.projectVersion\" : \"1.2.3\"," +
        "\"sonar.sources\" : \"src\" }");
    orchestrator.executeBuild(build);
  }

  @Test
  public void should_use_environment_prop() {
    SonarScanner build = newScanner(new File("projects/simple-sample"))
      .setEnvironmentVariable("SONAR_HOST_URL", "http://from-env.org");

    BuildRunner runner = new BuildRunner(orchestrator.getConfiguration());
    BuildResult buildResult = runner.runQuietly(null, build);

    assertThat(buildResult.isSuccess()).isFalse();
    assertThat(buildResult.getLogs())
      .contains("SonarQube server [http://from-env.org] can not be reached");
  }

  @Test
  public void should_skip_analysis() {
    SonarScanner build = newScanner(new File("projects/simple-sample"))
      .setProperty("sonar.host.url", "http://foo")
      .setEnvironmentVariable("SONARQUBE_SCANNER_PARAMS",
        "{ \"sonar.scanner.skip\":\"true\" }");

    BuildResult result = orchestrator.executeBuild(build);
    assertThat(result.getLogs()).contains("SonarScanner analysis skipped");
  }

  @Test
  public void should_fail_if_unable_to_connect() {
    SonarScanner build = newScanner(new File("projects/simple-sample"))
      //env property should be overridden
      .setEnvironmentVariable("SONAR_HOST_URL", "http://from-env.org")
      .setProperty("sonar.host.url", "http://foo");

    BuildResult result = orchestrator.executeBuildQuietly(build);
    // expect build failure
    assertThat(result.isSuccess()).isFalse();
    // with the following message
    assertThat(result.getLogs())
      .contains("SonarQube server [http://foo] can not be reached");
  }

  // SONARPLUGINS-3574
  @Test
  public void run_from_external_location() throws IOException {
    File tempDir = temp.newFolder();
    SonarScanner build = newScanner(tempDir)
      .setProperty("sonar.projectBaseDir",
        new File("projects/simple-sample").getAbsolutePath())
      .addArguments("-e");
    orchestrator.executeBuild(build);

    assertThat(getComponent("sample").getDescription())
      .isEqualTo("This is a sample");
    Map<String, Measure> projectMeasures = getMeasures("sample", "files",
      "ncloc", "classes", "violations");
    assertThat(projectMeasures.values().stream()
      .filter(measure -> measure.getValue() != null)
      .collect(Collectors.toList())).hasSize(4);
  }

  @Test
  public void verify_scanner_opts_env_variable_passed_as_jvm_argument() {
    SonarScanner build = newScanner(new File("projects/simple-sample"))
      .setEnvironmentVariable("SONAR_SCANNER_OPTS", "-Xmx1k");
    BuildResult executeBuild = orchestrator.executeBuildQuietly(build);
    assertThat(executeBuild.getLastStatus()).isNotEqualTo(0);
    String logs = executeBuild.getLogs();
    assertThat(logs).contains("Error occurred during initialization of VM");
    // Not the same message with JRE 8 and 11
    assertThat(logs).containsPattern("Too small (initial|maximum) heap");
  }

  // SQSCANNER-24
  @Test
  public void should_override_project_settings_path() {
    File projectHome = new File("projects/override-project-settings-path");
    SonarScanner build = newScanner(projectHome)
      .setProperty("project.settings",
        new File(projectHome, "conf/sq-project.properties").getAbsolutePath());
    orchestrator.executeBuild(build);

    assertThat(getComponent("sample-with-custom-settings-path").getName())
      .isEqualTo("Test with custom settings location");
  }

  // SQSCANNER-61
  @Test
  public void should_override_project_settings_path_using_env_variable() {
    File projectHome = new File("projects/override-project-settings-path");
    SonarScanner build = newScanner(projectHome)
      .setEnvironmentVariable("SONARQUBE_SCANNER_PARAMS", "{"
        + "\"project.settings\" : \"" + StringEscapeUtils.escapeJavaScript(
        new File(projectHome, "conf/sq-project.properties").getAbsolutePath())
        + "\"}");
    orchestrator.executeBuild(build);

    assertThat(getComponent("sample-with-custom-settings-path").getName())
      .isEqualTo("Test with custom settings location");
  }

}
