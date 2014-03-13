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

package org.projectodd.wunderboss;

import java.util.concurrent.Callable;

public class LoaderWrapper {

    public LoaderWrapper(ClassLoader loader) {
        this.loader = loader;
    }

    public Object callInLoader(Callable body) {
        Thread thread = Thread.currentThread();
        ClassLoader oldCL = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(this.loader);

            return body.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            thread.setContextClassLoader(oldCL);
        }

    }

    private final ClassLoader loader;
}
