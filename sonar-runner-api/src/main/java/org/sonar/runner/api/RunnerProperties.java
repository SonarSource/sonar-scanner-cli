/*
 * Sonar Runner - API
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
 * Mostly used properties that can be injected in {@link Runner#setProperty(String, String)}.
 * See <a href="http://docs.codehaus.org/pages/viewinfo.action?pageId=194314339">documentation</a> for more properties.
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
   */
  String TASK = "sonar.task";

  /**
   * Encoding of source and test files. By default it's the platform encoding.
   */
  String SOURCE_ENCODING = "sonar.sourceEncoding";

  /**
   * Working directory containing generated reports and temporary data.
   */
  String WORK_DIR = "sonar.working.directory";
}
