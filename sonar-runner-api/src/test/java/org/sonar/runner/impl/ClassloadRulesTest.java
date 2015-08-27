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
package org.sonar.runner.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.Before;

public class ClassloadRulesTest {
  private ClassloadRules rules;
  private Set<String> maskRules;
  private Set<String> unmaskRules;

  @Before
  public void setUp() {
    maskRules = new HashSet<>();
    unmaskRules = new HashSet<>();
  }

  @Test
  public void testUnmask() {
    unmaskRules.add("org.apache.ant.");
    rules = new ClassloadRules(maskRules, unmaskRules);

    assertThat(rules.canLoad("org.sonar.runner.Foo")).isFalse();
    assertThat(rules.canLoad("org.objectweb.asm.ClassVisitor")).isFalse();
    assertThat(rules.canLoad("org.apache")).isFalse();

    assertThat(rules.canLoad("org.apache.ant.Foo")).isTrue();
    assertThat(rules.canLoad("org.apache.ant.project.Project")).isTrue();
  }
  
  @Test
  public void testUnmaskAll() {
    unmaskRules.add("");
    rules = new ClassloadRules(maskRules, unmaskRules);
    
    assertThat(rules.canLoad("org.sonar.runner.Foo")).isTrue();
    assertThat(rules.canLoad("org.objectweb.asm.ClassVisitor")).isTrue();
    assertThat(rules.canLoad("org.apache")).isTrue();

    assertThat(rules.canLoad("org.apache.ant.Foo")).isTrue();
    assertThat(rules.canLoad("org.apache.ant.project.Project")).isTrue();
  }

  @Test
  public void testDefault() {
    rules = new ClassloadRules(maskRules, unmaskRules);
    assertThat(rules.canLoad("org.sonar.runner.Foo")).isFalse();
  }

  @Test
  public void testMaskAndUnmask() throws ClassNotFoundException {
    unmaskRules.add("org.apache.ant.");
    maskRules.add("org.apache.ant.foo.");
    rules = new ClassloadRules(maskRules, unmaskRules);

    assertThat(rules.canLoad("org.apache.ant.something")).isTrue();
    assertThat(rules.canLoad("org.apache.ant.foo.something")).isFalse();
    assertThat(rules.canLoad("org.apache")).isFalse();
  }
  
  @Test
  public void testUsedByMaven() {
    maskRules.add( "org.slf4j.LoggerFactory" );
    // Include slf4j Logger that is exposed by some Sonar components
    unmaskRules.add( "org.slf4j.Logger" );
    unmaskRules.add( "org.slf4j.ILoggerFactory" );
    // Exclude other slf4j classes
    // .unmask("org.slf4j.impl.")
    maskRules.add( "org.slf4j." );
    // Exclude logback
    maskRules.add( "ch.qos.logback." );
    maskRules.add( "org.sonar." );
    unmaskRules.add("org.sonar.runner.batch.");
    // Guava is not the same version in SonarQube classloader
    maskRules.add( "com.google.common" );
    // Include everything else
    unmaskRules.add( "" );
    
    rules = new ClassloadRules(maskRules, unmaskRules);
    
    assertThat(rules.canLoad("org.sonar.runner.batch.IsolatedLauncher")).isTrue();
  }
}
