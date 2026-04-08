/*
 * SonarSource :: IT :: SonarScanner CLI
 * Copyright (C) SonarSource Sàrl
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
package com.sonarsource.scanner.it;

import com.sonar.orchestrator.util.NetworkUtils;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.DeferredAuthentication;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Authentication.User;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

class ProxyServer {

  // Static deques — needed so Jetty can instantiate TrackingProxyServlet via reflection
  // (no-arg constructor, so can't inject instance references). Safe because only one
  // proxy runs at a time and stop() clears them.
  private static final ConcurrentLinkedDeque<String> requestsSeenByProxy = new ConcurrentLinkedDeque<>();
  private static final ConcurrentLinkedDeque<String> connectRequestsSeenByProxy = new ConcurrentLinkedDeque<>();

  private final Server server;
  private final int port;

  private ProxyServer(Server server, int port) {
    this.server = server;
    this.port = port;
  }

  /** Starts an unauthenticated proxy. */
  static ProxyServer start() throws Exception {
    return start(false, null, null);
  }

  /** Starts a proxy requiring Basic auth (Proxy-Authorization) on all requests and CONNECT tunnels. */
  static ProxyServer start(String user, String password) throws Exception {
    return start(true, user, password);
  }

  private static ProxyServer start(boolean withProxyAuth, String user, String password) throws Exception {
    int port = NetworkUtils.getNextAvailablePort(InetAddress.getLocalHost());

    QueuedThreadPool threadPool = new QueuedThreadPool();
    threadPool.setMaxThreads(500);

    Server server = new Server(threadPool);

    HttpConfiguration httpConfig = new HttpConfiguration();
    httpConfig.setSecureScheme("https");
    httpConfig.setSendServerVersion(true);
    httpConfig.setSendDateHeader(false);

    TrackingConnectHandler connectHandler = new TrackingConnectHandler(withProxyAuth, user, password);
    connectHandler.setHandler(proxyHandler(withProxyAuth, user, password));

    HandlerCollection handlers = new HandlerCollection();
    handlers.setHandlers(new Handler[] {connectHandler, new DefaultHandler()});
    server.setHandler(handlers);

    ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
    http.setPort(port);
    server.addConnector(http);

    server.start();
    return new ProxyServer(server, port);
  }

  int getPort() {
    return port;
  }

  Collection<String> getRequestsSeenByProxy() {
    return requestsSeenByProxy;
  }

  Collection<String> getConnectRequestsSeenByProxy() {
    return connectRequestsSeenByProxy;
  }

  void stop() throws Exception {
    server.stop();
    requestsSeenByProxy.clear();
    connectRequestsSeenByProxy.clear();
  }

  private static ServletContextHandler proxyHandler(boolean withProxyAuth, String user, String password) {
    ServletContextHandler contextHandler = new ServletContextHandler();
    if (withProxyAuth) {
      contextHandler.setSecurityHandler(basicAuth(user, password, "Private!"));
    }
    contextHandler.setServletHandler(newServletHandler());
    return contextHandler;
  }

  private static SecurityHandler basicAuth(String username, String password, String realm) {
    HashLoginService l = new HashLoginService(realm);

    UserStore userStore = new UserStore();
    userStore.addUser(username, Credential.getCredential(password), new String[] {"user"});
    l.setUserStore(userStore);

    Constraint constraint = new Constraint();
    constraint.setName(Constraint.__BASIC_AUTH);
    constraint.setRoles(new String[] {"user"});
    constraint.setAuthenticate(true);

    ConstraintMapping cm = new ConstraintMapping();
    cm.setConstraint(constraint);
    cm.setPathSpec("/*");

    ConstraintSecurityHandler csh = new ConstraintSecurityHandler();
    csh.setAuthenticator(new ProxyAuthenticator());
    csh.setRealmName("myrealm");
    csh.addConstraintMapping(cm);
    csh.setLoginService(l);

    return csh;
  }

  private static ServletHandler newServletHandler() {
    ServletHandler handler = new ServletHandler();
    handler.addServletWithMapping(TrackingProxyServlet.class, "/*");
    return handler;
  }

  /**
   * ConnectHandler subclass that:
   * <ul>
   *   <li>Optionally requires {@code Proxy-Authorization} on CONNECT requests</li>
   *   <li>Records the host:port of every successfully-authenticated CONNECT</li>
   * </ul>
   * <p>
   * When authentication is required and credentials are missing, the handler sends a well-formed
   * {@code 407} response. This allows the JDK {@link java.net.Authenticator} to read the challenge,
   * supply credentials, and retry the CONNECT on a new connection.
   */
  private static class TrackingConnectHandler extends ConnectHandler {

    private final boolean requireAuth;
    private final String user;
    private final String password;

    TrackingConnectHandler(boolean requireAuth, String user, String password) {
      this.requireAuth = requireAuth;
      this.user = user;
      this.password = password;
    }

    @Override
    protected void handleConnect(org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request,
      HttpServletResponse response, String serverAddress) {
      if (requireAuth && !hasValidCredentials(request)) {
        response.setStatus(HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED);
        response.setHeader("Proxy-Authenticate", "Basic realm=\"proxy\"");
        response.setContentLength(0);
        baseRequest.setHandled(true);
        return;
      }
      connectRequestsSeenByProxy.add(serverAddress);
      super.handleConnect(baseRequest, request, response, serverAddress);
    }

    private boolean hasValidCredentials(HttpServletRequest request) {
      String credentials = request.getHeader("Proxy-Authorization");
      if (credentials != null && credentials.startsWith("Basic ")) {
        String decoded = new String(Base64.getDecoder().decode(credentials.substring(6)), StandardCharsets.ISO_8859_1);
        int colon = decoded.indexOf(':');
        if (colon > 0) {
          String reqUser = decoded.substring(0, colon);
          String reqPass = decoded.substring(colon + 1);
          return user.equals(reqUser) && password.equals(reqPass);
        }
      }
      return false;
    }
  }

  // Must stay public static for Jetty servlet reflection
  public static class TrackingProxyServlet extends ProxyServlet {

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      requestsSeenByProxy.add(request.getRequestURI());
      super.service(request, response);
    }

    @Override
    protected void sendProxyRequest(HttpServletRequest clientRequest, HttpServletResponse proxyResponse, Request proxyRequest) {
      super.sendProxyRequest(clientRequest, proxyResponse, proxyRequest);
    }
  }

  /**
   * Authenticator for HTTP forward proxy that reads {@code Proxy-Authorization} instead of the
   * standard {@code Authorization} header.
   * Inspired from Jetty's {@code BasicAuthenticator} but adapted for proxy auth.
   */
  private static class ProxyAuthenticator extends LoginAuthenticator {

    @Override
    public String getAuthMethod() {
      return Constraint.__BASIC_AUTH;
    }

    @Override
    public Authentication validateRequest(ServletRequest req, ServletResponse res, boolean mandatory) throws ServerAuthException {
      HttpServletRequest request = (HttpServletRequest) req;
      HttpServletResponse response = (HttpServletResponse) res;
      String credentials = request.getHeader(HttpHeader.PROXY_AUTHORIZATION.asString());

      try {
        if (!mandatory) {
          return new DeferredAuthentication(this);
        }

        if (credentials != null) {
          int space = credentials.indexOf(' ');
          if (space > 0) {
            String method = credentials.substring(0, space);
            if ("basic".equalsIgnoreCase(method)) {
              credentials = credentials.substring(space + 1);
              credentials = new String(Base64.getDecoder().decode(credentials), StandardCharsets.ISO_8859_1);
              int i = credentials.indexOf(':');
              if (i > 0) {
                String username = credentials.substring(0, i);
                String password = credentials.substring(i + 1);
                UserIdentity user = login(username, password, request);
                if (user != null) {
                  return new UserAuthentication(getAuthMethod(), user);
                }
              }
            }
          }
        }

        if (DeferredAuthentication.isDeferred(response)) {
          return Authentication.UNAUTHENTICATED;
        }

        response.setHeader(HttpHeader.PROXY_AUTHENTICATE.asString(), "basic realm=\"" + _loginService.getName() + '"');
        response.sendError(HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED);
        return Authentication.SEND_CONTINUE;
      } catch (IOException e) {
        throw new ServerAuthException(e);
      }
    }

    @Override
    public boolean secureResponse(ServletRequest req, ServletResponse res, boolean mandatory, User validatedUser) throws ServerAuthException {
      return true;
    }
  }
}
