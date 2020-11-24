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

import org.apache.commons.jexl3.JexlOptions;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.parser.ASTJexlLambda;
import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.apache.commons.jexl3.parser.JexlNode;

import java.lang.reflect.Array;

import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.function.*;


import java.util.Objects;

/**
 * A Script closure.
 */
public class Closure extends Script {
    /** The frame. */
    protected final Frame frame;
    /** The context. */
    protected final JexlContext context;
    /** The number of arguments being curried. */
    protected final int curried;
    /** The chained closure. */
    protected final Closure chained;
    /** The caller. */
    protected final Interpreter caller;

    /**
     * Creates a closure.
     * @param theCaller the calling interpreter
     * @param lambda the lambda
     */
    protected Closure(final Interpreter theCaller, final ASTJexlScript lambda) {
        super(theCaller.jexl, null, lambda);
        caller = theCaller;
        frame = lambda.createFrame(theCaller.frame);
        context = theCaller.context;
        curried = 0;
        chained = null;
    }

    /**
     * Creates a curried version of a script.
     * @param base the base script
     * @param args the script arguments
     */
    protected Closure(final Script base, final Object[] args) {
        super(base.jexl, base.source, base.script);
        if (base instanceof Closure) {
            Closure closure = (Closure) base;
            Frame sf = closure.frame;

            boolean varArgs = script.isVarArgs();
            int baseCurried = closure.curried;

            if (varArgs) {
                if (baseCurried >= script.getArgCount()) {
                   frame = createNewVarArgFrame(sf, scriptArgs(baseCurried, args));
                } else {
                   frame = sf.assign(scriptArgs(baseCurried, args));
                }
            } else {
                frame = sf.assign(scriptArgs(args));
            }
            caller = closure.caller;
            context = closure.context;
            curried = baseCurried + args.length;
            chained = closure.chained;
        } else {
            caller = null;
            frame = script.createFrame(scriptArgs(args));
            context = null;
            curried = args.length;
            chained = null;
        }
    }

    /**
     * Creates a chained closure.
     * @param base the base closure
     * @param chained the chained closure
     */
    protected Closure(final Closure base, final Closure chained) {
        super(base.jexl, base.source, base.script);
        caller = base.caller;
        frame = base.frame.assign();
        context = base.context;
        curried = base.curried;
        this.chained = chained;
    }

    protected static Closure create(Interpreter theCaller, ASTJexlScript lambda) {
        int argCount = lambda.getArgCount();
        switch (argCount) {
            case 0: 
                return new ClosureSupplier(theCaller, lambda);
            case 1: 
                // Check first argument type
                Scope s = lambda.getScope();
                Class c = s.getVariableType(0);
                if (c == Integer.class || c == Integer.TYPE) {
                    return new ClosureIntFunction(theCaller, lambda);
                } else if (c == Long.class || c == Long.TYPE) {
                    return new ClosureLongFunction(theCaller, lambda);
                } else if (c == Double.class || c == Double.TYPE) {
                    return new ClosureDoubleFunction(theCaller, lambda);
                } else {
                    return new ClosureFunction(theCaller, lambda);
                }
            case 2: 
                return new ClosureBiFunction(theCaller, lambda);
            default: 
                return new Closure(theCaller, lambda);
        }
    }

    protected static Closure create(Script base, Object[] args) {
        String[] parms = base.getUnboundParameters();
        int argCount = parms != null ? parms.length : 0;
        if (args != null)
            argCount -= args.length;
        if (argCount < 0)
            argCount = 0;
        switch (argCount) {
            case 0: 
                return new ClosureSupplier(base, args);
            case 1:
                // Check last argument type
                Scope s = base.getScript().getScope();
                Class c = s.getVariableType(parms.length - 1);
                if (c == Integer.class || c == Integer.TYPE) {
                    return new ClosureIntFunction(base, args);
                } else if (c == Long.class || c == Long.TYPE) {
                    return new ClosureLongFunction(base, args);
                } else if (c == Double.class || c == Double.TYPE) {
                    return new ClosureDoubleFunction(base, args);
                } else {
                    return new ClosureFunction(base, args);
                }
            case 2:
                return new ClosureBiFunction(base, args);
            default:
                return new Closure(base, args); 
        }
    }

