package io.wunderboss.ruby.rack;

import io.undertow.servlet.spec.HttpServletRequestImpl;
import io.undertow.servlet.spec.HttpServletResponseImpl;
import io.wunderboss.ruby.RuntimeHelper;
import org.jboss.logging.Logger;
import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.builtin.IRubyObject;

import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RackServlet extends GenericServlet {

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.rackApplication = (IRubyObject) config.getServletContext().getAttribute("rack_application");
        this.runtime = this.rackApplication.getRuntime();
        RuntimeHelper.requireUnlessDefined(this.runtime, RESPONSE_HANDLER_RB, RESPONSE_HANDLER_CLASS_NAME);
        this.responseModule = RuntimeHelper.getClass(this.runtime, RESPONSE_HANDLER_CLASS_NAME);
        try {
            this.rackEnvironment = new RackEnvironment(this.runtime);
        } catch (IOException e) {
            throw new ServletException(e);
        }
    }

    @Override
    public final void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        if (request instanceof HttpServletRequestImpl && response instanceof HttpServletResponseImpl) {
            service((HttpServletRequestImpl) request, (HttpServletResponseImpl) response);
        }
    }

    public final void service(HttpServletRequestImpl request, HttpServletResponseImpl response) throws ServletException, IOException {
        if (((request.getPathInfo() == null) || (request.getPathInfo().equals("/"))) && !(request.getRequestURI().endsWith("/"))) {
            redirectToTrailingSlash(request, response);
            return;
        }

        RubyHash rackEnvHash = null;
        try {
            rackEnvHash = this.rackEnvironment.getEnv(request);
//            IRubyObject rackResponse = (IRubyObject) RuntimeHelper.call(this.runtime,
//                    this.rackApplication, "call", new Object[]{rackEnvHash});
//            RuntimeHelper.call(this.runtime, this.responseModule, RESPONSE_HANDLER_METHOD_NAME,
//                    new Object[]{rackResponse, response});
            IRubyObject rackResponse = this.rackApplication.callMethod(this.runtime.getCurrentContext(), "call", rackEnvHash);
            IRubyObject[] rubyObjects = new IRubyObject[2];
            rubyObjects[0] = rackResponse;
            rubyObjects[1] = JavaUtil.convertJavaToUsableRubyObject(this.runtime, response);
            this.responseModule.callMethod(this.runtime.getCurrentContext(), RESPONSE_HANDLER_METHOD_NAME, rubyObjects);
        } catch (RaiseException e) {
            throw new ServletException(e);
        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            if (rackEnvHash != null) {
                RackChannel input = (RackChannel) rackEnvHash.get("rack.input");
                if (input != null) {
                    input.close();
                }
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

    private Ruby runtime;
    private IRubyObject rackApplication;
    private RubyModule responseModule;
    private RackEnvironment rackEnvironment;

    public static final String RESPONSE_HANDLER_RB = "io/wunderboss/ruby/rack/response_handler";
    public static final String RESPONSE_HANDLER_CLASS_NAME = "WunderBoss::Rack::ResponseHandler";
    public static final String RESPONSE_HANDLER_METHOD_NAME = "handle";

    private static final Logger log = Logger.getLogger(RackServlet.class);

}
