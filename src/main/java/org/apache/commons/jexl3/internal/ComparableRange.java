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

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A range of integers.
 */
public class ComparableRange implements JexlArithmetic.Range, Iterable<Comparable> {

    protected final JexlArithmetic jexla;

    /** The lower boundary. */
    protected final Comparable from;
    /** The upper boundary. */
    protected final Comparable to;

    protected final boolean ascending;

    /**
     * Creates a range, ascending or descending depending on boundaries order.
     * @param from the lower inclusive boundary
     * @param to   the higher inclusive boundary
     * @return a range
     */
    public static ComparableRange create(final JexlArithmetic jexla, final Comparable from, final Comparable to) {
        return new ComparableRange(jexla, from, to);
    }
    /**
     * Creates a new range.
     * @param from the lower inclusive boundary
     * @param to  the higher inclusive boundary
     */
    protected ComparableRange(final JexlArithmetic jexla, final Comparable from, final Comparable to) {
        this.jexla = jexla;
        this.from = from;
        this.to = to;
        this.ascending = from.compareTo(to) <= 0;
    }

    /**
     * Gets the interval minimum value.
     * @return the low boundary
     */
    @Override
    public Comparable getFrom() {
        return from;
    }

    /**
     * Gets the interval maximum value.
     * @return the high boundary
     */
    @Override
    public Comparable getTo() {
        return to;
    }

    /**
     * Returns indicator whether the range is reversed.
     *
     * @return the reverse indicator
     */
    @Override
    public boolean isReverse() {
        return !ascending;
    }

    @Override
    public int hashCode() {
        int hash = getClass().hashCode();
        //CSOFF: MagicNumber
        hash = 13 * hash + this.from.hashCode();
        hash = 13 * hash + this.to.hashCode();
        //CSON: MagicNumber
        return hash;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ComparableRange other = (ComparableRange) obj;
        if (this.from.compareTo(other.from) != 0) {
            return false;
        }
        if (this.to.compareTo(other.to) != 0) {
            return false;
        }
        return true;
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean contains(final Object o) {
        if (o instanceof Comparable) {
            Comparable v = (Comparable) o;
            return from.compareTo(v) <= 0 && to.compareTo(v) >= 0;
        }
        return false;
    }

    @Override
    public Iterator<Comparable> iterator() {
        return ascending ? new AscIterator() : new DescIterator();
    }

    /**
     * An ascending iterator on a range.
     */
    class AscIterator implements Iterator<Comparable> {
        /** The current value. */
        private volatile Comparable cursor;
        /**
         * Creates a iterator on the range.
         * @param l low boundary
         * @param h high boundary
         */
        public AscIterator() {
            cursor = from;
        }

        @Override
        public boolean hasNext() {
            return cursor.compareTo(to) <= 0;
        }

        @Override
        public Comparable next() {
            int result = cursor.compareTo(to);
            if (result <= 0) {
                Comparable value = cursor;
                cursor = (Comparable) jexla.increment(cursor);
                return value;
            }

            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported.");
        }
    }

    /**
     * A descending iterator on a range.
     */
    class DescIterator implements Iterator<Comparable> {
        /** The current value. */
        private volatile Comparable cursor;
        /**
         * Creates a iterator on the range.
         * @param l low boundary
         * @param h high boundary
         */
        public DescIterator() {
            cursor = to;
        }

        @Override
        public boolean hasNext() {
            return cursor.compareTo(from) >= 0;
        }

        @Override
        public Comparable next() {

            int result = cursor.compareTo(from);
            if (result >= 0) {
                Comparable value = cursor;
                cursor = (Comparable) jexla.decrement(cursor);
                return value;
            }
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported.");
        }
    }

}

   