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

import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.ResourceLocation;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.junit.After;
import org.junit.Test;
import org.sonarqube.ws.WsMeasures.Measure;

import static java.lang.Integer.parseInt;
import static org.assertj.core.api.Assertions.assertThat;

public class DistributionTest extends ScannerTestCase {

  @After
  public void cleanup() {
    orchestrator.resetData();
  }

  @Test
  public void script_should_push_report_to_sonarqube() throws IOException, InterruptedException {
    String projectKey = "java:basedir-with-source";
    orchestrator.getServer().restoreProfile(ResourceLocation.create("/sonar-way-profile.xml"));
    orchestrator.getServer().provisionProject(projectKey, "Basedir with source");
    orchestrator.getServer().associateProjectToQualityProfile(projectKey, "java", "sonar-way");

    File projectDir = new File("projects/basedir-with-source");
    SonarScanner build = newScanner(projectDir, "sonar.projectKey", projectKey).useNative();
    orchestrator.executeBuild(build, true);

    Map<String, Measure> projectMeasures = getMeasures(projectKey, "files", "ncloc");
    assertThat(parseInt(projectMeasures.get("files").getValue())).isEqualTo(1);
    assertThat(parseInt(projectMeasures.get("ncloc").getValue())).isGreaterThan(1);
  }
}
