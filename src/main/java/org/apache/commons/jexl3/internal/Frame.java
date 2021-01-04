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

import java.util.Arrays;

/**
 * A call frame, created from a scope, stores the arguments and local variables in a "stack frame" (sic).
 * @since 3.0
 */
public final class Frame {
    /** The scope. */
    private final Scope scope;
    /** The actual stack frame. */
    private final Object[] stack;
    /** Number of curried parameters. */
    private final int curried;

    /**
     * Creates a new frame.
     * @param s the scope
     * @param r the stack frame
     * @param c the number of curried parameters
     */
    Frame(final Scope s, final Object[] r, final int c) {
        scope = s;
        stack = r;
        curried = c;
    }

    /**
     * Creates a new empty frame.
     * @param s the scope
     */
    public Frame(Scope s) {
        this(s, null, 0);
    }

    /**
     * Creates a new frame.
     * @param f the parent frame
     */
    protected Frame(Frame f, Object... values) {
        scope = f.scope;
        stack = f.stack != null ? f.stack.clone() : null;
        if (stack != null) {
            int nparm = scope.getArgCount();
            int ncopy = 0;
            if (values != null && values.length > 0) {
                ncopy = Math.min(nparm - f.curried, Math.min(nparm, values.length));
                System.arraycopy(values, 0, stack, f.curried, ncopy);
            }
            curried = f.curried + ncopy;
            // unbound parameters are defined as null
            Arrays.fill(stack, curried, nparm, null);
        } else {
            curried = f.curried;
        }
    }

    /**
     * Gets this script unbound parameters, i.e. parameters not bound through curry().
     * @return the parameter names
     */
    public String[] getUnboundParameters() {
        return scope.getParameters(curried);
    }

    /**
     * Gets the scope.
     * @return this frame scope
     */
    public Scope getScope() {
        return scope;
    }

    /**
     * Gets the stack length.
     * @return this frame stack size
     */
    public Integer getStackSize() {
        return stack != null ? stack.length : null;
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(this.stack);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Frame other = (Frame) obj;
        return Arrays.deepEquals(this.stack, other.stack);
    }

    /**
     * Gets a value.
     * @param s the offset in this frame
     * @return the stacked value
     */
    Object get(final int s) {
        return stack[s];
    }

    /**
     * Whether this frame defines a symbol, ie declared it and assigned it a value.
     * @param s the offset in this frame
     * @return true if this symbol has been assigned a value, false otherwise
     */
    boolean has(final int s) {
        return s >= 0 && s < stack.length && stack[s] != Scope.UNDECLARED;
    }

    /**
     * Sets a value.
     * @param r the offset in this frame
     * @param value the value to set in this frame
     */
    void set(final int r, final Object value) {
        stack[r] = value;
    }
    /**
     * Assign values to this frame.
     * @param values the values
     * @return this frame
     */
     public Frame assign(final Object... values) {
         if (stack != null) {
             return new Frame(this, values);
         }
         return this;
     }

     /**
      * Creates a clone of this frame.
      * @return new frame
      */
     public Frame clone() {
         return new Frame(this);
     }
}
