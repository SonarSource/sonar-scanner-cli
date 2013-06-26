/*
 * SonarQube Runner - Batch
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
package org.sonar.runner.batch;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class VersionUtilsTest {

  @Test
  public void testIsLessThan3_7() {
    assertThat(VersionUtils.isLessThan37("2.5")).isTrue();
    assertThat(VersionUtils.isLessThan37("3.0")).isTrue();
    assertThat(VersionUtils.isLessThan37("3.0.1")).isTrue();
    assertThat(VersionUtils.isLessThan37("3.6")).isTrue();
    assertThat(VersionUtils.isLessThan37("3.6-SNAPSHOT")).isTrue();
    assertThat(VersionUtils.isLessThan37("3.7")).isFalse();
    assertThat(VersionUtils.isLessThan37("4.0")).isFalse();
  }

}