    public static Closure create(Closure base, Closure chained) {
        String[] parms = base.getUnboundParameters();
        int argCount = parms != null ? parms.length : 0;
        switch (argCount) {
            case 0:
                return new ClosureSupplier(base, chained);
            case 1:
                // Check last argument type
                Scope s = base.getScript().getScope();
                Class c = s.getVariableType(parms.length - 1);
                if (c == Integer.class || c == Integer.TYPE) {
                    return new ClosureIntFunction(base, chained);
                } else if (c == Long.class || c == Long.TYPE) {
                    return new ClosureLongFunction(base, chained);
                } else if (c == Double.class || c == Double.TYPE) {
                    return new ClosureDoubleFunction(base, chained);
                } else {
                    return new ClosureFunction(base, chained);
                }
            case 2:
                return new ClosureBiFunction(base, chained);
            default:
                return new Closure(base, chained);
        }
    }

    @Override
    public String toString() {
        if (chained != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(super.toString());
            sb.append(" >> ");
            sb.append(chained.toString());
            return sb.toString();
        } else {
            return super.toString();
        }
    }

    /**
     * Creates this script interpreter.
     * @param context the context
     * @param frame the calling frame
     * @return  the interpreter
     */
    @Override
    protected Interpreter createInterpreter(JexlContext context, Frame frame) {
        if (context == null)
            context = this.context;
        JexlOptions opts = jexl.options(script, context);
        return jexl.createInterpreter(context, frame, opts, caller != null ? caller.current : null);
    }

    /**
     * Creates this script interpreter.
     * @param context the context
     * @param args    the script arguments
     * @return  the interpreter
     */
    @Override
    protected Interpreter createInterpreter(JexlContext context, Object... args) {
        return createInterpreter(context, getCallFrame(args));
    }


    /**
     * Appends additional arguments to the existing vararg parameter, creates new call frame if needed.
     * @param sf the call frame to append additional arguments to
     * @param args the parameters
     * @return the adjusted local frame
     */
    protected Frame createNewVarArgFrame(Frame sf, Object[] args) {
        Frame frame = sf;
        if (args != null && args.length > 0) {
           String[] params = getParameters();
           String name = params[params.length - 1];
           int varArgPos = frame.getScope().getSymbol(name);
           Class type = frame.getScope().getVariableType(varArgPos);
           if (type == null)
               type = Object.class;
           // Previous vararg array
           Object carg = frame.get(varArgPos);
           int len = carg != null ? Array.getLength(carg) : 0;
           // Added vararg array
           Object varg = args[0];
           int alen = varg != null ? Array.getLength(varg) : 0;
           int newlen = len + alen;
           if (newlen > 0) {
               // Clone frame
               frame = sf.clone();
               // Merge arrays
               Object result = Array.newInstance(type, newlen);
               System.arraycopy(carg, 0, result, 0, len);
               System.arraycopy(varg, 0, result, len, alen);
               frame.set(varArgPos, result);
           }
        }
        return frame;
    }

    /**
     * Creates call frame for the specified argument list.
     * @param args the parameters
     * @return the new local frame
     */
    protected Frame getCallFrame(Object[] args) {
        Frame local = null;
        if (frame != null) {
            boolean varArgs = script.isVarArgs();
            if (varArgs) {
                if (curried >= script.getArgCount()) {
                   local = createNewVarArgFrame(frame, scriptArgs(curried, args));
                } else {
                   local = frame.assign(scriptArgs(curried, args));
                }
            } else {
                local = frame.assign(scriptArgs(args));
            }
        } else {
            local = script.createFrame(scriptArgs(args));
        }
        return local;
    }

    @Override
    public String[] getUnboundParameters() {

        String[] scriptParams = super.getParameters();

        if (scriptParams == null || scriptParams.length == 0)
            return scriptParams;

        String[] unboundParams = frame.getUnboundParameters();

        boolean varArgs = script.isVarArgs();

        if (unboundParams.length == 0 && varArgs) {
            return new String[] {scriptParams[scriptParams.length - 1]};
        } else {
            return unboundParams;
        }
    }

    @Override
    public int hashCode() {
        // CSOFF: Magic number
        int hash = 17;
        hash = 31 * hash + (this.jexl != null ? this.jexl.hashCode() : 0);
        hash = 31 * hash + (this.source != null ? this.source.hashCode() : 0);
        hash = 31 * hash + (this.frame != null ? this.frame.hashCode() : 0);
        // CSON: Magic number
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Closure other = (Closure) obj;
        if (this.jexl != other.jexl) {
            return false;
        }
        if (!Objects.equals(this.source, other.source)) {
            return false;
        }
        if (!Objects.equals(this.frame, other.frame)) {
            return false;
        }
        return true;
    }

