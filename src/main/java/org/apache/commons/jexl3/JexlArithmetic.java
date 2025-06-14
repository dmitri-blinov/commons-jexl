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

package org.apache.commons.jexl3;

import static java.lang.StrictMath.floor;
import static org.apache.commons.jexl3.JexlOperator.EQ;

import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Collection;
import java.util.Map;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Optional;

import java.util.concurrent.Future;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.internal.Closure;
import org.apache.commons.jexl3.internal.introspection.IndexedType;

/**
 * Perform arithmetic, implements JexlOperator methods.
 *
 * <p>This is the class to derive to implement new operator behaviors.</p>
 *
 * <p>The 5 base arithmetic operators (+, - , *, /, %) follow the same evaluation rules regarding their arguments.</p>
 * <ol>
 *   <li>If both are null, result is 0</li>
 *   <li>If either is a BigDecimal, coerce both to BigDecimal and perform operation</li>
 *   <li>If either is a floating point number, coerce both to Double and perform operation</li>
 *   <li>Else treat as BigInteger, perform operation and attempt to narrow result:
 *     <ol>
 *       <li>if both arguments can be narrowed to Integer, narrow result to Integer</li>
 *       <li>if both arguments can be narrowed to Long, narrow result to Long</li>
 *       <li>Else return result as BigInteger</li>
 *     </ol>
 *   </li>
 * </ol>
 *
 * Note that the only exception thrown by JexlArithmetic is and must be ArithmeticException.
 *
 * @see JexlOperator
 * @since 2.0
 */
public class JexlArithmetic {

    /** Marker class for null operand exceptions. */
    public static class NullOperand extends ArithmeticException {
        private static final long serialVersionUID = 4720876194840764770L;
    }

    /** Double.MAX_VALUE as BigDecimal. */
    protected static final BigDecimal BIGD_DOUBLE_MAX_VALUE = BigDecimal.valueOf(Double.MAX_VALUE);

    /** Double.MIN_VALUE as BigDecimal. */
    protected static final BigDecimal BIGD_DOUBLE_MIN_VALUE = BigDecimal.valueOf(Double.MIN_VALUE);

    /** Long.MAX_VALUE as BigInteger. */
    protected static final BigInteger BIGI_LONG_MAX_VALUE = BigInteger.valueOf(Long.MAX_VALUE);

    /** Long.MIN_VALUE as BigInteger. */
    protected static final BigInteger BIGI_LONG_MIN_VALUE = BigInteger.valueOf(Long.MIN_VALUE);

    /** Default BigDecimal scale. */
    protected static final int BIGD_SCALE = -1;

    /** Double ONE */
    protected static final Double DOUBLE_ONE = 1.;

    /** Whether this JexlArithmetic instance behaves in strict or lenient mode. */
    private final boolean strict;

    /** Whether this JexlArithmetic instance allows null as argument to cast methods - toXXX(). */
    private final boolean strictCast;

    /** The big decimal math context. */
    private final MathContext mathContext;

    /** The big decimal scale. */
    private final int mathScale;

    /** The dynamic constructor. */
    private final Constructor<? extends JexlArithmetic> ctor;

    /**
     * Creates a JexlArithmetic.
     * <p>If you derive your own arithmetic, implement the
     * other constructor that may be needed when dealing with options.
     *
     * @param astrict whether this arithmetic is strict or lenient
     */
    public JexlArithmetic(final boolean astrict) {
        this(astrict, null, Integer.MIN_VALUE);
    }

    /**
     * Creates a JexlArithmetic.
     * <p>The constructor to define in derived classes.
     *
     * @param astrict     whether this arithmetic is lenient or strict
     * @param bigdContext the math context instance to use for +,-,/,*,% operations on big decimals.
     * @param bigdScale   the scale used for big decimals.
     */
    public JexlArithmetic(final boolean astrict, final MathContext bigdContext, final int bigdScale) {
        this.strict = astrict;
        this.mathContext = bigdContext == null ? MathContext.DECIMAL128 : bigdContext;
        this.mathScale = bigdScale == Integer.MIN_VALUE ? BIGD_SCALE : bigdScale;
        Constructor<? extends JexlArithmetic> actor = null;
        try {
            actor = getClass().getConstructor(boolean.class, MathContext.class, int.class);
        } catch (final Exception xany) {
            // ignore
        }
        this.ctor = actor;
        boolean cast = strict;
        // if isStrict is not overridden, we are in strict-cast mode
        if (cast) {
            try {
                Method istrict = getClass().getMethod("isStrict", JexlOperator.class);
                cast = JexlArithmetic.class == istrict.getDeclaringClass();
            } catch (Exception e) {
                // ignore
            }
        }
        this.strictCast = cast;
    }

    /**
     * Apply options to this arithmetic which eventually may create another instance.
     * @see #createWithOptions(boolean, java.math.MathContext, int)
     *
     * @param options the {@link JexlEngine.Options} to use
     * @return an arithmetic with those options set
     */
    public JexlArithmetic options(final JexlOptions options) {
        if (options != null) {
            final boolean ostrict = options.isStrictArithmetic();
            MathContext bigdContext = options.getMathContext();
            if (bigdContext == null) {
                bigdContext = getMathContext();
            }
            int bigdScale = options.getMathScale();
            if (bigdScale == Integer.MIN_VALUE) {
                bigdScale = getMathScale();
            }
            if (ostrict != isStrict()
                || bigdScale != getMathScale()
                || bigdContext != getMathContext()) {
                return createWithOptions(ostrict, bigdContext, bigdScale);
            }
        }
        return this;
    }

    /**
     * Apply options to this arithmetic which eventually may create another instance.
     * @see #createWithOptions(boolean, java.math.MathContext, int)
     *
     * @param options the {@link JexlEngine.Options} to use
     * @return an arithmetic with those options set
     * @deprecated 3.2
     */
    @Deprecated
    public JexlArithmetic options(final JexlEngine.Options options) {
        if (options != null) {
            boolean isstrict = Boolean.TRUE == options.isStrictArithmetic() || isStrict();
            MathContext bigdContext = options.getArithmeticMathContext();
            if (bigdContext == null) {
                bigdContext = getMathContext();
            }
            int bigdScale = options.getArithmeticMathScale();
            if (bigdScale == Integer.MIN_VALUE) {
                bigdScale = getMathScale();
            }
            if (isstrict != isStrict()
                || bigdScale != getMathScale()
                || bigdContext != getMathContext()) {
                return createWithOptions(isstrict, bigdContext, bigdScale);
            }
        }
        return this;
    }

    /**
     * Apply options to this arithmetic which eventually may create another instance.
     * @see #createWithOptions(boolean, java.math.MathContext, int)
     *
     * @param context the context that may extend {@link JexlContext.OptionsHandle} to use
     * @return a new arithmetic instance or this
     * @since 3.1
     */
    public JexlArithmetic options(final JexlContext context) {
        if (context instanceof JexlContext.OptionsHandle) {
            return options(((JexlContext.OptionsHandle) context).getEngineOptions());
        }
        if (context instanceof JexlEngine.Options) {
            return options((JexlEngine.Options) context);
        }
        return this;
    }

    /**
     * Creates a JexlArithmetic instance.
     * Called by options(...) method when another instance of the same class of arithmetic is required.
     * @see #options(org.apache.commons.jexl3.JexlEngine.Options)
     *
     * @param astrict     whether this arithmetic is lenient or strict
     * @param bigdContext the math context instance to use for +,-,/,*,% operations on big decimals.
     * @param bigdScale   the scale used for big decimals.
     * @return default is a new JexlArithmetic instance
     * @since 3.1
     */
    protected JexlArithmetic createWithOptions(final boolean astrict, final MathContext bigdContext, final int bigdScale) {
        if (ctor != null) {
            try {
                return ctor.newInstance(astrict, bigdContext, bigdScale);
            } catch (IllegalAccessException | IllegalArgumentException
                    | InstantiationException | InvocationTargetException xany) {
                // it was worth the try
            }
        }
        return new JexlArithmetic(astrict, bigdContext, bigdScale);
    }

    /**
     * The interface that uberspects JexlArithmetic classes.
     * <p>This allows overloaded operator methods discovery.</p>
     */
    public interface Uberspect {
        /**
         * Checks whether this uberspect has overloads for a given operator.
         *
         * @param operator the operator to check
         * @return true if an overload exists, false otherwise
         */
        boolean overloads(JexlOperator operator);

        /**
         * Gets the most specific method for an operator.
         *
         * @param operator the operator
         * @param args     the arguments
         * @return the most specific method or null if no specific override could be found
         */
        JexlMethod getOperator(JexlOperator operator, Object... args);

    }

    /**
     * Helper interface used when creating an array literal.
     *
     * <p>The default implementation creates an array and attempts to type it strictly.</p>
     *
     * <ul>
     *   <li>If all objects are of the same type, the array returned will be an array of that same type</li>
     *   <li>If all objects are Numbers, the array returned will be an array of Numbers</li>
     *   <li>If all objects are convertible to a primitive type, the array returned will be an array
     *       of the primitive type</li>
     * </ul>
     */
    public interface ArrayBuilder {

        /**
         * Adds a literal to the array.
         *
         * @param value the item to add
         */
        void add(Object value);

        /**
         * Creates the actual "array" instance.
         *
         * @param extended true when the last argument is ', ...'
         * @return the array
         */
        Object create(boolean extended);
    }

    /**
     * Called by the interpreter when evaluating a literal array.
     *
     * @param size the number of elements in the array
     * @return the array builder
     */
    public ArrayBuilder arrayBuilder(final int size) {
        return new org.apache.commons.jexl3.internal.ArrayBuilder(size);
    }

    /**
     * Helper interface used when creating a set literal.
     * <p>The default implementation creates a java.util.HashSet.</p>
     */
    public interface SetBuilder {
        /**
         * Adds a literal to the set.
         *
         * @param value the item to add
         */
        void add(Object value);

        /**
         * Creates the actual "set" instance.
         *
         * @return the set
         */
        Object create();
    }

    /**
     * Creates a set-builder.
     * @param size the number of elements in the set
     * @return a set-builder instance
     * @deprecated since 3.3.1
     */
    @Deprecated
    public SetBuilder setBuilder(final int size) {
        return setBuilder(size, false);
    }

    /**
     * Called by the interpreter when evaluating a literal set.
     *
     * @param size the number of elements in the set
     * @param extended whether the set is extended or not
     * @return the array builder
     */
    public SetBuilder setBuilder(final int size, final boolean extended) {
        return new org.apache.commons.jexl3.internal.SetBuilder(size, extended);
    }

    /**
     * Helper interface used when creating a map literal.
     * <p>The default implementation creates a java.util.HashMap.</p>
     */
    public interface MapBuilder {
        /**
         * Adds a new entry to the map.
         *
         * @param key   the map entry key
         * @param value the map entry value
         */
        void put(Object key, Object value);

        /**
         * Creates the actual "map" instance.
         *
         * @return the map
         */
        Object create();
    }

