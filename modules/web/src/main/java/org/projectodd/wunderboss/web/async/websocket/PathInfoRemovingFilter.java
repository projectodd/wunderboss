package org.projectodd.wunderboss.web.async.websocket;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

/**
 * This filter is used to force the JsrWebSocketFilter in
 * Wildfly to find the endpoint registered at the servlet path.
 * Endpoints are looked up for servletPath + pathInfo, but we can't
 * know all the pathInfos we need to register the endpoint under,
 * because they are handled by frameworks in ruby/clojure at the app-level.
 *
 * So, this filter sends a request down the chain that doesn't have a pathInfo
 * if the request is a websocket upgrade request.
 */
public class PathInfoRemovingFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest &&
                "websocket".equalsIgnoreCase(((HttpServletRequest) request).getHeader("Upgrade"))) {
            chain.doFilter(new PathInfoRemovingRequestFacade((HttpServletRequest)request), response);
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {

    }

    class PathInfoRemovingRequestFacade extends HttpServletRequestWrapper {
        public PathInfoRemovingRequestFacade(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getPathInfo() {
            return null;
        }
    }
}
