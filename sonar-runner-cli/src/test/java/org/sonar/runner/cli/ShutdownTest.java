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

import static org.mockito.Mockito.verify;
import static org.fest.assertions.Assertions.assertThat;
import static com.jayway.awaitility.Awaitility.await;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.jayway.awaitility.Duration;
import org.mockito.MockitoAnnotations;
import org.sonar.runner.cli.Exit;
import org.sonar.runner.cli.Shutdown;
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

    final Thread t = new HookCaller();
    t.start();

    await().atMost(Duration.TWO_SECONDS).pollDelay(50, TimeUnit.MILLISECONDS).until(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return t.isAlive();
      }
    });

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
