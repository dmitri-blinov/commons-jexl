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

import java.lang.reflect.Method;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.JexlCache;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlOperator;
import org.apache.commons.jexl3.internal.introspection.MethodExecutor;
import org.apache.commons.jexl3.internal.introspection.MethodKey;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.jexl3.parser.JexlNode;

/**
 * Helper class to deal with operator overloading and specifics.
 * @since 3.0
 */
public final class Operator implements JexlOperator.Uberspect {

    private static final String METHOD_IS_EMPTY = "isEmpty";
    private static final String METHOD_SIZE = "size";
    private static final String METHOD_CONTAINS = "contains";
    private static final String METHOD_STARTS_WITH = "startsWith";
    private static final String METHOD_ENDS_WITH = "endsWith";

    /**
     * The comparison operators.
     * <p>Used to determine if a compare method overload might be used.</p>
     */
    private static final Set<JexlOperator> CMP_OPS =
            EnumSet.of(JexlOperator.GT, JexlOperator.LT, JexlOperator.EQ, JexlOperator.GTE, JexlOperator.LTE);

    private static final Set<JexlOperator> POSTFIX_OPS =
            EnumSet.of(JexlOperator.GET_AND_INCREMENT, JexlOperator.GET_AND_DECREMENT);

    /** The uberspect. */
    private final JexlUberspect uberspect;
    /** The arithmetic instance being analyzed. */
    private final JexlArithmetic arithmetic;
    /** The set of overloaded operators. */
    private final Set<JexlOperator> overloads;
    /** The delegate if built as a 3.4 legacy. */
    private final JexlArithmetic.Uberspect delegate;
    /** Caching state: -1 unknown, 0 false, 1 true. */
    private volatile int caching = -1;

    /**
     * Helper for postfix assignment operators.
     * @param operator the operator
     * @return true if operator is a postfix operator (x++, y--)
     */

    private static boolean isPostfix(final JexlOperator operator) {
        return operator == JexlOperator.GET_AND_INCREMENT || operator == JexlOperator.GET_AND_DECREMENT;
    }

    /**
     * Creates an instance.
     * <p>Mostly used as a compatibility measure by delegating instead of extending.</p>
     *
     * @param theUberspect  the uberspect instance
     * @param theArithmetic the arithmetic instance used to delegate operator overloads
     */
    public Operator(final JexlUberspect theUberspect, final JexlArithmetic theArithmetic) {
        this.uberspect = theUberspect;
        this.arithmetic = theArithmetic;
        this.overloads = Collections.emptySet();
        this.delegate = theUberspect.getArithmetic(theArithmetic);
    }

    /**
     * Creates an instance.
     * @param theUberspect the uberspect instance
     * @param theArithmetic the arithmetic instance
     * @param theOverloads  the overloaded operators
     */
    public Operator(final JexlUberspect theUberspect,
                    final JexlArithmetic theArithmetic,
                    final Set<JexlOperator> theOverloads) {
        this(theUberspect, theArithmetic, theOverloads, -1);
    }

    /**
     * Creates an instance.
     * @param theUberspect the uberspect instance
     * @param theArithmetic the arithmetic instance
     * @param theOverloads  the overloaded operators
     * @param theCache the caching state
     */
    public Operator(final JexlUberspect theUberspect,
                    final JexlArithmetic theArithmetic,
                    final Set<JexlOperator> theOverloads,
                    final int theCache) {
        this.uberspect = theUberspect;
        this.arithmetic = theArithmetic;
        this.overloads = theOverloads;
        this.delegate = null;
        this.caching = theCache;
    }

    @Override
    public JexlMethod getOperator(final JexlOperator operator, final Object... args) {
        if (delegate != null) {
            return delegate.getOperator(operator, args);
        }
        if (overloads.contains(operator) && args != null && args.length == operator.getArity()) {
            return uberspectOperator(arithmetic, operator, args);
        }
        return null;
    }

    @Override
    public boolean overloads(final JexlOperator operator) {
        return delegate != null
            ? delegate.overloads(operator)
            : overloads.contains(operator);
    }

    /**
     * @return whether caching is enabled in the engine
     */
    private boolean isCaching() {
        int c = caching;
        if (c < 0) {
            synchronized(this) {
                c = caching;
                if (c < 0) {
                    JexlEngine jexl = JexlEngine.getThreadEngine();
                    caching = c = (jexl instanceof Engine) && ((Engine) jexl).cache != null ? 1 : 0;
                }
            }
        }
        return c > 0;
    }

