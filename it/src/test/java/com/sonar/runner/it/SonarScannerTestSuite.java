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

import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({JavaTest.class, MultimoduleTest.class, CacheTest.class})
public class SonarScannerTestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .setOrchestratorProperty("javaVersion", "LATEST_RELEASE")
    .addPlugin("java")
    .setOrchestratorProperty("findbugsVersion", "LATEST_RELEASE")
    .addPlugin("findbugs")
    .setOrchestratorProperty("javascriptVersion", "LATEST_RELEASE")
    .addPlugin("javascript")
    .build();

}
