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

import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.apache.commons.logging.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A cache of introspection information for a specific class instance.
 * Keys objects by an aggregation of the method name and the classes
 * that make up the parameters.
 * <p>
 * Originally taken from the Velocity tree so we can be self-sufficient.
 * </p>
 *
 * @see MethodKey
 * @since 1.0
 */
final class ClassMap {
    /**
     * A method that returns itself used as a marker for cache miss,
     * allows the underlying cache map to be strongly typed.
     *
     * @return itself as a method
     */
    public static Method cacheMiss() {
        try {
            return ClassMap.class.getMethod("cacheMiss");
        } catch (final Exception xio) {
            // this really can't make an error...
            return null;
        }
    }

    /**
     * The cache miss marker method.
     */
    static final Method CACHE_MISS = cacheMiss();
    /** A marker for getter parameter list. **/
    static final Class<?>[] GETTER_ARGS = new Class<?>[0];
    /** The Class. */
    private final Class aClass;
    /**
     * This is the cache to store and look up the method information.
     * <p>
     * It stores the association between:
     * - a key made of a method name and an array of argument types.
     * - a method.
     * </p>
     * <p>
     * Since the invocation of the associated method is dynamic, there is no need (nor way) to differentiate between
     * foo(int,int) and foo(Integer,Integer) since in practice only the latter form will be used through a call.
     * This of course, applies to all 8 primitive types.
     * </p>
     * Uses ConcurrentMap since 3.0, marginally faster than 2.1 under contention.
     */
    private final Map<MethodKey, Method> byKey;
    /**
     * Keep track of all methods with the same name; this is not modified after creation.
     */
    private final Map<String, Method[]> byName;
    /**
     * Keep track of all ambiguous methods with the same key
     */
    private final Set<String> ambiguous;
    /**
     * Cache of fields.
     */
    private final Map<String, Field> fieldCache;
    /**
     * Cache of property getters.
     */
    private final Map<String, Method> propertyGetters;
    /**
     * Cache of property setters.
     */
    private final Map<String, Map<Class, Method>> propertySetters;

    /**
     * Singleton for permissions non-allowed classes.
     */
    private static final ClassMap EMPTY = new ClassMap();

    /**
     * @return the empty classmap instance
     */
    static ClassMap empty() {
        return EMPTY;
    }

    /**
     * Empty map.
     */
    private ClassMap() {
        this.aClass = null;
        this.byKey = Collections.unmodifiableMap(new AbstractMap<MethodKey, Method>() {
            @Override
            public String toString() {
                return "emptyClassMap{}";
            }
            @Override
            public Set<Entry<MethodKey, Method>> entrySet() {
                return Collections.emptySet();
            }
            @Override public Method get(Object name) {
                return CACHE_MISS;
            }
        });
        this.byName = Collections.emptyMap();
        this.ambiguous = Collections.emptySet();
        this.fieldCache = Collections.emptyMap();
        // Property getters
        this.propertyGetters = Collections.emptyMap();
        // Property setters
        this.propertySetters = Collections.emptyMap();

    }

    /**
     * Standard constructor.
     *
     * @param aClass      the class to deconstruct.
     * @param permissions the permissions to apply during introspection
     * @param log         the logger.
     */
    @SuppressWarnings("LeakingThisInConstructor")
    ClassMap(final Class<?> aClass, final JexlPermissions permissions, final Log log) {
        this.byKey = new ConcurrentHashMap<>();
        this.byName = new HashMap<>();
        this.ambiguous = new HashSet<>();
        this.aClass = aClass;

        // eagerly cache methods
        create(this, permissions, aClass, log);
        // eagerly cache public fields
        final Field[] fields = aClass.getFields();
        if (fields.length > 0) {
            fieldCache = new HashMap<>();
            for (Field field : fields) {
                if (permissions.allow(field)) {
                    fieldCache.put(field.getName(), field);
                }
            }
        } else {
            fieldCache = Collections.emptyMap();
        }
        // Property getters
        propertyGetters = new ConcurrentHashMap<> ();
        // Property setters
        propertySetters = new ConcurrentHashMap<> ();
    }

