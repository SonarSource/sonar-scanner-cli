/*
 * Sonar Runner - API
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.fest.assertions.Assertions.assertThat;

public class LogsTest {

  private PrintStream oldSysout;
  private PrintStream oldSyserr;

  private ByteArrayOutputStream baosOut;
  private ByteArrayOutputStream baosErr;

  @Before
  public void prepare() {
    oldSysout = System.out;
    oldSyserr = System.err;
    baosOut = new ByteArrayOutputStream();
    System.setOut(new PrintStream(baosOut));
    baosErr = new ByteArrayOutputStream();
    System.setErr(new PrintStream(baosErr));
  }

  @After
  public void restore() {
    System.setOut(oldSysout);
    System.setErr(oldSyserr);
  }

  @Test
  public void shouldLogInfo() {
    Logs.info("info");
    assertThat(baosOut.toString()).contains("INFO: info");
    assertThat(baosErr.toString()).isEmpty();
  }

  @Test
  public void shouldLogError() {
    Logs.error("error");
    assertThat(baosOut.toString()).isEmpty();
    assertThat(baosErr.toString()).contains("ERROR: error");
  }

  @Test
  public void shouldLogErrorWithoutThrowable() {
    Logs.error("error", null);
    assertThat(baosOut.toString()).isEmpty();
    assertThat(baosErr.toString()).contains("ERROR: error");
  }

  @Test
  public void shouldLogErrorWithThrowable() {
    Logs.error("error", new RuntimeException());
    assertThat(baosOut.toString()).isEmpty();
    assertThat(baosErr.toString()).contains("ERROR: error").contains("RuntimeException");
  }

}
