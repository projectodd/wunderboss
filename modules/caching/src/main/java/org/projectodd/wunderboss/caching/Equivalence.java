/*
 * Copyright 2014-2015 Red Hat, Inc, and individual contributors.
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

package org.projectodd.wunderboss.caching;

import java.util.Arrays;

/**
 * This is essentially Infinispan's AnyServerEquivalence class, which
 * doesn't exist in 5.2 and lives in different places in 6 and 7.
 */
public class Equivalence {
    private static boolean isByteArray(Object obj) {
        return byte[].class == obj.getClass();
    }
    public int hashCode(Object obj) {
        if (isByteArray(obj)) {
            return 41 + Arrays.hashCode((byte[]) obj);
        } else {
            return obj.hashCode();
        }
    }
    public boolean equals(Object obj, Object otherObj) {
        if (obj == otherObj)
            return true;
        if (obj == null || otherObj == null)
            return false;
        if (isByteArray(obj) && isByteArray(otherObj))
            return Arrays.equals((byte[]) obj, (byte[]) otherObj);
        return obj.equals(otherObj);
    }
    public String toString(Object obj) {
        if (isByteArray(obj))
            return Arrays.toString((byte[]) obj);
        else
            return obj.toString();
    }
    public boolean isComparable(Object obj) {
        return obj instanceof Comparable;
    }
    public int compare(Object obj, Object otherObj) {
        return ((Comparable<Object>) obj).compareTo(otherObj);
    }
}
