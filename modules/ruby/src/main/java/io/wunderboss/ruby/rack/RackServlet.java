package io.wunderboss.ruby.rack;

import io.undertow.servlet.spec.HttpServletRequestImpl;
import io.undertow.servlet.spec.HttpServletResponseImpl;
import io.wunderboss.ruby.RuntimeHelper;
import org.jboss.logging.Logger;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Helpers;
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
        rackApplication = (IRubyObject) config.getServletContext().getAttribute("rack_application");
        runtime = this.rackApplication.getRuntime();
        RuntimeHelper.requireUnlessDefined(runtime, RESPONSE_HANDLER_RB, RESPONSE_HANDLER_CLASS_NAME);
        responseModule = RuntimeHelper.getClass(runtime, RESPONSE_HANDLER_CLASS_NAME);
        rackChannelClass = RackChannel.createRackChannelClass(runtime);
        try {
            rackEnvironment = new RackEnvironment(runtime);
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

        RackChannel inputChannel = null;
        try {
            inputChannel = new RackChannel(runtime, rackChannelClass, request.getInputStream());
            RubyHash rackEnvHash = rackEnvironment.getEnv(request, inputChannel);
            IRubyObject rackResponse = rackApplication.callMethod(runtime.getCurrentContext(), "call", rackEnvHash);
            IRubyObject servletResponse = JavaUtil.convertJavaToUsableRubyObject(runtime, response);
            Helpers.invoke(runtime.getCurrentContext(), responseModule,
                    RESPONSE_HANDLER_METHOD_NAME, rackResponse, servletResponse);
        } catch (RaiseException e) {
            throw new ServletException(e);
        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            if (inputChannel != null) {
                inputChannel.close();
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
    private RubyClass rackChannelClass;
    private RackEnvironment rackEnvironment;

    public static final String RESPONSE_HANDLER_RB = "io/wunderboss/ruby/rack/response_handler";
    public static final String RESPONSE_HANDLER_CLASS_NAME = "WunderBoss::Rack::ResponseHandler";
    public static final String RESPONSE_HANDLER_METHOD_NAME = "handle";

    private static final Logger log = Logger.getLogger(RackServlet.class);

}
