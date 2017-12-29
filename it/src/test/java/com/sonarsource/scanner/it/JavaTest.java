/*
 * SonarSource :: IT :: SonarQube Scanner
 * Copyright (C) 2009-2017 SonarSource SA
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
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.ResourceLocation;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.fest.assertions.Condition;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonarqube.ws.WsComponents.Component;
import org.sonarqube.ws.WsMeasures.Measure;

import static java.lang.Integer.parseInt;
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
      .setProperty("sonar.verbose", "true");
    // SONARPLUGINS-3061
    // Add a trailing slash
    build.setProperty("sonar.host.url", orchestrator.getServer().getUrl() + "/");
    orchestrator.executeBuild(build);

    Component project = getComponent("java:sample");
    assertThat(project.getName()).isEqualTo("Java Sample, with comma");
    assertThat(project.getDescription()).isEqualTo("This is a Java sample");

    Map<String, Measure> projectMeasures = getMeasures("java:sample", "files", "ncloc", "classes", "violations");
    // SONARPLUGINS-2399
    assertThat(parseInt(projectMeasures.get("files").getValue())).isEqualTo(2);
    assertThat(parseInt(projectMeasures.get("classes").getValue())).isEqualTo(2);
    assertThat(parseInt(projectMeasures.get("ncloc").getValue())).isGreaterThan(10);
    assertThat(parseInt(projectMeasures.get("violations").getValue())).isGreaterThan(0);

    Component file = getComponent("java:sample:src/basic/Hello.java");
    assertThat(file.getName()).isEqualTo("Hello.java");

    Map<String, Measure> fileMeasures = getMeasures("java:sample:src/basic/Hello.java", "files", "ncloc", "classes", "violations");
    assertThat(parseInt(fileMeasures.get("ncloc").getValue())).isEqualTo(7);
    assertThat(parseInt(fileMeasures.get("violations").getValue())).isGreaterThan(0);
  }

  /**
   * Only tests, no sources
   */
  @Test
  public void scan_java_tests() {
    orchestrator.getServer().restoreProfile(ResourceLocation.create("/sonar-way-profile.xml"));
    orchestrator.getServer().provisionProject("java:sampletest", "Java Sample");
    orchestrator.getServer().associateProjectToQualityProfile("java:sampletest", "java", "sonar-way");

    SonarScanner build = newScanner(new File("projects/java-sample"))
      .setProperty("sonar.projectKey", "java:sampletest")
      .setProperty("sonar.tests", "src")
      .setProperty("sonar.sources", "");
    orchestrator.executeBuild(build);

    Component file = getComponent("java:sampletest:src/basic/Hello.java");
    assertThat(file.getName()).isEqualTo("Hello.java");
    assertThat(file.getQualifier()).isEqualTo("UTS");
  }

  @Test
  public void scan_java_sources_and_bytecode() {
    orchestrator.getServer().restoreProfile(ResourceLocation.create("/requires-bytecode-profile.xml"));
    orchestrator.getServer().provisionProject("java:bytecode", "Java Bytecode Sample");
    orchestrator.getServer().associateProjectToQualityProfile("java:bytecode", "java", "requires-bytecode");

    SonarScanner build = newScanner(new File("projects/java-bytecode"));
    orchestrator.executeBuild(build);

    Component project = getComponent("java:bytecode");
    assertThat(project.getName()).isEqualTo("Java Bytecode Sample");

    Map<String, Measure> projectMeasures = getMeasures("java:bytecode", "violations");
    // the squid rules enabled in sonar-way-profile do not exist in SQ 3.0
    assertThat(parseInt(projectMeasures.get("violations").getValue())).isGreaterThan(0);

    assertThat(getMeasureAsInteger("java:bytecode:src/HasFindbugsViolation.java", "violations")).isGreaterThan(0);

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

    Map<String, Measure> projectMeasures = getMeasures("java:basedir-with-source", "files", "ncloc");
    assertThat(parseInt(projectMeasures.get("files").getValue())).isEqualTo(1);
    assertThat(parseInt(projectMeasures.get("ncloc").getValue())).isGreaterThan(1);
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

    Map<String, Measure> projectMeasures = getMeasures("SAMPLE", "files", "ncloc");
    assertThat(parseInt(projectMeasures.get("files").getValue())).isEqualTo(2);
    assertThat(parseInt(projectMeasures.get("ncloc").getValue())).isGreaterThan(1);
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
    assertThat(result.getLogs()).contains("Invalid value of sonar.sources for bad-source-dirs");
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
  public void should_skip_analysis() {
    SonarScanner build = newScanner(new File("projects/java-sample"))
      .setProperty("sonar.host.url", "http://foo")
      .setEnvironmentVariable("SONARQUBE_SCANNER_PARAMS", "{ \"sonar.scanner.skip\":\"true\" }");

    BuildResult result = orchestrator.executeBuild(build);
    assertThat(result.getLogs()).contains("SonarQube Scanner analysis skipped");
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

    assertThat(getComponent("java:sample").getDescription()).isEqualTo("This is a Java sample");
    Map<String, Measure> projectMeasures = getMeasures("java:sample", "files", "ncloc", "classes", "violations");
    assertThat(projectMeasures.values().stream().filter(measure -> measure.getValue() != null).collect(Collectors.toList())).hasSize(4);
  }

  @Test
  public void verify_env_variable() {
    SonarScanner build = newScanner(new File("projects/java-sample"))
      .setEnvironmentVariable("SONAR_SCANNER_OPTS", "-Xmx2m");
    BuildResult executeBuild = orchestrator.executeBuildQuietly(build);
    assertThat(executeBuild.getStatus()).isNotEqualTo(0);
    String logs = executeBuild.getLogs();
    assertThat(logs).satisfies(new Condition<String>() {
      @Override
      public boolean matches(String value) {
        return value.contains("java.lang.OutOfMemoryError") || value.contains("GC overhead limit exceeded");
      }
    });
  }

}
