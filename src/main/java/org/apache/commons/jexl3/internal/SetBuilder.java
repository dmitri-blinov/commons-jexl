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
package org.apache.commons.jexl3.internal;

import org.apache.commons.jexl3.JexlArithmetic;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Helper class to create set literals.
 */
public class SetBuilder implements JexlArithmetic.SetBuilder {
    /** The set being created. */
    protected final Set<Object> set;

    /**
     * Creates a new unordered builder with specified set size.
     * @param size the initial set size
     */
    public SetBuilder(final int size) {
        this(size, false);
    }


    /**
     * Creates a new builder with specified set size.
     * @param size the initial set size
     * @param ordered whether the set should be ordered
     */
    public SetBuilder(final int size, final boolean ordered) {
        set = ordered ? new LinkedHashSet<>(size) : new HashSet<>(size);
    }

    @Override
    public void add(final Object value) {
        set.add(value);
    }

    @Override
    public Set<?> create() {
        return set;
    }
}
