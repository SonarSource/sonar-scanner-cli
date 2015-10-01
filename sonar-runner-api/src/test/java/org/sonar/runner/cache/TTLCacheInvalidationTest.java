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
package org.sonar.runner.cache;

import org.junit.Before;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class TTLCacheInvalidationTest {
  private Path testFile;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void setUp() throws IOException {
    testFile = temp.newFile().toPath();
  }

  @Test
  public void testExpired() throws IOException {
    TTLCacheInvalidation invalidation = new TTLCacheInvalidation(-100);
    assertThat(invalidation.test(testFile)).isEqualTo(true);
  }

  @Test
  public void testValid() throws IOException {
    TTLCacheInvalidation invalidation = new TTLCacheInvalidation(100_000);
    assertThat(invalidation.test(testFile)).isEqualTo(false);
  }
}