    /**
     * Find a Field using its name.
     *
     * @param fieldName the field name
     * @return A Field object representing the field to invoke or null.
     */
    Field getField(final String fieldName) {
        return fieldCache.get(fieldName);
    }

    /**
     * Check if method name has ambiguous overloads.
     *
     * @param methodName the method name
     * @return true if the method in question has more than one signature.
     */
    boolean isMethodAmbiguous(final String methodname) {
        return ambiguous.contains(methodname);
    }

    /**
     * Gets the field names cached by this map.
     *
     * @return the array of field names
     */
    String[] getFieldNames() {
        return fieldCache.keySet().toArray(new String[0]);
    }

    /**
     * Gets the methods names cached by this map.
     *
     * @return the array of method names
     */
    String[] getMethodNames() {
        return byName.keySet().toArray(new String[0]);
    }

    /**
     * Gets all the methods with a given name from this map.
     *
     * @param methodName the seeked methods name
     * @return the array of methods (null or non-empty)
     */
    Method[] getMethods(final String methodName) {
        final Method[] lm = byName.get(methodName);
        if (lm != null && lm.length > 0) {
            return lm.clone();
        } else {
            return null;
        }
    }

    /**
     * Find a Method using the method name and parameter objects.
     * <p>
     * Look in the methodMap for an entry. If found,
     * it'll either be a CACHE_MISS, in which case we
     * simply give up, or it'll be a Method, in which
     * case, we return it.
     * </p>
     * <p>
     * If nothing is found, then we must actually go
     * and introspect the method from the MethodMap.
     * </p>
     *
     * @param methodKey the method key
     * @return A Method object representing the method to invoke or null.
     * @throws MethodKey.AmbiguousException When more than one method is a match for the parameters.
     */
    Method getMethod(final MethodKey methodKey) throws MethodKey.AmbiguousException {
        if (aClass == null) {
            return null;
        }

        try {
            // Look up by key
            Method cacheEntry = byKey.computeIfAbsent(methodKey, x -> {
                Method result = null;
                // That one is expensive...
                Method[] methodList = byName.get(x.getMethod());

                if (methodList != null) {
                    result = methodKey.getMostSpecificMethod(methodList);
                }
                return (result == null) ? CACHE_MISS : result;
            });

            // We looked this up before and failed.
            if (cacheEntry == CACHE_MISS) {
                return null;
            }
            return cacheEntry;
        } catch (final MethodKey.AmbiguousException ae) {
            // that's a miss :-)
            byKey.put(methodKey, CACHE_MISS);
            throw ae;
        }
    }

    /**
     * Find a Property accessor method.
     * @param name the property name
     * @return A Method object representing the property to invoke or null.
     * @throws MethodKey.AmbiguousException When more than one method is a match for the parameters.
     */
    Method getPropertyGetMethod(final String accessor, final String name) throws MethodKey.AmbiguousException {
        int start = accessor.length();
        StringBuilder sb = new StringBuilder(name.length() + start);
        sb.append(accessor).append(name);
        // uppercase nth char
        char c = sb.charAt(start);
        sb.setCharAt(start, Character.toUpperCase(c));
        Method result = getMethod(new MethodKey(sb.toString(), GETTER_ARGS));
        if (result == null) {
            // lowercase nth char
            sb.setCharAt(start, Character.toLowerCase(c));
            result = getMethod(new MethodKey(sb.toString(), GETTER_ARGS));
        }
        return result;
    }
    /**
     * Find a Property setter method.
     * @param name the property name
     * @return A Method object representing the property to invoke or null.
     * @throws MethodKey.AmbiguousException When more than one method is a match for the parameters.
     */
    Method getPropertySetMethod(final String accessor, final String name, final Class aClass) throws MethodKey.AmbiguousException {
        int start = accessor.length();
        StringBuilder sb = new StringBuilder(name.length() + start);
        sb.append(accessor).append(name);
        // uppercase nth char
        char c = sb.charAt(start);
        sb.setCharAt(start, Character.toUpperCase(c));
        String upper = sb.toString();
        Class[] args = {aClass};
        Method method = getMethod(new MethodKey(upper, args));
        if (method == null) {
            // lowercase nth char
            sb.setCharAt(start, Character.toLowerCase(c));
            String lower = sb.toString();
            method = getMethod(new MethodKey(lower, args));
        }
        return method;
    }

