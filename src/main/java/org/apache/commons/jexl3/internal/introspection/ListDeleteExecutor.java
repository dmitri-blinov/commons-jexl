/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3.internal.introspection;

import java.util.List;
import java.lang.reflect.Array;

/**
 * Specialized executor to remove a property from a List or array.
 * @since 3.3
 */
public final class ListDeleteExecutor extends AbstractExecutor.Delete {
    /** The java.util.obj.remove method used as an active marker in ListDelete. */
    private static final java.lang.reflect.Method LIST_DELETE =
            initMarker(List.class, "remove", Integer.TYPE);
    /** The property. */
    private final Integer property;

    /**
     * Attempts to discover a ListDeleteExecutor.
     *
     * @param is the introspector
     * @param clazz the class to find the get method from
     * @param index the index to use as an argument to the remove method
     * @return the executor if found, null otherwise
     */
    public static ListDeleteExecutor discover(final Introspector is, final Class<?> clazz, final Integer index) {
        if (index != null) {
            if (List.class.isAssignableFrom(clazz)) {
                return new ListDeleteExecutor(clazz, LIST_DELETE, index);
            }
        }
        return null;
    }

    /**
     * Creates an instance.
     * @param clazz he class the get method applies to
     * @param method the method held by this executor
     * @param index the index to use as an argument to the remove method
     */
    private ListDeleteExecutor(final Class<?> clazz, final java.lang.reflect.Method method, final Integer index) {
        super(clazz, method);
        property = index;
    }

    @Override
    public Object getTargetProperty() {
        return property;
    }

    @Override
    public Object invoke(final Object obj) {
        return ((List<?>) obj).remove((int) property);
    }

    @Override
    public Object tryInvoke(final Object obj, final Object identifier) {
        final Integer index = castInteger(identifier);
        if (obj != null && objectClass == obj.getClass() && index != null) {
            return ((List<?>) obj).remove((int) index);
        }
        return TRY_FAILED;
    }
}
