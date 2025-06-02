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

import java.util.Scanner;

public enum ScannerVersion {

  INSTANCE;

  private String version;

  ScannerVersion() {
    try (Scanner scanner = new Scanner(getClass().getResourceAsStream("/version.txt"), "UTF-8")) {
      this.version = scanner.next();
    }
  }

  public static String version() {
    return INSTANCE.version;
  }

}