    /**
     * Helper interface used when creating a range literal.
     */
    public interface Range {
        /**
         * Returns lowest range boundary.
         *
         * @return the range lowest boundary
         */
        Comparable getFrom();

        /**
         * Returns highest range boundary.
         *
         * @return the range highest boundary
         */
        Comparable getTo();

        /**
         * Returns indicator whether the range is reversed.
         *
         * @return the reverse indicator
         */
        boolean isReverse();
    }

    /** Marker class for coercion operand exceptions. */
    public static class CoercionException extends ArithmeticException {
        private static final long serialVersionUID = 202402081150L;

        /**
         * Simple ctor.
         * @param msg the exception message
         */
        public CoercionException(final String msg) {
            super(msg);
        }


        /**
         * Constructs a new instance.
         *
         * @param msg the detail message.
         * @param cause The cause of this Throwable.
         * @since 3.5.0
         */
        public CoercionException(final String msg, final Throwable cause) {
            super(msg);
            initCause(cause);
        }
    }

    /**
     * Creates a map-builder.
     * @param size the number of elements in the map
     * @return a map-builder instance
     * @deprecated 3.3
     */
    @Deprecated
    public MapBuilder mapBuilder(final int size) {
        return mapBuilder(size, false);
    }

    /**
     * Called by the interpreter when evaluating a literal map.
     *
     * @param size the number of elements in the map
     * @param extended whether the map is extended or not
     * @return the map builder
     */
    public MapBuilder mapBuilder(final int size, final boolean extended) {
        return new org.apache.commons.jexl3.internal.MapBuilder(size, extended);
    }

    /**
     * Creates a literal range.
     * <p>The default implementation only accepts integers and longs.</p>
     *
     * @param from the included lower bound value (null if none)
     * @param to   the included upper bound value (null if none)
     * @return the range as an iterable
     * @throws ArithmeticException as an option if creation fails
     */
    public Iterable<?> createRange(final Object from, final Object to) throws ArithmeticException {
        if (isLongPrecisionNumber(from) && isLongPrecisionNumber(to)) {
            final long lfrom = toLong(from);
            final long lto = toLong(to);
            if ((lfrom >= Integer.MIN_VALUE && lfrom <= Integer.MAX_VALUE)
                    && (lto >= Integer.MIN_VALUE && lto <= Integer.MAX_VALUE)) {
                return org.apache.commons.jexl3.internal.IntegerRange.create((int) lfrom, (int) lto);
            }
            return org.apache.commons.jexl3.internal.LongRange.create(lfrom, lto);
        } else if ((from == null || from instanceof Comparable) && (to == null || to instanceof Comparable)) {
            return org.apache.commons.jexl3.internal.ComparableRange.create(this, (Comparable) from, (Comparable)to);
        } else {
            throw new ArithmeticException("Range coercion: "
                + (from != null ? from.getClass().getName() : "null")
                + ".." 
                + (to != null ? to.getClass().getName() : "null"));

        }
    }

    /**
     * Checks if an operand is considered null.
     * @param value the operand
     * @return true if operand is considered null
     */
    protected boolean isNullOperand(Object value) {
        return value == null;
    }

    /**
     * Throws an NullOperand exception if arithmetic is strict-cast.
     * <p>This method is called by the cast methods ({@link #toBoolean(boolean, Object)},
     * {@link #toInteger(boolean, Object)}, {@link #toDouble(boolean, Object)},
     * {@link #toString(boolean, Object)}, {@link #toBigInteger(boolean, Object)},
     * {@link #toBigDecimal(boolean, Object)}) when they encounter a null argument.</p>
     *
     * @param strictCast whether strict cast is required
     * @param defaultValue the default value to return, if not strict
     * @param <T> the value type
     * @return the default value is strict is false
     * @throws JexlArithmetic.NullOperand if strict-cast
     * @since 3.3
     */
    protected <T> T controlNullOperand(boolean strictCast, T defaultValue) {
        if (strictCast) {
            throw new NullOperand();
        }
        return defaultValue;
    }

    /**
     * The result of +,/,-,*,% when both operands are null.
     * @param operator the actual operator
     * @return Integer(0) if lenient
     * @throws  JexlArithmetic.NullOperand if strict-cast
     * @since 3.3
     */
    protected Object controlNullNullOperands(JexlOperator operator) {
        if (isStrict(operator)) {
            throw new NullOperand();
        }
        return 0;
    }

    /**
     * Creates a map entry literal.
     *
     * @param key the entry key
     * @param value the entry value
     * @return the map entry
     * @throws ArithmeticException as an option if creation fails
     */
    public Map.Entry<Object,Object> createMapEntry(Object key, Object value) throws ArithmeticException {
        return new AbstractMap.SimpleEntry<Object, Object> (key, value);
    }


    /**
     * Coerce to a primitive boolean.
     * <p>Double.NaN, null, "false" and empty string coerce to false.</p>
     *
     * @param val value to coerce
     * @param strict true if the calling operator or casting is strict, false otherwise
     * @return the boolean value if coercion is possible, true if value was not null.
     */
    protected boolean toBoolean(final boolean strict, final Object val) {
        return isNullOperand(val)? controlNullOperand(strict, false) : toBoolean(val);
    }

    /**
     * Coerce to a primitive int.
     * <p>Double.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     *
     * @param strict true if the calling operator or casting is strict, false otherwise
     * @param val value to coerce
     * @return the value coerced to int
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible
     * @since 3.3
     */
    protected int toInteger(final boolean strict, final Object val) {
        return isNullOperand(val)? controlNullOperand(strict, 0) : toInteger(val);
    }

    /**
     * Coerce to a primitive long.
     * <p>Double.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     *
     * @param strict true if the calling operator or casting is strict, false otherwise
     * @param val value to coerce
     * @return the value coerced to long
     * @throws ArithmeticException if value is null and mode is strict or if coercion is not possible
     * @since 3.3
     */
    protected long toLong(final boolean strict, final Object val) {
        return isNullOperand(val)? controlNullOperand(strict, 0L) : toLong(val);
    }

    /**
     * Coerce to a BigInteger.
     * <p>Double.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     *
     * @param strict true if the calling operator or casting is strict, false otherwise
     * @param val the object to be coerced.
     * @return a BigDecimal
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible
     * @since 3.3
     */
    protected BigInteger toBigInteger(final boolean strict, final Object val) {
        return isNullOperand(val)? controlNullOperand(strict, BigInteger.ZERO) : toBigInteger(val);
    }

    /**
     * Coerce to a BigDecimal.
     * <p>Double.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     *
     * @param strict true if the calling operator or casting is strict, false otherwise
     * @param val the object to be coerced.
     * @return a BigDecimal.
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible
     * @since 3.3
     */
    protected BigDecimal toBigDecimal(final boolean strict, final Object val) {
        return isNullOperand(val)? controlNullOperand(strict, BigDecimal.ZERO) : toBigDecimal(val);
    }


    /**
     * Coerce to a primitive double.
     * <p>Double.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     *
     * @param strict true if the calling operator or casting is strict, false otherwise
     * @param val value to coerce.
     * @return The double coerced value.
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible
     * @since 3.3
     */
    protected double toDouble(final boolean strict, final Object val) {
        return isNullOperand(val)? controlNullOperand(strict, 0.d) : toDouble(val);
    }


    /**
     * Coerce to a string.
     * <p>Double.NaN coerce to the empty string.</p>
     *
     * @param strict true if the calling operator or casting is strict, false otherwise
     * @param val value to coerce.
     * @return The String coerced value.
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible
     * @since 3.3
     */
    protected String toString(final boolean strict, final Object val) {
        return isNullOperand(val)? controlNullOperand(strict, "") : toString(val);
    }

    /**
     * Checks whether this JexlArithmetic instance
     * strictly considers null as an error when used as operand unexpectedly.
     *
     * @return true if strict, false if lenient
     */
    public boolean isStrict() {
        return strict;
    }

    /**
     * Checks whether this JexlArithmetic instance
     * strictly considers null as an error when used as operand of a cast method (toXXX())..
     *
     * @return true if strict-cast, false if lenient
     */
    public boolean isStrictCast() {
        return strictCast;
    }

    /**
     * Checks whether this arithmetic considers a given operator as strict or null-safe.
     * <p>When an operator is strict, it does <em>not</em> accept null arguments when the arithmetic is strict.
     * If null-safe (ie not-strict), the operator does accept null arguments even if the arithmetic itself is strict.</p>
     * <p>The default implementation considers equal/not-equal operators as null-safe so one can check for null as in
     * <code>if (myvar == null) {...}</code>. Note that this operator is used for equal and not-equal syntax. The complete
     * list of operators that are not strict are (==, [], []=, ., .=, empty, size, contains). </p>
     * <p>
     *     An arithmetic refining its strict behavior handling for more operators must declare which by overriding
     * this method.
     * </p>
     * <p>
     *     If this method is overridden, the arithmetic instance is <em>NOT</em> in strict-cast mode. Tp restore the
     *     strict-cast behavior, override the {@link #isStrictCast()} method/
     * </p>
     * @param operator the operator to check for null-argument(s) handling
     * @return true if operator considers null arguments as errors, false if operator has appropriate semantics
     * for null argument(s)
     */
    public boolean isStrict(JexlOperator operator) {
        if (!isStrict())
            return false;

        if (operator != null) {
            switch (operator) {
                case EQ:
                case ARRAY_GET:
                case ARRAY_SET:
                case PROPERTY_GET:
                case PROPERTY_SET:
                case EMPTY:
                case SIZE:
                case CONTAINS:
                    return false;
            }
        }
        return true;
    }

    /**
     * The MathContext instance used for +,-,/,*,% operations on big decimals.
     *
     * @return the math context
     */
    public MathContext getMathContext() {
        return mathContext;
    }

    /**
     * The BigDecimal scale used for comparison and coercion operations.
     *
     * @return the scale
     */
    public int getMathScale() {
        return mathScale;
    }

    /**
     * Ensure a big decimal is rounded by this arithmetic scale and rounding mode.
     *
     * @param number the big decimal to round
     * @return the rounded big decimal
     */
    protected BigDecimal roundBigDecimal(final BigDecimal number) {
        final int mscale = getMathScale();
        if (mscale >= 0) {
            return number.setScale(mscale, getMathContext().getRoundingMode());
        }
        return number;
    }

    /**
     * The result of +,/,-,*,% when both operands are null.
     *
     * @return Integer(0) if lenient
     * @throws ArithmeticException if strict-cast
     */
    protected Object controlNullNullOperands() {
        if (isStrictCast()) {
            throw new NullOperand();
        }
        return 0;
    }

    /**
     * Throw a NPE if arithmetic is strict-cast.
     * <p>This method is called by the cast methods ({@link #toBoolean(Object)}, {@link #toInteger(Object)},
     * {@link #toDouble(Object)}, {@link #toString(Object)}, {@link #toBigInteger(Object)}, {@link #toBigDecimal(Object)})
     * when they encounter a null argument.</p>
     *
     * @throws ArithmeticException if strict
     */
    protected void controlNullOperand() {
        if (isStrictCast()) {
            throw new NullOperand();
        }
    }

