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


import org.apache.commons.jexl3.introspection.JexlPropertyGet;
import java.lang.reflect.Method;
import java.beans.IntrospectionException;

/**
 * Abstract an indexed property container.
 * <p>This allows getting properties from expressions like <code>var.container.property</code>.
 * This stores the container name and class as well as the list of available getter and setter methods.
 * It implements JexlPropertyGet since such a container can only be accessed from its owning instance (not set).
 */
public final class IndexedType implements JexlPropertyGet {
    /** The container name. */
    final String container;
    /** The container class. */
    final Class<?> clazz;
    /** The array of getter methods. */
    private final Method[] getters;
    /** Last get method used. */
    private volatile Method get;
    /** The array of setter methods. */
    private final Method[] setters;
    /** Last set method used. */
    private volatile Method set;

    /**
     * Attempts to find an indexed-property getter in an object.
     * The code attempts to find the list of methods getXXX() and setXXX().
     * Note that this is not equivalent to the strict bean definition of indexed properties; the type of the key
     * is not necessarily an int and the set/get arrays are not resolved.
     *
     * @param is the introspector
     * @param object the object
     * @param name the container name
     * @return a JexlPropertyGet is successful, null otherwise
     */
    public static JexlPropertyGet discover(final Introspector is, final Object object, final String name) {
        if (object != null && name != null && !name.isEmpty()) {
            final String base = Character.isUpperCase(name.charAt(0)) ? 
                name : 
                name.substring(0, 1).toUpperCase() + name.substring(1);
            final Method[] getters = is.getMethods(object.getClass(), "get" + base);
            if (getters != null) {
                final String container = name;
                final Class<?> clazz = object.getClass();
                final Method[] setters = is.getMethods(object.getClass(), "set" + base);
                return new IndexedType(container, clazz, getters, setters);
            }
        }
        return null;
    }

    /**
     * A generic indexed property container, exposes get(key) and set(key, value)
     * and solves method call dynamically based on arguments.
     * <p>Must remain public for introspection purpose.</p>
     */
    public static final class IndexedContainer {
        /** The container instance. */
        final Object container;
        /** The container type instance. */
        final IndexedType type;

        /**
         * Creates a new duck container.
         * @param theType the container type
         * @param theContainer the container instance
         */
        IndexedContainer(final IndexedType theType, final Object theContainer) {
            this.type = theType;
            this.container = theContainer;
        }

        /**
         * Gets the property container name.
         * @return the container name
         */
        public String getContainerName() {
            return type.container;
        }

        /**
         * Gets the property container class.
         * @return the container class
         */
        public Class<?> getContainerClass() {
            return type.clazz;
        }

        /**
         * Gets a property from this indexed container.
         * @param key the property key
         * @return the property value
         * @throws Exception if inner invocation fails
         */
        public Object get(final Object key) throws Exception {
            return type.invokeGet(container, key);
        }

        /**
         * Sets a property in this indexed container.
         * @param key the property key
         * @param value the property value
         * @return the invocation result (frequently null)
         * @throws Exception if inner invocation fails
         */
        public Object set(final Object key, final Object value) throws Exception {
            return type.invokeSet(container, key, value);
        }
    }

    /**
     * Creates a new indexed property container type.
     * @param name the container name
     * @param c the owning class
     * @param gets the array of getter methods
     * @param sets the array of setter methods
     */
    private IndexedType(final String name, final Class<?> c, final Method[] gets, final Method[] sets) {
        this.container = name;
        this.clazz = c;
        this.getters = gets;
        this.setters = sets;
    }

    @Override
    public Object invoke(final Object obj) throws Exception {
        if (obj != null && clazz == obj.getClass()) {
            return new IndexedContainer(this, obj);
        }
        throw new IntrospectionException("property resolution error");
    }

    @Override
    public Object tryInvoke(final Object obj, final Object key) {
        if (obj != null && key != null
            && clazz == obj.getClass()
            && container.equals(key.toString())) {
            return new IndexedContainer(this, obj);
        }
        return Uberspect.TRY_FAILED;
    }

    @Override
    public boolean tryFailed(final Object rval) {
        return rval == Uberspect.TRY_FAILED;
    }

    @Override
    public boolean isCacheable() {
        return true;
    }

    /**
     * Gets the value of a property from a container.
     * @param object the container instance (not null)
     * @param key the property key (not null)
     * @return the property value
     * @throws Exception if invocation failed;
     *         IntrospectionException if a property getter could not be found
     */
    Object invokeGet(final Object object, final Object key) throws Exception {
        if (getters != null && getters.length > 0) {
            Method jm = get;
            if (jm != null) {
                final Class<?>[] ptypes = jm.getParameterTypes();
                if (ptypes[0].isAssignableFrom(key.getClass())) {
                    return jm.invoke(object, key);
                }
            }
            final Object[] args = {key};
            final String methodName = getters[0].getName();
            final MethodKey km = new MethodKey(methodName, args);
            jm = km.getMostSpecificMethod(getters);
            if (jm != null) {
                final Object invoked = jm.invoke(object, args);
                get = jm;
                return invoked;
            }
        }
        throw new IntrospectionException("property get error: "
                + object.getClass().toString()
                + "@" + key.toString());
    }

    /**
     * Sets the value of a property in a container.
     * @param object the container instance (not null)
     * @param key the property key (not null)
     * @param value the property value (not null)
     * @return the result of the method invocation (frequently null)
     * @throws Exception if invocation failed;
     *         IntrospectionException if a property setter could not be found
     */
    Object invokeSet(final Object object, final Object key, final Object value) throws Exception {
        if (setters != null && setters.length > 0) {
            Method jm = set;
            if (jm != null) {
                final Class<?>[] ptypes = jm.getParameterTypes();
                if (ptypes[0].isAssignableFrom(key.getClass())
                    && (value == null
                        || ptypes[1].isAssignableFrom(value.getClass()))) {
                    return jm.invoke(object, key, value);
                }
            }
            final Object[] args = {key, value};
            final String methodName = setters[0].getName();
            final MethodKey km = new MethodKey(methodName, args);
            jm = km.getMostSpecificMethod(setters);
            if (jm != null) {
                final Object invoked = jm.invoke(object, args);
                set = jm;
                return invoked;
            }
        }
        throw new IntrospectionException("property set error: "
                + object.getClass().toString()
                + "@" + key.toString());
    }

}
