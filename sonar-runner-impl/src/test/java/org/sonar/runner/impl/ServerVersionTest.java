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

import org.junit.Test;

import java.net.ConnectException;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ServerVersionTest {

  ServerConnection connection = mock(ServerConnection.class);
  ServerVersion version = new ServerVersion(connection);

  @Test
  public void should_fail_on_connection_error() throws Exception {
    when(connection.downloadString("/api/server/version")).thenThrow(new ConnectException());
    try {
      version.version();
      fail();
    } catch (IllegalStateException e) {
      // success
    }
  }

  @Test
  public void should_fail_if_bad_version() throws Exception {
    when(connection.downloadString("/api/server/version")).thenReturn("");
    try {
      version.version();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Server version is not set");
    }
  }

  @Test
  public void test_2_x() throws Exception {
    when(connection.downloadString("/api/server/version")).thenReturn("2.10");
    assertThat(version.version()).isEqualTo("2.10");
    assertThat(version.is30Compatible()).isFalse();
    assertThat(version.is35Compatible()).isFalse();
    verify(connection, times(1)).downloadString("/api/server/version");
  }

  @Test
  public void test_3_0() throws Exception {
    when(connection.downloadString("/api/server/version")).thenReturn("3.0");
    assertThat(version.version()).isEqualTo("3.0");
    assertThat(version.is30Compatible()).isTrue();
    assertThat(version.is35Compatible()).isFalse();
    verify(connection, times(1)).downloadString("/api/server/version");
  }

  @Test
  public void test_3_1() throws Exception {
    when(connection.downloadString("/api/server/version")).thenReturn("3.1");
    assertThat(version.version()).isEqualTo("3.1");
    assertThat(version.is30Compatible()).isTrue();
    assertThat(version.is35Compatible()).isFalse();
    verify(connection, times(1)).downloadString("/api/server/version");
  }

  @Test
  public void test_3_5() throws Exception {
    when(connection.downloadString("/api/server/version")).thenReturn("3.5");
    assertThat(version.version()).isEqualTo("3.5");
    assertThat(version.is30Compatible()).isTrue();
    assertThat(version.is35Compatible()).isTrue();
    verify(connection, times(1)).downloadString("/api/server/version");
  }

  @Test
  public void test_3_5_1() throws Exception {
    when(connection.downloadString("/api/server/version")).thenReturn("3.5.1");
    assertThat(version.version()).isEqualTo("3.5.1");
    assertThat(version.is30Compatible()).isTrue();
    assertThat(version.is35Compatible()).isTrue();
    verify(connection, times(1)).downloadString("/api/server/version");
  }

  @Test
  public void test_3_6() throws Exception {
    when(connection.downloadString("/api/server/version")).thenReturn("3.6");
    assertThat(version.version()).isEqualTo("3.6");
    assertThat(version.is30Compatible()).isTrue();
    assertThat(version.is35Compatible()).isTrue();
    verify(connection, times(1)).downloadString("/api/server/version");
  }

  @Test
  public void test_4_0() throws Exception {
    when(connection.downloadString("/api/server/version")).thenReturn("4.0");
    assertThat(version.version()).isEqualTo("4.0");
    assertThat(version.is30Compatible()).isTrue();
    assertThat(version.is35Compatible()).isTrue();
    verify(connection, times(1)).downloadString("/api/server/version");
  }
}
