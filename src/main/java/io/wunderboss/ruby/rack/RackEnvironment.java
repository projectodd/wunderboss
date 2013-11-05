package io.wunderboss.ruby.rack;

import io.undertow.servlet.spec.HttpServletRequestImpl;
import org.jboss.logging.Logger;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyIO;
import org.jruby.RubyString;

import java.io.IOException;
import java.util.Enumeration;

public class RackEnvironment {

    public RackEnvironment(Ruby ruby) throws IOException {
        this.ruby = ruby;
        this.rackVersion = RubyArray.newArray(ruby);
        this.rackVersion.add(RubyFixnum.one(ruby));
        this.rackVersion.add(RubyFixnum.one(ruby));
        this.errors = new RubyIO(this.ruby, this.ruby.getErr());
        this.errors.setAutoclose(false);
    }

    public RubyHash getEnv(HttpServletRequestImpl request) throws IOException {
        RubyHash env = new RubyHash(this.ruby);

//        StreamSourceChannel inputChannel;
//        if(request.getExchange().isRequestChannelAvailable()) {
//            inputChannel = request.getExchange().getRequestChannel();
//        } else {
//            inputChannel = new EmptyStreamSourceChannel(request.getExchange().getIoThread());
//        }

        // Wrap the input stream in a RewindableChannel because Rack expects
        // 'rack.input' to be rewindable and a ServletInputStream is not
//        RewindableChannel rewindableChannel = new RewindableChannel(inputChannel);
//        RubyIO input = new RubyIO(this.ruby, rewindableChannel);
//        input.binmode();
//        input.setAutoclose(false);
        env.put(RubyString.newString(this.ruby, "rack.input"), new RackChannel(this.ruby));
        env.put(RubyString.newString(this.ruby, "rack.errors"), this.errors);

        // Don't use request.getPathInfo because that gets decoded by the container
        String pathInfo = request.getRequestURI();
        if (pathInfo.startsWith(request.getContextPath())) {
            pathInfo = pathInfo.substring(request.getContextPath().length());
        }
        if (pathInfo.startsWith(request.getServletPath())) {
            pathInfo = pathInfo.substring(request.getServletPath().length());
        }

        env.put(RubyString.newString(this.ruby, "REQUEST_METHOD"), RubyString.newString(this.ruby, request.getMethod()));
        env.put(RubyString.newString(this.ruby, "SCRIPT_NAME"), RubyString.newString(this.ruby,
                request.getContextPath() + request.getServletPath()));
        env.put(RubyString.newString(this.ruby, "PATH_INFO"), RubyString.newString(this.ruby, pathInfo));
        env.put(RubyString.newString(this.ruby, "QUERY_STRING"), RubyString.newString(this.ruby,
                request.getQueryString() == null ? "" : request.getQueryString()));
        env.put(RubyString.newString(this.ruby, "SERVER_NAME"), RubyString.newString(this.ruby, request.getServerName()));
        env.put(RubyString.newString(this.ruby, "SERVER_PORT"), RubyString.newString(this.ruby, request.getServerPort() + ""));
        env.put(RubyString.newString(this.ruby, "CONTENT_TYPE"), RubyString.newString(this.ruby, request.getContentType() + ""));
        env.put(RubyString.newString(this.ruby, "REQUEST_URI"), RubyString.newString(this.ruby,
                request.getContextPath() + request.getServletPath() + pathInfo));
        env.put(RubyString.newString(this.ruby, "REMOTE_ADDR"), RubyString.newString(this.ruby, request.getRemoteAddr()));
        env.put(RubyString.newString(this.ruby, "rack.url_scheme"), RubyString.newString(this.ruby, request.getScheme()));
        env.put(RubyString.newString(this.ruby, "rack.version"), this.rackVersion);
        env.put(RubyString.newString(this.ruby, "rack.multithread"), RubyBoolean.newBoolean(this.ruby, true));
        env.put(RubyString.newString(this.ruby, "rack.multiprocess"), RubyBoolean.newBoolean(this.ruby, true));
        env.put(RubyString.newString(this.ruby, "rack.run_once"), RubyBoolean.newBoolean(this.ruby, false));

        if (request.getContentLength() >= 0) {
            env.put(RubyString.newString(this.ruby, "CONTENT_LENGTH"), RubyFixnum.newFixnum(this.ruby, request.getContentLength()));
        }

        if ("https".equals(request.getScheme())) {
            env.put(RubyString.newString(this.ruby, "HTTPS"), RubyString.newString(this.ruby, "on"));
        }

        if (request.getHeaderNames() != null) {
            for (Enumeration<String> headerNames = request.getHeaderNames(); headerNames.hasMoreElements();) {
                String headerName = headerNames.nextElement();
                String envName = "HTTP_" + headerName.toUpperCase().replace('-', '_');

                String value = request.getHeader(headerName);

                env.put(RubyString.newString(this.ruby, envName), RubyString.newString(this.ruby, value));
            }
        }

        //env.put(RubyString.newString(this.ruby, "servlet_request"), request);
        //env.put(RubyString.newString(this.ruby, "java.servlet_request"), request);

        if (log.isTraceEnabled()) {
            log.trace("Created: " + env.inspect());
        }
        return env;
    }

    private static final Logger log = Logger.getLogger(RackEnvironment.class);

    private Ruby ruby;
    private RubyArray rackVersion;
    private RubyIO errors;


}