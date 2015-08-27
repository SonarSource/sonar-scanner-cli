/*
 * SonarQube Runner - API
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
package org.sonar.runner.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Matchers.eq;

import static org.mockito.Matchers.startsWith;

import org.junit.Test;
import org.junit.Before;

public class LoggerAdapterTest {
  private LoggerAdapter adapter;
  private LogOutput logOutput;

  @Before
  public void setUp() {
    logOutput = mock(LogOutput.class);
    adapter = new LoggerAdapter(logOutput);
  }

  @Test
  public void testDebug() {
    adapter.debug("debug");
    verify(logOutput).log("debug", LogOutput.Level.DEBUG);
    verifyNoMoreInteractions(logOutput);
  }
  
  @Test
  public void testInfo() {
    adapter.info("info");
    verify(logOutput).log("info", LogOutput.Level.INFO);
    verifyNoMoreInteractions(logOutput);
  }
  
  @Test
  public void testWarn() {
    adapter.warn("warn");
    verify(logOutput).log("warn", LogOutput.Level.WARN);
    verifyNoMoreInteractions(logOutput);
  }
  
  @Test
  public void testError() {
    adapter.error("error");
    verify(logOutput).log("error", LogOutput.Level.ERROR);
    verifyNoMoreInteractions(logOutput);
  }
  
  @Test
  public void testErrorThrowable() {
    adapter.error("error", new IllegalStateException("error"));
    verify(logOutput).log(startsWith("error\njava.lang.IllegalStateException: error"), eq(LogOutput.Level.ERROR));
    verifyNoMoreInteractions(logOutput);
  }
}
