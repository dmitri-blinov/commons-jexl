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

import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlUberspect;

import java.util.Comparator;
import java.util.concurrent.Callable;

import java.util.function.Supplier;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Function;
import java.util.function.DoubleFunction;
import java.util.function.LongFunction;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.LongUnaryOperator;
import java.util.function.IntUnaryOperator;
import java.util.function.IntPredicate;
import java.util.function.BiConsumer;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjLongConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.LongPredicate;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.ToLongBiFunction;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToIntBiFunction;


/**
 * A Method reference implementation.
 */
public class MethodReference {

    /** The engine for this reference. */
    protected final Engine jexl;
    /** The target. */
    protected final Object target;
    /** The method. */
    protected final JexlMethod method;
    /** The method. */
    protected final boolean isInstanceMethod;

    /**
     * Creates a method reference.
     * @param target the object target
     * @param method the method name
     */
    protected MethodReference(Interpreter theCaller, Object target, JexlMethod method) {
        this.jexl = theCaller.jexl;
        this.target = target;
        this.method = method;
        this.isInstanceMethod = target instanceof Class<?> && !method.isStatic();
    }

    protected static MethodReference create(Interpreter theCaller, Object target, String methodName) {
        final JexlUberspect uberspect = theCaller.uberspect;
        Class<?> c = target instanceof Class<?> ? (Class<?>) target : target.getClass();
        JexlMethod[] methods = "new".equals(methodName) ? uberspect.getConstructors(c) : uberspect.getMethods(target, methodName, false);
        if (methods == null || methods.length == 0) {
            return null;
        }
        for (int i = 0; i < methods.length; i++) {
            JexlMethod method = methods[i];
            if (!(target instanceof Class<?>) && method.isStatic()) {
                continue;
            }
            Class<?>[] parms = method.getParameterTypes();
            Class<?> type = method.getReturnType();
            int argCount = parms.length;
            if ((target instanceof Class<?>) && !method.isStatic()) {
                argCount++;
            }
            if (Void.TYPE == type) {
                switch (argCount) {
                    case 0: 
                        return new MethodReferenceRunnable(theCaller, target, method);
                    case 1:
                        return new MethodReferenceConsumer(theCaller, target, method);
                    case 2:
                        return new MethodReferenceBiConsumer(theCaller, target, method);
                }
            } else {
                switch (argCount) {
                    case 0:
                        return new MethodReferenceSupplier(theCaller, target, method);
                    case 1:
                        Class pc = parms.length == 1 ? parms[0] : null;
                        if (pc == Integer.class || pc == Integer.TYPE) {
                            return new MethodReferenceIntFunction(theCaller, target, method);
                        } else if (pc == Long.class || pc == Long.TYPE) {
                            return new MethodReferenceLongFunction(theCaller, target, method);
                        } else if (pc == Double.class || pc == Double.TYPE) {
                            return new MethodReferenceDoubleFunction(theCaller, target, method);
                        } else {
                            return new MethodReferenceFunction(theCaller, target, method);
                        }
                    case 2:
                        return new MethodReferenceBiFunction(theCaller, target, method);
                }
            }
            return new MethodReference(theCaller, target, method);
        }
        return null;
    }

