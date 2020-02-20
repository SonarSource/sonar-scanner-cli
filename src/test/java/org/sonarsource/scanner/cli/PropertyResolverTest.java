/*
 * SonarQube Scanner
 * Copyright (C) 2011-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.scanner.cli;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertyResolverTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void resolve_properties() {
    Properties map = new Properties();
    Map<String, String> env = new HashMap<>();

    // resolve
    map.put("A", "value a");
    map.put("B", "value b");
    map.put("C", "${A} ${B} ${nonexisting}");

    PropertyResolver resolver = new PropertyResolver(map, env);
    Properties resolved = resolver.resolve();
    assertThat(resolved.get("A")).isEqualTo("value a");
    assertThat(resolved.get("B")).isEqualTo("value b");
    assertThat(resolved.get("C")).isEqualTo("value a value b ");

    map.clear();
    map.put("sonar.login", "admin");
    map.put("sonar.password", "${sonar.login}");

    resolver = new PropertyResolver(map, env);
    resolved = resolver.resolve();
    assertThat(resolved.get("sonar.password")).isEqualTo("admin");
  }

  @Test
  public void use_env() {
    Properties map = new Properties();
    Map<String, String> env = new HashMap<>();

    // resolve
    map.put("A", "invalid");
    map.put("B", "value b");
    map.put("C", "${env.A} ${B} ${nonexisting}");
    env.put("A", "value a");

    PropertyResolver resolver = new PropertyResolver(map, env);
    Properties resolved = resolver.resolve();
    assertThat(resolved.get("A")).isEqualTo("invalid");
    assertThat(resolved.get("B")).isEqualTo("value b");
    assertThat(resolved.get("C")).isEqualTo("value a value b ");
  }

  @Test
  public void resolve_recursively() {
    Properties map = new Properties();
    Map<String, String> env = new HashMap<>();
    map.put("A", "value a");
    map.put("B", "${A}");
    map.put("C", "${A} ${B}");

    PropertyResolver resolver = new PropertyResolver(map, env);
    Properties resolved = resolver.resolve();
    assertThat(resolved.get("A")).isEqualTo("value a");
    assertThat(resolved.get("B")).isEqualTo("value a");
    assertThat(resolved.get("C")).isEqualTo("value a value a");
  }

  @Test
  public void dont_resolve_nested() {
    Properties map = new Properties();
    Map<String, String> env = new HashMap<>();
    map.put("A", "value a");
    map.put("B", "value b");
    map.put("C", "${A ${B}}");

    PropertyResolver resolver = new PropertyResolver(map, env);
    Properties resolved = resolver.resolve();
    assertThat(resolved.get("A")).isEqualTo("value a");
    assertThat(resolved.get("B")).isEqualTo("value b");
    assertThat(resolved.get("C")).isEqualTo("${A value b}");
  }

  @Test
  public void missing_var() {
    Map<String, String> env = new HashMap<>();
    Properties map = new Properties();
    map.put("A", "/path/${missing} var/");

    PropertyResolver resolver = new PropertyResolver(map, env);
    Properties resolved = resolver.resolve();
    assertThat(resolved.get("A")).isEqualTo("/path/ var/");

  }

  @Test
  public void fail_loop_properties_resolution() {
    Properties map = new Properties();
    Map<String, String> env = new HashMap<>();

    // resolve
    map.put("A", "${B}");
    map.put("B", "${A}");

    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("variable: B");
    PropertyResolver resolver = new PropertyResolver(map, env);
    resolver.resolve();
  }

  @Test
  public void preserve_empty() {
    Properties map = new Properties();
    Map<String, String> env = new HashMap<>();

    map.put("A", "");
    map.put("B", "${A}");

    PropertyResolver resolver = new PropertyResolver(map, env);
    Properties resolved = resolver.resolve();
    assertThat(resolved.get("A")).isEqualTo("");
    assertThat(resolved.get("B")).isEqualTo("");
  }
}
