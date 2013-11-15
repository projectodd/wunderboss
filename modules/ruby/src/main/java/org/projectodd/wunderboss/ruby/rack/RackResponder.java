package org.projectodd.wunderboss.ruby.rack;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.IOException;

public class RackResponder extends RubyObject {

    public static RubyClass createRackResponderClass(Ruby runtime) {
        RubyModule wunderBossModule = runtime.getOrCreateModule("WunderBoss");
        RubyClass rubyClass = wunderBossModule.getClass("RackResponder");
        if (rubyClass == null) {
            rubyClass = wunderBossModule.defineClassUnder("RackResponder",
                    runtime.getObject(), RACK_RESPONDER_ALLOCATOR);
            rubyClass.defineAnnotatedMethods(RackResponder.class);
        }
        return rubyClass;
    }

    private static final ObjectAllocator RACK_RESPONDER_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RackResponder(runtime, klass);
        }
    };

    private RackResponder(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public RackResponder(Ruby runtime, RubyClass metaClass, HttpServerExchange exchange) {
        super(runtime, metaClass);
        this.exchange = exchange;
    }

    @JRubyMethod(name = "response_code=")
    public IRubyObject setResponseCode(ThreadContext context, IRubyObject status) {
        exchange.setResponseCode((Integer) status.toJava((Integer.class)));
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "add_header")
    public IRubyObject addHeader(ThreadContext context, IRubyObject rubyKey, IRubyObject rubyValue) {
        // HTTP headers are always US_ASCII so we take a couple of shortcuts
        // for converting them from RubyStrings to Java Strings
        final HttpString key = new HttpString(((RubyString) rubyKey).getBytes());
        final String value = new String((((RubyString) rubyValue).getBytes()));
        // Leave out the transfer-encoding header since the container takes
        // care of chunking responses and adding that header
        if (!Headers.TRANSFER_ENCODING.equals(key) && !"chunked".equals(value)) {
            exchange.getResponseHeaders().add(key, value);
        }
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "write")
    public IRubyObject write(ThreadContext context, IRubyObject string) throws IOException {
        exchange.getOutputStream().write(((RubyString) string).getBytes());
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "flush")
    public IRubyObject flush(ThreadContext context) throws IOException {
        exchange.getOutputStream().flush();
        return getRuntime().getNil();
    }

    private HttpServerExchange exchange;
}
