package org.projectodd.wunderboss.ruby.rack;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.projectodd.wunderboss.ruby.RubyHelper;

import java.io.IOException;

public class RackHandler implements HttpHandler {

    public RackHandler(final IRubyObject rackApplication, final String context) throws IOException {
        this.rackApplication = rackApplication;
        this.context = context;
        runtime = rackApplication.getRuntime();
        RubyHelper.requireUnlessDefined(runtime, RESPONSE_HANDLER_RB, RESPONSE_HANDLER_CLASS_NAME);
        responseModule = RubyHelper.getClass(runtime, RESPONSE_HANDLER_CLASS_NAME);
        rackChannelClass = RackChannel.createRackChannelClass(runtime);
        rackResponderClass = RackResponder.createRackResponderClass(runtime);
        rackEnvironment = new RackEnvironment(runtime);

        // Lookup these methods once, instead of on every request
        rackCallMethod = rackApplication.getMetaClass().searchMethod(RACK_CALL_METHOD_NAME);
        responseHandleMethod = responseModule.getMetaClass().searchMethod(RESPONSE_HANDLER_METHOD_NAME);
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        exchange.startBlocking();
        RackChannel inputChannel = null;
        try {
            inputChannel = new RackChannel(runtime, rackChannelClass, exchange.getInputStream());
            RubyHash rackEnvHash = rackEnvironment.getEnv(exchange, inputChannel, context);
            ThreadContext context = runtime.getCurrentContext();
            IRubyObject rackResponse = rackCallMethod.call(context, rackApplication,
                    rackApplication.getMetaClass(), RACK_CALL_METHOD_NAME, rackEnvHash);
            RackResponder rackResponder = new RackResponder(runtime, rackResponderClass, exchange);
            responseHandleMethod.call(context, responseModule, responseModule.getMetaClass(),
                    RESPONSE_HANDLER_METHOD_NAME, rackResponse, rackResponder);
        } finally {
            if (inputChannel != null) {
                inputChannel.close();
            }
            exchange.endExchange();
        }
    }

    private IRubyObject rackApplication;
    private String context;
    private Ruby runtime;
    private RubyModule responseModule;
    private RubyClass rackChannelClass;
    private RubyClass rackResponderClass;
    private RackEnvironment rackEnvironment;
    private DynamicMethod rackCallMethod;
    private DynamicMethod responseHandleMethod;

    public static final String RACK_CALL_METHOD_NAME = "call";
    public static final String RESPONSE_HANDLER_RB = "wunderboss/rack/response_handler";
    public static final String RESPONSE_HANDLER_CLASS_NAME = "WunderBoss::Rack::ResponseHandler";
    public static final String RESPONSE_HANDLER_METHOD_NAME = "handle";
}
