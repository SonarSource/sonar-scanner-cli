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

import org.junit.BeforeClass;
import org.junit.rules.TemporaryFolder;
import org.junit.Rule;
import com.sonar.orchestrator.build.BuildFailureException;
import com.sonar.orchestrator.locator.ResourceLocation;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarRunner;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class CacheTest extends RunnerTestCase {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private File currentTemp = null;
  private static boolean serverRunning = false;

  @BeforeClass
  public static void setUpClass() {
    orchestrator.getServer().restoreProfile(ResourceLocation.create("/sonar-way-profile.xml"));
    orchestrator.getServer().provisionProject("java:sample", "Java Sample, with comma");
    orchestrator.getServer().associateProjectToQualityProfile("java:sample", "java", "sonar-way");
    serverRunning = true;
  }

  private static void ensureStarted() {
    if (!serverRunning) {
      orchestrator.start();
      serverRunning = true;
    }
  }

  private static void ensureStopped() {
    if (serverRunning) {
      orchestrator.stop();
      serverRunning = false;
    }
  }

  @Test
  public void testIssuesMode() throws IOException {
    // online, without cache -> should sync
    ensureStarted();
    SonarRunner build = createRunner("issues", true);
    BuildResult result = orchestrator.executeBuild(build, false);
    assertThat(result.isSuccess()).isTrue();

    // offline, with cache -> should run from cache
    ensureStopped();
    build = createRunner("issues", false);
    result = orchestrator.executeBuild(build, false);
    assertThat(result.isSuccess()).isTrue();

    // offline, without cache -> should fail
    build = createRunner("issues", true);
    try {
      result = orchestrator.executeBuild(build);
    } catch (BuildFailureException e) {
      assertThat(e.getResult().getLogs()).contains("Server is not accessible and data is not cached");
    }
  }

  @Test
  public void testPublishModeOffline() throws IOException {
    // online (cache not used)
    ensureStarted();
    SonarRunner build = createRunner("publish");
    BuildResult result = orchestrator.executeBuild(build, false);
    assertThat(result.isSuccess()).isTrue();

    // offline (cache not used) -> should fail
    ensureStopped();
    build = createRunner("publish", false);
    try {
      result = orchestrator.executeBuild(build);
    } catch (BuildFailureException e) {
      assertThat(e.getResult().getLogs()).contains("Fail to download libraries from server");
    }

  }

  private SonarRunner createRunner(String mode) throws IOException {
    return createRunner(mode, false);
  }

  private SonarRunner createRunner(String mode, boolean refreshCache) throws IOException {
    if (refreshCache || currentTemp == null) {
      currentTemp = temp.newFolder();
    }

    SonarRunner runner = newRunner(new File("projects/java-sample"))
      .setProperty("sonar.analysis.mode", mode)
      .setProperty("sonar.userHome", currentTemp.getAbsolutePath());

    return runner;
  }

}
