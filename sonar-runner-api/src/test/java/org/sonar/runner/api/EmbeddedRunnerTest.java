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
package org.sonar.runner.api;

import org.junit.Test;
import org.sonar.runner.impl.Constants;

import static org.fest.assertions.Assertions.assertThat;

public class EmbeddedRunnerTest {
  @Test
  public void should_create() {
    assertThat(EmbeddedRunner.create()).isNotNull().isInstanceOf(EmbeddedRunner.class);
  }

  @Test
  public void should_set_unmasked_packages() {
    EmbeddedRunner runner = EmbeddedRunner.create();
    assertThat(runner.property(Constants.UNMASKED_PACKAGES, null)).isNull();

    runner = EmbeddedRunner.create().setUnmaskedPackages("org.apache.ant", "org.ant");
    assertThat(runner.property(Constants.UNMASKED_PACKAGES, null)).isEqualTo("org.apache.ant,org.ant");
  }

  @Test
  public void should_add_extensions() {
    EmbeddedRunner runner = EmbeddedRunner.create();
    assertThat(runner.extensions()).isEmpty();

    FakeExtension fakeExtension = new FakeExtension();
    runner.addExtensions(fakeExtension);
    assertThat(runner.extensions()).containsExactly(fakeExtension);
  }

  static class FakeExtension {
  }
}
