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

package org.apache.commons.jexl3.introspection;

import org.apache.commons.jexl3.JexlException;

/**
 * Interface for deleting properties.
 * Ex.
 * <code>
 * ${foo.bar}
 * </code>
 *
 * @since 3.3
 */
public interface JexlPropertyDelete {
    /**
     * Method used to delete the property value of an object.
     *
     * @param obj the object to delete the property value from.
     * @return the property value.
     * @throws Exception on any error.
     */
    Object invoke(Object obj) throws Exception;

    /**
     * Attempts to reuse this JexlPropertyDelete, checking that it is compatible with
     * the actual set of arguments.
     *
     * @param obj the object to invoke the property delete upon
     * @param key the property key to delete
     * @return the result of the method invocation that should be checked by tryFailed to determine if it succeeded
     * or failed.
     * @throws JexlException.TryFailed if the underlying method was invoked but threw an exception
     * ({@link java.lang.reflect.InvocationTargetException})
     */
    Object tryInvoke(Object obj, Object key) throws JexlException.TryFailed;

    /**
     * Checks whether a tryInvoke failed or not.
     *
     * @param rval the value returned by tryInvoke
     * @return true if tryInvoke failed, false otherwise
     */
    boolean tryFailed(Object rval);

    /**
     * Specifies if this JexlPropertyDelete is cacheable and able to be reused for
     * this class of object it was returned for.
     *
     * @return true if can be reused for this class, false if not
     */
    boolean isCacheable();
}