    /**
     * Find a Property get accessor.
     * @param name the property name
     * @return A Method object representing the interface to invoke or null.
     * @throws MethodKey.AmbiguousException When more than one method is a match for the parameters.
     */
    Method getPropertyGet(final String name) throws MethodKey.AmbiguousException {
        if (aClass == null) {
            return null;
        }

        // Look up by name
        Method cacheEntry = propertyGetters.computeIfAbsent(name, x -> {
            Method m = getPropertyGetMethod("get", name);
            if (m == null) {
                m = getPropertyGetMethod("is", name);
                if (m != null && !(m.getReturnType() == Boolean.TYPE || m.getReturnType() == Boolean.class)) {
                    m = null;
                }
            }
            return m != null ? m : CACHE_MISS;
        });
        // We looked this up before and failed.
        if (cacheEntry == CACHE_MISS) {
            return null;
        }
        return cacheEntry;
    }

    /**
     * Find a Property set accessor.
     * @param name the property name
     * @param aClass the assigned value class type
     * @return A Method object representing the interface to invoke or null.
     * @throws MethodKey.AmbiguousException When more than one method is a match for the parameters.
     */
    Method getPropertySet(final String name, Class<?> aClass) throws MethodKey.AmbiguousException {
        if (aClass == null) {
            return null;
        }

        // Look up by name
        Map<Class, Method> setters = propertySetters.computeIfAbsent(name, x -> {
            return new ConcurrentHashMap<Class, Method> ();
        });

        Method cacheEntry = setters.computeIfAbsent(aClass, x -> {
            Method m = getPropertySetMethod("set", name, x);
            return m != null ? m : CACHE_MISS;
        });
        // We looked this up before and failed.
        if (cacheEntry == CACHE_MISS) {
            return null;
        }
        return cacheEntry;
    }

    /**
     * Finds an empty array property setter method by <code>property name</code>.
     * <p>This checks only one method with that name accepts an array as sole parameter.
     * @param name    the property name setter to find
     * @return        the sole method that accepts an array as parameter
     */
    Method lookupSetEmptyArrayProperty(final String name) {
        final String accessor = "set";
        int start = accessor.length();
        StringBuilder sb = new StringBuilder(name.length() + start);
        sb.append(accessor).append(name);
        // uppercase nth char
        char c = sb.charAt(start);
        sb.setCharAt(start, Character.toUpperCase(c));
        String upper = sb.toString();
        Method method = lookupSetEmptyArray(upper);
        if (method == null) {
            // lowercase nth char
            sb.setCharAt(start, Character.toLowerCase(c));
            String lower = sb.toString();
            method = lookupSetEmptyArray(lower);
        }
        return method;
    }

    /**
     * Finds an empty array property setter method by <code>methodName</code>.
     * <p>This checks only one method with that name accepts an array as sole parameter.
     * @param mname    the method name to find
     * @return         the sole method that accepts an array as parameter
     */
    Method lookupSetEmptyArray(String mname) {
        Method candidate = null;
        Method[] methods = getMethods(mname);
        if (methods != null) {
            for (Method method : methods) {
                Class<?>[] paramTypes = method.getParameterTypes();
                if (paramTypes.length == 1 && paramTypes[0].isArray()) {
                    if (candidate != null) {
                        // because the setter method is overloaded for different parameter type,
                        // return null here to report the ambiguity.
                        return null;
                    }
                    candidate = method;
                }
            }
        }
        return candidate;
    }

