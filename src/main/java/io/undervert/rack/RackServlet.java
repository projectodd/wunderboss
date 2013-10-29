package io.undervert.rack;

import io.undervert.RuntimeHelper;
import org.jboss.logging.Logger;
import org.jruby.Ruby;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.builtin.IRubyObject;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RackServlet implements Servlet {
    @Override
    public void init(ServletConfig config) throws ServletException {
        this.rackApplication = (IRubyObject) config.getServletContext().getAttribute("rack_application");
        this.runtime = this.rackApplication.getRuntime();
        RuntimeHelper.requireUnlessDefined(this.runtime, RackResponse.RESPONSE_HANDLER_RB, RackResponse.RESPONSE_HANDLER_CLASS_NAME);
    }

    @Override
    public ServletConfig getServletConfig() {
        return null;
    }

    @Override
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            service((HttpServletRequest) request, (HttpServletResponse) response);
        }
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (((request.getPathInfo() == null) || (request.getPathInfo().equals("/"))) && !(request.getRequestURI().endsWith("/"))) {
            String redirectUri = request.getRequestURI() + "/";
            String queryString = request.getQueryString();
            if (queryString != null) {
                redirectUri = redirectUri + "?" + queryString;
            }
            redirectUri = response.encodeRedirectURL(redirectUri);
            response.sendRedirect(redirectUri);
            return;
        }

        RackEnvironment rackEnv = null;

        try {
            rackEnv = new RackEnvironment(this.runtime, request);
            RackResponse rackResponse = new RackResponse((IRubyObject) RuntimeHelper.call(this.runtime,
                    this.rackApplication, "call", new Object[]{rackEnv.getEnv()}));
            rackResponse.respond(response);
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

    @Override
    public String getServletInfo() {
        return null;
    }

    @Override
    public void destroy() {
    }

    private Ruby runtime;
    private IRubyObject rackApplication;

    private static final Logger log = Logger.getLogger(RackServlet.class);

}
