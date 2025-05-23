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

import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlPropertyGet;
import org.apache.commons.jexl3.introspection.JexlPropertySet;
import org.apache.commons.jexl3.introspection.JexlPropertyDelete;

/**
 * Abstract class that is used to execute an arbitrary
 * method that is introspected. This is the superclass
 * for all other AbstractExecutor classes.
 *
 * @since 1.0
 */
abstract class AbstractExecutor {
    /** A marker for invocation failures in tryInvoke. */
    public static final Object TRY_FAILED = JexlEngine.TRY_FAILED;

    /**
     * A helper to initialize the marker methods (array.get, list.get, etc...).
     * @param clazz the class to introspect
     * @param name the name of the method
     * @param parms the parameters
     * @return the method
     */
    static java.lang.reflect.Method initMarker(final Class<?> clazz, final String name, final Class<?>... parms) {
        try {
            return clazz.getMethod(name, parms);
        } catch (final Exception xnever) {
            throw new Error(xnever);
        }
    }

    /**
     * Coerce an Object which must be a number to an Integer.
     * @param arg the Object to coerce
     * @return an Integer if it can be converted, null otherwise
     */
    static Integer castInteger(final Object arg) {
        return arg instanceof Integer ? (Integer) arg : arg instanceof Number ? ((Number) arg).intValue() : null;
    }

    /**
     * Coerce an Object to a String.
     * @param arg the Object to coerce
     * @return a String if it can be converted, null otherwise
     */
    static String castString(final Object arg) {
        return arg instanceof CharSequence || arg instanceof Integer ? arg.toString() : "";
    }

    /**
     * Creates an arguments array.
     * @param args the list of arguments
     * @return the arguments array
     */
    static Object[] makeArgs(final Object... args) {
        return args;
    }

    /**
     * Gets the class of an object or Object if null.
     * @param instance the instance
     * @return the class
     */
    static Class<?> classOf(final Object instance) {
        return instance == null ? Object.class : instance.getClass();
    }

    /**
     * Gets the class of an object or Void.class if null.
     * @param instance the instance
     * @return the class
     */
    static Class<?> typeOf(final Object instance) {
        return instance == null ? Void.class : instance.getClass();
    }

    /** The class this executor applies to. */
    protected final Class<?> objectClass;
    /** Method to be executed. */
    protected final java.lang.reflect.Method method;

    /**
     * Default and sole constructor.
     * @param theClass the class this executor applies to
     * @param theMethod the method held by this executor
     */
    protected AbstractExecutor(final Class<?> theClass, final java.lang.reflect.Method theMethod) {
        objectClass = theClass;
        method = theMethod;
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj instanceof AbstractExecutor && equals((AbstractExecutor) obj));
    }

    @Override
    public int hashCode() {
        return method != null ? method.hashCode() : 0;
    }

    /**
     *  Indicates whether some other executor is equivalent to this one.
     * @param arg the other executor to check
     * @return true if both executors are equivalent, false otherwise
     */
    public boolean equals(final AbstractExecutor arg) {
        // common equality check
        if (this.getClass() != arg.getClass()) {
            return false;
        }
        if (this.getMethod() != arg.getMethod()) {
            return false;
        }
        if (this.getTargetClass() != arg.getTargetClass()) {
            return false;
        }
        // specific equality check
        final Object lhsp = this.getTargetProperty();
        final Object rhsp = arg.getTargetProperty();
        if (lhsp == null && rhsp == null) {
            return true;
        }
        if (lhsp != null && rhsp != null) {
            return lhsp.equals(rhsp);
        }
        return false;
    }

    /**
     * Tell whether the executor is alive by looking
     * at the value of the method.
     *
     * @return boolean Whether the executor is alive.
     */
    public final boolean isAlive() {
        return (method != null);
    }

    /**
     * Specifies if this executor is cacheable and able to be reused for this
     * class of object it was returned for.
     *
     * @return true if can be reused for this class, false if not
     */
    public boolean isCacheable() {
        return method != null;
    }

    /**
     * Gets the method to be executed or used as a marker.
     * @return Method The method used by execute in derived classes.
     */
    public final java.lang.reflect.Method getMethod() {
        return method;
    }

    /**
     * Gets the object class targeted by this executor.
     * @return the target object class
     */
    public final Class<?> getTargetClass() {
        return objectClass;
    }

    /**
     * Gets the property targeted by this executor.
     * @return the target property
     */
    public Object getTargetProperty() {
        return null;
    }

    /**
     * Gets the method name used.
     * @return method name
     */
    public final String getMethodName() {
        return method != null ? method.getName() : "";
    }

    /**
     * Checks whether a tryExecute failed or not.
     * @param exec the value returned by tryExecute
     * @return true if tryExecute failed, false otherwise
     */
    public final boolean tryFailed(final Object exec) {
        return exec == TRY_FAILED;
    }

    /**
     * Abstract class that is used to execute an arbitrary 'get' method.
     */
    public abstract static class Get extends AbstractExecutor implements JexlPropertyGet {
        /**
         * Default and sole constructor.
         * @param theClass the class this executor applies to
         * @param theMethod the method held by this executor
         */
        protected Get(final Class<?> theClass, final java.lang.reflect.Method theMethod) {
            super(theClass, theMethod);
        }
    }

    /**
     * Abstract class that is used to execute an arbitrary 'set' method.
     */
    public abstract static class Set extends AbstractExecutor implements JexlPropertySet {
        /**
         * Default and sole constructor.
         * @param theClass the class this executor applies to
         * @param theMethod the method held by this executor
         */
        protected Set(final Class<?> theClass, final java.lang.reflect.Method theMethod) {
            super(theClass, theMethod);
        }
    }

    /**
     * Abstract class that is used to execute an arbitrary 'remove' method.
     */
    public abstract static class Delete extends AbstractExecutor implements JexlPropertyDelete {
        /**
         * Default and sole constructor.
         * @param theClass the class this executor applies to
         * @param theMethod the method held by this executor
         */
        protected Delete(final Class<?> theClass, final java.lang.reflect.Method theMethod) {
            super(theClass, theMethod);
        }
    }

    /**
     * Abstract class that is used to execute an arbitrary method.
     */
    public abstract static class Method extends AbstractExecutor implements JexlMethod {
        /** The method key discovered from the arguments. */
        protected final MethodKey key;

        /**
         * Creates a new instance.
         * @param c the class this executor applies to
         * @param m the method
         * @param k the MethodKey
         */
        protected Method(final Class<?> c, final java.lang.reflect.Method m, final MethodKey k) {
            super(c, m);
            key = k;
        }

        @Override
        public Object getTargetProperty() {
            return key;
        }

        @Override
        public final Class<?> getReturnType() {
            return method.getReturnType();
        }

        @Override
        public final Class<?>[] getParameterTypes() {
            return method.getParameterTypes();
        }

        @Override
        public final boolean isStatic() {
            return java.lang.reflect.Modifier.isStatic(method.getModifiers());
        }
    }
}
