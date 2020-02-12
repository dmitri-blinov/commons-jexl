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


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlException.VariableIssue;
import org.apache.commons.jexl3.JexlOperator;
import org.apache.commons.jexl3.JexlOptions;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlPropertyGet;
import org.apache.commons.jexl3.introspection.JexlPropertySet;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.jexl3.introspection.JexlUberspect.PropertyResolver;
import org.apache.commons.jexl3.parser.ASTArrayAccess;
import org.apache.commons.jexl3.parser.ASTArrayAccessSafe;
import org.apache.commons.jexl3.parser.ASTFunctionNode;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTIdentifierAccess;
import org.apache.commons.jexl3.parser.ASTMethodNode;
import org.apache.commons.jexl3.parser.ASTReference;
import org.apache.commons.jexl3.parser.ASTVar;
import org.apache.commons.jexl3.parser.JexlNode;
import org.apache.commons.jexl3.parser.JexlParser;
import org.apache.commons.jexl3.parser.ParserVisitor;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.logging.Log;

/**
 * The helper base of an interpreter of JEXL syntax.
 * @since 3.0
 */
public abstract class InterpreterBase extends ParserVisitor {
    /** The JEXL engine. */
    protected final Engine jexl;
    /** The logger. */
    protected final Log logger;
    /** The uberspect. */
    protected final JexlUberspect uberspect;
    /** The arithmetic handler. */
    protected JexlArithmetic arithmetic;
    /** The context to store/retrieve variables. */
    protected final JexlContext context;
    /** The options. */
    protected final JexlOptions options;
    /** Cache executors. */
    protected final boolean cache;
    /** Cancellation support. */
    protected final AtomicBoolean cancelled;
    /** Empty parameters for method matching. */
    protected static final Object[] EMPTY_PARAMS = new Object[0];
    /** The context to store/retrieve variables. */
    protected final JexlContext.NamespaceResolver ns;
    /** The operators evaluation delegate. */
    protected final Operators operators;
    /** The map of 'prefix:function' to object resolving as namespaces. */
    protected final Map<String, Object> functions;
    /** The map of dynamically creates namespaces, NamespaceFunctor or duck-types of those. */
    protected Map<String, Object> functors;

    /**
     * Creates an interpreter base.
     * @param engine   the engine creating this interpreter
     * @param opts     the evaluation options
     * @param aContext the evaluation context
     */
    protected InterpreterBase(Engine engine, JexlOptions opts, JexlContext aContext) {
        this.jexl = engine;
        this.logger = jexl.logger;
        this.uberspect = jexl.uberspect;
        this.context = aContext != null ? aContext : Engine.EMPTY_CONTEXT;
        this.cache = engine.cache != null;
        JexlArithmetic jexla = jexl.arithmetic;
        this.options = opts == null? engine.options(aContext) : opts;
        this.arithmetic = jexla.options(options);
        if (arithmetic != jexla && !arithmetic.getClass().equals(jexla.getClass())) {
            logger.warn("expected arithmetic to be " + jexla.getClass().getSimpleName()
                    + ", got " + arithmetic.getClass().getSimpleName()
            );
        }
        if (this.context instanceof JexlContext.NamespaceResolver) {
            ns = ((JexlContext.NamespaceResolver) context);
        } else {
            ns = Engine.EMPTY_NS;
        }
        AtomicBoolean acancel = null;
        if (this.context instanceof JexlContext.CancellationHandle) {
            acancel = ((JexlContext.CancellationHandle) context).getCancellation();
        }
        this.cancelled = acancel != null? acancel : new AtomicBoolean(false);
        this.functions = jexl.functions;
        this.functors = null;
        this.operators = new Operators(this);
    }

    /**
     * Copy constructor.
     * @param ii the base to copy
     * @param jexla the arithmetic instance to use (or null)
     */
    protected InterpreterBase(InterpreterBase ii, JexlArithmetic jexla) {
        jexl = ii.jexl;
        logger = ii.logger;
        uberspect = ii.uberspect;
        arithmetic = jexla;
        context = ii.context;
        options = ii.options.copy();
        cache = ii.cache;
        ns = ii.ns;
        operators = ii.operators;
        cancelled = ii.cancelled;
        functions = ii.functions;
        functors = ii.functors;
    }