    /**
     * Sets the captured index of a given symbol, ie the target index of a parent
     * captured symbol in this closure's frame.
     * <p>This is meant to allow a locally defined function to "see" and call
     * itself as a local (captured) variable;
     * in other words, this allows recursive call of a function.
     * @param symbol the symbol index (in the caller of this closure)
     * @param value the value to set in the local frame
     */
    public void setCaptured(int symbol, Object value) {
        if (script instanceof ASTJexlLambda) {
            ASTJexlLambda lambda = (ASTJexlLambda) script;
            Scope scope = lambda.getScope();
            if (scope != null) {
                Integer reg = scope.getCaptured(symbol);
                if (reg != null) {
                    frame.set(reg, value);
                }
            }
        }
    }

    @Override
    public Object evaluate(JexlContext context) {
        return execute(context != null ? context : this.context, (Object[])null);
    }

    @Override
    public Object execute(JexlContext context) {
        return execute(context != null ? context : this.context, (Object[])null);
    }

    @Override
    public Object execute(JexlContext context, Object... args) {
        Interpreter interpreter = createInterpreter(context, args);
        Object result = interpreter.runClosure(this, null);
        if (chained == null)
            return result;
        return result instanceof Object[] ? chained.execute(context, (Object[]) result) : chained.execute(context, result);
    }

    @Override
    public Callable callable(JexlContext context, Object... args) {
        return new CallableScript(createInterpreter(context, args)) {
            @Override
            public Object interpret() {
                Object result = interpreter.runClosure(Closure.this, null);
                if (chained == null)
                    return result;
                return result instanceof Object[] ? chained.execute(context, (Object[]) result) : chained.execute(context, result);
            }
        };
    }

