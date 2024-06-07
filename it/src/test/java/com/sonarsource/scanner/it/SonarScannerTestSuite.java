/*
 * SonarSource :: IT :: SonarScanner CLI
 * Copyright (C) 2009-2024 SonarSource SA
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
import com.sonar.orchestrator.junit4.OrchestratorRule;
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
  public static final OrchestratorRule ORCHESTRATOR = createOrchestrator();

  private static OrchestratorRule createOrchestrator() {
    String sonarVersion = System
      .getProperty("sonar.runtimeVersion", "DEV");
    return OrchestratorRule.builderEnv()
      .defaultForceAuthentication()
      .setSonarVersion(sonarVersion)
      .addBundledPluginToKeep("sonar-javascript")
      .addPlugin(MavenLocation.of("org.sonarsource.sonarqube", "sonar-xoo-plugin", sonarVersion))
      .build();
  }

}
