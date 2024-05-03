/*
 * SonarScanner CLI
 * Copyright (C) 2011-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonarsource.scanner.cli;

import java.util.Map;
import java.util.Properties;
import org.sonarsource.scanner.lib.ScannerEngineBootstrapper;

class ScannerEngineBootstrapperFactory {

  private final Logs logger;

  public ScannerEngineBootstrapperFactory(Logs logger) {
    this.logger = logger;
  }

  ScannerEngineBootstrapper create(Properties props, String isInvokedFrom) {
    String appName = "ScannerCLI";
    String appVersion = ScannerVersion.version();
    if (isInvokedFrom.contains("/")) {
      appName = isInvokedFrom.split("/")[0];
      appVersion = isInvokedFrom.split("/")[1];
    }

    return newScannerEngineBootstrapper(appName, appVersion)
      .addBootstrapProperties((Map) props);
  }

  ScannerEngineBootstrapper newScannerEngineBootstrapper(String appName, String appVersion) {
    return ScannerEngineBootstrapper.create(appName, appVersion, logger.getLogOutputAdapter());
  }


}