    /**
     * The float regular expression pattern.
     * <p>
     * The decimal and exponent parts are optional and captured allowing to determine if the number is a real
     * by checking whether one of these 2 capturing groups is not empty.
     */
    public static final Pattern FLOAT_PATTERN = Pattern.compile("^[+-]?\\d*(\\.\\d*)?([eE][+-]?\\d+)?$");

    /**
     * Test if the passed value is a floating point number, i.e. a float, double
     * or string with ( "." | "E" | "e").
     *
     * @param val the object to be tested
     * @return true if it is, false otherwise.
     */
    protected boolean isFloatingPointNumber(final Object val) {
        if (isFloatingPoint(val)) {
            return true;
        }
        if (val instanceof CharSequence) {
            final Matcher m = FLOAT_PATTERN.matcher((CharSequence) val);
            // first matcher group is decimal, second is exponent
            // one of them must exist hence start({1,2}) >= 0
            return m.matches() && (m.start(1) >= 0 || m.start(2) >= 0);
        }
        return false;
    }

    /**
     * Is Object a floating point number.
     *
     * @param o Object to be analyzed.
     * @return true if it is a Float or a Double.
     */
    protected boolean isFloatingPoint(final Object o) {
        if (o == null) {
            return false;
        }
        Class c = o.getClass();
        return c == Double.class
                || c == Float.class;
    }

    /**
     * Is Object a whole number.
     *
     * @param o Object to be analyzed.
     * @return true if Integer, Long, Byte, Short or Character.
     */
    protected boolean isNumberable(final Object o) {
        if (o == null) {
            return false;
        }
        Class c = o.getClass();
        return c == Integer.class
                || c == Long.class
                || c == Byte.class
                || c == Short.class
                || c == Character.class;
    }

    /**
     * The last method called before returning a result from a script execution.
     * @param returned the returned value
     * @return the controlled returned value
     */
    public Object controlReturn(Object returned) {
        return returned;
    }

    /**
     * Given a Number, return the value using the smallest type the result
     * will fit into.
     * <p>This works hand in hand with parameter 'widening' in java
     * method calls, e.g. a call to substring(int,int) with an int and a long
     * will fail, but a call to substring(int,int) with an int and a short will
     * succeed.</p>
     *
     * @param original the original number.
     * @return a value of the smallest type the original number will fit into.
     */
    public Number narrow(final Number original) {
        return narrowNumber(original, null);
    }

    /**
     * Whether we consider the narrow class as a potential candidate for narrowing the source.
     *
     * @param narrow the target narrow class
     * @param source the original source class
     * @return true if attempt to narrow source to target is accepted
     */
    protected boolean narrowAccept(final Class<?> narrow, final Class<?> source) {
        return narrow == null || narrow.equals(source);
    }

    /**
     * Given a Number, return back the value attempting to narrow it to a target class.
     *
     * @param original the original number
     * @param narrow   the attempted target class
     * @return the narrowed number or the source if no narrowing was possible
     */
    public Number narrowNumber(final Number original, final Class<?> narrow) {
        if (original == null) {
            return null;
        }
        Number result = original;
        if (original instanceof BigDecimal) {
            final BigDecimal bigd = (BigDecimal) original;
            // if it is bigger than a double, it can not be narrowed
            if (bigd.compareTo(BIGD_DOUBLE_MAX_VALUE) > 0
                || bigd.compareTo(BIGD_DOUBLE_MIN_VALUE) < 0) {
                return original;
            }
            try {
                final long l = bigd.longValueExact();
                // coerce to int when possible (int being so often used in method parms)
                if (narrowAccept(narrow, Integer.class)
                        && l <= Integer.MAX_VALUE
                        && l >= Integer.MIN_VALUE) {
                    return (int) l;
                }
                if (narrowAccept(narrow, Long.class)) {
                    return l;
                }
            } catch (final ArithmeticException xa) {
                // ignore, no exact value possible
            }
        } else if (original instanceof Double) {
            double value = original.doubleValue();
            if (narrowAccept(narrow, Float.class)
                    && value <= Float.MAX_VALUE
                    && value >= Float.MIN_VALUE) {
                result = result.floatValue();
            }
            // else it fits in a double only
        } else {
            if (original instanceof BigInteger) {
                final BigInteger bigi = (BigInteger) original;
                // if it is bigger than a Long, it can not be narrowed
                if (bigi.compareTo(BIGI_LONG_MAX_VALUE) > 0
                        || bigi.compareTo(BIGI_LONG_MIN_VALUE) < 0) {
                    return original;
                }
            }
            final long value = original.longValue();
            if (narrowAccept(narrow, Byte.class)
                    && value <= Byte.MAX_VALUE
                    && value >= Byte.MIN_VALUE) {
                // it will fit in a byte
                result = (byte) value;
            } else if (narrowAccept(narrow, Short.class)
                    && value <= Short.MAX_VALUE
                    && value >= Short.MIN_VALUE) {
                result = (short) value;
            } else if (narrowAccept(narrow, Integer.class)
                    && value <= Integer.MAX_VALUE
                    && value >= Integer.MIN_VALUE) {
                result = (int) value;
            }
            // else it fits in a long
        }
        return result;
    }

    /**
     * Given a BigInteger, narrow it to an Integer or Long if it fits and the arguments
     * class allow it.
     * <p>
     * The rules are:
     * if either arguments is a BigInteger, no narrowing will occur
     * if either arguments is a Long, no narrowing to Integer will occur
     * </p>
     *
     * @param lhs  the left hand side operand that lead to the bigi result
     * @param rhs  the right hand side operand that lead to the bigi result
     * @param bigi the BigInteger to narrow
     * @return an Integer or Long if narrowing is possible, the original BigInteger otherwise
     */
    protected Number narrowBigInteger(final Object lhs, final Object rhs, final BigInteger bigi) {
        //coerce to long if possible
        if (!(lhs instanceof BigInteger || rhs instanceof BigInteger)
                && bigi.compareTo(BIGI_LONG_MAX_VALUE) <= 0
                && bigi.compareTo(BIGI_LONG_MIN_VALUE) >= 0) {
            // coerce to int if possible
            final long l = bigi.longValue();
            // coerce to int when possible (int being so often used in method parms)
            if (!(lhs instanceof Long || rhs instanceof Long)
                    && l <= Integer.MAX_VALUE
                    && l >= Integer.MIN_VALUE) {
                return (int) l;
            }
            return l;
        }
        return bigi;
    }

    /**
     * Given a BigDecimal, attempt to narrow it to an Integer or Long if it fits if
     * one of the arguments is a numberable.
     *
     * @param lhs  the left hand side operand that lead to the bigd result
     * @param rhs  the right hand side operand that lead to the bigd result
     * @param bigd the BigDecimal to narrow
     * @return an Integer or Long if narrowing is possible, the original BigInteger otherwise
     */
    protected Number narrowBigDecimal(final Object lhs, final Object rhs, final BigDecimal bigd) {
        if (isNumberable(lhs) || isNumberable(rhs)) {
            try {
                final long l = bigd.longValueExact();
                // coerce to int when possible (int being so often used in method parms)
                if (l <= Integer.MAX_VALUE && l >= Integer.MIN_VALUE) {
                    return (int) l;
                }
                return l;
            } catch (final ArithmeticException xa) {
                // ignore, no exact value possible
            }
        }
        return bigd;
    }

    /**
     * Replace all numbers in an arguments array with the smallest type that will fit.
     *
     * @param args the argument array
     * @return true if some arguments were narrowed and args array is modified,
     *         false if no narrowing occurred and args array has not been modified
     */
    public boolean narrowArguments(final Object[] args) {
        boolean narrowed = false;
        if (args != null) {
            for (int a = 0; a < args.length; ++a) {
                final Object arg = args[a];
                if (arg instanceof Number) {
                    final Number narg = (Number) arg;
                    final Number narrow = narrow(narg);
                    if (!narg.equals(narrow)) {
                        args[a] = narrow;
                        narrowed = true;
                    }
                }
            }
        }
        return narrowed;
    }

    /**
     * Checks if object is eligible for fast integer arithmetic.
     *
     * @param object  argument
     */
    protected boolean isIntegerPrecisionNumber(Object value) {
        if (value == null) {
            return false;
        }
        Class c = value.getClass();
        return c == Integer.class || c == Short.class || c == Byte.class;
    }

    /**
     * Checks if object is eligible for fast long arithmetic.
     *
     * @param object  argument
     */
    protected boolean isLongPrecisionNumber(Object value) {
        if (value == null) {
            return false;
        }
        Class c = value.getClass();
        return c == Long.class || c == Integer.class || c == Short.class || c == Byte.class;
    }

    /**
     * Creates BigInteger from long by extending its byte representation with specified byte.
     *
     * @param x  value to be extended
     * @param msb  byte to extend value with
     */
    protected BigInteger extendedLong(long x, byte msb) {
        byte[] bi = new byte[9];
        bi[0] = msb;
        for (int i = 8; i > 0; i--) {
            bi[i] = (byte)(x & 0xFF);
            x >>= 8;
        }
        return new BigInteger(bi);
    }

    /**
     * Given a long, attempt to narrow it to an int.
     * <p>Narrowing will only occur if no operand is a Long.
     * @param lhs  the left hand side operand that lead to the long result
     * @param rhs  the right hand side operand that lead to the long result
     * @param r the long to narrow
     * @return an Integer if narrowing is possible, the original Long otherwise
     */
    protected Number narrowLong(final Object lhs, final Object rhs, final long r) {
        if (!(lhs instanceof Long || rhs instanceof Long) && (int) r == r) {
            return (int) r;
        }
        return r;
    }

    /**
     * Checks if value class is a number that can be represented exactly in a long.
     *
     * @param value  argument
     * @return true if argument can be represented by a long
     */
    protected Number asLongNumber(final Object value) {

        if (isLongPrecisionNumber(value))
            return (Number) value;

        if (value instanceof Boolean) {
            return (boolean) value ? 1L : 0L;
        }
        if (value instanceof AtomicBoolean) {
            final AtomicBoolean b = (AtomicBoolean) value;
            return b.get() ? 1L : 0L;
        }
        if (value == null && !strict) {
            return 0L;
        }
        return null;
    }