    /**
     * Tidy arguments based on operator arity.
     * <p>The interpreter may add a null to the arguments of operator expecting only one parameter.</p>
     * @param operator the operator
     * @param args the arguments (as seen by the interpreter)
     * @return the tidied arguments
     */
    private Object[] arguments(final JexlOperator operator, final Object...args) {
        return operator.getArity() == 1 && args.length > 1 ? new Object[]{args[0]} : args;
    }

    /**
     * Checks whether a method returns a boolean or a Boolean.
     * @param vm the JexlMethod (may be null)
     * @return true of false
     */
    private static boolean returnsBoolean(final JexlMethod vm) {
        if (vm != null) {
            final Class<?> rc = vm.getReturnType();
            return Boolean.TYPE.equals(rc) || Boolean.class.equals(rc);
        }
        return false;
    }

    /**
     * Checks whether a method returns an int or an Integer.
     * @param vm the JexlMethod (may be null)
     * @return true of false
     */
    private static boolean returnsInteger(final JexlMethod vm) {
        if (vm != null) {
            final Class<?> rc = vm.getReturnType();
            return Integer.TYPE.equals(rc) || Integer.class.equals(rc);
        }
        return false;
    }

    /**
     * Checks whether a method is a JexlArithmetic method.
     * @param vm the JexlMethod (may be null)
     * @return true of false
     */
    private static boolean isArithmetic(final JexlMethod vm) {
        if (vm instanceof MethodExecutor) {
            final Method method = ((MethodExecutor) vm).getMethod();
            return JexlArithmetic.class.equals(method.getDeclaringClass());
        }
        return false;
    }

    /**
     * Throw a NPE if operator is strict and one of the arguments is null.
     * @param arithmetic the JEXL arithmetic instance
     * @param operator the operator to check
     * @param arg1 the operand
     * @throws JexlArithmetic.NullOperand if operator is strict and an operand is null
     */
    private void controlNullOperands(final JexlArithmetic arithmetic, JexlOperator operator, Object arg1) {
        // only check operator if necessary
        if (arg1 == null) {
            // check operator only once if it is not strict
            if (arithmetic.isStrict(operator)) {
                throw new JexlArithmetic.NullOperand();
            }
        }
    }

    /**
     * Throw a NPE if operator is strict and one of the arguments is null.
     * @param arithmetic the JEXL arithmetic instance
     * @param operator the operator to check
     * @param args the operands
     * @throws JexlArithmetic.NullOperand if operator is strict and an operand is null
     */
    private void controlNullOperands(final JexlArithmetic arithmetic, JexlOperator operator, Object arg1, Object arg2) {
        controlNullOperands(arithmetic, operator, arg1);
        controlNullOperands(arithmetic, operator, arg2);
    }

    /**
     * Throw a NPE if operator is strict and one of the arguments is null.
     * @param arithmetic the JEXL arithmetic instance
     * @param operator the operator to check
     * @param args the operands
     * @throws JexlArithmetic.NullOperand if operator is strict and an operand is null
     */
    private void controlNullOperands(final JexlArithmetic arithmetic, JexlOperator operator, Object arg1, Object arg2, Object arg3) {
        controlNullOperands(arithmetic, operator, arg1);
        controlNullOperands(arithmetic, operator, arg2);
        controlNullOperands(arithmetic, operator, arg3);
    }

    /**
     * Triggered when an operator fails.
     * @param ref     the node where the error originated from
     * @param operator the operator symbol
     * @param cause    the cause of error (if any)
     * @return throws JexlException if strict and not silent, null otherwise

     */
    private <T> T operatorError(final JexlCache.Reference ref, final JexlOperator operator, final Throwable cause, T alt) {
        Interpreter interpreter = (Interpreter) Interpreter.INTER.get();
        return interpreter != null ? (T) interpreter.operatorError(ref, operator, cause) : alt;
    }

    /**
     * Seeks an implementation of an operator method in an arithmetic instance.
     * <p>Method must <em><>not/em belong to JexlArithmetic</p>
     * @param arithmetic the arithmetic instance
     * @param operator the operator
     * @param args the arguments
     * @return a JexlMethod instance or null
     */
    private JexlMethod uberspectOperator(final JexlArithmetic arithmetic,
                                       final JexlOperator operator,
                                       final Object... args) {
        final JexlMethod me = uberspect.getMethod(arithmetic, operator.getMethodName(), args);
        if (!(me instanceof MethodExecutor) ||
            !JexlArithmetic.class.equals(((MethodExecutor) me).getMethod().getDeclaringClass())) {
            return me;
        }
        return null;
    }

