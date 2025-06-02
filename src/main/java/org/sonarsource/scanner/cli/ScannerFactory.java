/*
 * SonarScanner CLI
 * Copyright (C) 2011-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.scanner.cli;

import java.util.Map;
import java.util.Properties;
import org.sonarsource.scanner.api.EmbeddedScanner;
import org.sonarsource.scanner.api.LogOutput;

class ScannerFactory {

  private final Logs logger;

  public ScannerFactory(Logs logger) {
    this.logger = logger;
  }

  EmbeddedScanner create(Properties props, String isInvokedFrom) {
    String appName = "ScannerCLI";
    String appVersion = ScannerVersion.version();
    if (!isInvokedFrom.equals("") && isInvokedFrom.contains("/")) {
      appName = isInvokedFrom.split("/")[0];
      appVersion = isInvokedFrom.split("/")[1];
    }

    return EmbeddedScanner.create(appName, appVersion, new DefaultLogOutput())
      .addGlobalProperties((Map) props);
  }

  class DefaultLogOutput implements LogOutput {
    @Override
    public void log(String formattedMessage, Level level) {
      switch (level) {
        case TRACE:
        case DEBUG:
          logger.debug(formattedMessage);
          break;
        case ERROR:
          logger.error(formattedMessage);
          break;
        case WARN:
          logger.warn(formattedMessage);
          break;
        case INFO:
        default:
          logger.info(formattedMessage);
      }
    }
  }
}
