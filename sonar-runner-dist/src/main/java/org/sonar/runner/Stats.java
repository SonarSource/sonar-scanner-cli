/*
 * SonarQube Runner - Distribution
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
package org.sonar.runner;

import org.sonar.runner.impl.Logs;

class Stats {
  private long startTime;

  Stats() {
  }

  Stats start() {
    startTime = System.currentTimeMillis();
    return this;
  }

  Stats stop() {
    long stopTime = System.currentTimeMillis() - startTime;
    Logs.info("Total time: " + formatTime(stopTime));

    System.gc();
    Runtime r = Runtime.getRuntime();
    long mb = 1024L * 1024;
    Logs.info("Final Memory: " + (r.totalMemory() - r.freeMemory()) / mb + "M/" + r.totalMemory() / mb + "M");

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