    /**
     * Populate the Map of direct hits. These are taken from all the public methods
     * that our class, its parents and their implemented interfaces provide.
     *
     * @param cache          the ClassMap instance we create
     * @param permissions    the permissions to apply during introspection
     * @param clazz          the class to cache
     * @param log            the Log
     */
    private static void create(final ClassMap cache, final JexlPermissions permissions, Class<?> clazz, final Log log) {
        //
        // Build a list of all elements in the class hierarchy. This one is bottom-first; we start
        // with the actual declaring class and its interfaces and then move up (superclass etc.) until we
        // hit java.lang.Object. That is important because it will give us the methods of the declaring class
        // which might in turn be abstract further up the tree.
        //
        // We also ignore all SecurityExceptions that might happen due to SecurityManager restrictions.
        //
        for (Class<?> classToReflect = clazz; classToReflect != null; classToReflect = classToReflect.getSuperclass()) {
            if (Modifier.isPublic(classToReflect.getModifiers()) && ClassTool.isExported(classToReflect)) {
                populateWithClass(cache, permissions, classToReflect, log);
            }
            final Class<?>[] interfaces = classToReflect.getInterfaces();
            for (final Class<?> anInterface : interfaces) {
                populateWithInterface(cache, permissions, anInterface, log);
            }
        }
        // now that we've got all methods keyed in, lets organize them by name
        if (!cache.byKey.isEmpty()) {
            final List<Method> lm = new ArrayList<>(cache.byKey.size());
            lm.addAll(cache.byKey.values());
            // sort all methods by name
            lm.sort(Comparator.comparing(Method::getName));
            // put all lists of methods with same name in byName cache
            int start = 0;
            while (start < lm.size()) {
                final String name = lm.get(start).getName();
                int end = start + 1;
                while (end < lm.size()) {
                    final String walk = lm.get(end).getName();
                    if (walk.equals(name)) {
                        end += 1;
                    } else {
                        break;
                    }
                }
                final Method[] lmn = lm.subList(start, end).toArray(new Method[0]);
                cache.byName.put(name, lmn);
                start = end;
            }
        }
    }

    /**
     * Recurses up interface hierarchy to get all super interfaces.
     *
     * @param cache       the cache to fill
     * @param permissions the permissions to apply during introspection
     * @param iface       the interface to populate the cache from
     * @param log         the Log
     */
    private static void populateWithInterface(final ClassMap cache,
                                              final JexlPermissions permissions,
                                              final Class<?> iface,
                                              final Log log) {
        if (Modifier.isPublic(iface.getModifiers())) {
            populateWithClass(cache, permissions, iface, log);
            final Class<?>[] supers = iface.getInterfaces();
            for (final Class<?> aSuper : supers) {
                populateWithInterface(cache, permissions, aSuper, log);
            }
        }
    }

    /**
     * Recurses up class hierarchy to get all super classes.
     *
     * @param cache       the cache to fill
     * @param permissions the permissions to apply during introspection
     * @param clazz       the class to populate the cache from
     * @param log         the Log
     */
    private static void populateWithClass(final ClassMap cache,
                                          final JexlPermissions permissions,
                                          final Class<?> clazz,
                                          final Log log) {
        try {
            final Method[] methods = clazz.getDeclaredMethods();
            for (final Method mi : methods) {
                // method must be public
                if (!Modifier.isPublic(mi.getModifiers())) {
                    continue;
                }
                // add method to byKey cache; do not override
                final MethodKey key = new MethodKey(mi);
                final Method pmi = cache.byKey.putIfAbsent(key, permissions.allow(mi) ? mi : CACHE_MISS);
                if (pmi != null && pmi != CACHE_MISS) {
                    cache.ambiguous.add(mi.getName());
                    if (log.isDebugEnabled() && !key.equals(new MethodKey(pmi))) {
                        // foo(int) and foo(Integer) have the same signature for JEXL
                        log.debug("Method " + pmi + " is already registered, key: " + key.debugString());
                    }
                }
            }
        } catch (final SecurityException se) {
            // Everybody feels better with...
            if (log.isDebugEnabled()) {
                log.debug("While accessing methods of " + clazz + ": ", se);
            }
        }
    }
}
