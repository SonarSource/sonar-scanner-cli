/*
 * Sonar Runner - Implementation
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
package org.sonar.runner.impl;


import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.List;

class NetworkUtil {
  static int getNextAvailablePort() {
    for (int index = 0; index < 5; index++) {
      try {
        ServerSocket socket = new ServerSocket(0);
        int unusedPort = socket.getLocalPort();
        socket.close();
        if (isValidPort(unusedPort)) {
          return unusedPort;
        }

      } catch (IOException e) {
        throw new IllegalStateException("Can not find an open network port", e);
      }
    }
    throw new IllegalStateException("Can not find an open network port");
  }

  // Firefox blocks some reserverd ports : http://www-archive.mozilla.org/projects/netlib/PortBanning.html
  private static final List<Integer> BLOCKED_PORTS = Arrays.asList(2049, 4045, 6000);

  static boolean isValidPort(int port) {
    return port > 1023 && !BLOCKED_PORTS.contains(port);
  }
}
