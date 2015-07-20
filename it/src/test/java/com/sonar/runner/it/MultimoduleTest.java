/*
 * SonarSource :: IT :: SonarQube Runner
 * Copyright (C) 2009 SonarSource
 * sonarqube@googlegroups.com
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
package com.sonar.runner.it;

import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarRunner;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class MultimoduleTest extends RunnerTestCase {

  /**
   * SONARPLUGINS-2202
   */
  @Test
  public void test_simplest_with_props_on_root() {
    SonarRunner build = newRunner(new File("projects/multi-module/simplest/simplest-with-props-on-root"));

    orchestrator.executeBuild(build);

    Resource rootProject = findResource("simplest-with-props-on-root");
    assertThat(rootProject.getName()).isEqualTo("Simplest multi-module project with all properties set on the root project");
    assertThat(rootProject.getVersion()).isEqualTo("1.2.3");

    // Verify that we have the modules
    Resource module1 = findResource("simplest-with-props-on-root:module1");
    assertThat(module1.getName()).isEqualTo("module1");
    assertThat(module1.getVersion()).isEqualTo("1.2.3");

    Resource module2 = findResource("simplest-with-props-on-root:module2");
    assertThat(module2.getName()).isEqualTo("module2");
    assertThat(module2.getVersion()).isEqualTo("1.2.3");

    // And verify that the working directories are all located in the root folder
    File workDir = new File("projects/multi-module/simplest/simplest-with-props-on-root/.sonar");
    assertThat(workDir).exists();
    assertThat(new File(workDir, "simplest-with-props-on-root_module1")).exists();
    assertThat(new File(workDir, "simplest-with-props-on-root_module2")).exists();
    assertThat(new File("projects/multi-module/simplest/simplest-with-props-on-root/module1/.sonar")).doesNotExist();
    assertThat(new File("projects/multi-module/simplest/simplest-with-props-on-root/module2/.sonar")).doesNotExist();
  }

  /**
   * SONARPLUGINS-2421
   */
  @Test
  public void test_multi_language_with_same_projectdir() {
    SonarRunner build = newRunner(new File("projects/multi-module/multi-language"));

    orchestrator.executeBuild(build);

    Resource rootProject = findResource("multi-language");
    assertThat(rootProject.getName()).isEqualTo("Simplest multi-language project");
    assertThat(rootProject.getVersion()).isEqualTo("1.2.3");

    // Verify that we have the modules
    Resource module1 = findResource("multi-language:java-module");
    assertThat(module1.getName()).isEqualTo("java-module");
    assertThat(module1.getVersion()).isEqualTo("1.2.3");

    Resource module2 = findResource("multi-language:js-module");
    assertThat(module2.getName()).isEqualTo("js-module");
    assertThat(module2.getVersion()).isEqualTo("1.2.3");
  }

  /**
   * SONARPLUGINS-2202
   */
  @Test
  public void test_simplest_with_props_on_each_module() {
    SonarRunner build = newRunner(new File("projects/multi-module/simplest/simplest-with-props-on-each-module"));

    orchestrator.executeBuild(build);

    Resource rootProject = findResource("simplest-with-props-each-module");
    assertThat(rootProject.getName()).isEqualTo("Simplest multi-module project with properties set on each module");
    assertThat(rootProject.getVersion()).isEqualTo("1.2.3");

    // Verify that we have the modules
    Resource module1 = findResource("simplest-with-props-each-module:module1");
    assertThat(module1.getName()).isEqualTo("module1");
    assertThat(module1.getVersion()).isEqualTo("1.2.3");

    Resource module2 = findResource("simplest-with-props-each-module:overridden-key-for-module2");
    assertThat(module2.getName()).isEqualTo("Module 2");
    assertThat(module2.getVersion()).isEqualTo("1.2.3");
  }

  /**
   * SONARPLUGINS-2295
   */
  @Test
  public void test_warning_when_source_folder_on_root_module() {
    SonarRunner build = newRunner(new File("projects/multi-module/simplest/simplest-with-props-on-each-module"));

    assertThat(orchestrator.executeBuild(build).getLogs()).contains("/!\\ A multi-module project can't have source folders");
  }

  /**
   * SONARPLUGINS-2202
   */
  @Test
  public void test_deep_path_for_modules() {
    SonarRunner build = newRunner(new File("projects/multi-module/customization/deep-path-for-modules"));

    orchestrator.executeBuild(build);

    Resource rootProject = findResource("deep-path-for-modules");
    assertThat(rootProject.getName()).isEqualTo("Project with deep path for modules");
    assertThat(rootProject.getVersion()).isEqualTo("1.2.3");

    // Verify that we have the modules
    Resource module1 = findResource("deep-path-for-modules:mod1");
    assertThat(module1.getName()).isEqualTo("Module 1");
    assertThat(module1.getVersion()).isEqualTo("1.2.3");

    Resource module2 = findResource("deep-path-for-modules:mod2");
    assertThat(module2.getName()).isEqualTo("Module 2");
    assertThat(module2.getVersion()).isEqualTo("1.2.3");
  }

  /**
   * SONARPLUGINS-2202
   */
  @Test
  public void test_module_path_with_space() {
    SonarRunner build = newRunner(new File("projects/multi-module/customization/module-path-with-space"));

    orchestrator.executeBuild(build);

    Resource rootProject = findResource("module-path-with-space");
    assertThat(rootProject.getName()).isEqualTo("Project with module path that contain spaces");
    assertThat(rootProject.getVersion()).isEqualTo("1.2.3");

    // Verify that we have the modules
    Resource module1 = findResource("module-path-with-space:module1");
    assertThat(module1.getName()).isEqualTo("Module 1");
    assertThat(module1.getVersion()).isEqualTo("1.2.3");

    Resource module2 = findResource("module-path-with-space:module2");
    assertThat(module2.getName()).isEqualTo("Module 2");
    assertThat(module2.getVersion()).isEqualTo("1.2.3");
  }

  /**
   * SONARPLUGINS-2202
   */
  @Test
  public void test_overwriting_parent_properties() {
    SonarRunner build = newRunner(new File("projects/multi-module/customization/overwriting-parent-properties"));

    orchestrator.executeBuild(build);

    Resource rootProject = findResource("overwriting-parent-properties");
    assertThat(rootProject.getName()).isEqualTo("Project with modules that overwrite properties");
    assertThat(rootProject.getVersion()).isEqualTo("1.2.3");
    assertThat(rootProject.getDescription()).isEqualTo("Description of root project");

    // Verify that we have the modules
    Resource module1 = findResource("overwriting-parent-properties:module1-new-key");
    assertThat(module1.getName()).isEqualTo("Module 1");
    assertThat(module1.getVersion()).isEqualTo("1.2.3");
    assertThat(module1.getDescription()).isEqualTo("Description of module 1");

    Resource module2 = findResource("overwriting-parent-properties:module2-new-key");
    assertThat(module2.getName()).isEqualTo("Module 2");
    assertThat(module2.getVersion()).isEqualTo("1.2.3");
    assertThat(module2.getDescription()).isEqualTo("Description of module 2");
  }

  /**
   * SONARPLUGINS-2202
   */
  @Test
  public void test_using_config_file_property() {
    SonarRunner build = newRunner(new File("projects/multi-module/advanced/using-config-file-prop"));

    orchestrator.executeBuild(build);

    Resource rootProject = findResource("using-config-file-prop");
    assertThat(rootProject.getName()).isEqualTo("Advanced use case - mostly used by the Ant task");
    assertThat(rootProject.getVersion()).isEqualTo("1.2.3");

    // Verify that we have the modules
    Resource module1 = findResource("using-config-file-prop:module1");
    assertThat(module1.getName()).isEqualTo("Module 1");
    assertThat(module1.getVersion()).isEqualTo("1.2.3");

    Resource module2 = findResource("using-config-file-prop:module2");
    assertThat(module2.getName()).isEqualTo("Module 2");
    assertThat(module2.getVersion()).isEqualTo("1.2.3");
  }

  /**
   * SONARPLUGINS-2202
   */
  @Test
  public void should_fail_if_unexisting_base_dir() {
    SonarRunner build = newRunner(new File("projects/multi-module/failures/unexisting-base-dir"));

    BuildResult result = orchestrator.executeBuildQuietly(build);
    // expect build failure
    assertThat(result.getStatus()).isNotEqualTo(0);
    // with the following message
    assertThat(result.getLogs()).contains("The base directory of the module 'module3' does not exist");

  }

  /**
   * SONARPLUGINS-2202
   */
  @Test
  public void should_fail_if_unexisting_config_file() {
    SonarRunner build = newRunner(new File("projects/multi-module/failures/unexisting-config-file"));

    BuildResult result = orchestrator.executeBuildQuietly(build);
    // expect build failure
    assertThat(result.getStatus()).isNotEqualTo(0);
    // with the following message
    assertThat(result.getLogs()).contains("The properties file of the module 'module1' does not exist");
  }

  private Resource findResource(String resourceKey) {
    return orchestrator.getServer().getWsClient().find(new ResourceQuery(resourceKey));
  }
}