    /**
     * Attempts finding a method in left and eventually narrowing right.
     * @param methodName the method name
     * @param right the left argument in the operator
     * @param left the right argument in the operator
     * @return a boolean is call was possible, null otherwise
     * @throws Exception if invocation fails
     */
    private Boolean booleanDuckCall(final String methodName, final Object left, final Object right) throws Exception {
        JexlMethod vm = uberspect.getMethod(left, methodName, right);
        if (returnsBoolean(vm)) {
            return (Boolean) vm.invoke(left, right);
        }
        final Object[] argv = { right };
        if (arithmetic.narrowArguments(argv)) {
            vm = uberspect.getMethod(left, methodName, argv);
            if (returnsBoolean(vm)) {
                return (Boolean) vm.invoke(left, argv);
            }
        }
        return null;
    }

    @Override
    public Object tryOverload(final JexlCache.Reference node, final JexlOperator operator, final Object arg1) {
        controlNullOperands(arithmetic, operator, arg1);
        Engine engine = (Engine) JexlEngine.getThreadEngine();
        try {
            return tryEval(isCaching() ? node : null, operator, arg1);
        } catch (final Exception any) {
            // ignore return if lenient, will return try_failed
            return operatorError(node, operator, any, JexlEngine.TRY_FAILED);
        }
    }

    @Override
    public Object tryOverload(final JexlCache.Reference node, final JexlOperator operator, final Object arg1, final Object arg2) {
        controlNullOperands(arithmetic, operator, arg1, arg2);
        Engine engine = (Engine) JexlEngine.getThreadEngine();
        try {
            return tryEval(engine == null || engine.cache != null ? node : null, operator, arg1, arg2);
        } catch (final Exception any) {
            // ignore return if lenient, will return try_failed
            return operatorError(node, operator, any, JexlEngine.TRY_FAILED);
        }
    }

    @Override
    public Object tryOverload(final JexlCache.Reference node, final JexlOperator operator, final Object arg1, final Object arg2, 
        final Object arg3) {
        controlNullOperands(arithmetic, operator, arg1, arg2, arg3);
        Engine engine = (Engine) JexlEngine.getThreadEngine();
        try {
            return tryEval(engine == null || engine.cache != null ? node : null, operator, arg1, arg2, arg3);
        } catch (final Exception any) {
            // ignore return if lenient, will return try_failed
            return operatorError(node, operator, any, JexlEngine.TRY_FAILED);
        }
    }

    /**
     * Tries operator evaluation, handles method resolution caching.
     * @param node the node
     * @param operator the operator
     * @param args the operator arguments
     * @return the operator evaluation result or TRY_FAILED
     */
    private Object tryEval(JexlCache.Reference node, final JexlOperator operator, final Object... args) {
        if (node != null) {
            final Object cached = node.getCache();
            if (cached instanceof JexlMethod) {
                // we found a method on previous call; try and reuse it (*1)
                final JexlMethod me = (JexlMethod) cached;
                Object eval = me.tryInvoke(operator.getMethodName(), arithmetic, args);
                if (!me.tryFailed(eval)) {
                    return eval;
                }
            } else if (cached instanceof MethodKey) {
                // check for a fail-fast, we tried to find an overload before but could not (*2)
                final MethodKey cachedKey = (MethodKey) cached;
                final MethodKey key = new MethodKey(operator.getMethodName(), args);
                if (key.equals(cachedKey)) {
                    return JexlEngine.TRY_FAILED;
                }
            }
        }

        // trying to find an operator overload
        JexlMethod vm = getOperator(operator, args);
        // no direct overload, any special case ?
        if (vm == null) {
           vm = getAlternateOverload(operator, args);
        }
        // *1: found a method, try it and cache it if successful
        if (vm != null) {
            final Object result = vm.tryInvoke(operator.getMethodName(), arithmetic, args);
            if (node != null && !vm.tryFailed(result)) {
                node.setCache(vm);
            }
            return result;
        }
        // *2: could not find an overload for this operator and arguments, keep track of the fail
        if (node != null) {
            MethodKey key = new MethodKey(operator.getMethodName(), args);
            node.setCache(key);
        }
        return JexlEngine.TRY_FAILED;
    }


