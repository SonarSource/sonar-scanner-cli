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

import com.sonar.orchestrator.build.BuildFailureException;

import com.sonar.orchestrator.locator.ResourceLocation;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarRunner;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class CacheTest extends RunnerTestCase {
  @Test
  public void testOffline() {
    assumeTrue(orchestrator.getServer().version().isGreaterThanOrEquals("5.2"));
    orchestrator.getServer().restoreProfile(ResourceLocation.create("/sonar-way-profile.xml"));

    SonarRunner build = createRunner(true);
    BuildResult result = orchestrator.executeBuild(build);
    stopServer();

    build = createRunner(false);
    try {
      result = orchestrator.executeBuild(build, false);
    } catch (BuildFailureException e) {
      // expected
    }

    build = createRunner(true);
    result = orchestrator.executeBuild(build, false);
    assertTrue(result.isSuccess());
  }

  private SonarRunner createRunner(boolean enableOffline) {
    SonarRunner runner = newRunner(new File("projects/java-sample"))
      .setProperty("sonar.analysis.mode", "preview")
      .setProfile("sonar-way");

    if (enableOffline) {
      runner.setProperty("sonar.enableOffline", "true");
    }

    return runner;
  }

}
