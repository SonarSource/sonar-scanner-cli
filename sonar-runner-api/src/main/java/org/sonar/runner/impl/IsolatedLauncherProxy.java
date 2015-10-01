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

import org.sonar.runner.cache.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;

public class IsolatedLauncherProxy implements InvocationHandler {
  private final Object proxied;
  private final ClassLoader cl;
  private final Logger logger;

  private IsolatedLauncherProxy(ClassLoader cl, Object proxied, Logger logger) {
    this.cl = cl;
    this.proxied = proxied;
    this.logger = logger;
  }

  public static <T> T create(ClassLoader cl, Class<T> interfaceClass, String proxiedClassName, Logger logger) throws ReflectiveOperationException {
    Object proxied = createProxiedObject(cl, proxiedClassName);
    // interfaceClass needs to be loaded with a parent ClassLoader (common to both ClassLoaders)
    // In addition, Proxy.newProxyInstance checks if the target ClassLoader sees the same class as the one given
    Class<?> loadedInterfaceClass = cl.loadClass(interfaceClass.getName());
    return (T) create(cl, proxied, loadedInterfaceClass, logger);
  }

  public static <T> T create(ClassLoader cl, Object proxied, Class<T> interfaceClass, Logger logger) {
    Class<?>[] c = {interfaceClass};
    return (T) Proxy.newProxyInstance(cl, c, new IsolatedLauncherProxy(cl, proxied, logger));
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    ClassLoader initialContextClassLoader = Thread.currentThread().getContextClassLoader();

    try {
      Thread.currentThread().setContextClassLoader(cl);
      logger.debug("Execution " + method.getName());
      return method.invoke(proxied, args);
    } catch (UndeclaredThrowableException | InvocationTargetException e) {
      throw unwrapException(e);
    } finally {
      Thread.currentThread().setContextClassLoader(initialContextClassLoader);
    }
  }

  private static Throwable unwrapException(Throwable e) {
    Throwable cause = e;

    while (cause.getCause() != null) {
      if (cause instanceof UndeclaredThrowableException || cause instanceof InvocationTargetException) {
        cause = cause.getCause();
      } else {
        break;
      }
    }
    return cause;
  }

  private static Object createProxiedObject(ClassLoader cl, String proxiedClassName) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    Class<?> proxiedClass = cl.loadClass(proxiedClassName);
    return proxiedClass.newInstance();
  }
}