    /**
     * Special handling of overloads where another attempt at finding a method may be attempted.
     * <p>As of 3.4.1, only the comparison operators attempting to use compare() are handled.</p>
     * @param operator the operator
     * @param args the arguments
     * @return an instance or null
     */
    private JexlMethod getAlternateOverload(final JexlOperator operator, final Object... args) {
        // comparison operators may use the compare overload in derived arithmetic
        if (CMP_OPS.contains(operator) && args.length == 2) {
            JexlMethod cmp = getOperator(JexlOperator.COMPARE, args);
            if (cmp != null) {
                return new Operator.CompareMethod(operator, cmp);
            }
            cmp = getOperator(JexlOperator.COMPARE, args[1], args[0]);
            if (cmp != null) {
                return new Operator.AntiCompareMethod(operator, cmp);
            }
        }
        return null;
    }

    /**
     * Delegates a comparison operator to a compare method.
     * The expected signature of the derived JexlArithmetic method is:
     * int compare(L left, R right);
     */
    private static class CompareMethod implements JexlMethod {
        protected final JexlOperator operator;
        protected final JexlMethod compare;

        CompareMethod(final JexlOperator op, final JexlMethod m) {
            operator = op;
            compare = m;
        }

        @Override
        public Class<?> getReturnType() {
            return Boolean.TYPE;
        }

        @Override
        public Object invoke(Object arithmetic, Object... params) throws Exception {
            return operate((int) compare.invoke(arithmetic, params));
        }

        @Override
        public boolean isCacheable() {
            return true;
        }

        @Override
        public boolean isStatic() {
            return compare.isStatic();
        }

        @Override
        public final Class<?>[] getParameterTypes() {
            return compare.getParameterTypes();
        }

        @Override
        public boolean tryFailed(Object rval) {
            return rval == JexlEngine.TRY_FAILED;
        }

        @Override
        public Object tryInvoke(String name, Object arithmetic, Object... params) throws JexlException.TryFailed {
            final Object cmp = compare.tryInvoke(JexlOperator.COMPARE.getMethodName(), arithmetic, params);
            return cmp instanceof Integer? operate((int) cmp) : JexlEngine.TRY_FAILED;
        }

        /**
         * From compare() int result to operator boolean result.
         * @param cmp the compare result
         * @return the operator applied
         */
        protected boolean operate(final int cmp) {
            switch(operator) {
                case EQ: return cmp == 0;
                case LT: return cmp < 0;
                case LTE: return cmp <= 0;
                case GT: return cmp > 0;
                case GTE: return cmp >= 0;
                default:
                  throw new ArithmeticException("unexpected operator " + operator);
            }
        }
    }


    /**
     * The reverse compare swaps left and right arguments and exploits the symmetry
     * of compare as in: compare(a, b) &lt=&gt -compare(b, a)
     */
    private static class AntiCompareMethod extends CompareMethod {

        AntiCompareMethod(final JexlOperator op, final JexlMethod m) {
            super(op, m);
        }

        @Override
        public Object invoke(Object arithmetic, Object... params) throws Exception {
            return operate(-(int) compare.invoke(arithmetic, params[1], params[0]));
        }

        @Override
        public Object tryInvoke(String name, Object arithmetic, Object... params) throws JexlException.TryFailed {
            Object cmp = compare.tryInvoke(JexlOperator.COMPARE.getMethodName(), arithmetic, params[1], params[0]);
            return cmp instanceof Integer? operate(-(int) cmp) : JexlEngine.TRY_FAILED;
        }
    }

