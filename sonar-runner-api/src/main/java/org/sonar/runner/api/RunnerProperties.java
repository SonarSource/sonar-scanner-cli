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
 * Mostly used properties that can be passed to {@link EmbeddedRunner#addGlobalProperties(java.util.Properties)}.
 * See <a href="http://docs.sonarqube.org/display/SONAR/Analysis+Parameters">documentation</a> for more properties.
 *
 * @since 2.2
 */
public interface RunnerProperties {
  /**
   * HTTP URL of Sonar server, "http://localhost:9000" by default
   */
  String HOST_URL = "sonar.host.url";

  /**
   * Task to execute, "scan" by default
   * @deprecated since 2.5 No more task starting from SQ 5.2
   */
  @Deprecated
  String TASK = "sonar.task";

  /**
   * Working directory containing generated reports and temporary data.
   */
  String WORK_DIR = "sonar.working.directory";
}
