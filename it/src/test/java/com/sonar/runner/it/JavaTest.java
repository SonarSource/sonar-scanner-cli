/*
 * SonarSource :: IT :: SonarQube Scanner
 * Copyright (C) 2009-2016 SonarSource SA
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
package com.sonar.runner.it;

import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.ResourceLocation;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang.SystemUtils;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.fest.assertions.Assertions.assertThat;

public class JavaTest extends ScannerTestCase {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @After
  public void cleanup() {
    orchestrator.resetData();
  }

  /**
   * No bytecode, only sources
   */
  @Test
  public void scan_java_sources() {
    orchestrator.getServer().restoreProfile(ResourceLocation.create("/sonar-way-profile.xml"));
    orchestrator.getServer().provisionProject("java:sample", "Java Sample, with comma");
    orchestrator.getServer().associateProjectToQualityProfile("java:sample", "java", "sonar-way");

    SonarScanner build = newScanner(new File("projects/java-sample"))
      .setProperty("sonar.verbose", "true")
      .addArguments("-e");
    // SONARPLUGINS-3061
    // Add a trailing slash
    build.setProperty("sonar.host.url", orchestrator.getServer().getUrl() + "/");
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient().find(new ResourceQuery("java:sample").setMetrics("files", "ncloc", "classes", "lcom4", "violations"));
    // SONARPLUGINS-2399
    assertThat(project.getName()).isEqualTo("Java Sample, with comma");
    assertThat(project.getDescription()).isEqualTo("This is a Java sample");
    assertThat(project.getVersion()).isEqualTo("1.2.3");
    assertThat(project.getMeasureIntValue("files")).isEqualTo(2);
    assertThat(project.getMeasureIntValue("classes")).isEqualTo(2);
    assertThat(project.getMeasureIntValue("ncloc")).isGreaterThan(10);
    assertThat(project.getMeasureIntValue("violations")).isGreaterThan(0);

    Resource file = orchestrator.getServer().getWsClient()
      .find(new ResourceQuery("java:sample:src/basic/Hello.java").setMetrics("files", "ncloc", "classes", "violations"));
    assertThat(file.getName()).isEqualTo("Hello.java");
    assertThat(file.getMeasureIntValue("ncloc")).isEqualTo(7);
    assertThat(file.getMeasureIntValue("violations")).isGreaterThan(0);
  }

  @Test
  public void scan_java_sources_and_bytecode() {
    orchestrator.getServer().restoreProfile(ResourceLocation.create("/requires-bytecode-profile.xml"));
    orchestrator.getServer().provisionProject("java:bytecode", "Java Bytecode Sample");
    orchestrator.getServer().associateProjectToQualityProfile("java:bytecode", "java", "requires-bytecode");

    SonarScanner build = newScanner(new File("projects/java-bytecode"));
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient().find(new ResourceQuery("java:bytecode").setMetrics("lcom4", "violations"));
    assertThat(project.getName()).isEqualTo("Java Bytecode Sample");
    // the squid rules enabled in sonar-way-profile do not exist in SQ 3.0
    assertThat(project.getMeasureIntValue("violations")).isGreaterThan(0);

    Resource file = orchestrator.getServer().getWsClient().find(new ResourceQuery("java:bytecode:src/HasFindbugsViolation.java").setMetrics("lcom4", "violations"));
    assertThat(file.getMeasureIntValue("violations")).isGreaterThan(0);

    // findbugs is executed on bytecode
    List<Issue> issues = orchestrator.getServer().wsClient().issueClient().find(IssueQuery.create().componentRoots("java:bytecode").rules("squid:S1147")).list();
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).ruleKey()).isEqualTo("squid:S1147");

    // Squid performs analysis of dependencies
    issues = orchestrator.getServer().wsClient().issueClient().find(IssueQuery.create().componentRoots("java:bytecode").rules("squid:CallToDeprecatedMethod")).list();
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).ruleKey()).isEqualTo("squid:CallToDeprecatedMethod");
  }

  @Test
  public void basedir_contains_java_sources() {
    orchestrator.getServer().restoreProfile(ResourceLocation.create("/sonar-way-profile.xml"));
    orchestrator.getServer().provisionProject("java:basedir-with-source", "Basedir with source");
    orchestrator.getServer().associateProjectToQualityProfile("java:basedir-with-source", "java", "sonar-way");

    SonarScanner build = newScanner(new File("projects/basedir-with-source"));
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient().find(new ResourceQuery("java:basedir-with-source").setMetrics("files", "ncloc"));
    assertThat(project.getMeasureIntValue("files")).isEqualTo(1);
    assertThat(project.getMeasureIntValue("ncloc")).isGreaterThan(1);
  }

  /**
   * Replace the maven format groupId:artifactId by a single key
   */
  @Test
  public void should_support_simple_project_keys() {
    orchestrator.getServer().restoreProfile(ResourceLocation.create("/sonar-way-profile.xml"));
    orchestrator.getServer().provisionProject("SAMPLE", "Java Sample, with comma");
    orchestrator.getServer().associateProjectToQualityProfile("SAMPLE", "java", "sonar-way");

    SonarScanner build = newScanner(new File("projects/java-sample"))
      .setProjectKey("SAMPLE");
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient().find(new ResourceQuery("SAMPLE").setMetrics("files", "ncloc"));
    assertThat(project.getMeasureIntValue("files")).isEqualTo(2);
    assertThat(project.getMeasureIntValue("ncloc")).isGreaterThan(1);
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
    assertThat(new File("projects/override-working-dir/.overridden-relative-sonar")).exists().isDirectory();
  }

  /**
   * SONARPLUGINS-1230
   */
  @Test
  public void should_override_working_dir_with_absolute_path() {
    File projectHome = new File("projects/override-working-dir");
    SonarScanner build = newScanner(projectHome)
      .setProperty("sonar.working.directory", new File(projectHome, ".overridden-absolute-sonar").getAbsolutePath());
    orchestrator.executeBuild(build);

    assertThat(new File("projects/override-working-dir/.sonar")).doesNotExist();
    assertThat(new File("projects/override-working-dir/.overridden-absolute-sonar")).exists().isDirectory();
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
    assertThat(result.getLogs()).contains("The folder 'bad' does not exist for 'bad-source-dirs'");
  }

  /**
   * SONARPLUGINS-2256
   */
  @Test
  public void should_warn_when_analysis_is_platform_dependent() {
    SonarScanner build = newScanner(new File("projects/java-sample"))
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
    SonarScanner build = newScanner(new File("projects/java-sample"))
      .setProperty("sonar.verbose", "true");
    String logs = orchestrator.executeBuild(build).getLogs();
    assertThat(logs).contains(expectedLog);
  }

  @Test
  public void should_use_environment_props() {
    SonarScanner build = newScanner(new File("projects/java-sample-no-properties"))
      .setEnvironmentVariable("SONARQUBE_SCANNER_PARAMS", "{"
        + "\"sonar.projectKey\" : \"java:sample\"," +
        "\"sonar.projectName\" : \"Java Sample, with comma\"," +
        "\"sonar.projectDescription\" : \"This is a Java sample\"," +
        "\"sonar.projectVersion\" : \"1.2.3\"," +
        "\"sonar.sources\" : \"src\" }");
    orchestrator.executeBuild(build);
  }

  @Test
  public void should_fail_if_unable_to_connect() {
    SonarScanner build = newScanner(new File("projects/java-sample"))
      .setProperty("sonar.host.url", "http://foo");

    BuildResult result = orchestrator.executeBuildQuietly(build);
    // expect build failure
    assertThat(result.getStatus()).isNotEqualTo(0);
    // with the following message
    assertThat(result.getLogs()).contains("SonarQube server [http://foo] can not be reached");
  }

  // SONARPLUGINS-3574
  @Test
  public void run_from_external_location() throws IOException {
    File tempDir = temp.newFolder();
    SonarScanner build = newScanner(tempDir)
      .setProperty("sonar.projectBaseDir", new File("projects/java-sample").getAbsolutePath())
      .addArguments("-e");
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient().find(new ResourceQuery("java:sample").setMetrics("files", "ncloc", "classes", "lcom4", "violations"));
    assertThat(project.getDescription()).isEqualTo("This is a Java sample");
    assertThat(project.getVersion()).isEqualTo("1.2.3");
  }

  @Test
  public void use_old_script_and_old_env_variable() {
    SonarScanner build = newScanner(new File("projects/java-sample"))
      .setUseOldSonarRunnerScript(true)
      .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx2m");
    BuildResult executeBuild = orchestrator.executeBuildQuietly(build);
    assertThat(executeBuild.getStatus()).isNotEqualTo(0);
    String logs = executeBuild.getLogs();
    if (SystemUtils.IS_OS_WINDOWS) {
      assertThat(logs).contains("WARN: sonar-runner.bat script is deprecated. Please use sonar-scanner.bat instead.");
      assertThat(logs).contains("WARN: SONAR_RUNNER_OPTS is deprecated. Please use SONAR_SCANNER_OPTS instead.");
    } else {
      assertThat(logs).contains("WARN: sonar-runner script is deprecated. Please use sonar-scanner instead.");
      assertThat(logs).contains("WARN: $SONAR_RUNNER_OPTS is deprecated. Please use $SONAR_SCANNER_OPTS instead.");
    }
    assertThat(logs).contains("java.lang.OutOfMemoryError");
  }

  @Test
  public void use_new_script_and_new_env_variable() {
    SonarScanner build = newScanner(new File("projects/java-sample"))
      .setEnvironmentVariable("SONAR_SCANNER_OPTS", "-Xmx2m");
    BuildResult executeBuild = orchestrator.executeBuildQuietly(build);
    assertThat(executeBuild.getStatus()).isNotEqualTo(0);
    String logs = executeBuild.getLogs();
    assertThat(logs).doesNotContain("sonar-runner");
    assertThat(logs).doesNotContain("SONAR_RUNNER_OPTS");
    assertThat(logs).contains("java.lang.OutOfMemoryError");
  }

}