    @Override
    public Object tryAssignOverload(final JexlCache.Reference node, final JexlOperator operator, final Consumer<Object> assignFun, final Object arg1, final Object arg2) {

        if (operator.getArity() != 2) {
            return JexlEngine.TRY_FAILED;
        }

        Object result = JexlEngine.TRY_FAILED;

        try {

            // if the operator is strict, the left-hand side can not be null
            controlNullOperands(arithmetic, operator, arg1, arg2);

            // attempt assignment operator overload
            if (overloads(operator)) {
                result = tryOverload(node, operator, arg1, arg2);

                if (result == JexlOperator.ASSIGN) {
                    return arg1;
                }

                if (result != JexlEngine.TRY_FAILED) {
                    return result;
                }
            }

            // let's attempt base operator overload
            final JexlOperator base = operator.getBaseOperator();
            if (base != null && overloads(base)) {
                result = tryOverload(node, base, arguments(base, arg1, arg2));

                if (result == JexlOperator.ASSIGN) {
                    return arg1;
                }

                if (result != JexlEngine.TRY_FAILED) {
                    return result;
                }
            }

            // no overload or overload failed, use base operator
            if (result == JexlEngine.TRY_FAILED) {
                result = performBaseOperation(operator, arg1, arg2);
            }

            // on success, assign value
            if (result != JexlEngine.TRY_FAILED) {
                assignFun.accept(result);
                // postfix implies return initial argument value
                if (POSTFIX_OPS.contains(operator)) {
                    result = arg1;
                }
            }
            return result;
        } catch (final Exception any) {
            return operatorError(node, operator, any, JexlEngine.TRY_FAILED);
        }
    }

    @Override
    public Object tryAssignOverload(JexlCache.Reference node, JexlOperator operator, final Consumer<Object> assignFun, Object arg1) {
        if (operator.getArity() != 1) {
            return JexlEngine.TRY_FAILED;
        }

        Object result = JexlEngine.TRY_FAILED;

        try {

            // if the operator is strict, the left-hand side can not be null
            controlNullOperands(arithmetic, operator, arg1);

            // attempt assignment operator overload
            if (overloads(operator)) {
                result = tryOverload(node, operator, arg1);

                if (result == JexlOperator.ASSIGN) {
                    return arg1;
                }

                if (result != JexlEngine.TRY_FAILED) {
                    return result;
                }
            }

            // let's attempt base operator overload
            final JexlOperator base = operator.getBaseOperator();
            if (base != null && overloads(base)) {
                result = tryOverload(node, base, arguments(base, arg1));

                if (result == JexlOperator.ASSIGN) {
                    return arg1;
                }

                if (result != JexlEngine.TRY_FAILED) {
                    return result;
                }
            }

            // no overload or overload failed, use base operator
            if (result == JexlEngine.TRY_FAILED) {
                result = performBaseOperation(operator, arg1, null);
            }

            // on success, assign value
            if (result != JexlEngine.TRY_FAILED) {
                assignFun.accept(result);
                // postfix implies return initial argument value
                if (POSTFIX_OPS.contains(operator)) {
                    result = arg1;
                }
            }
            return result;
        } catch (final Exception any) {
            return operatorError(node, operator, any, JexlEngine.TRY_FAILED);
        }
    }

    /**
     * Performs the base operation of an assignment.
     * @param operator the operator
     * @param args the arguments
     * @return the result
     */
    private Object performBaseOperation(final JexlOperator operator, final Object arg1, final Object arg2) {
        switch (operator) {
            case SELF_ADD:
                return arithmetic.selfAdd(arg1, arg2);
            case SELF_SUBTRACT:
                return arithmetic.selfSubtract(arg1, arg2);
            case SELF_MULTIPLY:
                return arithmetic.selfMultiply(arg1, arg2);
            case SELF_DIVIDE:
                return arithmetic.selfDivide(arg1, arg2);
            case SELF_MOD:
                return arithmetic.selfMod(arg1, arg2);
            case SELF_AND:
                return arithmetic.selfAnd(arg1, arg2);
            case SELF_OR:
                return arithmetic.selfOr(arg1, arg2);
            case SELF_DIFF:
                return arithmetic.selfDiff(arg1, arg2);
            case SELF_XOR:
                return arithmetic.selfXor(arg1, arg2);
            case SELF_SHIFTLEFT:
                return arithmetic.selfShiftLeft(arg1, arg2);
            case SELF_SHIFTRIGHT:
                return arithmetic.selfShiftRight(arg1, arg2);
            case SELF_SHIFTRIGHTU:
                return arithmetic.selfShiftRightUnsigned(arg1, arg2);
            case INCREMENT_AND_GET:
            case GET_AND_INCREMENT:
                return arithmetic.increment(arg1);
            case DECREMENT_AND_GET:
            case GET_AND_DECREMENT:
                return arithmetic.decrement(arg1);
            default:
                // unexpected, new operator added?
                throw new UnsupportedOperationException(operator.getOperatorSymbol());
        }
    }

