package io.wunderboss.ruby.rack;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RackChannel extends RubyObject {

    public static RubyClass getRackChannelClass(Ruby runtime) {
        RubyModule wunderBossModule = runtime.getOrCreateModule("WunderBoss");
        RubyClass rackChannel = wunderBossModule.getClass("RackChannel");
        if (rackChannel == null) {
            rackChannel = wunderBossModule.defineClassUnder("RackChannel",
                    runtime.getObject(), RACK_CHANNEL_ALLOCATOR);
            rackChannel.defineAnnotatedMethods(RackChannel.class);
        }
        return rackChannel;
    }

    private static final ObjectAllocator RACK_CHANNEL_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RackChannel(runtime, klass);
        }
    };

    private RackChannel(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public RackChannel(Ruby runtime) {
        super(runtime, getRackChannelClass(runtime));
    }

    @JRubyMethod
    public IRubyObject gets(ThreadContext context) {
        System.err.println("!!! Calling gets");
        return null;
    }

    @JRubyMethod(optional = 2)
    public IRubyObject read(ThreadContext context, IRubyObject[] args) {
        System.err.println("!!! Calling read");
        return null;
    }

    @JRubyMethod
    public IRubyObject each(ThreadContext context) {
        System.err.println("!!! Calling each");
        return null;
    }

    @JRubyMethod
    public RubyFixnum rewind(ThreadContext context) {
        System.err.println("!!! Calling rewind");
        return null;
    }
}
