/*
 * SonarQube Runner - CLI - Distribution
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
package org.sonar.runner.cli;

class Shutdown {
  static final int SUCCESS = 0;
  static final int ERROR = 1;
  private static final long DEFAULT_MAX_WAIT = 10_000;

  private long maxWait;
  ShutdownHook hook = new ShutdownHook();
  private boolean isReady = false;
  private boolean exiting = false;
  private Object lock = new Object();
  private Exit exit;

  Shutdown(Exit exit, boolean isInteractive) {
    this(exit, isInteractive, DEFAULT_MAX_WAIT);
  }

  Shutdown(Exit exit, boolean isInteractive, long maxWait) {
    this.maxWait = maxWait;
    this.exit = exit;
    if (isInteractive) {
      Runtime.getRuntime().addShutdownHook(hook);
    }
  }

  void exit(int status) {
    synchronized (lock) {
      signalReady(true);
    }
    exit.exit(status);
  }

  void signalReady(boolean ready) {
    synchronized (lock) {
      this.isReady = ready;
      lock.notifyAll();
    }
  }

  boolean shouldExit() {
    synchronized (lock) {
      return exiting;
    }
  }

  class ShutdownHook extends Thread {
    private ShutdownHook() {
      this.setName("shutdown-hook");
    }

    @Override
    public void run() {
      long startTime = System.currentTimeMillis();
      synchronized (lock) {
        exiting = true;

        while (!isReady) {
          long waitTime = startTime + maxWait - System.currentTimeMillis();
          if (waitTime <= 0) {
            break;
          }

          try {
            lock.wait(waitTime);
          } catch (InterruptedException e) {
            // continue
          }
        }
      }
    }
  }
}
