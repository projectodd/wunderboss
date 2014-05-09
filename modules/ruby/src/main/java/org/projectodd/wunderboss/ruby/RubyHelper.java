/*
 * Copyright 2014 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.projectodd.wunderboss.ruby;

import org.jboss.logging.Logger;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Ruby reflection helper utilities.
 *
 * @author Bob McWhirter <bmcwhirt@redhat.com>
 */
public class RubyHelper {

    /**
     * Set a property on a Ruby object, if possible.
     *
     * <p>
     * If the target responds to {@code name=}, the property will be set.
     * Otherwise, not.
     * </p>
     *
     * @param ruby
     *            The Ruby interpreter.
     * @param target
     *            The target object.
     * @param name
     *            The basic name of the property.
     * @param value
     *            The value to attempt to set.
     * @return {@code true} if successful, otherwise {@code false}
     */
    public static boolean setIfPossible(final Ruby ruby, final Object target, final String name, final Object value) {
        boolean success = false;
        if (defined( ruby, target, name + "=" )) {
            JavaEmbedUtils.invokeMethod( ruby, target, name + "=", new Object[] { value }, void.class );
            success = true;
        }
        return success;
    }

    public static Object getIfPossible(final Ruby ruby, final Object target, final String name) {
        Object result = null;
        if (defined( ruby, target, name )) {
            result = JavaEmbedUtils.invokeMethod( ruby, target, name, new Object[] {}, Object.class );
        }
        return result;
    }

    public static Object call(final Ruby ruby, final Object target, final String name, final Object[] parameters) {
        return JavaEmbedUtils.invokeMethod( ruby, target, name, parameters, Object.class );
    }

    public static Object callIfPossible(final Ruby ruby, final Object target, final String name, final Object[] parameters) {
        Object result = null;
        if (defined( ruby, target, name )) {
            result = JavaEmbedUtils.invokeMethod( ruby, target, name, parameters, Object.class );
        }

        return result;
    }

    public static boolean defined(final Ruby ruby, final Object target, final String name) {
        return (Boolean) JavaEmbedUtils.invokeMethod( ruby, target, "respond_to?", new Object[] { name }, Boolean.class );
    }

    public static RubyModule getClass(final Ruby ruby, final String className) {
        return ruby.getClassFromPath(className);
    }

    public static Object invokeClassMethod(Ruby ruby, String className, String name, Object[] parameters) {
        return call(ruby, getClass(ruby, className), name, parameters);
    }

    public static void require(Ruby ruby, String requirement) {
        try {
            evalScriptlet( ruby, "require %q(" + requirement + ")" );
        } catch (Throwable t) {
            log.errorf( t, "Unable to require file: %s", requirement );
        }
    }

    public static boolean requireIfAvailable(Ruby ruby, String requirement) {
        return requireIfAvailable( ruby, requirement, true );
    }

    /**
     * Calls "require 'requirement'" in the Ruby provided.
     * @returns boolean If successful, returns true, otherwise false.
     */
    public static boolean requireIfAvailable(Ruby ruby, String requirement, boolean logErrors) {
        boolean success = false;
        try {
            StringBuilder script = new StringBuilder();
            script.append("require %q(");
            script.append(requirement);
            script.append(")\n");
            evalScriptlet( ruby, script.toString(), false );
            success = true;
        } catch (Throwable t) {
            success = false;
            if (logErrors) {
                log.debugf( t, "Error encountered. Unable to require file: %s", requirement );
            }
        }
        return success;
    }

    public static void requireUnlessDefined(Ruby ruby, String requirement, String constant) {
        try {
            evalScriptlet( ruby, "require %q(" + requirement + ") unless defined?(" + constant + ")" );
        } catch (Throwable t) {
            log.errorf( t, "Unable to require file: %s", requirement );
        }
    }

    // ------------------------------------------------------------------------

    public static IRubyObject evalScriptlet(final Ruby ruby, final String script) {
        return evalScriptlet( ruby, script, true );
    }

    public static IRubyObject evalScriptlet(final Ruby ruby, final String script, final boolean logErrors) {
        try {
            return ruby.evalScriptlet( script );
        } catch (Exception e) {
            if (logErrors)
                log.errorf( e, "Error during evaluation: %s", script );
            throw e;
        }
    }

    public static IRubyObject executeScript(final Ruby ruby, final String script, final String location) {
        try {
            return ruby.executeScript( script, location );
        } catch (Exception e) {
            log.errorf( e, "Error during execution: %s", script );
            throw e;
        }
    }

    // ------------------------------------------------------------------------

    public static IRubyObject instantiate(Ruby ruby, String className) {
        return instantiate( ruby, className, new Object[] {} );
    }

    public static IRubyObject instantiate(final Ruby ruby, final String className, final Object[] parameters) {
        IRubyObject result = null;
        RubyModule rubyClass = ruby.getClassFromPath( className );

        if (rubyClass != null) {
            try {
                result = (IRubyObject) JavaEmbedUtils.invokeMethod( ruby, rubyClass, "new", parameters, IRubyObject.class );
            } catch (Exception e) {
                log.errorf( e, "Unable to instantiate: %s", className );
                throw e;
            }
        }

        return result;
    }


    @SuppressWarnings("rawtypes")
    public static RubyHash convertJavaMapToRubyHash(Ruby runtime, Map map) {
        RubyHash rubyHash = RubyHash.newHash( runtime );
        for (Object object : map.entrySet()) {
            Entry entry = (Entry) object;
            rubyHash.put( entry.getKey(), entry.getValue() );
        }
        return rubyHash;
    }

    public static final RubyString toUsAsciiRubyString(final Ruby runtime, final String string) {
        byte[] bytes = new byte[string.length()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) string.charAt(i);
        }
        return toUsAsciiRubyString(runtime, bytes);
    }

    public static final RubyString toUsAsciiRubyString(final Ruby runtime, final byte[] bytes) {
        return RubyString.newString(runtime, new ByteList(bytes, USASCIIEncoding.INSTANCE, false));
    }

    public static final RubyString toUnicodeRubyString(final Ruby runtime, final String string) {
        return RubyString.newUnicodeString(runtime, string);
    }

    private static final Logger log = Logger.getLogger(RubyHelper.class);
}

