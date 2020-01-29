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

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.MavenLocation;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ScannerTest.class, MultimoduleTest.class, DistributionTest.class})
public class SonarScannerTestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .setSonarVersion(System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE[6.7]"))
    // The scanner cli should still be compatible with previous LTS 6.7, and not the 7.9
    // at the time of writing, so the installed plugins should be compatible with
    // both 6.7 and 8.x. The latest releases of analysers drop the compatibility with
    // 6.7, that's why versions are hardcoded here.
    .addPlugin(MavenLocation.of("org.sonarsource.javascript", "sonar-javascript-plugin", "5.2.1.7778"))
    .build();

}