    /**
     * Implements the @FunctionalInterface interfaces with no arguments to help delegation.
     */
    public static class ClosureSupplier extends Closure implements
          Supplier<Object>, BooleanSupplier, DoubleSupplier, IntSupplier, LongSupplier,
          Callable<Object>, Runnable {
        /**
         * The base constructor.
         * @param intrprtr the interpreter to use
         */
        protected ClosureSupplier(Interpreter intrprtr, ASTJexlScript lambda) {
            super(intrprtr, lambda);
        }

        protected ClosureSupplier(Script base, Object[] args) {
            super(base, args);
        }

        protected ClosureSupplier(Closure base, Closure chained) {
            super(base, chained);
        }

        protected Object eval() {
            return execute(null);
        }

        @Override
        public void run() {
            eval();
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
    public static class ClosureFunction extends Closure implements
          Function<Object, Object>, DoubleFunction<Object>, LongFunction<Object>, IntFunction<Object>,
          UnaryOperator<Object>, Predicate<Object>,
          ToDoubleFunction<Object>, ToIntFunction<Object>, ToLongFunction<Object>,
          LongToDoubleFunction, LongToIntFunction, IntToDoubleFunction, IntToLongFunction,
          DoubleUnaryOperator, LongUnaryOperator, IntUnaryOperator,
          Consumer<Object>, DoubleConsumer, IntConsumer, LongConsumer {
        /**
         * The base constructor.
         * @param intrprtr the interpreter to use
         */
        protected ClosureFunction(Interpreter intrprtr, ASTJexlScript lambda) {
            super(intrprtr, lambda);
        }

        protected ClosureFunction(Script base, Object[] args) {
            super(base, args);
        }

        protected ClosureFunction(Closure base, Closure chained) {
            super(base, chained);
        }

        protected Object eval(Object arg) {
            return execute(null, arg);
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
     * Implements the @FunctionalInterface interfaces with one argument to help delegation.
     */
    public static class ClosureIntFunction extends Closure implements
          Function<Integer, Object>, IntFunction<Object>, 
          IntPredicate, IntToDoubleFunction, IntToLongFunction,
          ToDoubleFunction<Integer>, ToIntFunction<Integer>, ToLongFunction<Integer>,
          IntUnaryOperator, Consumer<Integer>, IntConsumer {
        /**
         * The base constructor.
         * @param intrprtr the interpreter to use
         */
        protected ClosureIntFunction(Interpreter intrprtr, ASTJexlScript lambda) {
            super(intrprtr, lambda);
        }

        protected ClosureIntFunction(Script base, Object[] args) {
            super(base, args);
        }

        protected ClosureIntFunction(Closure base, Closure chained) {
            super(base, chained);
        }

        protected Object eval(Object arg) {
            return execute(null, arg);
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
        public int applyAsInt(int arg) {
            return jexl.getArithmetic().toInteger(eval(arg));
        }

        @Override
        public int applyAsInt(Integer arg) {
            return jexl.getArithmetic().toInteger(eval(arg));
        }

        @Override
        public boolean test(int arg) {
            return jexl.getArithmetic().toBoolean(eval(arg));
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

        @Override
        public void accept(int arg) {
            eval(arg);
        }

        @Override
        public void accept(Integer arg) {
            eval(arg);
        }
    }

    /**
     * Implements the @FunctionalInterface interfaces with one argument to help delegation.
     */
    public static class ClosureLongFunction extends Closure implements
          Function<Long, Object>, LongFunction<Object>, 
          LongPredicate, LongToDoubleFunction, LongToIntFunction, 
          ToDoubleFunction<Long>, ToIntFunction<Long>, ToLongFunction<Long>,
          LongUnaryOperator, Consumer<Long>, LongConsumer {
        /**
         * The base constructor.
         * @param intrprtr the interpreter to use
         */
        protected ClosureLongFunction(Interpreter intrprtr, ASTJexlScript lambda) {
            super(intrprtr, lambda);
        }

        protected ClosureLongFunction(Script base, Object[] args) {
            super(base, args);
        }

        protected ClosureLongFunction(Closure base, Closure chained) {
            super(base, chained);
        }

        protected Object eval(Object arg) {
            return execute(null, arg);
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
        public long applyAsLong(long arg) {
            return jexl.getArithmetic().toLong(eval(arg));
        }

        @Override
        public long applyAsLong(Long arg) {
            return jexl.getArithmetic().toLong(eval(arg));
        }

        @Override
        public boolean test(long arg) {
            return jexl.getArithmetic().toBoolean(eval(arg));
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
        public int applyAsInt(long arg) {
            return jexl.getArithmetic().toInteger(eval(arg));
        }

        @Override
        public int applyAsInt(Long arg) {
            return jexl.getArithmetic().toInteger(eval(arg));
        }

        @Override
        public void accept(long arg) {
            eval(arg);
        }

        @Override
        public void accept(Long arg) {
            eval(arg);
        }
    }

    /**
     * Implements the @FunctionalInterface interfaces with one argument to help delegation.
     */
    public static class ClosureDoubleFunction extends Closure implements
          Function<Double, Object>, DoubleFunction<Object>,
          DoublePredicate, DoubleToIntFunction, DoubleToLongFunction,
          ToDoubleFunction<Double>, ToIntFunction<Double>, ToLongFunction<Double>,
          DoubleUnaryOperator, Consumer<Double>, DoubleConsumer {
        /**
         * The base constructor.
         * @param intrprtr the interpreter to use
         */
        protected ClosureDoubleFunction(Interpreter intrprtr, ASTJexlScript lambda) {
            super(intrprtr, lambda);
        }

        protected ClosureDoubleFunction(Script base, Object[] args) {
            super(base, args);
        }

        protected ClosureDoubleFunction(Closure base, Closure chained) {
            super(base, chained);
        }

        protected Object eval(Object arg) {
            return execute(null, arg);
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
        public double applyAsDouble(double arg) {
            return jexl.getArithmetic().toDouble(eval(arg));
        }

        @Override
        public boolean test(double arg) {
            return jexl.getArithmetic().toBoolean(eval(arg));
        }

        @Override
        public double applyAsDouble(Double arg) {
            return jexl.getArithmetic().toDouble(eval(arg));
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
        public long applyAsLong(double arg) {
            return jexl.getArithmetic().toLong(eval(arg));
        }

        @Override
        public long applyAsLong(Double arg) {
            return jexl.getArithmetic().toLong(eval(arg));
        }

        @Override
        public void accept(Double arg) {
            eval(arg);
        }

        @Override
        public void accept(double arg) {
            eval(arg);
        }
    }

    /**
     * Implements the @FunctionalInterface interfaces with two arguments to help delegation.
     */
    public static class ClosureBiFunction extends Closure implements
          Comparator<Object>, BiFunction<Object, Object, Object>, BiPredicate<Object, Object>,
          BinaryOperator<Object>, DoubleBinaryOperator, LongBinaryOperator, IntBinaryOperator,
          BiConsumer<Object, Object>, ObjDoubleConsumer<Object>, ObjLongConsumer<Object>, ObjIntConsumer<Object>,
          ToDoubleBiFunction<Object, Object>, ToLongBiFunction<Object, Object>, ToIntBiFunction<Object, Object> {
        /**
         * The base constructor.
         * @param intrprtr the interpreter to use
         */
        protected ClosureBiFunction(Interpreter intrprtr, ASTJexlScript lambda) {
            super(intrprtr, lambda);
        }

        protected ClosureBiFunction(Script base, Object[] args) {
            super(base, args);
        }

        protected ClosureBiFunction(Closure base, Closure chained) {
            super(base, chained);
        }

        protected Object eval(Object arg1, Object arg2) {
            return execute(null, arg1, arg2);
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

}
