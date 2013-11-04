package io.wunderboss.rack;

import io.wunderboss.RuntimeHelper;
import org.jboss.logging.Logger;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.builtin.IRubyObject;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RackServlet implements Servlet {
    public static final String RESPONSE_HANDLER_RB = "io/wunderboss/rack/response_handler";
    public static final String RESPONSE_HANDLER_CLASS_NAME = "WunderBoss::Rack::ResponseHandler";
    public static final String RESPONSE_HANDLER_METHOD_NAME = "handle";

    @Override
    public void init(ServletConfig config) throws ServletException {
        this.rackApplication = (IRubyObject) config.getServletContext().getAttribute("rack_application");
        this.runtime = this.rackApplication.getRuntime();
        RuntimeHelper.requireUnlessDefined(this.runtime, RESPONSE_HANDLER_RB, RESPONSE_HANDLER_CLASS_NAME);
        this.responseModule = RuntimeHelper.getClass(this.runtime, RESPONSE_HANDLER_CLASS_NAME);
    }

    @Override
    public ServletConfig getServletConfig() {
        return null;
    }

    @Override
    public final void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            service((HttpServletRequest) request, (HttpServletResponse) response);
        }
    }

    public final void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (((request.getPathInfo() == null) || (request.getPathInfo().equals("/"))) && !(request.getRequestURI().endsWith("/"))) {
            redirectToTrailingSlash(request, response);
            return;
        }

        RackEnvironment rackEnv = null;

        try {
            rackEnv = new RackEnvironment(this.runtime, request);
            IRubyObject rackResponse = (IRubyObject) RuntimeHelper.call(this.runtime,
                    this.rackApplication, "call", new Object[]{rackEnv.getEnv()});
            RuntimeHelper.call(this.runtime, this.responseModule, RESPONSE_HANDLER_METHOD_NAME,
                    new Object[] { rackResponse, response});
        } catch (RaiseException e) {
            throw new ServletException(e);
        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            if (rackEnv != null) {
                rackEnv.close();
            }
        }
    }

    private void redirectToTrailingSlash(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String redirectUri = request.getRequestURI() + "/";
        String queryString = request.getQueryString();
        if (queryString != null) {
            redirectUri = redirectUri + "?" + queryString;
        }
        redirectUri = response.encodeRedirectURL(redirectUri);
        response.sendRedirect(redirectUri);
    }

    @Override
    public String getServletInfo() {
        return null;
    }

    @Override
    public void destroy() {
    }

    private Ruby runtime;
    private IRubyObject rackApplication;
    private RubyModule responseModule;

    private static final Logger log = Logger.getLogger(RackServlet.class);

}
