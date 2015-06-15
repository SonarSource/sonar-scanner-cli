/*
 * SonarQube Runner - Implementation
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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import org.sonar.runner.batch.BatchIsolatedLauncher;
import org.junit.Test;
import org.junit.Before;

public class IsolatedLauncherProxyTest {
  BatchIsolatedLauncher laucherProxy = null;
  ClassLoader cl = null;

  @Before
  public void setUp() {
    cl = new URLClassLoader(new URL[0], Thread.currentThread().getContextClassLoader());
  }

  @Test
  public void delegate_proxied() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    String str = "test";
    CharSequence s = IsolatedLauncherProxy.create(cl, str, CharSequence.class);
    assertThat(s).isEqualTo(str);
  }

  @Test(expected = IllegalStateException.class)
  public void exceptions_unwrapped() throws ReflectiveOperationException {
    Runnable r = IsolatedLauncherProxy.create(cl, Runnable.class, ExceptionThrower.class.getName());
    r.run();
  }

  @Test
  public void create_proxied() throws Exception {
    Callable<?> c = IsolatedLauncherProxy.create(cl, Callable.class, SimpleClass.class.getName());
    assertThat(c.getClass().getClassLoader()).isEqualTo(cl);
    assertThat(c.getClass().getClassLoader()).isNotEqualTo(Thread.currentThread().getContextClassLoader());
    assertThat(c.call()).isEqualTo(URLClassLoader.class.getSimpleName());
  }

  public static class ExceptionThrower implements Runnable {
    @Override
    public void run() {
      throw new IllegalStateException("message");
    }
  }

  public static class SimpleClass implements Callable<String> {
    @Override
    public String call() throws Exception {
      return Thread.currentThread().getContextClassLoader().getClass().getSimpleName();
    }
  }
}
