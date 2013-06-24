/*
 * SonarQube Runner - API
 * Copyright (C) 2011 SonarSource
 * dev@sonar.codehaus.org
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
 * Most commonly used properties of the task "scan". These properties are injected in {@link Runner#setProperty(String, String)}.
 * See <a href="http://docs.codehaus.org/pages/viewinfo.action?pageId=194314339">documentation</a> for more properties.
 *
 * @since 2.2
 */
public interface ScanProperties {

  /**
   * Default task
   *
   * @see RunnerProperties#TASK
   */
  String SCAN_TASK = "scan";

  /**
   * Required project key
   */
  String PROJECT_KEY = "sonar.projectKey";


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
   * Optional paths to binaries, for example to declare the directory of Java bytecode. Example : "binDir"
   */
  String PROJECT_BINARY_DIRS = "sonar.binaries";

  /**
   * Optional comma-separated list of paths to libraries. Example : <code>path/to/library/*.jar,path/to/specific/library/myLibrary.jar,parent/*.jar</code>
   */
  String PROJECT_LIBRARIES = "sonar.libraries";

  String PROJECT_LANGUAGE = "sonar.language";

  /**
   * It becomes quickly necessary to input historical data and to highlight some events. It is possible by going for example in a subversion tag
   * and use this property. Format is yyyy-MM-dd, for example 2010-12-25.
   */
  String PROJECT_DATE = "sonar.projectDate";

  /**
   * Property used to specify the base directory of the project to analyse. Default is ".".
   */
  String PROJECT_BASEDIR = "sonar.projectBaseDir";

  /**
   * Encoding of source and test files. By default it's the platform encoding.
   */
  String PROJECT_SOURCE_ENCODING = "sonar.sourceEncoding";

}