    /**
     * Attempt to call close() if supported.
     * <p>This is used when dealing with auto-closeable (duck-like) objects
     * @param closeable the object we'd like to close
     */
    protected void closeIfSupported(Object closeable) {
        if (closeable != null) {
            try {
                if (closeable instanceof AutoCloseable) {
                    ((AutoCloseable)closeable).close();
                } else {
                    JexlMethod mclose = uberspect.getMethod(closeable, "close", EMPTY_PARAMS);
                    if (mclose != null) {
                       mclose.invoke(closeable, EMPTY_PARAMS);
                    }
                }
            } catch (Exception xignore) {
                logger.warn(xignore);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected static <T extends Throwable> void doThrow(Throwable t) throws T {
        throw (T) t;
    }

    protected class ResourceManager implements AutoCloseable {
        protected Object r;

        protected ResourceManager(Object resource) {
            r = resource;
        }

        @Override
        public void close() throws Exception {
            if (r instanceof AutoCloseable) {
                ((AutoCloseable)r).close();
            } else if (r != null) {
                JexlMethod mclose = uberspect.getMethod(r, "close", EMPTY_PARAMS);
                if (mclose != null) {
                    try {
                        mclose.invoke(r, EMPTY_PARAMS);
                    } catch (InvocationTargetException ex) {
                        InterpreterBase.<RuntimeException>doThrow(ex.getCause());
                    }
                }
            }
        }
    }

    /**
     * Resolves a namespace, eventually allocating an instance using context as constructor argument.
     * <p>
     * The lifetime of such instances span the current expression or script evaluation.</p>
     * @param prefix the prefix name (may be null for global namespace)
     * @param node   the AST node
     * @return the namespace instance
     */
    protected Object resolveNamespace(String prefix, JexlNode node) {
        Object namespace;
        // check whether this namespace is a functor
        synchronized (this) {
            if (functors != null) {
                namespace = functors.get(prefix);
                if (namespace != null) {
                    return namespace;
                }
            }
        }
        // check if namespace is a resolver
        namespace = ns.resolveNamespace(prefix);
        if (namespace == null) {
            namespace = functions.get(prefix);
            if (prefix != null && namespace == null) {
                throw new JexlException(node, "no such function namespace " + prefix, null);
            }
        }
        // shortcut if ns is known to be not-a-functor
        final boolean cacheable = cache;
        Object cached = cacheable ? node.jjtGetValue() : null;
        if (cached != JexlContext.NamespaceFunctor.class) {
            // allow namespace to instantiate a functor with context if possible, not an error otherwise
            Object functor = null;
            if (namespace instanceof JexlContext.NamespaceFunctor) {
                functor = ((JexlContext.NamespaceFunctor) namespace).createFunctor(context);
            } else if (namespace instanceof Class<?> || namespace instanceof String) {
                // attempt to reuse last ctor cached in volatile JexlNode.value
                if (cached instanceof JexlMethod) {
                    try {
                        Object eval = ((JexlMethod) cached).tryInvoke(null, context);
                        if (JexlEngine.TRY_FAILED != eval) {
                            functor = eval;
                        }
                    } catch (JexlException.TryFailed xtry) {
                        throw new JexlException(node, "unable to instantiate namespace " + prefix, xtry.getCause());
                    }
                }
                if (functor == null) {
                    JexlMethod ctor = uberspect.getConstructor(namespace, context);
                    if (ctor != null) {
                        try {
                            functor = ctor.invoke(namespace, context);
                            if (cacheable && ctor.isCacheable()) {
                                node.jjtSetValue(ctor);
                            }
                        } catch (Exception xinst) {
                            throw new JexlException(node, "unable to instantiate namespace " + prefix, xinst);
                        }
                    }
                }
            }
            // got a functor, store it and return it
            if (functor != null) {
                synchronized (this) {
                    if (functors == null) {
                        functors = new HashMap<String, Object>();
                    }
                    functors.put(prefix, functor);
                }
                return functor;
            } else {
                // use the NamespaceFunctor class to tag this node as not-a-functor
                node.jjtSetValue(JexlContext.NamespaceFunctor.class);
            }
        }
        return namespace;
    }

    /**
     * Defines a variable.
     * @param var the variable to define
     * @param frame the frame in which it will be defined
     * @return true if definition succeeded, false otherwise
     */
    protected boolean defineVariable(ASTVar var, LexicalFrame frame) {
        int symbol = var.getSymbol();
        if (symbol < 0) {
            return false;
        }
        if (var.isRedefined()) {
            return false;
        }
        return frame.defineSymbol(symbol, var.isCaptured());
    }

    /**
     * Gets a value of a defined local variable or from the context.
     * @param frame the local frame
     * @param block the lexical block if any
     * @param identifier the variable node
     * @return the value
     */
    protected Object getVariable(Frame frame, LexicalScope block, ASTIdentifier identifier) {
        int symbol = identifier.getSymbol();
        // if we have a symbol, we have a scope thus a frame
        if (options.isLexicalShade() && identifier.isShaded()) {
            return undefinedVariable(identifier, identifier.getName());
        }
        if (symbol >= 0) {
            if (frame.has(symbol)) {
                Object value = frame.get(symbol);
                if (value != Scope.UNDEFINED) {
                    return value;
                }
            }
        }
        String name = identifier.getName();
        Object value = context.get(name);
        if (value == null && !context.has(name)) {
            if (identifier.jjtGetParent() instanceof ASTReference) {
                Class type = JexlParser.resolveType(name);
                if (type != null)
                    return type;
            } else {
                return isSafe() ? null : unsolvableVariable(identifier, name, true); // undefined
            }
        }
        return value;
    }

    /**
     * Sets a variable in the global context.
     * <p>If interpretation applies lexical shade, the variable must exist (ie
     * the context has(...) method returns true) otherwise an error occurs.
     * @param node the node
     * @param name the variable name
     * @param value the variable value
     */
    protected void setContextVariable(JexlNode node, String name, Object value) {
        if (options.isLexicalShade() && !context.has(name)) {
            throw new JexlException.Variable(node, name, true);
        }
        try {
            context.set(name, value);
        } catch (UnsupportedOperationException xsupport) {
            throw new JexlException(node, "context is readonly", xsupport);
        }
    }

    /**
     * Whether this interpreter is currently evaluating with a strict engine flag.
     * @return true if strict engine, false otherwise
     */
    protected boolean isStrictEngine() {
        return options.isStrict();
    }

    /**
     * Whether this interpreter ignores null in navigation expression as errors.
     * @return true if safe, false otherwise
     */
    protected boolean isSafe() {
        return options.isSafe();
    }

    /**
     * Whether this interpreter is currently evaluating with a silent mode.
     * @return true if silent, false otherwise
     */
    protected boolean isSilent() {
        return options.isSilent();
    }

    /**
     * Whether this interpreter is currently evaluating with assertions enabled.
     * @return true if assertions enabled, false otherwise
     */
    protected boolean isAssertions() {
        return options.isAssertions();
    }

    /**
     * @return true if interrupt throws a JexlException.Cancel.
     */
    protected boolean isCancellable() {
        return options.isCancellable();
    }

    /**
     * Finds the node causing a NPE for diadic operators.
     * @param xrt   the RuntimeException
     * @param node  the parent node
     * @param left  the left argument
     * @param right the right argument
     * @return the left, right or parent node
     */
    protected JexlNode findNullOperand(RuntimeException xrt, JexlNode node, Object left, Object right) {
        if (xrt instanceof JexlArithmetic.NullOperand) {
            if (left == null) {
                return node.jjtGetChild(0);
            }
            if (right == null) {
                return node.jjtGetChild(1);
            }
        }
        return node;
    }

    /**
     * Triggered when a variable can not be resolved.
     * @param node  the node where the error originated from
     * @param var   the variable name
     * @param undef whether the variable is undefined or null
     * @return throws JexlException if strict and not silent, null otherwise
     */
    protected Object unsolvableVariable(JexlNode node, String var, boolean undef) {
        return variableError(node, var, undef? VariableIssue.UNDEFINED : VariableIssue.NULLVALUE);
    }

    /**
     * Triggered when a variable is lexically known as undefined.
     * @param node  the node where the error originated from
     * @param var   the variable name
     * @return throws JexlException if strict and not silent, null otherwise
     */
    protected Object undefinedVariable(JexlNode node, String var) {
        return variableError(node, var, VariableIssue.UNDEFINED);
    }

    /**
     * Triggered when a variable is lexically known as being redefined.
     * @param node  the node where the error originated from
     * @param var   the variable name
     * @return throws JexlException if strict and not silent, null otherwise
     */
    protected Object redefinedVariable(JexlNode node, String var) {
        return variableError(node, var, VariableIssue.REDEFINED);
    }

    /**
     * Triggered when a variable generates an issue.
     * @param node  the node where the error originated from
     * @param var   the variable name
     * @param issue the issue type
     * @return throws JexlException if strict and not silent, null otherwise
     */
    protected Object variableError(JexlNode node, String var, VariableIssue issue) {
        if (isStrictEngine() && !node.isTernaryProtected()) {
            throw new JexlException.Variable(node, var, issue);
        } else if (logger.isDebugEnabled()) {
            logger.debug(JexlException.variableError(node, var, issue));
        }
        return null;
    }
    /**
     * Triggered when a method can not be resolved.
     * @param node   the node where the error originated from
     * @param method the method name
     * @return throws JexlException if strict and not silent, null otherwise
     */
    protected Object unsolvableMethod(JexlNode node, String method) {
        return unsolvableMethod(node, method, null);
    }

    /**
     * Triggered when a method can not be resolved.
     * @param node   the node where the error originated from
     * @param method the method name
     * @param args the method arguments
     * @return throws JexlException if strict and not silent, null otherwise
     */
    protected Object unsolvableMethod(JexlNode node, String method, Object[] args) {
        if (isStrictEngine()) {
            throw new JexlException.Method(node, method, args);
        } else if (logger.isDebugEnabled()) {
            logger.debug(JexlException.methodError(node, method, args));
        }
        return null;
    }

    /**
     * Triggered when a property can not be resolved.
     * @param node  the node where the error originated from
     * @param property   the property node
     * @param cause the cause if any
     * @param undef whether the property is undefined or null
     * @return throws JexlException if strict and not silent, null otherwise
     */
    protected Object unsolvableProperty(JexlNode node, String property, boolean undef, Throwable cause) {
        if (isStrictEngine() && !node.isTernaryProtected()) {
            throw new JexlException.Property(node, property, undef, cause);
        } else if (logger.isDebugEnabled()) {
            logger.debug(JexlException.propertyError(node, property, undef));
        }
        return null;
    }

    /**
     * Checks whether a reference child node holds a local variable reference.
     * @param node  the reference node
     * @param which the child we are checking
     * @return true if child is local variable, false otherwise
     */
    protected boolean isLocalVariable(ASTReference node, int which) {
        return (node.jjtGetNumChildren() > which
                && node.jjtGetChild(which) instanceof ASTIdentifier
                && ((ASTIdentifier) node.jjtGetChild(which)).getSymbol() >= 0);
    }

    /**
     * Checks whether a reference child node holds a function call.
     * @param node  the reference node
     * @return true if child is function call, false otherwise
     */
    protected boolean isFunctionCall(ASTReference node) {
        return (node.jjtGetNumChildren() > 0
                && node.jjtGetChild(0) instanceof ASTFunctionNode);
    }

    /**
     * Pretty-prints a failing property (de)reference.
     * <p>Used by calls to unsolvableProperty(...).</p>
     * @param node the property node
     * @return the (pretty) string
     */
    protected String stringifyProperty(JexlNode node) {
        if (node instanceof ASTArrayAccess) {
            return "["
                    + stringifyPropertyValue(node.jjtGetChild(0))
                    + "]";
        }
        if (node instanceof ASTArrayAccessSafe) {
            return "?["
                    + stringifyPropertyValue(node.jjtGetChild(0))
                    + "]";
        }
        if (node instanceof ASTMethodNode) {
            return stringifyPropertyValue(node.jjtGetChild(0));
        }
        if (node instanceof ASTFunctionNode) {
            return stringifyPropertyValue(node.jjtGetChild(0));
        }
        if (node instanceof ASTIdentifier) {
            return ((ASTIdentifier) node).getName();
        }
        if (node instanceof ASTReference) {
            return stringifyProperty(node.jjtGetChild(0));
        }
        return stringifyPropertyValue(node);
    }

    /**
     * Pretty-prints a failing property value (de)reference.
     * <p>Used by calls to unsolvableProperty(...).</p>
     * @param node the property node
     * @return the (pretty) string value
     */
    protected static String stringifyPropertyValue(JexlNode node) {
        return node != null? new Debugger().depth(1).data(node) : "???";
    }

    /**
     * Triggered when an operator fails.
     * @param node     the node where the error originated from
     * @param operator the method name
     * @param cause    the cause of error (if any)
     * @return throws JexlException if strict and not silent, null otherwise
     */
    protected Object operatorError(JexlNode node, JexlOperator operator, Throwable cause) {
        if (isStrictEngine()) {
            throw new JexlException.Operator(node, operator.getOperatorSymbol(), cause);
        } else if (logger.isDebugEnabled()) {
            logger.debug(JexlException.operatorError(node, operator.getOperatorSymbol()), cause);
        }
        return null;
    }

    /**
     * Triggered when an annotation processing fails.
     * @param node     the node where the error originated from
     * @param annotation the annotation name
     * @param cause    the cause of error (if any)
     * @return throws a JexlException if strict and not silent, null otherwise
     */
    protected Object annotationError(JexlNode node, String annotation, Throwable cause) {
        if (isStrictEngine()) {
            throw new JexlException.Annotation(node, annotation, cause);
        } else if (logger.isDebugEnabled()) {
            logger.debug(JexlException.annotationError(node, annotation), cause);
        }
        return null;
    }

    /**
     * Triggered when method, function or constructor invocation fails with an exception.
     * @param node       the node triggering the exception
     * @param methodName the method/function name
     * @param xany       the cause
     * @return a JexlException that will be thrown
     */
    protected JexlException invocationException(JexlNode node, String methodName, Throwable xany) {
        Throwable cause = xany.getCause();
        if (cause instanceof JexlException) {
            return (JexlException) cause;
        }
        if (cause instanceof InterruptedException) {
            return new JexlException.Cancel(node);
        }
        return new JexlException(node, methodName, xany);
    }

    /**
     * Cancels this evaluation, setting the cancel flag that will result in a JexlException.Cancel to be thrown.
     * @return false if already cancelled, true otherwise
     */
    protected  boolean cancel() {
        return cancelled.compareAndSet(false, true);
    }

    /**
     * Checks whether this interpreter execution was cancelled due to thread interruption.
     * @return true if cancelled, false otherwise
     */
    protected boolean isCancelled() {
        return cancelled.get() | Thread.currentThread().isInterrupted();
    }

    /**
     * Throws a JexlException.Cancel if script execution was cancelled.
     * @param node the node being evaluated
     */
    protected void cancelCheck(JexlNode node) {
        if (isCancelled()) {
            throw new JexlException.Cancel(node);
        }
    }

    /**
     * Concatenate arguments in call(...).
     * <p>When target == context, we are dealing with a global namespace function call
     * @param target the pseudo-method owner, first to-be argument
     * @param narrow whether we should attempt to narrow number arguments
     * @param args   the other (non null) arguments
     * @return the arguments array
     */
    protected Object[] functionArguments(Object target, boolean narrow, Object[] args) {
        // when target == context, we are dealing with the null namespace
        if (target == null || target == context) {
            if (narrow) {
                arithmetic.narrowArguments(args);
            }
            return args;
        }
        // makes target 1st args, copy others - optionally narrow numbers
        Object[] nargv = new Object[args.length + 1];
        if (narrow) {
            nargv[0] = functionArgument(true, target);
            for (int a = 1; a <= args.length; ++a) {
                nargv[a] = functionArgument(true, args[a - 1]);
            }
        } else {
            nargv[0] = target;
            System.arraycopy(args, 0, nargv, 1, args.length);
        }
        return nargv;
    }

    /**
     * Concatenate arguments in call(...).
     * @param target the pseudo-method owner, first to-be argument
     * @param narrow whether we should attempt to narrow number arguments
     * @param args   the other (non null) arguments
     * @return the arguments array
     */
    protected Object[] callArguments(Object target, boolean narrow, Object[] args) {
        // makes target 1st args, copy others - optionally narrow numbers
        Object[] nargv = new Object[args.length + 1];
        if (narrow) {
            nargv[0] = functionArgument(true, target);
            for (int a = 1; a <= args.length; ++a) {
                nargv[a] = functionArgument(true, args[a - 1]);
            }
        } else {
            nargv[0] = target;
            System.arraycopy(args, 0, nargv, 1, args.length);
        }
        return nargv;
    }

    /**
     * Optionally narrows an argument for a function call.
     * @param narrow whether narrowing should occur
     * @param arg    the argument
     * @return the narrowed argument
     */
    protected Object functionArgument(boolean narrow, Object arg) {
        return narrow && arg instanceof Number ? arithmetic.narrow((Number) arg) : arg;
    }

    /**
     * Cached function call.
     */
    protected static class Funcall implements JexlNode.Funcall {
        /** Whether narrow should be applied to arguments. */
        protected final boolean narrow;
        /** The JexlMethod to delegate the call to. */
        protected final JexlMethod me;
        /**
         * Constructor.
         * @param jme  the method
         * @param flag the narrow flag
         */
        protected Funcall(JexlMethod jme, boolean flag) {
            this.me = jme;
            this.narrow = flag;
        }

        /**
         * Try invocation.
         * @param ii     the interpreter
         * @param name   the method name
         * @param target the method target
         * @param args   the method arguments
         * @return the method invocation result (or JexlEngine.TRY_FAILED)
         */
        protected Object tryInvoke(InterpreterBase ii, String name, Object target, Object[] args) throws Exception {
            return me.tryInvoke(name, target, ii.functionArguments(null, narrow, args));
        }
    }

    /**
     * Cached arithmetic function call.
     */
    protected static class ArithmeticFuncall extends Funcall {
        /**
         * Constructor.
         * @param jme  the method
         * @param flag the narrow flag
         */
        protected ArithmeticFuncall(JexlMethod jme, boolean flag) {
            super(jme, flag);
        }

        @Override
        protected Object tryInvoke(InterpreterBase ii, String name, Object target, Object[] args) throws Exception {
            return me.tryInvoke(name, ii.arithmetic, ii.functionArguments(target, narrow, args));
        }
    }

    /**
     * Cached context function call.
     */
    protected static class ContextFuncall extends Funcall {
        /**
         * Constructor.
         * @param jme  the method
         * @param flag the narrow flag
         */
        protected ContextFuncall(JexlMethod jme, boolean flag) {
            super(jme, flag);
        }

        @Override
        protected Object tryInvoke(InterpreterBase ii, String name, Object target, Object[] args) throws Exception {
            return me.tryInvoke(name, ii.context, ii.functionArguments(target, narrow, args));
        }
    }

    /**
     * A ctor that needs a context as 1st argument.
     */
    protected static class ContextualCtor extends Funcall {
        /**
         * Constructor.
         * @param jme the method
         * @param flag the narrow flag
         */
        protected ContextualCtor(JexlMethod jme, boolean flag) {
            super(jme, flag);
        }

        @Override
        protected Object tryInvoke(InterpreterBase ii, String name, Object target, Object[] args) throws Exception {
            return me.tryInvoke(name, target, ii.callArguments(ii.context, narrow, args));
        }
    }

    /**
     * Helping dispatch function calls.
     */
    protected class CallDispatcher {
        /**
         * The syntactic node.
         */
        final JexlNode node;
        /**
         * Whether solution is cacheable.
         */
        boolean cacheable = true;
        /**
         * Whether arguments have been narrowed.
         */
        boolean narrow = false;
        /**
         * The method to call.
         */
        JexlMethod vm = null;
        /**
         * The method invocation target.
         */
        Object target = null;
        /**
         * The actual arguments.
         */
        Object[] argv = null;
        /**
         * The cacheable funcall if any.
         */
        Funcall funcall = null;

        /**
         * Dispatcher ctor.
         *
         * @param anode the syntactic node.
         * @param acacheable whether resolution can be cached
         */
        CallDispatcher(JexlNode anode, boolean acacheable) {
            this.node = anode;
            this.cacheable = acacheable;
        }

        /**
         * Whether the method is a target method.
         *
         * @param ntarget the target instance
         * @param mname the method name
         * @param arguments the method arguments
         * @return true if arithmetic, false otherwise
         */
        protected boolean isTargetMethod(Object ntarget, String mname, final Object[] arguments) {
            // try a method
            vm = uberspect.getMethod(ntarget, mname, arguments);
            if (vm != null) {
                argv = arguments;
                target = ntarget;
                if (cacheable && vm.isCacheable()) {
                    funcall = new Funcall(vm, narrow);
                }
                return true;
            }
            return false;
        }

        /**
         * Whether the method is a context method.
         *
         * @param mname the method name
         * @param arguments the method arguments
         * @return true if arithmetic, false otherwise
         */
        protected boolean isContextMethod(String mname, final Object[] arguments) {
            vm = uberspect.getMethod(context, mname, arguments);
            if (vm != null) {
                argv = arguments;
                target = context;
                if (cacheable && vm.isCacheable()) {
                    funcall = new ContextFuncall(vm, narrow);
                }
                return true;
            }
            return false;
        }

        /**
         * Whether the method is an arithmetic method.
         *
         * @param mname the method name
         * @param arguments the method arguments
         * @return true if arithmetic, false otherwise
         */
        protected boolean isArithmeticMethod(String mname, final Object[] arguments) {
            vm = uberspect.getMethod(arithmetic, mname, arguments);
            if (vm != null) {
                argv = arguments;
                target = arithmetic;
                if (cacheable && vm.isCacheable()) {
                    funcall = new ArithmeticFuncall(vm, narrow);
                }
                return true;
            }
            return false;
        }

        /**
         * Attempt to reuse last funcall cached in volatile JexlNode.value (if
         * it was cacheable).
         *
         * @param ntarget the target instance
         * @param mname the method name
         * @param arguments the method arguments
         * @return TRY_FAILED if invocation was not possible or failed, the
         * result otherwise
         */
        protected Object tryEval(final Object ntarget, final String mname, final Object[] arguments) throws Exception {
            // do we have  a method/function name ?
            // attempt to reuse last funcall cached in volatile JexlNode.value (if it was not a variable)
            if (mname != null && cacheable && ntarget != null) {
                Object cached = node.jjtGetValue();
                if (cached instanceof Funcall) {
                    return ((Funcall) cached).tryInvoke(InterpreterBase.this, mname, ntarget, arguments);
                }
            }
            return JexlEngine.TRY_FAILED;
        }

        /**
         * Evaluates the method previously dispatched.
         *
         * @param mname the method name
         * @return the method invocation result
         * @throws Exception when invocation fails
         */
        protected Object eval(String mname) throws Exception {
            // we have either evaluated and returned or might have found a method
            if (vm != null) {
                // vm cannot be null if xjexl is null
                Object eval = vm.invoke(target, argv);
                // cache executor in volatile JexlNode.value
                if (funcall != null) {
                    node.jjtSetValue(funcall);
                }
                return eval;
            }
            return unsolvableMethod(node, mname, argv);
        }
    }

    /**
     * Gets an attribute of an object.
     *
     * @param object    to retrieve value from
     * @param attribute the attribute of the object, e.g. an index (1, 0, 2) or key for a map
     * @param node      the node that evaluated as the object
     * @return the attribute value
     */
    protected Object getAttribute(Object object, Object attribute, JexlNode node) {
        if (object == null) {
            throw new JexlException(node, "object is null");
        }
        cancelCheck(node);
        final JexlOperator operator = node != null &&
                (node.jjtGetParent() instanceof ASTArrayAccess ||
                 node.jjtGetParent() instanceof ASTArrayAccessSafe)
                ? JexlOperator.ARRAY_GET : JexlOperator.PROPERTY_GET;
        Object result = operators.tryOverload(node, operator, object, attribute);
        if (result != JexlEngine.TRY_FAILED) {
            return result;
        }
        Exception xcause = null;
        try {
            // attempt to reuse last executor cached in volatile JexlNode.value
            if (node != null && cache) {
                Object cached = node.jjtGetValue();
                if (cached instanceof JexlPropertyGet) {
                    JexlPropertyGet vg = (JexlPropertyGet) cached;
                    Object value = vg.tryInvoke(object, attribute);
                    if (!vg.tryFailed(value)) {
                        return value;
                    }
                }
            }

            // attempt arithmetic implementation
            result = (operator == JexlOperator.ARRAY_GET) ? arithmetic.arrayGet(object, attribute)
                : arithmetic.propertyGet(object, attribute);
            if (result != JexlEngine.TRY_FAILED) {
                return result;
            }

            // resolve that property
            List<PropertyResolver> resolvers = uberspect.getResolvers(operator, object);
            JexlPropertyGet vg = uberspect.getPropertyGet(resolvers, object, attribute);
            if (vg != null) {
                Object value = vg.invoke(object);
                // cache executor in volatile JexlNode.value
                if (node != null && cache && vg.isCacheable()) {
                    node.jjtSetValue(vg);
                }
                return value;
            }
        } catch (Exception xany) {
            xcause = xany;
        }
        // lets fail
        if (node != null) {
            boolean safe = (node instanceof ASTIdentifierAccess) && ((ASTIdentifierAccess) node).isSafe();
            if (safe) {
                return null;
            } else {
                String attrStr = attribute != null ? attribute.toString() : null;
                return unsolvableProperty(node, attrStr, true, xcause);
            }
        } else {
            // direct call
            String error = "unable to get object property"
                    + ", class: " + object.getClass().getName()
                    + ", property: " + attribute;
            throw new UnsupportedOperationException(error, xcause);
        }
    }

    /**
     * Sets an attribute of an object.
     *
     * @param object    to set the value to
     * @param attribute the attribute of the object, e.g. an index (1, 0, 2) or key for a map
     * @param value     the value to assign to the object's attribute
     * @param node      the node that evaluated as the object
     */
    protected void setAttribute(Object object, Object attribute, Object value, JexlNode node, JexlOperator operator) {
        cancelCheck(node);
        Object result = operators.tryOverload(node, operator, object, attribute, value);
        if (result != JexlEngine.TRY_FAILED) {
            return;
        }
        Exception xcause = null;
        try {
            // check if we need to typecast value first
            Class type = object != null ? object.getClass() : null;
            if (type != null && type.isArray()) {
                type = arithmetic.getWrapperClass(type.getComponentType());
                if (!type.isInstance(value)) {
                    if (arithmetic.isStrict()) {
                        value = arithmetic.implicitCast(type, value);
                    } else {
                        value = arithmetic.cast(type, value);
                    }
                }
            }
            // attempt to reuse last executor cached in volatile JexlNode.value
            if (node != null && cache) {
                Object cached = node.jjtGetValue();
                if (cached instanceof JexlPropertySet) {
                    JexlPropertySet setter = (JexlPropertySet) cached;
                    Object eval = setter.tryInvoke(object, attribute, value);
                    if (!setter.tryFailed(eval)) {
                        return;
                    }
                }
            }
            // attempt arithmetic implementation
            result = (operator == JexlOperator.ARRAY_SET) ? arithmetic.arraySet(object, attribute, value)
                : arithmetic.propertySet(object, attribute, value);
            if (result != JexlEngine.TRY_FAILED) {
                return;
            }
            List<PropertyResolver> resolvers = uberspect.getResolvers(operator, object);
            JexlPropertySet vs = uberspect.getPropertySet(resolvers, object, attribute, value);
            // if we can't find an exact match, narrow the value argument and try again
            if (vs == null) {
                // replace all numbers with the smallest type that will fit
                Object[] narrow = {value};
                if (arithmetic.narrowArguments(narrow)) {
                    vs = uberspect.getPropertySet(resolvers, object, attribute, narrow[0]);
                }
            }
            if (vs != null) {
                // cache executor in volatile JexlNode.value
                vs.invoke(object, value);
                if (node != null && cache && vs.isCacheable()) {
                    node.jjtSetValue(vs);
                }
                return;
            }
        } catch (Exception xany) {
            xcause = xany;
        }
        // lets fail
        if (node != null) {
            String attrStr = attribute != null ? attribute.toString() : null;
            unsolvableProperty(node, attrStr, true, xcause);
        } else {
            // direct call
            String error = "unable to set object property"
                    + ", class: " + object.getClass().getName()
                    + ", property: " + attribute
                    + ", argument: " + value.getClass().getSimpleName();
            throw new UnsupportedOperationException(error, xcause);
        }
    }
}