    @Override
    public boolean startsWith(final JexlCache.Reference node, final JexlOperator operator, final Object left, final Object right) {
        final boolean starts;
        try {
            // try operator overload
            final Object result = overloads(JexlOperator.STARTSWITH)
                    ? tryOverload(node, JexlOperator.STARTSWITH, left, right)
                    : null;
            if (result instanceof Boolean) {
                starts = (Boolean) result;
            } else {
                // use arithmetic / pattern matching ?
                final Boolean matched = arithmetic.startsWith(left, right);
                if (matched != null) {
                    starts = matched;
                } else {
                    // try a left.startsWith(right) method
                    final Boolean duck = booleanDuckCall(METHOD_STARTS_WITH, left, right);
                    if (duck != null) {
                        starts = duck;
                    } else {
                        // defaults to equal
                        starts = arithmetic.equals(left, right);
                    }
                }
            }
            // not-startswith is !starts-with
            return (JexlOperator.STARTSWITH == operator) == starts;
        } catch (final Exception any) {
            return operatorError(node, operator, any, false);
        }
    }

    @Override
    public boolean endsWith(final JexlCache.Reference node, final JexlOperator operator, final Object left, final Object right) {
        try {
            final boolean ends;
            // try operator overload
            final Object result = overloads(JexlOperator.ENDSWITH)
                ? tryOverload(node, JexlOperator.ENDSWITH, left, right)
                : null;
            if (result instanceof Boolean) {
                ends = (Boolean) result;
            } else {
                // use arithmetic / pattern matching ?
                final Boolean matched = arithmetic.endsWith(left, right);
                if (matched != null) {
                    ends = matched;
                } else {
                    // try a left.endsWith(right) method
                    final Boolean duck = booleanDuckCall(METHOD_ENDS_WITH, left, right);
                    if (duck != null) {
                        ends = duck;
                    } else {
                        // defaults to equal
                        ends = arithmetic.equals(left, right);
                    }
                }


            }
            // not-endswith is !ends-with
            return (JexlOperator.ENDSWITH == operator) == ends;
        } catch (final Exception any) {
            return operatorError(node, operator, any, false);
        }
    }

    @Override
    public boolean contains(final JexlCache.Reference node, final JexlOperator operator, final Object left, final Object right) {
        final boolean contained;
        try {
            // try operator overload
            final Object result = overloads(JexlOperator.CONTAINS)
                    ? tryOverload(node, JexlOperator.CONTAINS, left, right)
                    : null;
            if (result instanceof Boolean) {
                contained = (Boolean) result;
            } else {
                // use arithmetic / pattern matching ?
                final Boolean matched = arithmetic.contains(left, right);
                if (matched != null) {
                    contained = matched;
                } else {
                    // try a left.contains(right) method
                    final Boolean duck = booleanDuckCall(METHOD_CONTAINS, left, right);
                    if (duck != null) {
                        contained = duck;
                    } else {
                        // defaults to equal
                        contained = arithmetic.equals(left, right);
                    }
                }
            }
            // not-contains is !contains
            return (JexlOperator.CONTAINS == operator) == contained;
        } catch (final Exception any) {
            return operatorError(node, operator, any, false);
        }
    }

