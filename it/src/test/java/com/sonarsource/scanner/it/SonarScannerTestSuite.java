/*
 * SonarSource :: IT :: SonarScanner CLI
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

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.locator.MavenLocation;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ScannerTest.class, MultimoduleTest.class,
                DistributionTest.class})
public class SonarScannerTestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = createOrchestrator();

  private static Orchestrator createOrchestrator() {
    String sonarVersion = System
      .getProperty("sonar.runtimeVersion", "LATEST_RELEASE[7.9]");
    OrchestratorBuilder builder = Orchestrator.builderEnv()
      .setSonarVersion(
        sonarVersion);
    // The javascript language plugin needs to be installed to allow for
    // tests to pass. If not installed test fail with a "no languages
    // installed" error.
    MavenLocation javascriptPlugin = MavenLocation
      .of("org.sonarsource.javascript", "sonar-javascript-plugin",
        "5.2.1.7778");
    // Since version 8.5 languages are bundled and located in a different
    // location then other plugins. So install this in the correct location.
    if (sonarVersion.startsWith("LATEST_RELEASE[7.9]")) {
      builder.addPlugin(javascriptPlugin);
    } else {
      builder.addBundledPlugin(javascriptPlugin);
    }
    return builder.build();
  }

}
