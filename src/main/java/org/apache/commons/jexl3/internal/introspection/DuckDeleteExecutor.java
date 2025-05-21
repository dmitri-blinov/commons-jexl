/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3.internal.introspection;

import java.lang.reflect.InvocationTargetException;
import org.apache.commons.jexl3.JexlException;

/**
 * Specialized executor to get a property from an object.
 * <p>Duck as in duck-typing for an interface like:
 * <code>
 * interface Get {
 *      Object get(Object key);
 * }
 * </code>
 * </p>
 * @since 3.3
 */
public final class DuckDeleteExecutor extends AbstractExecutor.Delete {
    /** The property, may be null. */
    private final Object property;
    /** The arguments list */
    private final Object[] args;

    /**
     * Attempts to discover a DuckGetExecutor.
     * @param is the introspector
     * @param clazz the class to find the get method from
     * @param identifier the key to use as an argument to the get method
     * @return the executor if found, null otherwise
     */
    public static DuckDeleteExecutor discover(final Introspector is, final Class<?> clazz, final Object identifier) {
        final java.lang.reflect.Method method = is.getMethod(clazz, "remove", makeArgs(identifier));
        return method == null? null : new DuckDeleteExecutor(clazz, method, identifier);
    }

    /**
     * Creates an instance.
     * @param clazz he class the get method applies to
     * @param method the method held by this executor
     * @param identifier the property to get
     */
    private DuckDeleteExecutor(final Class<?> clazz, final java.lang.reflect.Method method, final Object identifier) {
        super(clazz, method);
        property = identifier;
        args = new Object[] {property};
    }

    @Override
    public Object getTargetProperty() {
        return property;
    }

    @Override
    public Object invoke(final Object obj) throws IllegalAccessException, InvocationTargetException {
        return method.invoke(obj, args);
    }

    @Override
    public Object tryInvoke(final Object obj, final Object key) {
        if (obj != null
            && objectClass == obj.getClass()
            // ensure method name matches the property name
            && ((property == null && key == null)
                 || (property != null && property.equals(key)))) {
            try {
                return method.invoke(obj, args);
            } catch (IllegalAccessException | IllegalArgumentException xill) {
                return TRY_FAILED;// fail
            } catch (final InvocationTargetException xinvoke) {
                throw JexlException.tryFailed(xinvoke); // throw
            }
        }
        return TRY_FAILED;
    }
}
