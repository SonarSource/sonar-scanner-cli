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

import org.sonar.runner.impl.Logs;

import java.nio.charset.Charset;
import java.util.Locale;

class SourceEncoding {

  void init(Runner runner) {
    boolean onProject = Utils.taskRequiresProject(runner.properties());
    if (onProject) {
      String sourceEncoding = runner.property(ScanProperties.PROJECT_SOURCE_ENCODING, "");
      boolean platformDependent = false;
      if ("".equals(sourceEncoding)) {
        sourceEncoding = Charset.defaultCharset().name();
        platformDependent = true;
        runner.setProperty(ScanProperties.PROJECT_SOURCE_ENCODING, sourceEncoding);
      }
      Logs.info("Default locale: \"" + Locale.getDefault() + "\", source code encoding: \"" + sourceEncoding + "\""
          + (platformDependent ? " (analysis is platform dependent)" : ""));
    }
  }
}
