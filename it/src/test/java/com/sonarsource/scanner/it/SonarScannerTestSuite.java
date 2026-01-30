/*
 * SonarSource :: IT :: SonarScanner CLI
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.junit4.OrchestratorRule;
import com.sonar.orchestrator.locator.MavenLocation;
import java.util.List;
import java.util.Map;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ScannerTest.class, MultimoduleTest.class,
                DistributionTest.class})
public class SonarScannerTestSuite {

  // Mapping from SonarQube Server versions to Community Build versions for the xoo plugin.
  // The xoo plugin is only published with Community Build releases, not Server releases.
  private static final Map<String, String> SERVER_TO_COMMUNITY_VERSION = Map.of(
    "LATEST_RELEASE[2025.1]", "LATEST_RELEASE[25.1.0]",
    "LATEST_RELEASE[2026.1]", "LATEST_RELEASE[26.1.0]"
  );

  @ClassRule
  public static final OrchestratorRule ORCHESTRATOR = createOrchestrator();

  private static OrchestratorRule createOrchestrator() {
    String sonarVersion = System
      .getProperty("sonar.runtimeVersion", "DEV");
    boolean isCommunity = List.of("LATEST_RELEASE", "DEV").contains(sonarVersion);
    var builder = OrchestratorRule.builderEnv()
      .defaultForceAuthentication()
      .setSonarVersion(sonarVersion)
      .setEdition(isCommunity ? Edition.COMMUNITY : Edition.DEVELOPER)
      .addBundledPluginToKeep("sonar-javascript")
      .addPlugin(MavenLocation.of("org.sonarsource.sonarqube", "sonar-xoo-plugin",
        SERVER_TO_COMMUNITY_VERSION.getOrDefault(sonarVersion, sonarVersion)));
    if (!isCommunity) {
      builder.activateLicense();
    }
    return builder.build();
  }

}
