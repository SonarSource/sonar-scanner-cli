/*
 * SonarQube Runner - API
 * Copyright (C) 2011 SonarSource
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
package org.sonar.runner.api;

/**
 * Most commonly used properties for a SonarQube analysis. These properties are passed to {@link EmbeddedRunner#runAnalysis(java.util.Properties)}.
 * See <a href="http://docs.sonarqube.org/display/SONAR/Analysis+Parameters">documentation</a> for more properties.
 *
 * @since 2.2
 */
public interface ScanProperties {

  /**
   * Default task
   *
   * @see RunnerProperties#TASK
   * @deprecated since 2.5 No more task since SQ 5.2
   */
  @Deprecated
  String SCAN_TASK = "scan";

  /**
   * Required project key
   */
  String PROJECT_KEY = "sonar.projectKey";

  /**
   * Used to define the exact key of each module. If {@link #PROJECT_KEY} is used instead on a module, then final key of the module will be <parent module key>:<PROJECT_KEY>.
   * @since SonarQube 4.1
   */
  String MODULE_KEY = "sonar.moduleKey";

  String PROJECT_NAME = "sonar.projectName";

  String PROJECT_VERSION = "sonar.projectVersion";

  /**
   * Optional description
   */
  String PROJECT_DESCRIPTION = "sonar.projectDescription";

  /**
   * Required paths to source directories, separated by commas, for example: "srcDir1,srcDir2"
   */
  String PROJECT_SOURCE_DIRS = "sonar.sources";

  /**
   * Optional paths to test directories, separated by commas, for example: "testDir1,testDir2"
   */
  String PROJECT_TEST_DIRS = "sonar.tests";

  /**
   * Property used to specify the base directory of the project to analyse. Default is ".".
   */
  String PROJECT_BASEDIR = "sonar.projectBaseDir";

  /**
   * Encoding of source and test files. By default it's the platform encoding.
   */
  String PROJECT_SOURCE_ENCODING = "sonar.sourceEncoding";

}
