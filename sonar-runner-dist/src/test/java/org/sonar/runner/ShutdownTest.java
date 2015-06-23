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

import static org.mockito.Mockito.verify;
import static org.fest.assertions.Assertions.assertThat;
import org.mockito.MockitoAnnotations;
import org.mockito.Mock;
import org.junit.Test;
import org.junit.Before;

public class ShutdownTest {
  @Mock
  private Exit exit;
  private Shutdown shutdown;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    shutdown = new Shutdown(exit);
  }

  @Test
  public void testShutdown() {
    shutdown.exit(3);
    verify(exit).exit(3);
  }

  @Test(timeout = 60_000)
  public void testWaitReady() throws InterruptedException {
    shutdown = new Shutdown(exit, 100_000);
    shutdown.signalReady(false);
    assertThat(shutdown.shouldExit()).isFalse();
    
    Thread t = new HookCaller();
    t.start();
    Thread.sleep(1000);
    
    assertThat(t.isAlive()).isTrue();
    assertThat(shutdown.shouldExit()).isTrue();

    shutdown.signalReady(true);
    t.join();
  }

  @Test(timeout = 60_000)
  public void testTimeout() throws InterruptedException {
    shutdown = new Shutdown(exit, 0);
    
    Thread t = new HookCaller();
    t.start();
    t.join();
  }

  private class HookCaller extends Thread {
    @Override
    public void run() {
      shutdown.hook.run();
    }
  }
}
