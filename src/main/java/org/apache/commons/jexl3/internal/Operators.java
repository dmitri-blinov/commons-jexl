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
import java.util.EnumSet;
import java.util.Set;

import org.apache.commons.jexl3.JexlArithmetic;
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
public final class Operators {

    /**
     * Helper for postfix assignment operators.
     * @param operator the operator
     * @return true if operator is a postfix operator (x++, y--)
     */

    private static boolean isPostfix(final JexlOperator operator) {
        return operator == JexlOperator.GET_AND_INCREMENT || operator == JexlOperator.GET_AND_DECREMENT;
    }


    /**
     * The comparison operators.
     * <p>Used to determine if a compare method overload might be used.</p>
     */
    private static final Set<JexlOperator> CMP_OPS =
            EnumSet.of(JexlOperator.GT, JexlOperator.LT, JexlOperator.EQ, JexlOperator.GTE, JexlOperator.LTE);

    /** The owner. */
    private final InterpreterBase interpreter;
    /** The overloaded arithmetic operators. */
    private final JexlArithmetic.Uberspect operators;

    /**
     * Constructor.
     * @param owner the owning interpreter
     */
    Operators(final InterpreterBase owner) {
        final JexlArithmetic arithmetic = owner.arithmetic;
        final JexlUberspect uberspect = owner.uberspect;
        this.interpreter = owner;
        this.operators = uberspect.getArithmetic(arithmetic);
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
    private void controlNullOperands(JexlOperator operator, Object arg1) {
        // only check operator if necessary
        if (arg1 == null) {
            final JexlArithmetic arithmetic = interpreter.arithmetic;
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
    private void controlNullOperands(JexlOperator operator, Object arg1, Object arg2) {
        controlNullOperands(operator, arg1);
        controlNullOperands(operator, arg2);
    }

    /**
     * Throw a NPE if operator is strict and one of the arguments is null.
     * @param arithmetic the JEXL arithmetic instance
     * @param operator the operator to check
     * @param args the operands
     * @throws JexlArithmetic.NullOperand if operator is strict and an operand is null
     */
    private void controlNullOperands(JexlOperator operator, Object arg1, Object arg2, Object arg3) {
        controlNullOperands(operator, arg1);
        controlNullOperands(operator, arg2);
        controlNullOperands(operator, arg3);
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
        final JexlUberspect uberspect = interpreter.uberspect;
        JexlMethod vm = uberspect.getMethod(left, methodName, right);
        if (returnsBoolean(vm)) {
            return (Boolean) vm.invoke(left, right);
        }
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        final Object[] argv = { right };
        if (arithmetic.narrowArguments(argv)) {
            vm = uberspect.getMethod(left, methodName, argv);
            if (returnsBoolean(vm)) {
                return (Boolean) vm.invoke(left, argv);
            }
        }
        return null;
    }

    /**
     * Attempts to call an operator.
     * <p>
     *     This performs the null argument control against the strictness of the operator.
     * </p>
     * <p>
     * This takes care of finding and caching the operator method when appropriate.
     * </p>
     * @param node     the syntactic node
     * @param operator the operator
     * @param arg1     the first argument
     * @return the result of the operator evaluation or TRY_FAILED
     */
    protected Object tryOverload(final JexlNode node, final JexlOperator operator, final Object arg1) {
        controlNullOperands(operator, arg1);
        if (operators != null && operators.overloads(operator)) {
            return callOverload(node, operator, arg1);
        }
        return JexlEngine.TRY_FAILED;
    }

    /**
     * Attempts to call an operator.
     * <p>
     * This takes care of finding and caching the operator method when appropriate
     * @param node     the syntactic node
     * @param operator the operator
     * @param arg1     the first argument
     * @param arg2     the second argument
     * @return the result of the operator evaluation or TRY_FAILED
     */
    protected Object tryOverload(final JexlNode node, final JexlOperator operator, final Object arg1, final Object arg2) {
        controlNullOperands(operator, arg1, arg2);
        if (operators != null && operators.overloads(operator)) {
            return callOverload(node, operator, arg1, arg2);
        }
        return JexlEngine.TRY_FAILED;
    }

    /**
     * Attempts to call an operator.
     * <p>
     * This takes care of finding and caching the operator method when appropriate
     * @param node     the syntactic node
     * @param operator the operator
     * @param arg1     the first argument
     * @param arg2     the second argument
     * @param arg3     the third argument
     * @return the result of the operator evaluation or TRY_FAILED
     */
    protected Object tryOverload(final JexlNode node, final JexlOperator operator, final Object arg1, final Object arg2, 
        final Object arg3) {
        controlNullOperands(operator, arg1, arg2, arg3);
        if (operators != null && operators.overloads(operator)) {
            return callOverload(node, operator, arg1, arg2, arg3);
        }
        return JexlEngine.TRY_FAILED;
    }

    /**
     * Call an operator.
     * <p>
     * This takes care of finding and caching the operator method when appropriate
     * @param node     the syntactic node
     * @param operator the operator
     * @param args     the arguments
     * @return the result of the operator evaluation or TRY_FAILED
     */
    protected Object callOverload(final JexlNode node, final JexlOperator operator, final Object... args) {
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        final boolean cache = interpreter.cache && node != null;
        try {
            if (cache) {
                Object cached = node.jjtGetValue();
                if (cached instanceof JexlMethod) {
                    JexlMethod me = (JexlMethod) cached;
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
            JexlMethod vm = operators.overloads(operator) ? operators.getOperator(operator, args) : null;
            // no direct overload, any special case ?
            if (vm == null) {
               vm = getAlternateOverload(operator, args);
            }
            // *1: found a method, try it and cache it if successful
            if (vm != null) {
                final Object result = vm.tryInvoke(operator.getMethodName(), arithmetic, args);
                if (cache && !vm.tryFailed(result)) {
                    node.jjtSetValue(vm);
                }
                return result;
            }
            // *2: could not find an overload for this operator and arguments, keep track of the fail
            if (cache) {
                MethodKey key = new MethodKey(operator.getMethodName(), args);
                node.jjtSetValue(key);
            }
        } catch (final Exception xany) {
            // ignore return if lenient, will return try_failed
            interpreter.operatorError(node, operator, xany);
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
        if (CMP_OPS.contains(operator)) {
            JexlMethod cmp = operators.getOperator(JexlOperator.COMPARE, args);
            if (cmp != null) {
                return new CompareMethod(operator, cmp);
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

        CompareMethod(JexlOperator op, JexlMethod m) {
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
            Object cmp = compare.tryInvoke(JexlOperator.COMPARE.getMethodName(), arithmetic, params);
            if (cmp instanceof Integer) {
                return operate((int) cmp);
            }
            return JexlEngine.TRY_FAILED;
        }

        private boolean operate(final int cmp) {
            switch(operator) {
                case EQ: return cmp == 0;
                case LT: return cmp < 0;
                case LTE: return cmp <= 0;
                case GT: return cmp > 0;
                case GTE: return cmp >= 0;
            }
            throw new ArithmeticException("unexpected operator " + operator);
        }
    }

    /**
     * Evaluates two-argument assign operator.
     * <p>
     * This takes care of finding and caching the operator method when appropriate.
     * If an overloads returns a value not-equal to TRY_FAILED, it means the side-effect is complete.
     * Otherwise, a += b &lt;=&gt; a = a + b
     * </p>
     * @param node     the syntactic node
     * @param operator the operator
     * @param args     the arguments, the first one being the target of assignment
     * @return JexlEngine.TRY_FAILED if no operation was performed,
     *         the value to use as the side effect argument otherwise
     */
    protected Object tryAssignOverload(final JexlNode node, final JexlOperator operator, final Object arg1, final Object arg2) {
        if (operator.getArity() != 2) {
            return JexlEngine.TRY_FAILED;
        }
        // try to call overload with side effect
        Object result = tryOverload(node, operator, arg1, arg2);
        if (result != JexlEngine.TRY_FAILED) {
            return result;
        }

        // call base operator
        final JexlOperator base = operator.getBaseOperator();
        if (operators != null && base != null && operators.overloads(base)) {
            result = callAssignOverload(node, operator, arg1, arg2);
            if (result != JexlEngine.TRY_FAILED) {
                return result;
            }
        }

        final JexlArithmetic arithmetic = interpreter.arithmetic;
        // base eval
        try {
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
                default:
                    // unexpected, new operator added?
                    throw new UnsupportedOperationException(operator.getOperatorSymbol());
            }
        } catch (final Exception xany) {
            interpreter.operatorError(node, base, xany);
        }
        return JexlEngine.TRY_FAILED;
    }

    /**
     * Evaluates one-argument assign operator.
     * <p>
     * This takes care of finding and caching the operator method when appropriate.
     * If an overloads returns a value not-equal to TRY_FAILED, it means the side-effect is complete.
     * Otherwise, a += b &lt;=&gt; a = a + b
     * </p>
     * @param node     the syntactic node
     * @param operator the operator
     * @param args     the arguments, the first one being the target of assignment
     * @return JexlEngine.TRY_FAILED if no operation was performed,
     *         the value to use as the side effect argument otherwise
     */
    protected Object tryAssignOverload(JexlNode node, JexlOperator operator, Object arg1) {
        if (operator.getArity() != 1) {
            return JexlEngine.TRY_FAILED;
        }
        // try to call overload with side effect
        Object result = tryOverload(node, operator, arg1);
        if (result != JexlEngine.TRY_FAILED) {
            return result;
        }
        // call base operator
        JexlOperator base = operator.getBaseOperator();
        if (operators != null && base != null && operators.overloads(base)) {
            // in case there is an overload on the base operator
            result = callAssignOverload(node, operator, arg1);
            if (result != JexlEngine.TRY_FAILED) {
                return result;
            }
        }
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        // default implementation for self-* operators
        try {
            switch (operator) {
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
        } catch (Exception xany) {
            interpreter.operatorError(node, base, xany);
        }
        return JexlEngine.TRY_FAILED;
    }

    /**
     * Call an assign operator.
     * <p>
     * This takes care of finding the operator method when appropriate
     * @param node     the syntactic node
     * @param operator the operator
     * @param args     the arguments
     * @return the result of the operator evaluation or TRY_FAILED
     */
    protected Object callAssignOverload(JexlNode node, JexlOperator base, Object... args) {
        // call base operator
        try {
            JexlMethod vm = operators.getOperator(base, args);
            if (vm != null && !isArithmetic(vm)) {
                final JexlArithmetic arithmetic = interpreter.arithmetic;
                Object result = vm.invoke(arithmetic, args);
                if (result != JexlEngine.TRY_FAILED) {
                    return result;
                }
            }
        } catch (Exception xany) {
            interpreter.operatorError(node, base, xany);
        }
        return JexlEngine.TRY_FAILED;
    }

    /**
     * The 'startsWith' operator implementation.
     * @param node     the node
     * @param operator the calling operator, $= or $!
     * @param left     the left operand
     * @param right    the right operand
     * @return true if left starts with right, false otherwise
     */
    boolean startsWith(final JexlNode node, final JexlOperator operator, final Object left, final Object right) {
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        final boolean starts;
        try {
            // try operator overload
            final Object result = operators.overloads(JexlOperator.STARTSWITH)
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
                    final Boolean duck = booleanDuckCall("startsWith", left, right);
                    if (duck != null) {
                        starts = duck;
                    } else {
                        // defaults to equal
                        starts = arithmetic.equals(left, right);
                    }
                }
            }
            return (JexlOperator.STARTSWITH == operator) == starts;
        } catch (final Exception xrt) {
            interpreter.operatorError(node, operator, xrt);
            return false;
        }
    }

    /**
     * The 'endsWith' operator implementation.
     * @param node     the node
     * @param operator the calling operator, ^= or ^!
     * @param left     the left operand
     * @param right    the right operand
     * @return true if left ends with right, false otherwise
     */
    boolean endsWith(final JexlNode node, final JexlOperator operator, final Object left, final Object right) {
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        try {
            final boolean ends;
            // try operator overload
            final Object result = operators.overloads(JexlOperator.ENDSWITH)
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
                    final Boolean duck = booleanDuckCall("endsWith", left, right);
                    if (duck != null) {
                        ends = duck;
                    } else {
                        // defaults to equal
                        ends = arithmetic.equals(left, right);
                    }
                }


            }
            return (JexlOperator.ENDSWITH == operator) == ends;
        } catch (final Exception xrt) {
            interpreter.operatorError(node, operator, xrt);
            return false;
        }
    }

    /**
     * The 'match'/'in' operator implementation.
     * <p>
     * Note that 'x in y' or 'x matches y' means 'y contains x' ;
     * the JEXL operator arguments order syntax is the reverse of this method call.
     * </p>
     * @param node  the node
     * @param operator    the calling operator, =~ or !~
     * @param right the left operand
     * @param left  the right operand
     * @return true if left matches right, false otherwise
     */
    boolean contains(final JexlNode node, final JexlOperator operator, final Object left, final Object right) {
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        final boolean contained;
        try {
            // try operator overload
            final Object result = operators.overloads(JexlOperator.CONTAINS)
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
                    final Boolean duck = booleanDuckCall("contains", left, right);
                    if (duck != null) {
                        contained = duck;
                    } else {
                        // defaults to equal
                        contained = arithmetic.equals(left, right);
                    }
                }
            }
            return (JexlOperator.CONTAINS == operator) == contained;
        } catch (final Exception xrt) {
            interpreter.operatorError(node, operator, xrt);
            return false;
        }
    }

    /**
     * The 'greaterThanOrEqual' operator implementation.
     * @param node     the node
     * @param left     the left operand
     * @param right    the right operand
     * @return true if left greater or equlas right, false otherwise
     */
    protected boolean greaterThanOrEqual(final JexlNode node, final Object left, final Object right) {
        try {
            final JexlArithmetic arithmetic = interpreter.arithmetic;
            // try operator overload
            Object result = tryOverload(node, JexlOperator.GTE, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? arithmetic.toBoolean(result)
                   : arithmetic.greaterThanOrEqual(left, right);
        } catch (ArithmeticException xrt) {
            throw interpreter.createException(interpreter.findNullOperand(node, left, right), ">= error", xrt);
        }
    }

    /**
     * The 'greaterThan' operator implementation.
     * @param node     the node
     * @param left     the left operand
     * @param right    the right operand
     * @return true if left greater right, false otherwise
     */
    protected boolean greaterThan(final JexlNode node, final Object left, final Object right) {
        try {
            final JexlArithmetic arithmetic = interpreter.arithmetic;
            // try operator overload
            Object result = tryOverload(node, JexlOperator.GT, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? arithmetic.toBoolean(result)
                   : arithmetic.greaterThan(left, right);
        } catch (ArithmeticException xrt) {
            throw interpreter.createException(interpreter.findNullOperand(node, left, right), "> error", xrt);
        }
    }

    /**
     * The 'lessThanOrEqual' operator implementation.
     * @param node     the node
     * @param left     the left operand
     * @param right    the right operand
     * @return true if left less or equlas right, false otherwise
     */
    protected boolean lessThanOrEqual(final JexlNode node, final Object left, final Object right) {
        try {
            final JexlArithmetic arithmetic = interpreter.arithmetic;
            // try operator overload
            Object result = tryOverload(node, JexlOperator.LTE, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? arithmetic.toBoolean(result)
                   : arithmetic.lessThanOrEqual(left, right);
        } catch (ArithmeticException xrt) {
            throw interpreter.createException(interpreter.findNullOperand(node, left, right), "<= error", xrt);
        }
    }

    /**
     * The 'lessThan' operator implementation.
     * @param node     the node
     * @param left     the left operand
     * @param right    the right operand
     * @return true if left greater right, false otherwise
     */
    protected boolean lessThan(final JexlNode node, final Object left, final Object right) {
        try {
            final JexlArithmetic arithmetic = interpreter.arithmetic;
            // try operator overload
            Object result = tryOverload(node, JexlOperator.LT, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? arithmetic.toBoolean(result)
                   : arithmetic.lessThan(left, right);
        } catch (ArithmeticException xrt) {
            throw interpreter.createException(interpreter.findNullOperand(node, left, right), "< error", xrt);
        }
    }

    /**
     * The 'equals' operator implementation.
     * @param node     the node
     * @param op       the calling operator
     * @param left     the left operand
     * @param right    the right operand
     * @return true if left equals right, false otherwise
     */
    boolean equals(final JexlNode node, JexlOperator op, final Object left, final Object right) {
        try {
            final JexlArithmetic arithmetic = interpreter.arithmetic;
            // try operator overload
            Object result = tryOverload(node, JexlOperator.EQ, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? arithmetic.toBoolean(result)
                   : arithmetic.equals(left, right);
        } catch (ArithmeticException xrt) {
            throw interpreter.createException(interpreter.findNullOperand(node, left, right), op + " error", xrt);
        }
    }

    /**
     * Check for emptiness of various types: Collection, Array, Map, String, and anything that has a boolean isEmpty()
     * method.
     * <p>Note that the result may not be a boolean.
     *
     * @param node   the node holding the object
     * @param object the object to check the emptiness of
     * @return the evaluation result
     */
    Object empty(final JexlNode node, final Object object) {
        if (object == null) {
            return true;
        }
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        Object result = operators.overloads(JexlOperator.EMPTY)
                ? tryOverload(node, JexlOperator.EMPTY, object)
                : JexlEngine.TRY_FAILED;
        if (result != JexlEngine.TRY_FAILED) {
            return arithmetic.toBoolean(result);
        } else {
            result = arithmetic.isEmpty(object, null);
            if (result == null) {
                final JexlUberspect uberspect = interpreter.uberspect;
                result = false;
                // check if there is an isEmpty method on the object that returns a
                // boolean and if so, just use it
                final JexlMethod vm = uberspect.getMethod(object, "isEmpty", InterpreterBase.EMPTY_PARAMS);
                if (returnsBoolean(vm)) {
                    try {
                        result = vm.invoke(object, InterpreterBase.EMPTY_PARAMS);
                    } catch (final Exception xany) {
                        return interpreter.operatorError(node, JexlOperator.EMPTY, xany);
                    }
                }
            }
        }
        return !(result instanceof Boolean) || (Boolean) result;
    }

    /**
     * Calculate the {@code size} of various types:
     * Collection, Array, Map, String, and anything that has an int size() method.
     * <p>Note that the result may not be an integer.
     *
     * @param node   the node that gave the value to size
     * @param object the object to get the size of
     * @return the evaluation result
     */
    Object size(final JexlNode node, final Object object) {
        if (object == null) {
            return 0;
        }
        Object result = operators.overloads(JexlOperator.SIZE)
                ? tryOverload(node, JexlOperator.SIZE, object)
                : JexlEngine.TRY_FAILED;
        if (result != JexlEngine.TRY_FAILED) {
            return result;
        } else {
            final JexlArithmetic arithmetic = interpreter.arithmetic;
            result = arithmetic.size(object, null);
            if (result == null) {
                final JexlUberspect uberspect = interpreter.uberspect;
                // check if there is a size method on the object that returns an
                // integer and if so, just use it
                final JexlMethod vm = uberspect.getMethod(object, "size", InterpreterBase.EMPTY_PARAMS);
                if (returnsInteger(vm)) {
                    try {
                        result = vm.invoke(object, InterpreterBase.EMPTY_PARAMS);
                    } catch (final Exception xany) {
                        interpreter.operatorError(node, JexlOperator.SIZE, xany);
                    }
                }
            }
        }
        return result instanceof Number ? ((Number) result).intValue() : 0;
    }

    /**
     * Dereferences anything that has an Object get() method.
     *
     * @param node   the node holding the object
     * @param object the object to be dereferenced
     * @return the evaluation result
     */
    protected Object indirect(JexlNode node, Object object) {
        Object result = tryOverload(node, JexlOperator.INDIRECT, object);
        if (result != JexlEngine.TRY_FAILED) {
            return result;
        }
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        result = arithmetic.indirect(object);
        if (result == JexlEngine.TRY_FAILED) {
            // check if there is a get() method on the object if so, just use it
            final JexlUberspect uberspect = interpreter.uberspect;
            JexlMethod vm = uberspect.getMethod(object, "get", Interpreter.EMPTY_PARAMS);
            if (vm != null) {
                try {
                    result = vm.invoke(object, Interpreter.EMPTY_PARAMS);
                } catch (Exception xany) {
                    interpreter.operatorError(node, JexlOperator.INDIRECT, xany);
                }
            }
        }
        return result;
    }

    /**
     * Assigns a value to anything that has an Object set(Object value) method.
     *
     * @param node   the node holding the object
     * @param object the object to be dereferenced
     * @param right  the value to be assigned
     * @return the evaluation result
     */
    protected Object indirectAssign(JexlNode node, Object object, Object right) {
        Object result = tryOverload(node, JexlOperator.INDIRECT_ASSIGN, object, right);
        if (result != JexlEngine.TRY_FAILED) {
            return result;
        }
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        result = arithmetic.indirectAssign(object, right);
        if (result == JexlEngine.TRY_FAILED) {
            // check if there is a set(Object) method on the object and if so, just use it
            Object[] argv = {right};
            final JexlUberspect uberspect = interpreter.uberspect;
            JexlMethod vm = uberspect.getMethod(object, "set", argv);
            if (vm != null) {
                try {
                    result = vm.invoke(object, argv);
                } catch (Exception xany) {
                    interpreter.operatorError(node, JexlOperator.INDIRECT_ASSIGN, xany);
                }
            }
        }
        return result;
    }

}