    /**
     * Add two values together.
     * <p>
     * If any numeric add fails on coercion to the appropriate type,
     * treat as Strings and do concatenation.
     * </p>
     *
     * @param left  left argument
     * @param right  right argument
     * @return left + right.
     */
    public Object add(final Object left, final Object right) {
        if (left == null && right == null) {
            return controlNullNullOperands(JexlOperator.ADD);
        }
        final boolean strconcat = strict
                            ? left instanceof String || right instanceof String
                            : left instanceof String && right instanceof String;
        if (!strconcat) {
            try {
                // if either are no longer than integers use that type
                if (isLongPrecisionNumber(left) && isLongPrecisionNumber(right)) {
                    long l = ((Number) left).longValue();
                    long r = ((Number) right).longValue();
                    long result = l + r;
                    if (left instanceof Long || right instanceof Long) {
                        if ((l & r & ~result) < 0) {
                            return extendedLong(result, (byte) -1);
                        } else if ((~l & ~r & result) < 0) {
                            return extendedLong(result, (byte) 0);
                        } else {
                            return result;
                        }
                    } else if (result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE) {
                        return (int) result;
                    } else {
                        return result;
                    }
                }
                // if either are bigdecimal use that type
                if (left instanceof BigDecimal || right instanceof BigDecimal) {
                    final BigDecimal l = toBigDecimal(left);
                    final BigDecimal r = toBigDecimal(right);
                    final BigDecimal result = l.add(r, getMathContext());
                    return narrowBigDecimal(left, right, result);
                }
                // if either are floating point (double or float) use double
                if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
                    final double l = toDouble(left);
                    final double r = toDouble(right);
                    return l + r;
                }
                // otherwise treat as (big) integers
                final BigInteger l = toBigInteger(left);
                final BigInteger r = toBigInteger(right);
                final BigInteger result = l.add(r);
                return narrowBigInteger(left, right, result);
            } catch (final ArithmeticException nfe) {
                // ignore and continue in sequence
            }
        }
        return (left == null? "" : toString(left)).concat(right == null ? "" : toString(right));
    }

    /**
     * Default self assign implementation for Add.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left + right.
     */
    public Object selfAdd(Object left, Object right) {
        return add(left, right);
    }

    /**
     * Default self assign implementation for Increment.
     *
     * @param left  left argument
     * @return ++left.
     */
    public Object increment(Object left) {
        if (left instanceof BigInteger) {
            return selfAdd(left, BigInteger.ONE);
        }
        if (left instanceof BigDecimal) {
            return selfAdd(left, BigDecimal.ONE);
        }
        if (isFloatingPoint(left)) {
            return selfAdd(left, DOUBLE_ONE);
        }

        return selfAdd(left, 1);
    }

    /**
     * Divide the left value by the right.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left / right
     * @throws ArithmeticException if right == 0
     */
    public Object divide(final Object left, final Object right) {
        if (left == null && right == null) {
            return controlNullNullOperands(JexlOperator.DIVIDE);
        }
        // if either are no longer than long use that type
        if (isLongPrecisionNumber(left) && isLongPrecisionNumber(right)) {
            long l = ((Number) left).longValue();
            long r = ((Number) right).longValue();
            if (r == 0L) {
                throw new ArithmeticException("/");
            }
            long result = l / r;
            if (left instanceof Long || right instanceof Long) {
                return result;
            } else if (result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE) {
                return (int) result;
            } else {
                return result;
            }
        }
        // if either are bigdecimal use that type
        if (left instanceof BigDecimal || right instanceof BigDecimal) {
            final BigDecimal l = toBigDecimal(left);
            final BigDecimal r = toBigDecimal(right);
            if (BigDecimal.ZERO.equals(r)) {
                throw new ArithmeticException("/");
            }
            final BigDecimal result = l.divide(r, getMathContext());
            return narrowBigDecimal(left, right, result);
        }
        // if either are floating point (double or float) use double
        if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
            final double l = toDouble(left);
            final double r = toDouble(right);
            if (r == 0.0) {
                throw new ArithmeticException("/");
            }
            return l / r;
        }
        // otherwise treat as integers
        final BigInteger l = toBigInteger(left);
        final BigInteger r = toBigInteger(right);
        if (BigInteger.ZERO.equals(r)) {
            throw new ArithmeticException("/");
        }
        final BigInteger result = l.divide(r);
        return narrowBigInteger(left, right, result);
    }

    /**
     * Default self assign implementation for Divide.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left / right.
     */
    public Object selfDivide(Object left, Object right) {
        return divide(left, right);
    }

    /**
     * left value modulo right.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left % right
     * @throws ArithmeticException if right == 0.0
     */
    public Object mod(final Object left, final Object right) {
        if (left == null && right == null) {
            return controlNullNullOperands(JexlOperator.MOD);
        }
        // if either are no longer than long use that type
        if (isLongPrecisionNumber(left) && isLongPrecisionNumber(right)) {
            long l = ((Number) left).longValue();
            long r = ((Number) right).longValue();
            if (r == 0L) {
                throw new ArithmeticException("%");
            }
            long result = l % r;
            if (left instanceof Long || right instanceof Long) {
                return result;
            } else if (result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE) {
                return (int) result;
            } else {
                return result;
            }
        }
        // if either are bigdecimal use that type
        if (left instanceof BigDecimal || right instanceof BigDecimal) {
            final BigDecimal l = toBigDecimal(left);
            final BigDecimal r = toBigDecimal(right);
            if (BigDecimal.ZERO.equals(r)) {
                throw new ArithmeticException("%");
            }
            final BigDecimal remainder = l.remainder(r, getMathContext());
            return narrowBigDecimal(left, right, remainder);
        }
        // if either are floating point (double or float) use double
        if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
            final double l = toDouble(left);
            final double r = toDouble(right);
            if (r == 0.0) {
                throw new ArithmeticException("%");
            }
            return l % r;
        }
        // otherwise treat as integers
        final BigInteger l = toBigInteger(left);
        final BigInteger r = toBigInteger(right);
        if (BigInteger.ZERO.equals(r)) {
            throw new ArithmeticException("%");
        }
        final BigInteger result = l.remainder(r);
        return narrowBigInteger(left, right, result);
    }

    /**
     * Default self assign implementation for Mod.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left % right.
     */
    public Object selfMod(Object left, Object right) {
        return mod(left, right);
    }

    /**
     * Checks if the product of the arguments overflows a {@code long}.
     * <p>see java8 Math.multiplyExact
     * @param x the first value
     * @param y the second value
     * @param r the product
     * @return true if product fits a long, false if it overflows
     */
    @SuppressWarnings("MagicNumber")
    protected static boolean isMultiplyExact(final long x, final long y, final long r) {
        final long ax = Math.abs(x);
        final long ay = Math.abs(y);
        // Some bits greater than 2^31 that might cause overflow
        // Check the result using the divide operator
        // and check for the special case of Long.MIN_VALUE * -1
        return !(((ax | ay) >>> (Integer.SIZE - 1) != 0)
                 && ((y != 0 && r / y != x)
                     || (x == Long.MIN_VALUE && y == -1)));
    }

    /**
     * Multiply the left value by the right.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left * right.
     */
    public Object multiply(final Object left, final Object right) {
        if (left == null && right == null) {
            return controlNullNullOperands(JexlOperator.MULTIPLY);
        }
        // if either are no longer than integers use that type
        if (isIntegerPrecisionNumber(left) && isIntegerPrecisionNumber(right)) {
            long l = ((Number) left).longValue();
            long r = ((Number) right).longValue();
            long result = l * r;
            if (result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE) {
                return (int) result;
            } else {
                return result;
            }
        }
        // if either are bigdecimal use that type
        if (left instanceof BigDecimal || right instanceof BigDecimal) {
            final BigDecimal l = toBigDecimal(left);
            final BigDecimal r = toBigDecimal(right);
            final BigDecimal result = l.multiply(r, getMathContext());
            return narrowBigDecimal(left, right, result);
        }
        // if either are floating point (double or float) use double
        if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
            final double l = toDouble(left);
            final double r = toDouble(right);
            return l * r;
        }
        // otherwise treat as integers
        final BigInteger l = toBigInteger(left);
        final BigInteger r = toBigInteger(right);
        final BigInteger result = l.multiply(r);
        return narrowBigInteger(left, right, result);
    }

    /**
     * Default self assign implementation for Multiply.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left * right.
     */
    public Object selfMultiply(Object left, Object right) {
        return multiply(left, right);
    }

    /**
     * Subtract the right value from the left.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left - right.
     */
    public Object subtract(final Object left, final Object right) {
        if (left == null && right == null) {
            return controlNullNullOperands(JexlOperator.SUBTRACT);
        }
        // if either are no longer than integers use that type
        if (isLongPrecisionNumber(left) && isLongPrecisionNumber(right)) {
            long l = ((Number) left).longValue();
            long r = ((Number) right).longValue();
            long result = l - r;
            if (left instanceof Long || right instanceof Long) {
                if ((l & ~r & ~result) < 0) {
                    return extendedLong(result, (byte) 0);
                } else if ((~l & r & result) < 0) {
                    return extendedLong(result, (byte) -1);
                } else {
                    return result;
                }
            } else if (result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE) {
                return (int) result;
            } else {
                return result;
            }
        }
        // if either are bigdecimal use that type
        if (left instanceof BigDecimal || right instanceof BigDecimal) {
            final BigDecimal l = toBigDecimal(left);
            final BigDecimal r = toBigDecimal(right);
            final BigDecimal result = l.subtract(r, getMathContext());
            return narrowBigDecimal(left, right, result);
        }
        // if either are floating point (double or float) use double
        if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
            final double l = toDouble(left);
            final double r = toDouble(right);
            return l - r;
        }
        // otherwise treat as integers
        final BigInteger l = toBigInteger(left);
        final BigInteger r = toBigInteger(right);
        final BigInteger result = l.subtract(r);
        return narrowBigInteger(left, right, result);
    }

    /**
     * Default self assign implementation for Subtract.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left - right.
     */
    public Object selfSubtract(Object left, Object right) {
        return subtract(left, right);
    }

    /**
     * Default self assign implementation for Decrement.
     *
     * @param left  left argument
     * @return --left.
     */
    public Object decrement(Object left) {
        if (left instanceof BigInteger) {
            return selfSubtract(left, BigInteger.ONE);
        }
        if (left instanceof BigDecimal) {
            return selfSubtract(left, BigDecimal.ONE);
        }
        if (isFloatingPoint(left)) {
            return selfSubtract(left, DOUBLE_ONE);
        }
        return selfSubtract(left, 1);
    }

    /**
     * Negates a value (unary minus for numbers).
     *
     * @param val the value to negate
     * @return the negated value
     */
    public Object negate(final Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof Integer) {
            return -((Integer) val);
        }
        if (val instanceof Double) {
            return -((Double) val);
        }
        if (val instanceof Long) {
            return -((Long) val);
        }
        if (val instanceof BigDecimal) {
            return ((BigDecimal) val).negate();
        }
        if (val instanceof BigInteger) {
            return ((BigInteger) val).negate();
        }
        if (val instanceof Float) {
            return -((Float) val);
        }
        if (val instanceof Short) {
            return (short) -((Short) val);
        }
        if (val instanceof Byte) {
            return (byte) -((Byte) val);
        }
        if (val instanceof Boolean) {
            return !(Boolean) val;
        }
        if (val instanceof AtomicBoolean) {
            return !((AtomicBoolean) val).get();
        }
        if (val instanceof Range) {
            Range r = (Range) val;
            return createRange(r.getTo(), r.getFrom());
        }
        throw new ArithmeticException("Object negate:(" + val + ")");
    }

    /**
     * Whether negate called with a given argument will always return the same result.
     * <p>This is used to determine whether negate results on number literals can be cached.
     * If the result on calling negate with the same constant argument may change between calls,
     * which means the function is not deterministic, this method must return false.
     * @see #isNegateStable()
     * @return true if negate is idempotent, false otherwise
     */
    public boolean isNegateStable() {
        return true;
    }

    /**
     * Shifts a bit pattern to the right.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left &lt;&lt; right.
     */
    public Object shiftLeft(Object left, Object right) {
        if (left == null && right == null) {
            return controlNullNullOperands();
        }
        if (left instanceof Closure) {
            if (right instanceof Closure) {
                return Closure.create((Closure) right, (Closure) left);
            } else if (right instanceof Object[]) {
                return ((Closure) left).execute(null, (Object[]) right);
            } else {
                return ((Closure) left).execute(null, right);
            }
        }
        // if either are no longer than integers use that type
        if (isIntegerPrecisionNumber(left)) {
            int l = ((Number) left).intValue();
            int r = toInteger(right);
            int result = l << r;
            return result;
        }
        // if either are no longer than long integers use that type
        if (isLongPrecisionNumber(left)) {
            long l = ((Number) left).longValue();
            int r = toInteger(right);
            long result = l << r;
            return result;
        }
        // otherwise treat as big integers
        BigInteger l = toBigInteger(left);
        int r = toInteger(right);
        BigInteger result = l.shiftLeft(r);
        return result;
    }

    /**
     * Default self assign implementation for Left Shift.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left &lt;&lt;= right.
     */
    public Object selfShiftLeft(Object left, Object right) {
        return shiftLeft(left, right);
    }

    /**
     * Shifts a bit pattern to the right.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left &gt;&gt; right.
     */
    public Object shiftRight(Object left, Object right) {

        if (left == null && right == null) {
            return controlNullNullOperands();
        }
        if (right instanceof Closure) {
            if (left instanceof Closure) {
                return Closure.create((Closure) left, (Closure) right);
            } else if (left instanceof Object[]) {
                return ((Closure) right).execute(null, (Object[]) left);
            } else {
                return ((Closure) right).execute(null, left);
            }

        }
        // if either are no longer than integers use that type
        if (isIntegerPrecisionNumber(left)) {
            int l = ((Number) left).intValue();
            int r = toInteger(right);
            int result = l >> r;
            return result;
        }
        // if either are no longer than long integers use that type
        if (isLongPrecisionNumber(left)) {
            long l = ((Number) left).longValue();
            int r = toInteger(right);
            long result = l >> r;
            return result;
        }
        // otherwise treat as big integers
        BigInteger l = toBigInteger(left);
        int r = toInteger(right);
        BigInteger result = l.shiftRight(r);
        return result;
    }

    /**
     * Default self assign implementation for Right Shift.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left &gt;&gt; right.
     */
    public Object selfShiftRight(Object left, Object right) {
        return shiftRight(left, right);
    }

    /**
     * Shifts a bit pattern to the right unsigned.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left &gt;&gt;&gt; right.
     */
    public Object shiftRightUnsigned(Object left, Object right) {
        if (left == null && right == null) {
            return controlNullNullOperands();
        }
        // if either are no longer than integers use that type
        if (isIntegerPrecisionNumber(left)) {
            int l = ((Number) left).intValue();
            int r = toInteger(right);
            int result = l >>> r;
            return result;
        }
        // otherwise treat as long integers
        long l = toLong(left);
        int r = toInteger(right);
        long result = l >>> r;
        return result;
    }

    /**
     * Default self assign implementation for Right Unsigned Shift.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left &gt;&gt;&gt; right.
     */
    public Object selfShiftRightUnsigned(Object left, Object right) {
        return shiftRightUnsigned(left, right);
    }

    /**
     * Positivize value (unary plus for numbers).
     * <p>C/C++/C#/Java perform integral promotion of the operand, ie
     * cast to int if type can be represented as int without loss of precision.
     * @see #isPositivizeStable()
     * @param val the value to positivize
     * @return the positive value
     */
    public Object positivize(final Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof Byte) {
            return +(Byte) val;
        } else if (val instanceof Short) {
            return +(Short) val;
        } else if (val instanceof Character) {
            return +(Character) val;
        } else if (val instanceof Number) {
            return val;
        } else if (val instanceof Boolean) {
            return val;
        } else if (val instanceof AtomicBoolean) {
            return ((AtomicBoolean) val).get();
        }

        throw new ArithmeticException("Object positivize:(" + val + ")");
    }

    /**
     * Whether positivize called with a given argument will always return the same result.
     * <p>This is used to determine whether positivize results on number literals can be cached.
     * If the result on calling positivize with the same constant argument may change between calls,
     * which means the function is not deterministic, this method must return false.
     * @return true if positivize is idempotent, false otherwise
     */
    public boolean isPositivizeStable() {
        return true;
    }

    /**
     * Test if a condition is true or false.
     * @param object the object to use as condition
     * @return true or false
     * @since 3.3
     */
    public boolean testPredicate(final Object object) {
        final boolean strictCast = isStrict(JexlOperator.CONDITION);
        return toBoolean(strictCast, object);
    }

    /**
     * Test if left contains right (right matches/in left).
     * <p>Beware that this &quot;contains &quot; method arguments order is the opposite of the
     * &quot;in/matches&quot; operator arguments.
     * <code>x =~ y</code> means <code>y contains x</code> thus <code>contains(x, y)</code>.</p>
     * <p>When this method returns null during evaluation, the operator code continues trying to find
     * one through the uberspect.</p>
     * @param container the container
     * @param value the value
     * @return test result or null if there is no arithmetic solution
     */
    public Boolean contains(final Object container, final Object value) {
        if (value == null && container == null) {
            //if both are null L == R
            return true;
        }
        if (value == null || container == null) {
            // we know both aren't null, therefore L != R
            return false;
        }
        // use arithmetic / pattern matching ?
        if (container instanceof java.util.regex.Pattern) {
            return ((java.util.regex.Pattern) container).matcher(value.toString()).matches();
        }
        if (container instanceof CharSequence) {
            return value.toString().matches(container.toString());
        }
        // try contains on map key
        if (container instanceof Map<?, ?>) {
            if (value instanceof Map<?, ?>) {
                return ((Map<?, ?>) container).keySet().containsAll(((Map<?, ?>) value).keySet());
            }
            return ((Map<?, ?>) container).containsKey(value);
        }
        // try contains on collection
        return collectionContains(container, value);
    }

    /**
     * Checks whether a potential collection contains another.
     * <p>Made protected to make it easier to override if needed.</p>
     * @param collection the container which can be a collection or an array (even of primitive)
     * @param value the value which can be a collection or an array (even of primitive) or a singleton
     * @return test result or null if there is no arithmetic solution
     */
    protected Boolean collectionContains(final Object collection, final Object value) {
        // convert arrays if needed
        final Object left = arrayWrap(collection);
        if (left instanceof Collection) {
            final Object right = arrayWrap(value);
            if (right instanceof Collection) {
                return ((Collection<?>) left).containsAll((Collection<?>) right);
            }
            return ((Collection<?>) left).contains(value);
        }
        return null;
    }

    /**
     * Attempts transformation of potential array in an abstract list or leave as is.
     * <p>An array (as in int[]) is not convenient to call methods so when encountered we turn them into lists</p>
     * @param container an array or on object
     * @return an abstract list wrapping the array instance or the initial argument
     * @see org.apache.commons.jexl3.internal.introspection.ArrayListWrapper
     */
    private static Object arrayWrap(final Object container) {
        return container.getClass().isArray()
                ? new org.apache.commons.jexl3.internal.introspection.ArrayListWrapper(container)
                : container;
    }

    /**
     * Test if left ends with right.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left $= right if there is no arithmetic solution
     */
    public Boolean endsWith(final Object left, final Object right) {
        if (left == null && right == null) {
            //if both are null L == R
            return true;
        }
        if (left == null || right == null) {
            // we know both aren't null, therefore L != R
            return false;
        }
        if (left instanceof CharSequence) {
            return (toString(left)).endsWith(toString(right));
        }
        return null;
    }

    /**
     * Test if left starts with right.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left ^= right or null if there is no arithmetic solution
     */
    public Boolean startsWith(final Object left, final Object right) {
        if (left == null && right == null) {
            //if both are null L == R
            return true;
        }
        if (left == null || right == null) {
            // we know both aren't null, therefore L != R
            return false;
        }
        if (left instanceof CharSequence) {
            return (toString(left)).startsWith(toString(right));
        }
        return null;
    }

    /**
     * Check for emptiness of various types: Number, Collection, Array, Map, String.
     * <p>Override or overload this method to add new signatures to the empty operators.
     * @param object the object to check the emptiness of
     * @return the boolean or false if object is not null
     * @since 3.2
     */
    public Boolean empty(final Object object) {
        return object == null || isEmpty(object, false);
    }

    /**
     * Check for emptiness of various types: Number, Collection, Array, Map, String.
     *
     * @param object the object to check the emptiness of
     * @return the boolean or null if there is no arithmetic solution
     */
    public Boolean isEmpty(final Object object) {
        return isEmpty(object, object == null);
    }

    /**
     * Check for emptiness of various types: Number, Collection, Array, Map, String.
     *
     * @param object the object to check the emptiness of
     * @param def the default value if object emptiness can not be determined
     * @return the boolean or null if there is no arithmetic solution
     */
    public Boolean isEmpty(final Object object, final Boolean def) {
        if (object != null) {
            if (object instanceof Double) {
                double d = ((Number) object).doubleValue();
                return Double.isNaN(d) || d == 0.d;
            }
            if (object instanceof Float) {
                float f = ((Number) object).floatValue();
                return Float.isNaN(f) || f == 0.f;
            }
            if (object instanceof BigDecimal) {
                return BigDecimal.ZERO.equals(object);
            }
            if (object instanceof BigInteger) {
                return BigInteger.ZERO.equals(object);
            }
            if (object instanceof Number) {
                long l = ((Number) object).longValue();
                return l == 0L;
            }
            if (object instanceof CharSequence) {
                return ((CharSequence) object).length() == 0;
            }
            if (object.getClass().isArray()) {
                return Array.getLength(object) == 0;
            }
            if (object instanceof Collection<?>) {
                return ((Collection<?>) object).isEmpty();
            }
            // Map isn't a collection
            if (object instanceof Map<?, ?>) {
                return ((Map<?, ?>) object).isEmpty();
            }
            if (object instanceof Iterator<?>) {
                return !((Iterator<?>) object).hasNext();
            }
        }
        return def;
    }

    /**
     * Calculate the <code>size</code> of various types: Collection, Array, Map, String.
     *
     * @param object the object to get the size of
     * @return the <i>size</i> of object, 0 if null, 1 if there is no <i>better</i> solution
     */
    public Integer size(final Object object) {
        return size(object, object == null? 0 : 1);
    }

    /**
     * Calculate the <code>size</code> of various types: Collection, Array, Map, String.
     *
     * @param object the object to get the size of
     * @param def the default value if object size can not be determined
     * @return the size of object or null if there is no arithmetic solution
     */
    public Integer size(final Object object, final Integer def) {
        if (object instanceof CharSequence) {
            return ((CharSequence) object).length();
        }
        if (object.getClass().isArray()) {
            return Array.getLength(object);
        }
        if (object instanceof Collection<?>) {
            return ((Collection<?>) object).size();
        }
        if (object instanceof Map<?, ?>) {
            return ((Map<?, ?>) object).size();
        }
        return def;
    }

    /**
     * Dereferences various types: SoftReference, AtomicReference, Optional, ThreadLocal etc
     *
     * @param object the object to be derefenced
     * @return the object or TRY_FAILED if there is no dereference path
     */
    public Object indirect(Object object) {
        if (object instanceof Reference) {
            return ((Reference) object).get();
        }
        if (object instanceof ThreadLocal) {
            return ((ThreadLocal) object).get();
        }
        if (object instanceof Optional) {
            Optional o = (Optional) object;
            return strict || o.isPresent() ? o.get() : null;
        }
        if (object instanceof AtomicReference) {
            return ((AtomicReference) object).get();
        }
        if (object instanceof AtomicBoolean) {
            return ((AtomicBoolean) object).get();
        }
        if (object instanceof AtomicInteger) {
            return ((AtomicInteger) object).get();
        }
        if (object instanceof AtomicLong) {
            return ((AtomicLong) object).get();
        }
        return JexlEngine.TRY_FAILED;
    }

    /**
     * Property get operator
     *
     * @param object the object to be derefenced
     * @param key the property key
     * @return JexlEngine.TRY_FAILED or other result if succesful
     * @throws Exception if access fails
     */
    public Object propertyGet(Object object, Object key) throws Exception {
        if (object instanceof IndexedType.IndexedContainer) {
            return ((IndexedType.IndexedContainer) object).get(key);
        }
        if (object != null && object.getClass().isArray()) {
            return "length".equals(key) ? Array.getLength(object) : Array.get(object, toInteger(key));
        }
        if (object instanceof JexlContext) {
            return ((JexlContext) object).get(toString(key));
        }
        return JexlEngine.TRY_FAILED;
    }

    /**
     * Field get operator
     *
     * @param object the object to be derefenced
     * @param key the field key
     * @return JexlEngine.TRY_FAILED or other result if succesful
     * @throws Exception if access fails
     */
    public Object fieldGet(Object object, Object key) throws Exception {
        return JexlEngine.TRY_FAILED;
    }

    /**
     * Array get operator
     *
     * @param object the object to be derefenced
     * @param key the property key
     * @return JexlEngine.TRY_FAILED or other result if succesful
     * @throws Exception if access fails
     */
    public Object arrayGet(Object object, Object key) throws Exception {
        return propertyGet(object, key);
    }

    /**
     * Property set operator
     *
     * @param object the object to be derefenced
     * @param key the property key
     * @param value the value to assign to property
     * @return JexlEngine.TRY_FAILED or other result if succesful
     * @throws Exception if assignment fails
     */
    public Object propertySet(Object object, Object key, Object value) throws Exception {
        if (object instanceof IndexedType.IndexedContainer) {
            return ((IndexedType.IndexedContainer) object).set(key, value);
        }
        if (object != null && object.getClass().isArray()) {
            Array.set(object, toInteger(key), value);
            return null;
        }
        if (object instanceof JexlContext) {
            ((JexlContext) object).set(toString(key), value);
            return null;
        }
        return JexlEngine.TRY_FAILED;
    }

    /**
     * Field set operator
     *
     * @param object the object to be derefenced
     * @param key the field key
     * @param value the value to assign to property
     * @return JexlEngine.TRY_FAILED or other result if succesful
     * @throws Exception if assignment fails
     */
    public Object fieldSet(Object object, Object key, Object value) throws Exception {
        return JexlEngine.TRY_FAILED;
    }

    /**
     * Array set operator
     *
     * @param object the object to be derefenced
     * @param key the property key
     * @param value the value to assign to property
     * @return JexlEngine.TRY_FAILED or other result if succesful
     * @throws Exception if assignment fails
     */
    public Object arraySet(Object object, Object key, Object value) throws Exception {
        return propertySet(object, key, value);
    }

    /**
     * Property delete operator
     *
     * @param object the object to be derefenced
     * @param key the property key
     * @return JexlEngine.TRY_FAILED or other result if succesful
     * @throws Exception if assignment fails
     */
    public Object propertyDelete(Object object, Object key) throws Exception {
        if (object instanceof JexlContext) {
            ((JexlContext) object).set(toString(key), null);
            return null;
        }
        return JexlEngine.TRY_FAILED;
    }

    /**
     * Array delete operator
     *
     * @param object the object to be derefenced
     * @param key the property key
     * @return JexlEngine.TRY_FAILED or other result if succesful
     * @throws Exception if assignment fails
     */
    public Object arrayDelete(Object object, Object key) throws Exception {
        return propertyDelete(object, key);
    }

    /**
     * Assigns value to various types: ThreadLocal, AtomicReference etc
     *
     * @param object the object to be derefenced
     * @param value the value to assign
     * @return JexlOperator.ASSIGN or null if there is no dereferenced assignment path
     */
    public Object indirectAssign(Object object, Object value) {
        if (object instanceof ThreadLocal) {
            ((ThreadLocal) object).set(value);
            return value;
        }
        if (object instanceof AtomicReference) {
            ((AtomicReference) object).set(value);
            return value;
        }
        if (object instanceof AtomicBoolean) {
            ((AtomicBoolean) object).set(toBoolean(value));
            return value;
        }
        if (object instanceof AtomicInteger) {
            ((AtomicInteger) object).set(toInteger(value));
            return value;
        }
        if (object instanceof AtomicLong) {
            ((AtomicLong) object).set(toLong(value));
            return value;
        }
        return JexlEngine.TRY_FAILED;
    }

    /**
     * Performs a bitwise and.
     *
     * @param left  the left operand
     * @param right the right operator
     * @return left &amp; right
     */
    public Object and(final Object left, final Object right) {
        final long l = toLong(left);
        final long r = toLong(right);
        return l & r;
    }

    /**
     * Default self assign implementation for bitwise and.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left &amp; right.
     */
    public Object selfAnd(Object left, Object right) {
        return and(left, right);
    }

    /**
     * Performs a bitwise or.
     *
     * @param left  the left operand
     * @param right the right operator
     * @return left | right
     */
    public Object or(final Object left, final Object right) {
        final long l = toLong(left);
        final long r = toLong(right);
        return l | r;
    }

    /**
     * Performs a bitwise diff.
     *
     * @param left  the left operand
     * @param right the right operator
     * @return left \ right
     */
    public Object diff(final Object left, final Object right) {
        final long l = toLong(left);
        final long r = toLong(right);
        return l & ~r;
    }

    /**
     * Default self assign implementation for bitwise or.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left | right.
     */
    public Object selfOr(Object left, Object right) {
        return or(left, right);
    }

    /**
     * Default self assign implementation for bitwise difference.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left \ right.
     */
    public Object selfDiff(Object left, Object right) {
        return diff(left, right);
    }

    /**
     * Performs a bitwise xor.
     *
     * @param left  the left operand
     * @param right the right operator
     * @return left ^ right
     */
    public Object xor(final Object left, final Object right) {
        final long l = toLong(left);
        final long r = toLong(right);
        return l ^ r;
    }

    /**
     * Default self assign implementation for bitwise xor.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left ^ right.
     */
    public Object selfXor(Object left, Object right) {
        return xor(left, right);
    }

    /**
     * Performs a bitwise complement.
     *
     * @param val the operand
     * @return ~val
     */
    public Object complement(final Object val) {
        final long l = toLong(val);
        return ~l;
    }

    /**
     * Performs a logical not.
     *
     * @param val the operand
     * @return !val
     */
    public Object not(final Object val) {
        final boolean strictCast = isStrict(JexlOperator.NOT);
        return !toBoolean(strictCast, val);
    }

    /**
     * Performs an explicit type cast.
     *
     * @param type the type
     * @param val the operand
     * @return cast value
     */
    public Object cast(Class type, Object val) {
        if (type == Object.class)
            return val;

        type = getWrapperClass(type);

        if (type.isInstance(val)) {
            return val;
        } 

        if (val instanceof Optional) {
            return cast(type, ((Optional) val).get());
        }

        if (type == Integer.class) {
            return toInteger(val);
        } else if (type == Long.class) {
            return toLong(val);
        } else if (type == Float.class) {
            return toFloat(val);
        } else if (type == Double.class) {
            return toDouble(val);
        } else if (type == Boolean.class) {
            return toBoolean(val);
        } else if (type == Byte.class) {
            return toByte(val);
        } else if (type == Short.class) {
            return toShort(val);
        } else if (type == Character.class) {
            return toCharacter(val);
        } else if (type == String.class) {
            return toString(val);
        } else if (type == BigInteger.class) {
            return toBigInteger(val);
        } else if (type == BigDecimal.class) {
            return toBigDecimal(val);
        } else if (type == Future.class) {
            return toFuture(val);
        }
        return type.cast(val);
    }

    /**
     * Performs an implicit type cast.
     *
     * @param type the type
     * @param val the operand
     * @return cast value
     */
    public Object implicitCast(Class type, Object val) {

        if (type == Object.class)
            return val;

        type = getWrapperClass(type);

        if (type.isInstance(val)) {
            return val;
        } else if (type == Short.class && val instanceof Byte) {
            return ((Number) val).shortValue();
        } else if (type == Integer.class && (val instanceof Byte || val instanceof Short || val instanceof AtomicInteger)) {
            return ((Number) val).intValue();
        } else if (type == Long.class && (val instanceof Byte || val instanceof Short || val instanceof Integer ||
                                   val instanceof AtomicInteger || val instanceof AtomicLong)) {
            return ((Number) val).longValue();
        } else if (type == Float.class && (val instanceof Byte || val instanceof Short || val instanceof Integer || 
                                   val instanceof Long || val instanceof AtomicInteger || val instanceof AtomicLong)) {
            return ((Number) val).floatValue();
        } else if (type == Double.class) {
            return ((Number) val).doubleValue();
        } else if (val instanceof CharSequence && type == Character.class) {
            CharSequence cs = (CharSequence) val;
            if (cs.length() == 1) {
                return cs.charAt(0);
            }
        } else if (type == Boolean.class && val instanceof AtomicBoolean) {
            return ((AtomicBoolean) val).get();
        }
        return type.cast(val);
    }

    /**
     * Returns a wrapper class of the type.
     *
     * @param type the type
     * @return wrapper class
     */
    public static Class getWrapperClass(Class type) {
        if (type.isPrimitive()) {
            if (type == Integer.TYPE) {
                return Integer.class;
            } else if (type == Long.TYPE) {
                return Long.class;
            } else if (type == Float.TYPE) {
                return Float.class;
            } else if (type == Double.TYPE) {
                return Double.class;
            } else if (type == Boolean.TYPE) {
                return Boolean.class;
            } else if (type == Byte.TYPE) {
                return Byte.class;
            } else if (type == Short.TYPE) {
                return Short.class;
            } else if (type == Character.TYPE) {
                return Character.class;
            }
        }
        return type;
    }
	
    /**
     * Any override of this method (pre 3.3) should be modified to match the new signature.
     * @param left left operand
     * @param right right operand
     * @param symbol the operator symbol
     * @return -1 if left &lt; right; +1 if left &gt; right; 0 if left == right
     * {@link JexlArithmetic#compare(Object, Object, JexlOperator)}
     * @deprecated 3.3
     */
    @Deprecated
    protected int compare(final Object left, final Object right, final String symbol) {
        JexlOperator operator;
        try {
            operator = JexlOperator.valueOf(symbol);
        } catch (final IllegalArgumentException xill) {
            // ignore
            operator = JexlOperator.EQ;
        }
        return doCompare(left, right, operator);
    }

    /**
     * Determines if the compare method(Object, Object, String) is overriden in this class or one of its
     * superclasses.
     */
    private final boolean compare321 = computeCompare321(this);
    private static boolean computeCompare321(final JexlArithmetic arithmetic) {
        Class<?> arithmeticClass = arithmetic.getClass();
        while(arithmeticClass != JexlArithmetic.class) {
            try {
                Method cmp = arithmeticClass.getDeclaredMethod("compare", Object.class, Object.class, String.class);
               if (cmp != null && cmp.getDeclaringClass() != JexlArithmetic.class) {
                   return true;
               }
            } catch (NoSuchMethodException xany) {
                arithmeticClass = arithmeticClass.getSuperclass();
            }
        }
        return false;
    }

    /**
     * Performs a comparison.
     *
     * @param left     the left operand
     * @param right    the right operator
     * @param operator the operator
     * @return -1 if left &lt; right; +1 if left &gt; right; 0 if left == right
     * @throws ArithmeticException if either left or right is null
     */
    protected int compare(final Object left, final Object right, final JexlOperator operator) {
        // this is a temporary way of allowing pre-3.3 code that overrode compare() to still call
        // the user method. This method will merge with doCompare in 3.4 and the compare321 flag will disappear.
        return compare321
                ? compare(left, right, operator.toString())
                : doCompare(left, right, operator);
    }

    private int doCompare(final Object left, final Object right, final JexlOperator operator) {
        final boolean strictCast = isStrict(operator);
        if (left != null && right != null) {
            try {
                if (left instanceof BigDecimal || right instanceof BigDecimal) {
                    final BigDecimal l = toBigDecimal(strictCast, left);
                    final BigDecimal r = toBigDecimal(strictCast, right);
                    return l.compareTo(r);
                }
                if (left instanceof BigInteger || right instanceof BigInteger) {
                    final BigInteger l = toBigInteger(strictCast, left);
                    final BigInteger r = toBigInteger(strictCast, right);
                    return l.compareTo(r);
                }
                if (isFloatingPoint(left) || isFloatingPoint(right)) {
                    final double lhs = toDouble(strictCast, left);
                    final double rhs = toDouble(strictCast, right);
                    if (Double.isNaN(lhs)) {
                        if (Double.isNaN(rhs)) {
                            return 0;
                        }
                        return -1;
                    }
                    if (Double.isNaN(rhs)) {
                        // lhs is not NaN
                        return +1;
                    }
                    return Double.compare(lhs, rhs);
                }
                if (isNumberable(left) || isNumberable(right)) {
                    final long lhs = toLong(strictCast, left);
                    final long rhs = toLong(strictCast, right);
                    return Long.compare(lhs, rhs);
                }
                if (left instanceof String || right instanceof String) {
                    return toString(left).compareTo(toString(right));
                }
            } catch (final CoercionException ignore) {
                // ignore it, continue in sequence
            }

            if (EQ == operator) {
                return left.equals(right) ? 0 : -1;
            }

            if (left instanceof Comparable<?>) {
                @SuppressWarnings("unchecked") // OK because of instanceof check above
                final Comparable<Object> comparable = (Comparable<Object>) left;
                try {
                    return comparable.compareTo(right);
                } catch (final ClassCastException castException) {
                    // ignore it, continue in sequence
                }
            }
            if (right instanceof Comparable<?>) {
                @SuppressWarnings("unchecked") // OK because of instanceof check above
                final Comparable<Object> comparable = (Comparable<Object>) right;
                try {
                    return -comparable.compareTo(left);
                } catch (final ClassCastException castException) {
                    // ignore it, continue in sequence
                }
            }
        }
        throw new ArithmeticException("Object comparison:(" + left +
                " " + operator.getOperatorSymbol()
                + " " + right + ")");
    }

    /**
     * Test if left and right are equal.
     *
     * @param left  left argument
     * @param right right argument
     * @return the test result
     */
    public boolean equals(final Object left, final Object right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        final boolean strictCast = isStrict(EQ);
        if (left instanceof Boolean || right instanceof Boolean) {
            return toBoolean(left) == toBoolean(strictCast, right);
        }
        return compare(left, right, EQ) == 0;
    }

    /**
     * Test if left &lt; right.
     *
     * @param left  left argument
     * @param right right argument
     * @return the test result
     */
    public boolean lessThan(final Object left, final Object right) {
        if ((left == right) || (left == null) || (right == null)) {
            return false;
        }
        return compare(left, right, JexlOperator.LT) < 0;

    }

    /**
     * Test if left &gt; right.
     *
     * @param left  left argument
     * @param right right argument
     * @return the test result
     */
    public boolean greaterThan(final Object left, final Object right) {
        if ((left == right) || left == null || right == null) {
            return false;
        }
        return compare(left, right, JexlOperator.GT) > 0;
    }

    /**
     * Test if left &lt;= right.
     *
     * @param left  left argument
     * @param right right argument
     * @return the test result
     */
    public boolean lessThanOrEqual(final Object left, final Object right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return compare(left, right, JexlOperator.LTE) <= 0;
    }

    /**
     * Test if left &gt;= right.
     *
     * @param left  left argument
     * @param right right argument
     * @return the test result
     */
    public boolean greaterThanOrEqual(final Object left, final Object right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return compare(left, right, JexlOperator.GTE) >= 0;
    }

    /**
     * Coerce to a primitive boolean.
     * <p>Double.NaN, null, "false" and empty string coerce to false.</p>
     *
     * @param val value to coerce
     * @return the boolean value if coercion is possible, true if value was not null.
     */
    public boolean toBoolean(final Object val) {
        if (val == null) {
            return controlNullOperand(strict, false);
        }
        if (val instanceof Boolean) {
            return ((Boolean) val);
        }
        if (val instanceof Number) {
            final double number = toDouble(val);
            return !Double.isNaN(number) && number != 0.d;
        }
        if (val instanceof AtomicBoolean) {
            return ((AtomicBoolean) val).get();
        }
        if (val instanceof String) {
            final String strval = val.toString();
            return !strval.isEmpty() && !"false".equals(strval);
        }
        if (val instanceof Character) {
            char c = (Character) val;
            return 'T' == c || 't' == c;
        }

        // non null value is true
        return true;
    }

    /**
     * Coerce to a primitive byte.
     * <p>Double.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     *
     * @param val value to coerce
     * @return the value coerced to byte
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible
     */
    public byte toByte(Object val) {
        if (val == null) {
            return controlNullOperand(strict, (byte) 0);
        } else if (val instanceof Double) {
            Double dval = (Double) val;
            if (Double.isNaN(dval)) {
                return 0;
            } else {
                return dval.byteValue();
            }
        } else if (val instanceof Number) {
            return ((Number) val).byteValue();
        } else if (val instanceof String) {
            if ("".equals(val)) {
                return 0;
            }
            return Byte.parseByte((String) val);
        } else if (val instanceof Boolean) {
            return ((Boolean) val) ? BigInteger.ONE.byteValue() : BigInteger.ZERO.byteValue();
        } else if (val instanceof AtomicBoolean) {
            return ((AtomicBoolean) val).get() ? BigInteger.ONE.byteValue() : BigInteger.ZERO.byteValue();
        } else if (val instanceof Character) {
            return (byte)(char)val;
        }

        throw new ArithmeticException("Byte coercion: "
                + val.getClass().getName() + ":(" + val + ")");
    }

    /**
     * Coerce to a primitive short.
     * <p>Double.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     *
     * @param val value to coerce
     * @return the value coerced to short
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible
     */
    public short toShort(Object val) {
        if (val == null) {
            return controlNullOperand(strict, (short) 0);
        } else if (val instanceof Double) {
            Double dval = (Double) val;
            if (Double.isNaN(dval)) {
                return 0;
            } else {
                return dval.shortValue();
            }
        } else if (val instanceof Number) {
            return ((Number) val).shortValue();
        } else if (val instanceof String) {
            if ("".equals(val)) {
                return 0;
            }
            return Short.parseShort((String) val);
        } else if (val instanceof Boolean) {
            return ((Boolean) val) ? BigInteger.ONE.shortValue() : BigInteger.ZERO.shortValue();
        } else if (val instanceof AtomicBoolean) {
            return ((AtomicBoolean) val).get() ? BigInteger.ONE.shortValue() : BigInteger.ZERO.shortValue();
        } else if (val instanceof Character) {
            return (short)(char)val;
        }

        throw new ArithmeticException("Short coercion: "
                + val.getClass().getName() + ":(" + val + ")");
    }

    /**
     * Coerce to a primitive int.
     * <p>Double.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     *
     * @param val value to coerce
     * @return the value coerced to int
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible
     */
    public int toInteger(final Object val) {
        if (val == null) {
            return controlNullOperand(strict, 0);
        }
        if (val instanceof Double) {
            final double dval = (Double) val;
            return Double.isNaN(dval)? 0 : (int) dval;
        }
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        if (val instanceof String) {
            return parseInteger((String) val);
        }
        if (val instanceof Boolean) {
            return (boolean) val ? 1 : 0;
        }
        if (val instanceof AtomicBoolean) {
            return ((AtomicBoolean) val).get() ? 1 : 0;
        }
        if (val instanceof Character) {
            return ((Character) val);
        }
        throw new CoercionException("Integer coercion: "
                + val.getClass().getName() + ":(" + val + ")");
    }

    /**
     * Coerce to a primitive long.
     * <p>Double.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     *
     * @param val value to coerce
     * @return the value coerced to long
     * @throws ArithmeticException if value is null and mode is strict or if coercion is not possible
     */
    public long toLong(final Object val) {
        if (val == null) {
            return controlNullOperand(strict, 0L);
        }
        if (val instanceof Double) {
            final double dval = (Double) val;
            return Double.isNaN(dval)? 0L : (long) dval;
        }
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        if (val instanceof String) {
            return parseLong((String) val);
        }
        if (val instanceof Boolean) {
            return (boolean) val ? 1L : 0L;
        }
        if (val instanceof AtomicBoolean) {
            return ((AtomicBoolean) val).get() ? 1L : 0L;
        }
        if (val instanceof Character) {
            return ((Character) val);
        }
        throw new CoercionException("Long coercion: "
                + val.getClass().getName() + ":(" + val + ")");
    }


    /**
     * Convert a string to a double.
     * <>Empty string is considered as NaN.</>
     * @param arg the arg
     * @return a double
     * @throws ArithmeticException if the string can not be coerced into a double
     */
    private double parseDouble(String arg) throws ArithmeticException {
        try {
            return arg.isEmpty()? Double.NaN : Double.parseDouble(arg);
        } catch (NumberFormatException e) {
            final ArithmeticException arithmeticException = new ArithmeticException("Double coercion: ("+ arg +")");
            arithmeticException.initCause(e);
            throw arithmeticException;
        }
    }

    /**
     * Converts a string to a long.
     * <p>This ensure the represented number is a natural (not a real).</p>
     * @param arg the arg
     * @return a long
     * @throws ArithmeticException if the string can not be coerced into a long
     */
    private long parseLong(String arg) throws ArithmeticException {
        final double d = parseDouble(arg);
        if (Double.isNaN(d)) {
            return 0L;
        }
        final double f = floor(d);
        if (d == f) {
            return (long) d;
        }
        throw new CoercionException("Long coercion: ("+ arg +")");
    }

    /**
     * Converts a string to an int.
     * <p>This ensure the represented number is a natural (not a real).</p>
     * @param arg the arg
     * @return an int
     * @throws ArithmeticException if the string can not be coerced into a long
     */
    private int parseInteger(String arg) throws ArithmeticException {
        final long l = parseLong(arg);
        final int i = (int) l;
        if ((long) i == l) {
            return i;
        }
        throw new CoercionException("Int coercion: ("+ arg +")");
    }

    /**
     * Converts a string to a big integer.
     * <>Empty string is considered as 0.</>
     * @param arg the arg
     * @return a big integer
     * @throws ArithmeticException if the string can not be coerced into a big integer
     */
    private BigInteger parseBigInteger(String arg) throws ArithmeticException {
        if (arg.isEmpty()) {
            return BigInteger.ZERO;
        }
        try {
            return new BigInteger(arg);
        } catch(NumberFormatException xformat) {
            // ignore, try harder
        }
        return BigInteger.valueOf(parseLong(arg));
    }

    /**
     * Coerce to a BigInteger.
     * <p>Double.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     *
     * @param val the object to be coerced.
     * @return a BigDecimal
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible
     */
    public BigInteger toBigInteger(final Object val) {
        if (val == null) {
            return controlNullOperand(strict, BigInteger.ZERO);
        }
        if (val instanceof BigInteger) {
            return (BigInteger) val;
        }
        if (val instanceof Double) {
            final Double dval = (Double) val;
            if (Double.isNaN(dval)) {
                return BigInteger.ZERO;
            }
            return BigInteger.valueOf(dval.longValue());
        }
        if (val instanceof BigDecimal) {
            return ((BigDecimal) val).toBigInteger();
        }
        if (val instanceof Number) {
            return BigInteger.valueOf(((Number) val).longValue());
        }
        if (val instanceof Boolean) {
            return BigInteger.valueOf((boolean) val ? 1L : 0L);
        }
        if (val instanceof AtomicBoolean) {
            return BigInteger.valueOf(((AtomicBoolean) val).get() ? 1L : 0L);
        }
        if (val instanceof String) {
            return parseBigInteger((String) val);
        }
        if (val instanceof Character) {
            final int i = ((Character) val);
            return BigInteger.valueOf(i);
        }
        throw new CoercionException("BigInteger coercion: "
                + val.getClass().getName() + ":(" + val + ")");
    }

    /**
     * Coerce to a BigDecimal.
     * <p>Double.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     *
     * @param val the object to be coerced.
     * @return a BigDecimal.
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible
     */
    public BigDecimal toBigDecimal(final Object val) {
        if (val == null) {
            return controlNullOperand(strict, BigDecimal.ZERO);
        }
        if (val instanceof BigDecimal) {
            return roundBigDecimal((BigDecimal) val);
        }
        if (val instanceof Double) {
            if (Double.isNaN(((Double) val))) {
                return BigDecimal.ZERO;
            }
            return roundBigDecimal(new BigDecimal(val.toString(), getMathContext()));
        }
        if (val instanceof Number) {
            return roundBigDecimal(new BigDecimal(val.toString(), getMathContext()));
        }
        if (val instanceof Boolean) {
            return BigDecimal.valueOf((boolean) val ? 1. : 0.);
        }
        if (val instanceof AtomicBoolean) {
            return BigDecimal.valueOf(((AtomicBoolean) val).get() ? 1L : 0L);
        }
        if (val instanceof String) {
            final String string = (String) val;
            if ("".equals(string)) {
                return BigDecimal.ZERO;
            }
            return roundBigDecimal(new BigDecimal(string, getMathContext()));
        }
        if (val instanceof Character) {
            final int i = ((Character) val);
            return new BigDecimal(i);
        }
        throw new CoercionException("BigDecimal coercion: "
                + val.getClass().getName() + ":(" + val + ")");
    }

    /**
     * Coerce to a primitive double.
     * <p>Double.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     *
     * @param val value to coerce.
     * @return The double coerced value.
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible
     */
    public double toDouble(final Object val) {
        if (val == null) {
            return controlNullOperand(strict, 0.d);
        }
        if (val instanceof Double) {
            return ((Double) val);
        }
        if (val instanceof Number) {
            //The below construct is used rather than ((Number)val).doubleValue() to ensure
            //equality between comparing new Double( 6.4 / 3 ) and the jexl expression of 6.4 / 3
            return Double.parseDouble(String.valueOf(val));
        }
        if (val instanceof Boolean) {
            return (boolean) val ? 1. : 0.;
        }
        if (val instanceof AtomicBoolean) {
            return ((AtomicBoolean) val).get() ? 1. : 0.;
        }
        if (val instanceof String) {
            return parseDouble((String) val);
        }
        if (val instanceof Character) {
            final int i = ((Character) val);
            return i;
        }
        throw new CoercionException("Double coercion: "
                + val.getClass().getName() + ":(" + val + ")");
    }

    /**
     * Coerce to a primitive float.
     * <p>Float.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     *
     * @param val value to coerce.
     * @return The float coerced value.
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible
     */
    public float toFloat(Object val) {
        if (val == null) {
            return controlNullOperand(strict, 0.f);
        } else if (val instanceof Float) {
            return ((Float) val);
        } else if (val instanceof Number) {
            //The below construct is used rather than ((Number)val).floatValue() to ensure
            //equality between comparing new Float( 6.4 / 3 ) and the jexl expression of 6.4 / 3
            return Float.parseFloat(String.valueOf(val));
        } else if (val instanceof Boolean) {
            return ((Boolean) val) ? 1.f : 0.f;
        } else if (val instanceof AtomicBoolean) {
            return ((AtomicBoolean) val).get() ? 1.f : 0.f;
        } else if (val instanceof String) {
            String string = (String) val;
            if ("".equals(string)) {
                return Float.NaN;
            } else {
                // the spec seems to be iffy about this.  Going to give it a wack anyway
                return Float.parseFloat(string);
            }
        } else if (val instanceof Character) {
            int i = ((Character) val);
            return i;
        }
        throw new ArithmeticException("Float coercion: "
                + val.getClass().getName() + ":(" + val + ")");
    }

    /**
     * Coerce to a char.
     *
     * @param val value to coerce.
     * @return The char coerced value.
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible
     */
    public char toCharacter(Object val) {
        if (val == null) {
            return controlNullOperand(strict, '\0');
        } else if (val instanceof Number) {
            int i = ((Number) val).intValue();
            return (char) i;
        } else if (val instanceof CharSequence) {
            CharSequence cs = (CharSequence) val;
            return cs.length() > 0 ? cs.charAt(0) : '\0';
        } else {
            String s = toString(val);
            return s.length() > 0 ? s.charAt(0) : '\0';
        }
    }

    /**
     * Coerce to a string.
     * <p>Double.NaN coerce to the empty string.</p>
     *
     * @param val value to coerce.
     * @return The String coerced value.
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible
     */
    public String toString(final Object val) {
        if (val == null) {
            return controlNullOperand(strict, "");
        }
        if (!isFloatingPoint(val)) {
            return val.toString();
        }
        final Double dval = toDouble(val);
        if (Double.isNaN(dval)) {
            return "";
        }
        return dval.toString();
    }

    /**
     * Coerce to a Future.
     *
     * @param val object to be coerced.
     * @return a Future
     */
    public Future toFuture(final Object val) {
        if (val instanceof Future) {
            return (Future) val;
        }
        return CompletableFuture.completedFuture(val);
    }

    /**
     * Use or overload and() instead.
     * @param lhs left hand side
     * @param rhs right hand side
     * @return lhs &amp; rhs
     * @see JexlArithmetic#and
     * @deprecated 3.0
     */
    @Deprecated
    public final Object bitwiseAnd(final Object lhs, final Object rhs) {
        return and(lhs, rhs);
    }

    /**
     * Use or overload or() instead.
     *
     * @param lhs left hand side
     * @param rhs right hand side
     * @return lhs | rhs
     * @see JexlArithmetic#or
     * @deprecated 3.0
     */
    @Deprecated
    public final Object bitwiseOr(final Object lhs, final Object rhs) {
        return or(lhs, rhs);
    }

    /**
     * Use or overload xor() instead.
     *
     * @param lhs left hand side
     * @param rhs right hand side
     * @return lhs ^ rhs
     * @see JexlArithmetic#xor
     * @deprecated 3.0
     */
    @Deprecated
    public final Object bitwiseXor(final Object lhs, final Object rhs) {
        return xor(lhs, rhs);
    }

    /**
     * Use or overload not() instead.
     *
     * @param arg argument
     * @return !arg
     * @see JexlArithmetic#not
     * @deprecated 3.0
     */
    @Deprecated
    public final Object logicalNot(final Object arg) {
        return not(arg);
    }

    /**
     * Use or overload contains() instead.
     *
     * @param lhs left hand side
     * @param rhs right hand side
     * @return contains(rhs, lhs)
     * @see JexlArithmetic#contains
     * @deprecated 3.0
     */
    @Deprecated
    public final Object matches(final Object lhs, final Object rhs) {
        return contains(rhs, lhs);
    }
}
