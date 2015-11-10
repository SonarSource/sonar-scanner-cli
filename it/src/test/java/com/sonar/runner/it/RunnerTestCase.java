/*
 * SonarSource :: IT :: SonarQube Scanner
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

import com.sonar.orchestrator.version.Version;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.SonarRunner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.io.File;

public abstract class RunnerTestCase {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  public static Orchestrator orchestrator = null;

  @BeforeClass
  public static void startServer() {
    OrchestratorBuilder builder = Orchestrator.builderEnv()
      // TODO Java projects should be replaced by Xoo projects
      .setOrchestratorProperty("javaVersion", "LATEST_RELEASE")
      .addPlugin("java")
      .setOrchestratorProperty("findbugsVersion", "LATEST_RELEASE")
      .addPlugin("findbugs")
      .setOrchestratorProperty("javascriptVersion", "LATEST_RELEASE")
      .addPlugin("javascript");

    orchestrator = builder.build();
    orchestrator.start();
  }

  @AfterClass
  public static void stopServer() {
    if (orchestrator != null) {
      orchestrator.stop();
      orchestrator = null;
    }
  }

  SonarRunner newRunner(File baseDir, String... keyValueProperties) {
    SonarRunner runner = SonarRunner.create(baseDir, keyValueProperties);
    String runnerVersion = Version.create(orchestrator.getConfiguration().getString("sonarRunner.version")).toString();
    runner.setRunnerVersion(runnerVersion);
    return runner;
  }
}
