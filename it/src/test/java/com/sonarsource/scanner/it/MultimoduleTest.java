/*
 * SonarSource :: IT :: SonarQube Scanner
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.io.File;
import org.junit.After;
import org.junit.Test;
import org.sonarqube.ws.WsComponents.Component;

import static org.fest.assertions.Assertions.assertThat;

public class MultimoduleTest extends ScannerTestCase {

  @After
  public void cleanup() {
    orchestrator.resetData();
  }

  /**
   * SONARPLUGINS-2202
   */
  @Test
  public void test_simplest_with_props_on_root() {
    SonarScanner build = newScanner(new File("projects/multi-module/simplest/simplest-with-props-on-root"));

    orchestrator.executeBuild(build);

    assertThat(getComponent("simplest-with-props-on-root").getName()).isEqualTo("Simplest multi-module project with all properties set on the root project");

    // Verify that we have the modules
    assertThat(getComponent("simplest-with-props-on-root:module1").getName()).isEqualTo("module1");

    assertThat(getComponent("simplest-with-props-on-root:module2").getName()).isEqualTo("module2");

    // And verify that the working directories are all located in the root folder
    File workDir = new File("projects/multi-module/simplest/simplest-with-props-on-root/.scannerwork");
    assertThat(workDir).exists();
    assertThat(new File(workDir, "simplest-with-props-on-root_module1")).exists();
    assertThat(new File(workDir, "simplest-with-props-on-root_module2")).exists();
    assertThat(new File("projects/multi-module/simplest/simplest-with-props-on-root/module1/.scannerwork")).doesNotExist();
    assertThat(new File("projects/multi-module/simplest/simplest-with-props-on-root/module2/.scannerwork")).doesNotExist();
  }

  /**
   * SONARPLUGINS-2421
   */
  @Test
  public void test_multi_language_with_same_projectdir() {
    SonarScanner build = newScanner(new File("projects/multi-module/multi-language"));

    orchestrator.executeBuild(build);

    assertThat(getComponent("multi-language").getName()).isEqualTo("Simplest multi-language project");

    // Verify that we have the modules
    assertThat(getComponent("multi-language:java-module").getName()).isEqualTo("java-module");

    assertThat(getComponent("multi-language:js-module").getName()).isEqualTo("js-module");
  }

  /**
   * SONARPLUGINS-2202
   */
  @Test
  public void test_simplest_with_props_on_each_module() {
    SonarScanner build = newScanner(new File("projects/multi-module/simplest/simplest-with-props-on-each-module"));

    orchestrator.executeBuild(build);

    assertThat(getComponent("simplest-with-props-each-module").getName()).isEqualTo("Simplest multi-module project with properties set on each module");

    // Verify that we have the modules
    assertThat(getComponent("simplest-with-props-each-module:module1").getName()).isEqualTo("module1");

    assertThat(getComponent("simplest-with-props-each-module:overridden-key-for-module2").getName()).isEqualTo("Module 2");
  }

  /**
   * SONARPLUGINS-2202
   */
  @Test
  public void test_deep_path_for_modules() {
    SonarScanner build = newScanner(new File("projects/multi-module/customization/deep-path-for-modules"));

    orchestrator.executeBuild(build);

    assertThat(getComponent("deep-path-for-modules").getName()).isEqualTo("Project with deep path for modules");

    // Verify that we have the modules
    assertThat(getComponent("deep-path-for-modules:mod1").getName()).isEqualTo("Module 1");

    assertThat(getComponent("deep-path-for-modules:mod2").getName()).isEqualTo("Module 2");
  }

  /**
   * SONARPLUGINS-2202
   */
  @Test
  public void test_module_path_with_space() {
    SonarScanner build = newScanner(new File("projects/multi-module/customization/module-path-with-space"));

    orchestrator.executeBuild(build);

    assertThat(getComponent("module-path-with-space").getName()).isEqualTo("Project with module path that contain spaces");

    // Verify that we have the modules
    assertThat(getComponent("module-path-with-space:module1").getName()).isEqualTo("Module 1");

    assertThat(getComponent("module-path-with-space:module2").getName()).isEqualTo("Module 2");
  }

  /**
   * SONARPLUGINS-2202
   */
  @Test
  public void test_overwriting_parent_properties() {
    SonarScanner build = newScanner(new File("projects/multi-module/customization/overwriting-parent-properties"));

    orchestrator.executeBuild(build);

    Component rootProject = getComponent("overwriting-parent-properties");
    assertThat(rootProject.getName()).isEqualTo("Project with modules that overwrite properties");
    assertThat(rootProject.getDescription()).isEqualTo("Description of root project");

    // Verify that we have the modules
    Component module1 = getComponent("overwriting-parent-properties:module1-new-key");
    assertThat(module1.getName()).isEqualTo("Module 1");
    assertThat(module1.getDescription()).isEqualTo("Description of module 1");

    Component module2 = getComponent("overwriting-parent-properties:module2-new-key");
    assertThat(module2.getName()).isEqualTo("Module 2");
    assertThat(module2.getDescription()).isEqualTo("Description of module 2");
  }

  /**
   * SONARPLUGINS-2202
   */
  @Test
  public void test_using_config_file_property() {
    SonarScanner build = newScanner(new File("projects/multi-module/advanced/using-config-file-prop"));

    orchestrator.executeBuild(build);

    assertThat(getComponent("using-config-file-prop").getName()).isEqualTo("Advanced use case - mostly used by the Ant task");

    // Verify that we have the modules
    assertThat(getComponent("using-config-file-prop:module1").getName()).isEqualTo("Module 1");

    assertThat(getComponent("using-config-file-prop:module2").getName()).isEqualTo("Module 2");
  }

  /**
   * SONARPLUGINS-2202
   */
  @Test
  public void should_fail_if_unexisting_base_dir() {
    SonarScanner build = newScanner(new File("projects/multi-module/failures/unexisting-base-dir"));

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
    SonarScanner build = newScanner(new File("projects/multi-module/failures/unexisting-config-file"));

    BuildResult result = orchestrator.executeBuildQuietly(build);
    // expect build failure
    assertThat(result.getStatus()).isNotEqualTo(0);
    // with the following message
    assertThat(result.getLogs()).contains("The properties file of the module 'module1' does not exist");
  }

}