    public Object invoke(Object... args) {
        try {
            if (isInstanceMethod) {
                if (args == null || args.length == 0) {
                    throw new IllegalArgumentException();
                }
                Object[] varg = (args.length) == 1 ? InterpreterBase.EMPTY_PARAMS : new Object[args.length - 1];
                System.arraycopy(args, 1, varg, 0, args.length - 1);
                return method.invoke(args[0], varg);
            } else {
                return method.invoke(target, args);
            }
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    /**
     * Implements the @FunctionalInterface interfaces with no arguments to help delegation.
     */
    public static class MethodReferenceRunnable extends MethodReference implements Runnable {

        /**
         * The base constructor.
         * @param intrprtr the interpreter to use
         */
        protected MethodReferenceRunnable(Interpreter theCaller, Object target, JexlMethod method) {
            super(theCaller, target, method);
        }

        @Override
        public void run() {
            invoke();
        }
    }

    /**
     * Implements the @FunctionalInterface interfaces with no arguments to help delegation.
     */
    public static class MethodReferenceSupplier extends MethodReference implements 
          Supplier<Object>, BooleanSupplier, DoubleSupplier, IntSupplier, LongSupplier, Callable<Object> {
        /**
         * The base constructor.
         * @param intrprtr the interpreter to use
         */
        protected MethodReferenceSupplier(Interpreter theCaller, Object target, JexlMethod method) {
            super(theCaller, target, method);
        }

        protected Object eval() {
            return invoke();
        }

        @Override
        public Object call() {
            return eval();
        }

        @Override
        public Object get() {
            return eval();
        }

        @Override
        public boolean getAsBoolean() {
            return jexl.getArithmetic().toBoolean(eval());
        }

        @Override
        public double getAsDouble() {
            return jexl.getArithmetic().toDouble(eval());
        }

        @Override
        public int getAsInt() {
            return jexl.getArithmetic().toInteger(eval());
        }

        @Override
        public long getAsLong() {
            return jexl.getArithmetic().toLong(eval());
        }
    }

    /**
     * Implements the @FunctionalInterface interfaces with one argument to help delegation.
     */
    public static class MethodReferenceFunction extends MethodReference implements 
          Function<Object, Object>, DoubleFunction<Object>, LongFunction<Object>, IntFunction<Object>,
          UnaryOperator<Object>, Predicate<Object>, 
          ToDoubleFunction<Object>, ToIntFunction<Object>, ToLongFunction<Object>,
          LongToDoubleFunction, LongToIntFunction, IntToDoubleFunction, IntToLongFunction,
          DoubleUnaryOperator, LongUnaryOperator, IntUnaryOperator {
        /**
         * The base constructor.
         * @param intrprtr the interpreter to use
         */
        protected MethodReferenceFunction(Interpreter theCaller, Object target, JexlMethod method) {
            super(theCaller, target, method);
        }

        protected Object eval(Object arg) {
            return invoke(arg);
        }

        @Override
        public Object apply(Object arg) {
            return eval(arg);
        }

        @Override
        public Object apply(double arg) {
            return eval(arg);
        }

        @Override
        public Object apply(int arg) {
            return eval(arg);
        }

        @Override
        public Object apply(long arg) {
            return eval(arg);
        }

        @Override
        public double applyAsDouble(double arg) {
            return jexl.getArithmetic().toDouble(eval(arg));
        }

        @Override
        public long applyAsLong(long arg) {
            return jexl.getArithmetic().toLong(eval(arg));
        }

        @Override
        public int applyAsInt(int arg) {
            return jexl.getArithmetic().toInteger(eval(arg));
        }

        @Override
        public boolean test(Object arg) {
            return jexl.getArithmetic().toBoolean(eval(arg));
        }

        @Override
        public double applyAsDouble(Object arg) {
            return jexl.getArithmetic().toDouble(eval(arg));
        }

        @Override
        public int applyAsInt(Object arg) {
            return jexl.getArithmetic().toInteger(eval(arg));
        }

        @Override
        public long applyAsLong(Object arg) {
            return jexl.getArithmetic().toLong(eval(arg));
        }

        @Override
        public double applyAsDouble(long arg) {
            return jexl.getArithmetic().toDouble(eval(arg));
        }

        @Override
        public int applyAsInt(long arg) {
            return jexl.getArithmetic().toInteger(eval(arg));
        }

        @Override
        public double applyAsDouble(int arg) {
            return jexl.getArithmetic().toDouble(eval(arg));
        }

        @Override
        public long applyAsLong(int arg) {
            return jexl.getArithmetic().toLong(eval(arg));
        }
    }

    /**
     * Implements the @FunctionalInterface interfaces with one integer-compatible argument to help delegation.
     */
    public static class MethodReferenceIntFunction extends MethodReference implements 
          Function<Integer, Object>, IntFunction<Object>,
          IntPredicate, IntToDoubleFunction, IntToLongFunction,
          ToDoubleFunction<Integer>, ToIntFunction<Integer>, ToLongFunction<Integer>,
          IntUnaryOperator {
        /**
         * The base constructor.
         * @param intrprtr the interpreter to use
         */
        protected MethodReferenceIntFunction(Interpreter theCaller, Object target, JexlMethod method) {
            super(theCaller, target, method);
        }

        protected Object eval(Object arg) {
            return invoke(arg);
        }

        @Override
        public Object apply(Integer arg) {
            return eval(arg);
        }

        @Override
        public Object apply(int arg) {
            return eval(arg);
        }

        @Override
        public boolean test(int arg) {
            return jexl.getArithmetic().toBoolean(eval(arg));
        }

        @Override
        public int applyAsInt(int arg) {
            return jexl.getArithmetic().toInteger(eval(arg));
        }

        @Override
        public int applyAsInt(Integer arg) {
            return jexl.getArithmetic().toInteger(eval(arg));
        }

        @Override
        public double applyAsDouble(int arg) {
            return jexl.getArithmetic().toDouble(eval(arg));
        }

        @Override
        public double applyAsDouble(Integer arg) {
            return jexl.getArithmetic().toDouble(eval(arg));
        }

        @Override
        public long applyAsLong(int arg) {
            return jexl.getArithmetic().toLong(eval(arg));
        }

        @Override
        public long applyAsLong(Integer arg) {
            return jexl.getArithmetic().toLong(eval(arg));
        }
    }

    /**
     * Implements the @FunctionalInterface interfaces with one long-compatible argument to help delegation.
     */
    public static class MethodReferenceLongFunction extends MethodReference implements 
          Function<Long, Object>, LongFunction<Object>,
          LongPredicate, LongToDoubleFunction, LongToIntFunction,
          ToDoubleFunction<Long>, ToIntFunction<Long>, ToLongFunction<Long>,
          LongUnaryOperator {
        /**
         * The base constructor.
         * @param intrprtr the interpreter to use
         */
        protected MethodReferenceLongFunction(Interpreter theCaller, Object target, JexlMethod method) {
            super(theCaller, target, method);
        }

        protected Object eval(Object arg) {
            return invoke(arg);
        }

        @Override
        public Object apply(Long arg) {
            return eval(arg);
        }

        @Override
        public Object apply(long arg) {
            return eval(arg);
        }

        @Override
        public boolean test(long arg) {
            return jexl.getArithmetic().toBoolean(eval(arg));
        }

        @Override
        public int applyAsInt(long arg) {
            return jexl.getArithmetic().toInteger(eval(arg));
        }

        @Override
        public int applyAsInt(Long arg) {
            return jexl.getArithmetic().toInteger(eval(arg));
        }

        @Override
        public double applyAsDouble(long arg) {
            return jexl.getArithmetic().toDouble(eval(arg));
        }

        @Override
        public double applyAsDouble(Long arg) {
            return jexl.getArithmetic().toDouble(eval(arg));
        }

        @Override
        public long applyAsLong(long arg) {
            return jexl.getArithmetic().toLong(eval(arg));
        }

        @Override
        public long applyAsLong(Long arg) {
            return jexl.getArithmetic().toLong(eval(arg));
        }
    }

    /**
     * Implements the @FunctionalInterface interfaces with one double-compatible argument to help delegation.
     */
    public static class MethodReferenceDoubleFunction extends MethodReference implements 
          Function<Double, Object>, DoubleFunction<Object>,
          DoublePredicate, DoubleToLongFunction, DoubleToIntFunction,
          ToDoubleFunction<Double>, ToIntFunction<Double>, ToLongFunction<Double>,
          DoubleUnaryOperator {
        /**
         * The base constructor.
         * @param intrprtr the interpreter to use
         */
        protected MethodReferenceDoubleFunction(Interpreter theCaller, Object target, JexlMethod method) {
            super(theCaller, target, method);
        }

        protected Object eval(Object arg) {
            return invoke(arg);
        }

        @Override
        public Object apply(Double arg) {
            return eval(arg);
        }

        @Override
        public Object apply(double arg) {
            return eval(arg);
        }

        @Override
        public boolean test(double arg) {
            return jexl.getArithmetic().toBoolean(eval(arg));
        }

        @Override
        public int applyAsInt(double arg) {
            return jexl.getArithmetic().toInteger(eval(arg));
        }

        @Override
        public int applyAsInt(Double arg) {
            return jexl.getArithmetic().toInteger(eval(arg));
        }

        @Override
        public double applyAsDouble(double arg) {
            return jexl.getArithmetic().toDouble(eval(arg));
        }

        @Override
        public double applyAsDouble(Double arg) {
            return jexl.getArithmetic().toDouble(eval(arg));
        }

        @Override
        public long applyAsLong(double arg) {
            return jexl.getArithmetic().toLong(eval(arg));
        }

        @Override
        public long applyAsLong(Double arg) {
            return jexl.getArithmetic().toLong(eval(arg));
        }
    }

    /**
     * Implements the @FunctionalInterface interfaces with one argument to help delegation.
     */
    public static class MethodReferenceConsumer extends MethodReference implements 
          Consumer<Object>, DoubleConsumer, IntConsumer, LongConsumer {
        /**
         * The base constructor.
         * @param intrprtr the interpreter to use
         */
        protected MethodReferenceConsumer(Interpreter theCaller, Object target, JexlMethod method) {
            super(theCaller, target, method);
        }

        protected Object eval(Object arg) {
            return invoke(arg);
        }

        @Override
        public void accept(Object arg) {
            eval(arg);
        }

        @Override
        public void accept(double arg) {
            eval(arg);
        }

        @Override
        public void accept(int arg) {
            eval(arg);
        }

        @Override
        public void accept(long arg) {
            eval(arg);
        }
    }

    /**
     * Implements the @FunctionalInterface interfaces with two arguments to help delegation.
     */
    public static class MethodReferenceBiFunction extends MethodReference implements 
          Comparator<Object>, BiFunction<Object, Object, Object>, BiPredicate<Object, Object>, 
          BinaryOperator<Object>, DoubleBinaryOperator, LongBinaryOperator, IntBinaryOperator,
          ToDoubleBiFunction<Object, Object>, ToLongBiFunction<Object, Object>, ToIntBiFunction<Object, Object> {
        /**
         * The base constructor.
         * @param intrprtr the interpreter to use
         */
        protected MethodReferenceBiFunction(Interpreter theCaller, Object target, JexlMethod method) {
            super(theCaller, target, method);
        }

        protected Object eval(Object arg1, Object arg2) {
            return invoke(arg1, arg2);
        }

        @Override
        public int compare(Object arg1, Object arg2) {
            return jexl.getArithmetic().toInteger(eval(arg1, arg2));
        }

        @Override
        public Object apply(Object arg1, Object arg2) {
            return eval(arg1, arg2);
        }

        @Override
        public double applyAsDouble(Object arg1, Object arg2) {
            return jexl.getArithmetic().toDouble(eval(arg1, arg2));
        }

        @Override
        public double applyAsDouble(double arg1, double arg2) {
            return jexl.getArithmetic().toDouble(eval(arg1, arg2));
        }

        @Override
        public long applyAsLong(Object arg1, Object arg2) {
            return jexl.getArithmetic().toLong(eval(arg1, arg2));
        }

        @Override
        public long applyAsLong(long arg1, long arg2) {
            return jexl.getArithmetic().toLong(eval(arg1, arg2));
        }

        @Override
        public int applyAsInt(Object arg1, Object arg2) {
            return jexl.getArithmetic().toInteger(eval(arg1, arg2));
        }

        @Override
        public int applyAsInt(int arg1, int arg2) {
            return jexl.getArithmetic().toInteger(eval(arg1, arg2));
        }

        @Override
        public boolean test(Object arg1, Object arg2) {
            return jexl.getArithmetic().toBoolean(eval(arg1, arg2));
        }
    }

    /**
     * Implements the @FunctionalInterface interfaces with two arguments to help delegation.
     */
    public static class MethodReferenceBiConsumer extends MethodReference implements 
          BiConsumer<Object, Object>, ObjDoubleConsumer<Object>, ObjLongConsumer<Object>, ObjIntConsumer<Object> {
        /**
         * The base constructor.
         * @param intrprtr the interpreter to use
         */
        protected MethodReferenceBiConsumer(Interpreter theCaller, Object target, JexlMethod method) {
            super(theCaller, target, method);
        }

        protected Object eval(Object arg1, Object arg2) {
            return invoke(arg1, arg2);
        }

        @Override
        public void accept(Object arg1, Object arg2) {
            eval(arg1, arg2);
        }

        @Override
        public void accept(Object arg1, double arg2) {
            eval(arg1, arg2);
        }

        @Override
        public void accept(Object arg1, long arg2) {
            eval(arg1, arg2);
        }

        @Override
        public void accept(Object arg1, int arg2) {
            eval(arg1, arg2);
        }
    }

}
