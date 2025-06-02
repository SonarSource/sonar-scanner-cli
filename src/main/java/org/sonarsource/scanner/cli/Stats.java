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

class Stats {
  private final Logs logger;
  private long startTime;

  Stats(Logs logger) {
    this.logger = logger;
  }

  Stats start() {
    startTime = System.currentTimeMillis();
    return this;
  }

  Stats stop() {
    long stopTime = System.currentTimeMillis() - startTime;
    logger.info("Total time: " + formatTime(stopTime));

    System.gc();
    Runtime r = Runtime.getRuntime();
    long mb = 1024L * 1024;
    logger.info("Final Memory: " + (r.totalMemory() - r.freeMemory()) / mb + "M/" + r.totalMemory() / mb + "M");

    return this;
  }

  static String formatTime(long time) {
    long h = time / (60 * 60 * 1000);
    long m = (time - h * 60 * 60 * 1000) / (60 * 1000);
    long s = (time - h * 60 * 60 * 1000 - m * 60 * 1000) / 1000;
    long ms = time % 1000;
    final String format;
    if (h > 0) {
      format = "%1$d:%2$02d:%3$02d.%4$03ds";
    } else if (m > 0) {
      format = "%2$d:%3$02d.%4$03ds";
    } else {
      format = "%3$d.%4$03ds";
    }
    return String.format(format, h, m, s, ms);
  }
}