    @Override
    public boolean greaterThanOrEqual(final JexlCache.Reference node, final Object left, final Object right) {
        try {
            // try operator overload
            Object result = tryOverload(node, JexlOperator.GTE, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? arithmetic.toBoolean(result)
                   : arithmetic.greaterThanOrEqual(left, right);
        } catch (final Exception any) {
            return operatorError(node, JexlOperator.GTE, any, false);
        }
    }

    @Override
    public boolean greaterThan(final JexlCache.Reference node, final Object left, final Object right) {
        try {
            // try operator overload
            Object result = tryOverload(node, JexlOperator.GT, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? arithmetic.toBoolean(result)
                   : arithmetic.greaterThan(left, right);
        } catch (final Exception any) {
            return operatorError(node, JexlOperator.GT, any, false);
        }
    }

    @Override
    public boolean lessThanOrEqual(final JexlCache.Reference node, final Object left, final Object right) {
        try {
            // try operator overload
            Object result = tryOverload(node, JexlOperator.LTE, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? arithmetic.toBoolean(result)
                   : arithmetic.lessThanOrEqual(left, right);
        } catch (final Exception any) {
            return operatorError(node, JexlOperator.LTE, any, false);
        }
    }

    @Override
    public boolean lessThan(final JexlCache.Reference node, final Object left, final Object right) {
        try {
            // try operator overload
            Object result = tryOverload(node, JexlOperator.LT, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? arithmetic.toBoolean(result)
                   : arithmetic.lessThan(left, right);
        } catch (final Exception any) {
            return operatorError(node, JexlOperator.LT, any, false);
        }
    }

    @Override
    public boolean equals(final JexlCache.Reference node, final JexlOperator op, final Object left, final Object right) {
        try {
            // try operator overload
            Object result = tryOverload(node, JexlOperator.EQ, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? arithmetic.toBoolean(result)
                   : arithmetic.equals(left, right);
        } catch (final Exception any) {
            return operatorError(node, op, any, false);
        }
    }

    @Override
    public Object empty(final JexlCache.Reference node, final Object object) {
        if (object == null) {
            return true;
        }
        Object result = overloads(JexlOperator.EMPTY)
                ? tryOverload(node, JexlOperator.EMPTY, object)
                : JexlEngine.TRY_FAILED;
        if (result != JexlEngine.TRY_FAILED) {
            return arithmetic.toBoolean(result);
        } else {
            result = arithmetic.isEmpty(object, null);
            if (result == null) {
                result = false;
                // check if there is an isEmpty method on the object that returns a
                // boolean and if so, just use it
                final JexlMethod vm = uberspect.getMethod(object, METHOD_IS_EMPTY, InterpreterBase.EMPTY_PARAMS);
                if (returnsBoolean(vm)) {
                    try {
                        result = vm.invoke(object, InterpreterBase.EMPTY_PARAMS);
                    } catch (final Exception xany) {
                        return operatorError(node, JexlOperator.EMPTY, xany, false);
                    }
                }
            }
        }
        return !(result instanceof Boolean) || (Boolean) result;
    }

    @Override
    public Object size(final JexlCache.Reference node, final Object object) {
        if (object == null) {
            return 0;
        }
        Object result = overloads(JexlOperator.SIZE)
                ? tryOverload(node, JexlOperator.SIZE, object)
                : JexlEngine.TRY_FAILED;
        if (result != JexlEngine.TRY_FAILED) {
            return result;
        } else {
            result = arithmetic.size(object, null);
            if (result == null) {
                // check if there is a size method on the object that returns an
                // integer and if so, just use it
                final JexlMethod vm = uberspect.getMethod(object, METHOD_SIZE, InterpreterBase.EMPTY_PARAMS);
                if (returnsInteger(vm)) {
                    try {
                        result = vm.invoke(object, InterpreterBase.EMPTY_PARAMS);
                    } catch (final Exception xany) {
                        return operatorError(node, JexlOperator.SIZE, xany, 0);
                    }
                }
            }
        }
        return result instanceof Number ? ((Number) result).intValue() : 0;
    }

    @Override
    public Object indirect(JexlCache.Reference node, Object object) {
        Object result = tryOverload(node, JexlOperator.INDIRECT, object);
        if (result != JexlEngine.TRY_FAILED) {
            return result;
        }
        result = arithmetic.indirect(object);
        if (result == JexlEngine.TRY_FAILED) {
            // check if there is a get() method on the object if so, just use it
            JexlMethod vm = uberspect.getMethod(object, "get", Interpreter.EMPTY_PARAMS);
            if (vm != null) {
                try {
                    result = vm.invoke(object, Interpreter.EMPTY_PARAMS);
                } catch (final Exception any) {
                    return operatorError(node, JexlOperator.INDIRECT, any, JexlEngine.TRY_FAILED);
                }
            }
        }
        return result;
    }

    @Override
    public Object indirectAssign(JexlCache.Reference node, Object object, Object right) {
        Object result = tryOverload(node, JexlOperator.INDIRECT_ASSIGN, object, right);
        if (result != JexlEngine.TRY_FAILED) {
            return result;
        }
        result = arithmetic.indirectAssign(object, right);
        if (result == JexlEngine.TRY_FAILED) {
            // check if there is a set(Object) method on the object and if so, just use it
            Object[] argv = {right};
            JexlMethod vm = uberspect.getMethod(object, "set", argv);
            if (vm != null) {
                try {
                    result = vm.invoke(object, argv);
                } catch (final Exception any) {
                    return operatorError(node, JexlOperator.INDIRECT_ASSIGN, any, JexlEngine.TRY_FAILED);

                }
            }
        }
        return result;
    }

}
