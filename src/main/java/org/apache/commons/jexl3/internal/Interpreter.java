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
//CSOFF: FileLength
package org.apache.commons.jexl3.internal;

import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlInfo;
import org.apache.commons.jexl3.JexlOperator;
import org.apache.commons.jexl3.JexlOptions;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.JxltEngine;

import org.apache.commons.jexl3.introspection.JexlMethod;

import org.apache.commons.jexl3.parser.ASTAddNode;
import org.apache.commons.jexl3.parser.ASTAndNode;
import org.apache.commons.jexl3.parser.ASTAnnotatedStatement;
import org.apache.commons.jexl3.parser.ASTAnnotation;
import org.apache.commons.jexl3.parser.ASTArguments;
import org.apache.commons.jexl3.parser.ASTArrayAccess;
import org.apache.commons.jexl3.parser.ASTArrayAccessSafe;
import org.apache.commons.jexl3.parser.ASTArrayConstructorNode;
import org.apache.commons.jexl3.parser.ASTArrayLiteral;
import org.apache.commons.jexl3.parser.ASTArrayOpenDimension;
import org.apache.commons.jexl3.parser.ASTAssertStatement;
import org.apache.commons.jexl3.parser.ASTAssignment;
import org.apache.commons.jexl3.parser.ASTAttributeReference;
import org.apache.commons.jexl3.parser.ASTAwaitFunction;
import org.apache.commons.jexl3.parser.ASTBitwiseAndNode;
import org.apache.commons.jexl3.parser.ASTBitwiseComplNode;
import org.apache.commons.jexl3.parser.ASTBitwiseOrNode;
import org.apache.commons.jexl3.parser.ASTBitwiseDiffNode;
import org.apache.commons.jexl3.parser.ASTBitwiseXorNode;
import org.apache.commons.jexl3.parser.ASTBlock;
import org.apache.commons.jexl3.parser.ASTBooleanLiteral;
import org.apache.commons.jexl3.parser.ASTBreak;
import org.apache.commons.jexl3.parser.ASTCastNode;
import org.apache.commons.jexl3.parser.ASTCatchBlock;
import org.apache.commons.jexl3.parser.ASTClassLiteral;
import org.apache.commons.jexl3.parser.ASTConstructorNode;
import org.apache.commons.jexl3.parser.ASTContinue;
import org.apache.commons.jexl3.parser.ASTCurrentNode;
import org.apache.commons.jexl3.parser.ASTDecrementGetNode;
import org.apache.commons.jexl3.parser.ASTGetDecrementNode;
import org.apache.commons.jexl3.parser.ASTDelete;
import org.apache.commons.jexl3.parser.ASTDivNode;
import org.apache.commons.jexl3.parser.ASTDoWhileStatement;
import org.apache.commons.jexl3.parser.ASTEQNode;
import org.apache.commons.jexl3.parser.ASTEQPredicate;
import org.apache.commons.jexl3.parser.ASTERNode;
import org.apache.commons.jexl3.parser.ASTERPredicate;
import org.apache.commons.jexl3.parser.ASTEWNode;
import org.apache.commons.jexl3.parser.ASTEWPredicate;
import org.apache.commons.jexl3.parser.ASTElvisNode;
import org.apache.commons.jexl3.parser.ASTEmptyFunction;
import org.apache.commons.jexl3.parser.ASTEnumerationNode;
import org.apache.commons.jexl3.parser.ASTEnumerationReference;
import org.apache.commons.jexl3.parser.ASTExpressionStatement;
import org.apache.commons.jexl3.parser.ASTExtVar;
import org.apache.commons.jexl3.parser.ASTFieldAccess;
import org.apache.commons.jexl3.parser.ASTForStatement;
import org.apache.commons.jexl3.parser.ASTForInitializationNode;
import org.apache.commons.jexl3.parser.ASTForTerminationNode;
import org.apache.commons.jexl3.parser.ASTForIncrementNode;
import org.apache.commons.jexl3.parser.ASTForeachStatement;
import org.apache.commons.jexl3.parser.ASTForeachVar;
import org.apache.commons.jexl3.parser.ASTFunctionNode;
import org.apache.commons.jexl3.parser.ASTFunctionStatement;
import org.apache.commons.jexl3.parser.ASTFunctionVar;
import org.apache.commons.jexl3.parser.ASTGENode;
import org.apache.commons.jexl3.parser.ASTGEPredicate;
import org.apache.commons.jexl3.parser.ASTGTNode;
import org.apache.commons.jexl3.parser.ASTGTPredicate;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTIdentifierAccess;
import org.apache.commons.jexl3.parser.ASTIdentifierAccessJxlt;
import org.apache.commons.jexl3.parser.ASTIncrementGetNode;
import org.apache.commons.jexl3.parser.ASTGetIncrementNode;
import org.apache.commons.jexl3.parser.ASTIndirectNode;
import org.apache.commons.jexl3.parser.ASTInitialization;
import org.apache.commons.jexl3.parser.ASTInitializedArrayConstructorNode;
import org.apache.commons.jexl3.parser.ASTInitializedCollectionConstructorNode;
import org.apache.commons.jexl3.parser.ASTInitializedMapConstructorNode;
import org.apache.commons.jexl3.parser.ASTInlineFieldEntry;
import org.apache.commons.jexl3.parser.ASTInlineFieldNEEntry;
import org.apache.commons.jexl3.parser.ASTInlineFieldNullEntry;
import org.apache.commons.jexl3.parser.ASTInlinePropertyAssignment;
import org.apache.commons.jexl3.parser.ASTInlinePropertyArrayEntry;
import org.apache.commons.jexl3.parser.ASTInlinePropertyArrayNullEntry;
import org.apache.commons.jexl3.parser.ASTInlinePropertyArrayNEEntry;
import org.apache.commons.jexl3.parser.ASTInlinePropertyEntry;
import org.apache.commons.jexl3.parser.ASTInlinePropertyNEEntry;
import org.apache.commons.jexl3.parser.ASTInlinePropertyNullEntry;
import org.apache.commons.jexl3.parser.ASTInnerConstructorNode;
import org.apache.commons.jexl3.parser.ASTIfStatement;
import org.apache.commons.jexl3.parser.ASTIOFNode;
import org.apache.commons.jexl3.parser.ASTISNode;
import org.apache.commons.jexl3.parser.ASTJexlLambda;
import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.apache.commons.jexl3.parser.ASTJxltLiteral;
import org.apache.commons.jexl3.parser.ASTLENode;
import org.apache.commons.jexl3.parser.ASTLEPredicate;
import org.apache.commons.jexl3.parser.ASTLTNode;
import org.apache.commons.jexl3.parser.ASTLTPredicate;
import org.apache.commons.jexl3.parser.ASTMapEntry;
import org.apache.commons.jexl3.parser.ASTMapEntryLiteral;
import org.apache.commons.jexl3.parser.ASTMapEnumerationNode;
import org.apache.commons.jexl3.parser.ASTMapLiteral;
import org.apache.commons.jexl3.parser.ASTMapProjectionNode;
import org.apache.commons.jexl3.parser.ASTMethodNode;
import org.apache.commons.jexl3.parser.ASTMethodReference;
import org.apache.commons.jexl3.parser.ASTModNode;
import org.apache.commons.jexl3.parser.ASTMulNode;
import org.apache.commons.jexl3.parser.ASTMultipleAssignment;
import org.apache.commons.jexl3.parser.ASTMultipleIdentifier;
import org.apache.commons.jexl3.parser.ASTMultipleVarStatement;
import org.apache.commons.jexl3.parser.ASTMultiVar;
import org.apache.commons.jexl3.parser.ASTNEAssignment;
import org.apache.commons.jexl3.parser.ASTNENode;
import org.apache.commons.jexl3.parser.ASTNEPredicate;
import org.apache.commons.jexl3.parser.ASTNEWNode;
import org.apache.commons.jexl3.parser.ASTNEWPredicate;
import org.apache.commons.jexl3.parser.ASTNINode;
import org.apache.commons.jexl3.parser.ASTNIOFNode;
import org.apache.commons.jexl3.parser.ASTNRNode;
import org.apache.commons.jexl3.parser.ASTNRPredicate;
import org.apache.commons.jexl3.parser.ASTNSWNode;
import org.apache.commons.jexl3.parser.ASTNSWPredicate;
import org.apache.commons.jexl3.parser.ASTNotNode;
import org.apache.commons.jexl3.parser.ASTNullAssignment;
import org.apache.commons.jexl3.parser.ASTNullLiteral;
import org.apache.commons.jexl3.parser.ASTNullpNode;
import org.apache.commons.jexl3.parser.ASTNumberLiteral;
import org.apache.commons.jexl3.parser.ASTOrNode;
import org.apache.commons.jexl3.parser.ASTPipeNode;
import org.apache.commons.jexl3.parser.ASTPointerNode;
import org.apache.commons.jexl3.parser.ASTProjectionNode;
import org.apache.commons.jexl3.parser.ASTQualifiedConstructorNode;
import org.apache.commons.jexl3.parser.ASTRangeNode;
import org.apache.commons.jexl3.parser.ASTReference;
import org.apache.commons.jexl3.parser.ASTEnclosedExpression;
import org.apache.commons.jexl3.parser.ASTRegexLiteral;
import org.apache.commons.jexl3.parser.ASTRemove;
import org.apache.commons.jexl3.parser.ASTReturnStatement;
import org.apache.commons.jexl3.parser.ASTSWNode;
import org.apache.commons.jexl3.parser.ASTSWPredicate;
import org.apache.commons.jexl3.parser.ASTSelectionNode;
import org.apache.commons.jexl3.parser.ASTSetAddNode;
import org.apache.commons.jexl3.parser.ASTSetAndNode;
import org.apache.commons.jexl3.parser.ASTSetDivNode;
import org.apache.commons.jexl3.parser.ASTSetLiteral;
import org.apache.commons.jexl3.parser.ASTSetModNode;
import org.apache.commons.jexl3.parser.ASTSetMultNode;
import org.apache.commons.jexl3.parser.ASTSetOperand;
import org.apache.commons.jexl3.parser.ASTSetOrNode;
import org.apache.commons.jexl3.parser.ASTSetDiffNode;
import org.apache.commons.jexl3.parser.ASTSetSubNode;
import org.apache.commons.jexl3.parser.ASTSetShiftLeftNode;
import org.apache.commons.jexl3.parser.ASTSetShiftRightNode;
import org.apache.commons.jexl3.parser.ASTSetShiftRightUnsignedNode;
import org.apache.commons.jexl3.parser.ASTSetXorNode;
import org.apache.commons.jexl3.parser.ASTShiftLeftNode;
import org.apache.commons.jexl3.parser.ASTShiftRightNode;
import org.apache.commons.jexl3.parser.ASTShiftRightUnsignedNode;
import org.apache.commons.jexl3.parser.ASTSimpleLambda;
import org.apache.commons.jexl3.parser.ASTSizeFunction;
import org.apache.commons.jexl3.parser.ASTStartCountNode;
import org.apache.commons.jexl3.parser.ASTStopCountNode;
import org.apache.commons.jexl3.parser.ASTStringLiteral;
import org.apache.commons.jexl3.parser.ASTStringBuilderLiteral;
import org.apache.commons.jexl3.parser.ASTSubNode;
import org.apache.commons.jexl3.parser.ASTSwitchCaseLabel;
import org.apache.commons.jexl3.parser.ASTSwitchExpression;
import org.apache.commons.jexl3.parser.ASTSwitchExpressionCase;
import org.apache.commons.jexl3.parser.ASTSwitchExpressionDefault;
import org.apache.commons.jexl3.parser.ASTSwitchStatement;
import org.apache.commons.jexl3.parser.ASTSwitchStatementCase;
import org.apache.commons.jexl3.parser.ASTSwitchStatementDefault;
import org.apache.commons.jexl3.parser.ASTSynchronizedStatement;
import org.apache.commons.jexl3.parser.ASTTernaryNode;
import org.apache.commons.jexl3.parser.ASTTextBlockLiteral;
import org.apache.commons.jexl3.parser.ASTThisNode;
import org.apache.commons.jexl3.parser.ASTThrowStatement;
import org.apache.commons.jexl3.parser.ASTTryStatement;
import org.apache.commons.jexl3.parser.ASTTryVar;
import org.apache.commons.jexl3.parser.ASTTryWithResourceStatement;
import org.apache.commons.jexl3.parser.ASTTryResource;
import org.apache.commons.jexl3.parser.ASTTypeLiteral;
import org.apache.commons.jexl3.parser.ASTUnaryMinusNode;
import org.apache.commons.jexl3.parser.ASTUnaryPlusNode;
import org.apache.commons.jexl3.parser.ASTUnderscoreLiteral;
import org.apache.commons.jexl3.parser.ASTVar;
import org.apache.commons.jexl3.parser.ASTVarStatement;
import org.apache.commons.jexl3.parser.ASTWhileStatement;
import org.apache.commons.jexl3.parser.ASTYieldStatement;
import org.apache.commons.jexl3.parser.JexlNode;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.AbstractMap;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Predicate;

import java.lang.reflect.Array;
/**
 * An interpreter of JEXL syntax.
 *
 * @since 2.0
 */
public class Interpreter extends InterpreterBase {
    /** Frame height. */
    protected int fp;
    /** Symbol values. */
    protected final Frame frame;
    /** Block micro-frames. */
    protected LexicalFrame block;
    /** Current evaluation target. */
    protected Object current;

    /**
     * The thread local interpreter.
     */
    protected static final java.lang.ThreadLocal<Interpreter> INTER =
                       new java.lang.ThreadLocal<>();

    /**
     * Creates an interpreter.
     * @param engine   the engine creating this interpreter
     * @param opts     the evaluation options, flags modifying evaluation behavior
     * @param aContext the evaluation context, global variables, methods and functions
     * @param info     the script info
     * @param eFrame   the evaluation frame, arguments and local variables
     */
    protected Interpreter(final Engine engine, final JexlOptions opts, final JexlContext aContext, final JexlInfo info, final Frame eFrame) {
        this(engine, opts, aContext, info, eFrame, null);
    }

    /**
     * Creates an interpreter.
     * @param engine   the engine creating this interpreter
     * @param opts     the evaluation options, flags modifying evaluation behavior
     * @param aContext the evaluation context, global variables, methods and functions
     * @param info     the script info
     * @param eFrame   the evaluation frame, arguments and local variables
     * @param current  the current evaluation object
     */
    protected Interpreter(final Engine engine, final JexlOptions opts, final JexlContext aContext, final JexlInfo info, final Frame eFrame, 
                          final Object current) {
        super(engine, opts, aContext, info);
        this.frame = eFrame;
        this.current = current;
    }

    /**
     * Copy constructor.
     * @param ii  the interpreter to copy
     * @param jexla the arithmetic instance to use (or null)
     */
    protected Interpreter(final Interpreter ii, final JexlArithmetic jexla) {
        super(ii, jexla);
        frame = ii.frame;
        block = ii.block != null? new LexicalFrame(ii.block) : null;
    }

    /**
     * Swaps the current thread local interpreter.
     * @param inter the interpreter or null
     * @return the previous thread local interpreter
     */
    protected Interpreter putThreadInterpreter(final Interpreter inter) {
        final Interpreter pinter = INTER.get();
        INTER.set(inter);
        return pinter;
    }

    /**
     * Interpret the given script/expression.
     * <p>
     * If the underlying JEXL engine is silent, errors will be logged through
     * its logger as warning.
     * @param node the script or expression to interpret.
     * @return the result of the interpretation.
     * @throws JexlException if any error occurs during interpretation.
     */
    public Object interpret(final JexlNode node) {
        JexlContext.ThreadLocal tcontext = null;
        JexlEngine tjexl = null;
        Interpreter tinter = null;
        try {
            tinter = putThreadInterpreter(this);
            if (tinter != null) {
                fp = tinter.fp + 1;
            }
            if (context instanceof JexlContext.ThreadLocal) {
                tcontext = jexl.putThreadLocal((JexlContext.ThreadLocal) context);
            }
            tjexl = jexl.putThreadEngine(jexl);
            if (fp > jexl.stackOverflow) {
                throw new JexlException.StackOverflow(detailedInfo(node), "jexl (" + jexl.stackOverflow + ")", null);
            }
            cancelCheck(node);
            Object result = null;
            try {
                result = node.jjtAccept(this, null);
            } catch (JexlException.Return xreturn) {
                result = xreturn.getValue();
            } catch (JexlException.Yield xyield) {
                result = xyield.getValue();
            }
            // Check return type
            Scope s = frame != null ? frame.getScope() : null;
            Class type = s != null ? s.getReturnType() : null;
            if (type != null) {
                if (type == Void.TYPE) {
                    return null;
                }
                if (arithmetic.isStrict()) {
                    result = arithmetic.implicitCast(type, result);
                } else {
                    result = arithmetic.cast(type, result);
                }
                if (type.isPrimitive() && result == null) {
                    throw createException(node, "not null return value required");
                }
            }
            return arithmetic.controlReturn(result);
        } catch (final StackOverflowError xstack) {
            final JexlException xjexl = new JexlException.StackOverflow(detailedInfo(node), "jvm", xstack);
            if (!isSilent()) {
                throw xjexl.clean();
            }
            if (logger.isWarnEnabled()) {
                logger.warn(xjexl.getMessage(), xjexl.getCause());
            }
        } catch (final JexlException.Cancel xcancel) {
            // cancelled |= Thread.interrupted();
            cancelled.weakCompareAndSet(false, Thread.interrupted());
            if (isCancellable()) {
                throw xcancel.clean();
            }
        } catch (final JexlException xjexl) {
            if (!isSilent()) {
                throw xjexl.clean();
            }
            if (logger.isWarnEnabled()) {
                logger.warn(xjexl.getMessage(), xjexl.getCause());
            }
        } finally {
            // clean functors at top level
            if (fp == 0) {
                synchronized (this) {
                    if (functors != null) {
                        for (final Object functor : functors.values()) {
                            closeIfSupported(functor);
                        }
                        functors.clear();
                        functors = null;
                    }
                }
            }
            jexl.putThreadEngine(tjexl);
            if (context instanceof JexlContext.ThreadLocal) {
                jexl.putThreadLocal(tcontext);
            }
            if (tinter != null) {
                fp = tinter.fp - 1;
            }
            putThreadInterpreter(tinter);
        }
        return null;
    }

    /**
     * Gets an attribute of an object.
     *
     * @param object    to retrieve value from
     * @param attribute the attribute of the object, e.g. an index (1, 0, 2) or key for a map
     * @return the attribute value
     */
    public Object getAttribute(final Object object, final Object attribute) {
        return getAttribute(object, attribute, null);
    }

    /**
     * Sets an attribute of an object.
     *
     * @param object    to set the value to
     * @param attribute the attribute of the object, e.g. an index (1, 0, 2) or key for a map
     * @param value     the value to assign to the object's attribute
     */
    public void setAttribute(final Object object, final Object attribute, final Object value) {
        setAttribute(object, attribute, value, null, JexlOperator.PROPERTY_SET);
    }

    @Override
    protected Object visit(final ASTAddNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.ADD, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.add(left, right);
        } catch (final ArithmeticException xrt) {
            throw createException(findNullOperand(node, left, right), "+ error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTSubNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.SUBTRACT, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.subtract(left, right);
        } catch (final ArithmeticException xrt) {
            throw createException(findNullOperand(node, left, right), "- error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTMulNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.MULTIPLY, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.multiply(left, right);
        } catch (final ArithmeticException xrt) {
            throw createException(findNullOperand(node, left, right), "* error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTDivNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.DIVIDE, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.divide(left, right);
        } catch (final ArithmeticException xrt) {
            if (!arithmetic.isStrict()) {
                return 0.0d;
            }
            throw createException(findNullOperand(node, left, right), "/ error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTModNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.MOD, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.mod(left, right);
        } catch (final ArithmeticException xrt) {
            if (!arithmetic.isStrict()) {
                return 0.0d;
            }
            throw createException(findNullOperand(node, left, right), "% error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTShiftLeftNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.SHIFTLEFT, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.shiftLeft(left, right);
        } catch (final ArithmeticException xrt) {
            throw createException(findNullOperand(node, left, right), "<< error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTShiftRightNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.SHIFTRIGHT, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.shiftRight(left, right);
        } catch (final ArithmeticException xrt) {
            throw createException(findNullOperand(node, left, right), ">> error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTShiftRightUnsignedNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.SHIFTRIGHTU, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.shiftRightUnsigned(left, right);
        } catch (final ArithmeticException xrt) {
            throw createException(findNullOperand(node, left, right), ">> error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTBitwiseAndNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.AND, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.and(left, right);
        } catch (final ArithmeticException xrt) {
            throw createException(findNullOperand(node, left, right), "& error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTBitwiseOrNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.OR, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.or(left, right);
        } catch (final ArithmeticException xrt) {
            throw createException(findNullOperand(node, left, right), "| error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTBitwiseDiffNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.DIFF, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.diff(left, right);
        } catch (final ArithmeticException xrt) {
            throw createException(findNullOperand(node, left, right), "\\ error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTBitwiseXorNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.XOR, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.xor(left, right);
        } catch (final ArithmeticException xrt) {
            throw createException(findNullOperand(node, left, right), "^ error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTISNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return left == right;
    }

    @Override
    protected Object visit(final ASTNINode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return left != right;
    }

    /**
     * Checks relational set operand.
     * @param node        the node
     * @param operand     the set operand to check
     * @param left        the value to check operand against
     * @param data        the data
     * @param p           the predicate to check
     * @return the result
     */
    protected boolean checkSetOperand(JexlNode node, ASTSetOperand operand, Object left, Object data, Predicate<Object> p) {
        boolean any = operand.isAny();
        int numChildren = operand.jjtGetNumChildren();
        for (int i = 0; i < numChildren; i++) {
            cancelCheck(node);
            JexlNode child = operand.jjtGetChild(i);
            if (child instanceof ASTEnumerationNode || child instanceof ASTEnumerationReference) {
                Iterator<?> it = (Iterator<?>) child.jjtAccept(this, data);
                if (it != null) {
                    try {
                        while (it.hasNext()) {
                            Object right = it.next();
                            Boolean ok = p.test(right);
                            if (ok && any || !ok && !any) {
                                return ok;
                            }
                        }
                    } finally {
                        closeIfSupported(it);
                    }
                }
            } else {
                Object right = child.jjtAccept(this, data);
                Boolean ok = p.test(right);
                if (ok && any || !ok && !any) {
                    return ok;
                }
            }
        }
        return !any;
    }

    @Override
    protected Object visit(final ASTEQNode node, final Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        JexlNode operand = node.jjtGetChild(1);
        if (operand instanceof ASTSetOperand) {
            return checkSetOperand(node, (ASTSetOperand) operand, left, data, (right) -> {
                return operators.equals(node, JexlOperator.EQ, left, right);
            });
        } else {
            Object right = operand.jjtAccept(this, data);
            return operators.equals(node, JexlOperator.EQ, left, right);
        }
    }

    @Override
    protected Object visit(final ASTEQPredicate node, final Object data) {
        final JexlNode right = node.jjtGetChild(0);
        if (right instanceof ASTSetOperand) {
            ASTSetOperand operand = (ASTSetOperand) right;
            return createPredicate(JexlOperator.EQ, node, false, operand.isAny(), (Object[]) right.jjtAccept(this, data));
        } else {
            return createPredicate(JexlOperator.EQ, node, false, true, right.jjtAccept(this, data));
        }
    }

    @Override
    protected Object visit(final ASTNENode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        JexlNode operand = node.jjtGetChild(1);
        if (operand instanceof ASTSetOperand) {
            return checkSetOperand(node, (ASTSetOperand) operand, left, data, (right) -> {
                return !operators.equals(node, JexlOperator.NE, left, right);
            });
        } else {
            Object right = operand.jjtAccept(this, data);
            return !operators.equals(node, JexlOperator.NE, left, right);
        }
    }

    @Override
    protected Object visit(final ASTNEPredicate node, final Object data) {
        final JexlNode right = node.jjtGetChild(0);
        if (right instanceof ASTSetOperand) {
            ASTSetOperand operand = (ASTSetOperand) right;
            return createPredicate(JexlOperator.EQ, node, true, operand.isAny(), (Object[]) right.jjtAccept(this, data));
        } else {
            return createPredicate(JexlOperator.EQ, node, true, true, right.jjtAccept(this, data));
        }
    }

    @Override
    protected Object visit(final ASTGENode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        JexlNode operand = node.jjtGetChild(1);
        if (operand instanceof ASTSetOperand) {
            return checkSetOperand(node, (ASTSetOperand) operand, left, data, (right) -> {
                return operators.greaterThanOrEqual(node, left, right);
            });
        } else {
            Object right = operand.jjtAccept(this, data);
            return operators.greaterThanOrEqual(node, left, right);
        }
    }

    @Override
    protected Object visit(final ASTGEPredicate node, final Object data) {
        final JexlNode right = node.jjtGetChild(0);
        if (right instanceof ASTSetOperand) {
            ASTSetOperand operand = (ASTSetOperand) right;
            return createPredicate(JexlOperator.GTE, node, false, operand.isAny(), (Object[]) right.jjtAccept(this, data));
        } else {
            return createPredicate(JexlOperator.GTE, node, false, true, right.jjtAccept(this, data));
        }
    }

    @Override
    protected Object visit(final ASTGTNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        JexlNode operand = node.jjtGetChild(1);
        if (operand instanceof ASTSetOperand) {
            return checkSetOperand(node, (ASTSetOperand) operand, left, data, (right) -> {
                return operators.greaterThan(node, left, right);
            });
        } else {
            Object right = operand.jjtAccept(this, data);
            return operators.greaterThan(node, left, right);
        }
    }

    @Override
    protected Object visit(final ASTGTPredicate node, final Object data) {
        final JexlNode right = node.jjtGetChild(0);
        if (right instanceof ASTSetOperand) {
            ASTSetOperand operand = (ASTSetOperand) right;
            return createPredicate(JexlOperator.GT, node, false, operand.isAny(), (Object[]) right.jjtAccept(this, data));
        } else {
            return createPredicate(JexlOperator.GT, node, false, true, right.jjtAccept(this, data));
        }
    }

    @Override
    protected Object visit(final ASTLENode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        JexlNode operand = node.jjtGetChild(1);
        if (operand instanceof ASTSetOperand) {
            return checkSetOperand(node, (ASTSetOperand) operand, left, data, (right) -> {
                return operators.lessThanOrEqual(node, left, right);
            });
        } else {
            Object right = operand.jjtAccept(this, data);
            return operators.lessThanOrEqual(node, left, right);
        }
    }

    @Override
    protected Object visit(final ASTLEPredicate node, final Object data) {
        final JexlNode right = node.jjtGetChild(0);
        if (right instanceof ASTSetOperand) {
            ASTSetOperand operand = (ASTSetOperand) right;
            return createPredicate(JexlOperator.LTE, node, false, operand.isAny(), (Object[]) right.jjtAccept(this, data));
        } else {
            return createPredicate(JexlOperator.LTE, node, false, true, right.jjtAccept(this, data));
        }
    }

    @Override
    protected Object visit(final ASTLTNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        JexlNode operand = node.jjtGetChild(1);
        if (operand instanceof ASTSetOperand) {
            return checkSetOperand(node, (ASTSetOperand) operand, left, data, (right) -> {
                return operators.lessThan(node, left, right);
            });
        } else {
            Object right = operand.jjtAccept(this, data);
            return operators.lessThan(node, left, right);
        }
    }

    @Override
    protected Object visit(final ASTLTPredicate node, final Object data) {
        final JexlNode right = node.jjtGetChild(0);
        if (right instanceof ASTSetOperand) {
            ASTSetOperand operand = (ASTSetOperand) right;
            return createPredicate(JexlOperator.LT, node, false, operand.isAny(), (Object[]) right.jjtAccept(this, data));
        } else {
            return createPredicate(JexlOperator.LT, node, false, true, right.jjtAccept(this, data));
        }
    }

    @Override
    protected Object visit(final ASTSWNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        JexlNode operand = node.jjtGetChild(1);
        if (operand instanceof ASTSetOperand) {
            return checkSetOperand(node, (ASTSetOperand) operand, left, data, (right) -> {
                return operators.startsWith(node, JexlOperator.STARTSWITH, left, right);
            });
        } else {
            Object right = operand.jjtAccept(this, data);
            return operators.startsWith(node, JexlOperator.STARTSWITH, left, right);
        }
    }

    @Override
    protected Object visit(final ASTSWPredicate node, final Object data) {
        final JexlNode right = node.jjtGetChild(0);
        if (right instanceof ASTSetOperand) {
            ASTSetOperand operand = (ASTSetOperand) right;
            return createPredicate(JexlOperator.STARTSWITH, node, false, operand.isAny(), (Object[]) right.jjtAccept(this, data));
        } else {
            return createPredicate(JexlOperator.STARTSWITH, node, false, true, right.jjtAccept(this, data));
        }
    }

    @Override
    protected Object visit(final ASTNSWNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        JexlNode operand = node.jjtGetChild(1);
        if (operand instanceof ASTSetOperand) {
            return checkSetOperand(node, (ASTSetOperand) operand, left, data, (right) -> {
                return operators.startsWith(node, JexlOperator.NOT_STARTSWITH, left, right);
            });
        } else {
            Object right = operand.jjtAccept(this, data);
            return operators.startsWith(node, JexlOperator.NOT_STARTSWITH, left, right);
        }
    }

    @Override
    protected Object visit(final ASTNSWPredicate node, final Object data) {
        final JexlNode right = node.jjtGetChild(0);
        if (right instanceof ASTSetOperand) {
            ASTSetOperand operand = (ASTSetOperand) right;
            return createPredicate(JexlOperator.STARTSWITH, node, true, operand.isAny(), (Object[]) right.jjtAccept(this, data));
        } else {
            return createPredicate(JexlOperator.STARTSWITH, node, true, true, right.jjtAccept(this, data));
        }
    }

    @Override
    protected Object visit(final ASTEWNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        JexlNode operand = node.jjtGetChild(1);
        if (operand instanceof ASTSetOperand) {
            return checkSetOperand(node, (ASTSetOperand) operand, left, data, (right) -> {
                return operators.endsWith(node, JexlOperator.ENDSWITH, left, right);
            });
        } else {
            Object right = operand.jjtAccept(this, data);
            return operators.endsWith(node, JexlOperator.ENDSWITH, left, right);
        }
    }

    @Override
    protected Object visit(final ASTEWPredicate node, final Object data) {
        final JexlNode right = node.jjtGetChild(0);
        if (right instanceof ASTSetOperand) {
            ASTSetOperand operand = (ASTSetOperand) right;
            return createPredicate(JexlOperator.ENDSWITH, node, false, operand.isAny(), (Object[]) right.jjtAccept(this, data));
        } else {
            return createPredicate(JexlOperator.ENDSWITH, node, false, true, right.jjtAccept(this, data));
        }
    }

    @Override
    protected Object visit(final ASTNEWNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        JexlNode operand = node.jjtGetChild(1);
        if (operand instanceof ASTSetOperand) {
            return checkSetOperand(node, (ASTSetOperand) operand, left, data, (right) -> {
                return operators.endsWith(node, JexlOperator.NOT_ENDSWITH, left, right);
            });
        } else {
            Object right = operand.jjtAccept(this, data);
            return operators.endsWith(node, JexlOperator.NOT_ENDSWITH, left, right);
        }
    }

    @Override
    protected Object visit(final ASTNEWPredicate node, final Object data) {
        final JexlNode right = node.jjtGetChild(0);
        if (right instanceof ASTSetOperand) {
            ASTSetOperand operand = (ASTSetOperand) right;
            return createPredicate(JexlOperator.ENDSWITH, node, true, operand.isAny(), (Object[]) right.jjtAccept(this, data));
        } else {
            return createPredicate(JexlOperator.ENDSWITH, node, true, true, right.jjtAccept(this, data));
        }
    }

    @Override
    protected Object visit(final ASTERNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        JexlNode operand = node.jjtGetChild(1);
        if (operand instanceof ASTSetOperand) {
            return checkSetOperand(node, (ASTSetOperand) operand, left, data, (right) -> {
                return operators.contains(node, JexlOperator.CONTAINS, right, left);
            });
        } else {
            final Object right = operand.jjtAccept(this, data);
            // note the arguments inversion between 'in'/'matches' and 'contains'
            // if x in y then y contains x
            return operators.contains(node, JexlOperator.CONTAINS, right, left);
        }
    }

    @Override
    protected Object visit(final ASTERPredicate node, final Object data) {
        final JexlNode right = node.jjtGetChild(0);
        if (right instanceof ASTSetOperand) {
            ASTSetOperand operand = (ASTSetOperand) right;
            return createPredicate(JexlOperator.CONTAINS, node, false, operand.isAny(), (Object[]) right.jjtAccept(this, data));
        } else {
            return createPredicate(JexlOperator.CONTAINS, node, false, true, right.jjtAccept(this, data));
        }
    }

    @Override
    protected Object visit(final ASTNRNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        JexlNode operand = node.jjtGetChild(1);
        if (operand instanceof ASTSetOperand) {
            return checkSetOperand(node, (ASTSetOperand) operand, left, data, (right) -> {
                return operators.contains(node, JexlOperator.NOT_CONTAINS, right, left);
            });
        } else {
            final Object right = operand.jjtAccept(this, data);
            // note the arguments inversion between (not) 'in'/'matches' and  (not) 'contains'
            // if x not-in y then y not-contains x
            return operators.contains(node, JexlOperator.NOT_CONTAINS, right, left);
        }
    }

    @Override
    protected Object visit(final ASTNRPredicate node, final Object data) {
        final JexlNode right = node.jjtGetChild(0);
        if (right instanceof ASTSetOperand) {
            ASTSetOperand operand = (ASTSetOperand) right;
            return createPredicate(JexlOperator.CONTAINS, node, true, operand.isAny(), (Object[]) right.jjtAccept(this, data));
        } else {
            return createPredicate(JexlOperator.CONTAINS, node, true, true, right.jjtAccept(this, data));
        }
    }

    @Override
    protected Object visit(final ASTIOFNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        if (left != null) {
            Class k = left.getClass();
            ASTTypeLiteral right = (ASTTypeLiteral) node.jjtGetChild(1);
            Class type = right.getLiteral();
            int i = right.getArray();
            while (i-- > 0) {
                if (k.isArray()) {
                    k = k.getComponentType();
                } else {
                    return Boolean.FALSE;
                }
            }
            return type == null ? Boolean.TRUE : type.isAssignableFrom(k);
        }
        return Boolean.FALSE;
    }

    @Override
    protected Object visit(final ASTNIOFNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        if (left != null) {
            Class k = left.getClass();
            ASTTypeLiteral right = (ASTTypeLiteral) node.jjtGetChild(1);
            Class type = right.getLiteral();
            int i = right.getArray();
            while (i-- > 0) {
                if (k.isArray()) {
                    k = k.getComponentType();
                } else {
                    return Boolean.TRUE;
                }
            }
            return type == null ? Boolean.FALSE : !type.isAssignableFrom(k);
        }
        return Boolean.TRUE;
    }

    @Override
    protected Object visit(final ASTRangeNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.createRange(left, right);
        } catch (final ArithmeticException xrt) {
            throw createException(findNullOperand(node, left, right), ".. error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTUnaryMinusNode node, final Object data) {
        // use cached value if literal
        final Object value = node.jjtGetValue();
        if (value instanceof Number) {
            return value;
        }
        final JexlNode valNode = node.jjtGetChild(0);
        final Object val = valNode.jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.NEGATE, val);
            if (result != JexlEngine.TRY_FAILED) {
                return result;
            }
            Object number = arithmetic.negate(val);
            // attempt to recoerce to literal class
            // cache if number literal and negate is idempotent
            if (number instanceof Number && valNode instanceof ASTNumberLiteral) {
                number = arithmetic.narrowNumber((Number) number, ((ASTNumberLiteral) valNode).getLiteralClass());
                if (arithmetic.isNegateStable()) {
                    node.jjtSetValue(number);
                }
            }
            return number;
        } catch (final ArithmeticException xrt) {
            throw createException(valNode, "- error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTUnaryPlusNode node, final Object data) {
        // use cached value if literal
        final Object value = node.jjtGetValue();
        if (value instanceof Number) {
            return value;
        }
        final JexlNode valNode = node.jjtGetChild(0);
        final Object val = valNode.jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.POSITIVIZE, val);
            if (result != JexlEngine.TRY_FAILED) {
                return result;
            }
            final Object number = arithmetic.positivize(val);
            if (valNode instanceof ASTNumberLiteral
                && number instanceof Number
                && arithmetic.isPositivizeStable()) {
                node.jjtSetValue(number);
            }
            return number;
        } catch (final ArithmeticException xrt) {
            throw createException(valNode, "- error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTIndirectNode node, final Object data) {
        final Object val = node.jjtGetChild(0).jjtAccept(this, data);
        if (val == null) {
            if (isStrictEngine()) {
                throw createException(node, "Null dereference", null);
            } else {
                return null;
            }
        }
        if (val instanceof GetPointer) {
            return ((GetPointer) val).get();
        }

        return operators.indirect(node, val);
    }

    /**
     * Declares pointer dereference operator
     */
    public interface GetPointer {
        public Object get();
    }

    /**
     * Declares pointer dereference assignment operator
     */
    public interface SetPointer {
        public void set(Object right);
    }

    /**
     * Pointer to a final local variable.
     *
     */
    public class FinalVarPointer implements GetPointer {

        protected ASTIdentifier node;

        protected FinalVarPointer(ASTIdentifier node) {
            this.node = node;
        }

        @Override
        public Object get() {
            return frame.get(node.getSymbol());
        }
    }

    /**
     * Pointer to a local variable.
     *
     */
    public class VarPointer extends FinalVarPointer implements SetPointer {

        protected VarPointer(ASTIdentifier node) {
            super(node);
        }

        @Override
        public void set(Object value) {
            executeAssign(node, node, value, null, null);
        }
    }

    /**
     * Pointer to a context variable.
     *
     */
    public class ContextVarPointer implements GetPointer, SetPointer {

        protected String name;

        protected ContextVarPointer(String name) {
            this.name = name;
        }

        @Override
        public Object get() {
            return context.get(name);
        }

        @Override
        public void set(Object value) {
            context.set(name, value);
        }
    }

    /**
     * Pointer to a bean property.
     *
     */
    public class PropertyPointer implements GetPointer, SetPointer {

        protected JexlNode propertyNode;
        protected Object object;
        protected String property;

        protected PropertyPointer(JexlNode node, Object object, String property) {
            this.propertyNode = node;
            this.object = object;
            this.property = property;
        }

        @Override
        public Object get() {
            return getAttribute(object, property, propertyNode);
        }

        @Override
        public void set(Object value) {
            setAttribute(object, property, value, propertyNode, JexlOperator.PROPERTY_SET);
        }
    }

    /**
     * Pointer to an indexed element.
     *
     */
    public class ArrayPointer implements GetPointer, SetPointer {

        protected JexlNode propertyNode;
        protected Object object;
        protected Object index;

        protected ArrayPointer(JexlNode node, Object object, Object index) {
            this.propertyNode = node;
            this.object = object;
            this.index = index;
        }

        @Override
        public Object get() {
            return getAttribute(object, index, propertyNode);
        }

        @Override
        public void set(Object value) {
            setAttribute(object, index, value, propertyNode, JexlOperator.ARRAY_SET);
        }
    }

    @Override
    protected Object visit(final ASTPointerNode node, final Object data) {
        JexlNode left = node.jjtGetChild(0);
        if (left instanceof ASTIdentifier) {
            ASTIdentifier var = (ASTIdentifier) left;
            if (data != null) {
                return new PropertyPointer(var, data, var.getName());
            } else {
                int symbol = var.getSymbol();
                if (symbol >= 0) {
                    return var.isConstant() ? new FinalVarPointer(var) : new VarPointer(var);
                } else {
                    return new ContextVarPointer(var.getName());
                }
            }
        } else {
            Object object = data;
            int last = left.jjtGetNumChildren() - 1;
            boolean antish = true;
            // 1: follow children till penultimate, resolve dot/array
            JexlNode objectNode = null;
            StringBuilder ant = null;
            int v = 1;
            // start at 1 if symbol
            for (int c = 0; c < last; ++c) {
                objectNode = left.jjtGetChild(c);
                object = objectNode.jjtAccept(this, object);
                if (object != null) {
                    // disallow mixing antish variable & bean with same root; avoid ambiguity
                    antish = false;
                } else if (antish) {
                    if (ant == null) {
                        JexlNode first = left.jjtGetChild(0);
                        if (first instanceof ASTIdentifier && ((ASTIdentifier) first).getSymbol() < 0) {
                            ant = new StringBuilder(((ASTIdentifier) first).getName());
                        } else {
                            break;
                        }
                    }
                    for (; v <= c; ++v) {
                        JexlNode child = left.jjtGetChild(v);
                        if (child instanceof ASTIdentifierAccess) {
                            ant.append('.');
                            ant.append(((ASTIdentifierAccess) objectNode).getName());
                        } else {
                            break;
                        }
                    }
                    object = context.get(ant.toString());
                } else {
                    throw createException(objectNode, "illegal address");
                }
            }
            // 2: last objectNode will perform assignement in all cases
            JexlNode propertyNode = left.jjtGetChild(last);
            if (propertyNode instanceof ASTIdentifierAccess) {
                String property = String.valueOf(evalIdentifier((ASTIdentifierAccess) propertyNode));
                if (object == null) {
                    // deal with antish variable
                    if (ant != null) {
                        if (last > 0) {
                            ant.append('.');
                        }
                        ant.append(property);
                        return new ContextVarPointer(ant.toString());
                    } else {
                        return new ContextVarPointer(property);
                    }
                }
                return new PropertyPointer(propertyNode, object, property);
            } else if (propertyNode instanceof ASTArrayAccess) {
                // can have multiple nodes - either an expression, integer literal or reference
                int numChildren = propertyNode.jjtGetNumChildren() - 1;
                for (int i = 0; i < numChildren; i++) {
                    JexlNode nindex = propertyNode.jjtGetChild(i);
                    Object index = nindex.jjtAccept(this, null);
                    object = getAttribute(object, index, nindex);
                }
                propertyNode = propertyNode.jjtGetChild(numChildren);
                Object property = propertyNode.jjtAccept(this, null);
                return new ArrayPointer(propertyNode, object, property);
            } else {
                throw createException(objectNode, "illegal pointer form");
            }
        }
    }

    @Override
    protected Object visit(final ASTBitwiseComplNode node, final Object data) {
        final Object arg = node.jjtGetChild(0).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.COMPLEMENT, arg);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.complement(arg);
        } catch (ArithmeticException xrt) {
            throw createException(node, "~ error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTNotNode node, final Object data) {
        final Object val = node.jjtGetChild(0).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.NOT, val);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.not(val);
        } catch (ArithmeticException xrt) {
            throw createException(node, "! error", xrt);
        }
    }

    private boolean testPredicate(JexlNode node, Object condition) {
        final Object predicate = operators.tryOverload(node, JexlOperator.CONDITION, condition);
        return  arithmetic.testPredicate(predicate != JexlEngine.TRY_FAILED? predicate : condition);
    }

    @Override
    protected Object visit(final ASTCastNode node, final Object data) {
        // Type
        ASTTypeLiteral type = (ASTTypeLiteral) node.jjtGetChild(0);
        Class c = type.getType();
        // Value
        Object val = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.cast(c, val);
        } catch (ArithmeticException xrt) {
            throw createException(node, "cast error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTEnumerationReference node, final Object data) {
        cancelCheck(node);
        final int numChildren = node.jjtGetNumChildren();
        // pass first piece of data in and loop through children
        Object object = data;
        JexlNode objectNode = null;
        for (int c = 0; c < numChildren; c++) {
            objectNode = node.jjtGetChild(c);
            // attempt to evaluate the property within the object)
            object = objectNode.jjtAccept(this, object);
            cancelCheck(node);
        }
        return object;
    }

    @Override
    protected Object visit(final ASTEnumerationNode node, final Object data) {
        JexlNode valNode = node.jjtGetChild(0);
        if (valNode instanceof ASTSimpleLambda) {
            ASTJexlLambda generator = (ASTJexlLambda) valNode;
            return new GeneratorIterator(generator);
        } else {
            Object iterableValue = valNode.jjtAccept(this, data);
            if (iterableValue != null) {
                Object forEach = operators.tryOverload(node, JexlOperator.FOR_EACH_INDEXED, iterableValue);
                Iterator<?> itemsIterator = forEach instanceof Iterator ? 
                    (Iterator<?>) forEach : 
                    uberspect.getIndexedIterator(iterableValue);
                return itemsIterator;
            } else {
                return null;
            }
        }
    }

    public class GeneratorIterator implements Iterator<Object> {

        protected final JexlNode node;
        protected final Interpreter generator;

        protected int i;
        protected boolean nextValue;
        protected Object value;

        protected GeneratorIterator(ASTJexlLambda script) {
            // Execution block is the first child
            this.node = script.jjtGetChild(0);
            Frame scope = script.createFrame(frame);
            generator = jexl.createResumableInterpreter(context, scope, options, info);
            i = -1;
        }

        protected void prepareNextValue() {
            try {
                i += 1;
                generator.interpret(node);
                nextValue = false;
            } catch (JexlException.Yield ex) {
                value = ex.getValue();
                nextValue = true;
            } catch (JexlException.Cancel xcancel) {
                nextValue = false;
                // cancelled |= Thread.interrupted();
                cancelled.weakCompareAndSet(false, Thread.interrupted());
                if (isCancellable()) {
                    throw xcancel.clean();
                }
            } catch (JexlException xjexl) {
                nextValue = false;
                if (!isSilent()) {
                    throw xjexl.clean();
                }
            }
        }

        @Override
        public boolean hasNext() {
            if (i == -1) {
                prepareNextValue();
            }
            return nextValue;
        }

        @Override
        public Object next() {
            cancelCheck(node);
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Object result = value;
            prepareNextValue();
            return result;
        }

        @Override
        public void remove() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    protected Object visit(final ASTExpressionStatement node, final Object data) {
        cancelCheck(node);
        // Try unknown identifier as a method
        JexlNode child = node.jjtGetChild(0);
        if (child instanceof ASTIdentifier) {
            ASTIdentifier identifier = (ASTIdentifier) child;
            String name = identifier.getName();
            int symbol = identifier.getSymbol();
            if (symbol < 0 && !context.has(name)) {
               JexlMethod vm = uberspect.getMethod(arithmetic, name, EMPTY_PARAMS);
               if (vm != null) {
                   try {
                      Object eval = vm.invoke(arithmetic, EMPTY_PARAMS);
                      if (cache && vm.isCacheable()) {
                          Funcall funcall = new ArithmeticFuncall(vm, false);
                          identifier.jjtSetValue(funcall);
                      }
                      return eval;
                   } catch (JexlException xthru) {
                       throw xthru;
                   } catch (Exception xany) {
                       throw invocationException(identifier, name, xany);
                   }
               }
            }
        }
        return child.jjtAccept(this, data);
    }

    @Override
    protected Object visit(final ASTFunctionStatement node, final Object data) {
        cancelCheck(node);
        // Declare variable
        JexlNode left = node.jjtGetChild(0);
        left.jjtAccept(this, data);
        // Create function
        Object right = Closure.create(this, (ASTJexlLambda) node.jjtGetChild(1));
        return executeAssign(node, left, right, null, data);
    }

    @Override
    protected Object visit(final ASTIfStatement node, final Object data) {
        cancelCheck(node);
        try {
            final JexlNode testNode = node.jjtGetChild(0);
            final Object condition = testNode.jjtAccept(this, null);
            if (testPredicate(testNode, condition)) {
                // first objectNode is true statement
                return node.jjtGetChild(1).jjtAccept(this, null);
            }
            final int numChildren = node.jjtGetNumChildren();
            if (numChildren > 2) {
                // if there is an else, execute it.
                return node.jjtGetChild(2).jjtAccept(this, null);
            }
            return null;
        } catch (JexlException.Break stmtBreak) {
            String target = stmtBreak.getLabel();
            if (target == null || !target.equals(node.getLabel())) {
                throw stmtBreak;
            }
            // break
            return null;
        } catch (ArithmeticException xrt) {
            throw createException(node.jjtGetChild(0), "if error", xrt);
        }
    }

    /**
     * Base visitation for blocks.
     * @param node the block
     * @param data the usual data
     * @return the result of the last expression evaluation
     */
    private Object visitBlock(final ASTBlock node, final Object data) {
        Object result = null;
        final int numChildren = node.jjtGetNumChildren();
        for (int i = 0; i < numChildren; i++) {
            cancelCheck(node);
            try {
                result = node.jjtGetChild(i).jjtAccept(this, data);
            } catch (JexlException.Break stmtBreak) {
                String target = stmtBreak.getLabel();
                if (target != null && target.equals(node.getLabel())) {
                    break;
                } else {
                    throw stmtBreak;
                }
            }
        }
        return result;
    }

    @Override
    protected Object visit(final ASTBlock node, final Object data) {
        final int cnt = node.getSymbolCount();
        if (cnt <= 0) {
            return visitBlock(node, data);
        }
        LexicalScope lexical = block;
        try {
            block = new LexicalFrame(frame, block);
            return visitBlock(node, data);
        } finally {
            block = block.pop();
        }
    }

    @Override
    protected Object visit(final ASTReturnStatement node, final Object data) {
        cancelCheck(node);
        final Object val = node.jjtGetNumChildren() == 1
            ? node.jjtGetChild(0).jjtAccept(this, data)
            : null;
        throw new JexlException.Return(detailedInfo(node), null, val);
    }

    @Override
    protected Object visit(final ASTYieldStatement node, final Object data) {
        cancelCheck(node);
        Object val = node.jjtGetChild(0).jjtAccept(this, data);
        throw new JexlException.Yield(detailedInfo(node), null, val);
    }

    @Override
    protected Object visit(final ASTContinue node, final Object data) {
        cancelCheck(node);
        throw new JexlException.Continue(detailedInfo(node), node.getLabel());
    }

    @Override
    protected Object visit(final ASTRemove node, final Object data) {
        cancelCheck(node);
        throw new JexlException.Remove(detailedInfo(node), node.getLabel());
    }

    @Override
    protected Object visit(final ASTBreak node, final Object data) {
        cancelCheck(node);
        throw new JexlException.Break(detailedInfo(node), node.getLabel());
    }

    @Override
    protected Object visit(final ASTDelete node, final Object data) {
        cancelCheck(node);

        // left contains the reference to remove from
        JexlNode left = node.jjtGetChild(0);

        Object object = null;

        // 0: determine initial object & property:
        final int last = left.jjtGetNumChildren() - 1;

        if (!(left instanceof ASTReference)) {
            throw createException(left, "illegal assignment form 0");
        }
        // 1: follow children till penultimate, resolve dot/array
        JexlNode objectNode = null;
        StringBuilder ant = null;
        int v = 1;
        // start at 1 if symbol
        main: for (int c = 0; c < last; ++c) {
            objectNode = left.jjtGetChild(c);
            object = objectNode.jjtAccept(this, object);
            if (object == null) {
                throw createException(objectNode, "illegal assignment form");
            }
        }
        // 2: last objectNode will perform removal in all cases
        JexlNode propertyNode = left.jjtGetChild(last);
        final Object property;

        if (propertyNode instanceof ASTIdentifierAccess) {
            final ASTIdentifierAccess propertyId = (ASTIdentifierAccess) propertyNode;
            // property of an object ?
            property = evalIdentifier(propertyId);
        } else if (propertyNode instanceof ASTArrayAccess) {
            // can have multiple nodes - either an expression, integer literal or reference
            final int numChildren = propertyNode.jjtGetNumChildren() - 1;
            for (int i = 0; i < numChildren; i++) {
                final JexlNode nindex = propertyNode.jjtGetChild(i);
                final Object index = nindex.jjtAccept(this, null);
                object = getAttribute(object, index, nindex);
            }
            propertyNode = propertyNode.jjtGetChild(numChildren);
            property = propertyNode.jjtAccept(this, null);
        } else {
            throw createException(objectNode, "illegal assignment form");
        }
        // we may have a null property as in map[null], no check needed.
        // we can not *have* a null object though.
        if (object == null) {
            // no object, we fail
            return unsolvableProperty(objectNode, "<null>.<?>", true, null);
        }

        final JexlOperator operator = propertyNode != null && propertyNode.jjtGetParent() instanceof ASTArrayAccess
                                      ? JexlOperator.ARRAY_DELETE : JexlOperator.PROPERTY_DELETE;
        deleteAttribute(object, property, propertyNode, operator);
        return null; // 4

    }

    @Override
    protected Object visit(final ASTForStatement node, final Object data) {
        cancelCheck(node);
        final boolean lexical = options.isLexical();
        if (lexical) {
              // create lexical frame
              block = new LexicalFrame(frame, block);
        }
        try {
            final int numChildren = node.jjtGetNumChildren();
            // Initialize for-loop
            node.jjtGetChild(0).jjtAccept(this, data);
            /* third objectNode is the statement to execute */
            JexlNode statement = node.jjtGetNumChildren() > 3 ? node.jjtGetChild(3) : null;
            JexlNode condition = node.jjtGetChild(1); 
            while (testPredicate(condition, condition.jjtAccept(this, data))) {
                cancelCheck(node);
                // Execute loop body
                if (statement != null) {
                    try {
                        statement.jjtAccept(this, data);
                    } catch (JexlException.Break stmtBreak) {
                        String target = stmtBreak.getLabel();
                        if (target == null || target.equals(node.getLabel())) {
                            break;
                        } else {
                            throw stmtBreak;
                        }
                    } catch (JexlException.Continue stmtContinue) {
                        String target = stmtContinue.getLabel();
                        if (target != null && !target.equals(node.getLabel())) {
                            throw stmtContinue;
                        }
                        // continue;
                    }
                }
                // for-increment node
                node.jjtGetChild(2).jjtAccept(this, data);
            }
            // undefined result
            return null;
        } finally {
            // restore lexical frame
            if (lexical) {
                block = block.pop();
            }
        }
    }

    @Override
    protected Object visit(final ASTForInitializationNode node, final Object data) {
        Object result = null;
        final int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            cancelCheck(node);
            result = node.jjtGetChild(i).jjtAccept(this, data);
        }
        return result;
    }

    @Override
    protected Object visit(final ASTForTerminationNode node, final Object data) {
        if (node.jjtGetNumChildren() == 0) {
            return Boolean.TRUE;
        } else {
            return arithmetic.toBoolean(node.jjtGetChild(0).jjtAccept(this, data));
        }
    }

    @Override
    protected Object visit(final ASTForIncrementNode node, final Object data) {
        Object result = null;
        final int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            cancelCheck(node);
            result = node.jjtGetChild(i).jjtAccept(this, data);
        }
        return result;
    }

    @Override
    protected Object visit(final ASTForeachStatement node, final Object data) {
        cancelCheck(node);
        Object result = null;
        /* first objectNode is the loop variable */
        ASTForeachVar loopReference = (ASTForeachVar) node.jjtGetChild(0);
        ASTIdentifier loopVariable = (ASTIdentifier) loopReference.jjtGetChild(0);
        ASTIdentifier loopValueVariable = loopReference.jjtGetNumChildren() > 1 ? 
            (ASTIdentifier) loopReference.jjtGetChild(1) : 
            null;
        final boolean lexical = loopVariable.isLexical() || options.isLexical();
        if (lexical) {
            // create lexical frame
            final LexicalFrame locals = new LexicalFrame(frame, block);
            final int symbol = loopVariable.getSymbol();
            final boolean loopSymbol = symbol >= 0 && loopVariable instanceof ASTVar;
            if (loopSymbol && !defineVariable((ASTVar) loopVariable, locals)) {
                return redefinedVariable(node, loopVariable.getName());
            }
            if (loopValueVariable != null) {
                final int valueSymbol = loopValueVariable.getSymbol();
                if (loopSymbol && !defineVariable((ASTVar) loopValueVariable, locals)) {
                    return redefinedVariable(node, loopValueVariable.getName());
                }
            }
            block = locals;
        }

        try {
            /* second objectNode is the variable to iterate */
            final Object iterableValue = node.jjtGetChild(1).jjtAccept(this, data);
            Iterator<?> itemsIterator = null;
            // make sure there is a value to iterate on
            if (iterableValue != null) {
                if (loopValueVariable != null) {
                    // get an iterator for the collection/array etc via the introspector.
                    Object forEach = operators.tryOverload(node, JexlOperator.FOR_EACH_INDEXED, iterableValue);
                    itemsIterator = forEach instanceof Iterator ? 
                         (Iterator<?>) forEach : 
                         uberspect.getIndexedIterator(iterableValue);
                } else {
                    // get an iterator for the collection/array etc via the introspector.
                    Object forEach = operators.tryOverload(node, JexlOperator.FOR_EACH, iterableValue);
                    itemsIterator = forEach instanceof Iterator ? 
                         (Iterator<?>) forEach : 
                         uberspect.getIterator(iterableValue);
                }
            }
            if (itemsIterator != null) {
                int i = -1;
                try {
                    /* third objectNode is the statement to execute */
                    final JexlNode statement = node.jjtGetNumChildren() >= 3 ? node.jjtGetChild(2) : null;
                    while (itemsIterator.hasNext()) {
                        cancelCheck(node);
                        i += 1;
                        // set loopVariable to value of iterator
                        final Object value = itemsIterator.next();
                        if (loopValueVariable != null) {
                            if (value instanceof Map.Entry<?,?>) {
                                Map.Entry<?,?> entry = (Map.Entry<?,?>) value;
                                executeAssign(node, loopVariable, entry.getKey(), null, data);
                                executeAssign(node, loopValueVariable, entry.getValue(), null, data);
                            } else {
                                executeAssign(node, loopVariable, i, null, data);
                                executeAssign(node, loopValueVariable, value, null, data);
                            }
                        } else {
                            executeAssign(node, loopVariable, value, null, data);
                        }
                        if (statement != null) {
                            try {
                                // execute statement
                                result = statement.jjtAccept(this, data);
                            } catch (final JexlException.Break stmtBreak) {
                                String target = stmtBreak.getLabel();
                                if (target == null || target.equals(node.getLabel())) {
                                    break;
                                } else {
                                    throw stmtBreak;
                                }
                            } catch (final JexlException.Continue stmtContinue) {
                                String target = stmtContinue.getLabel();
                                if (target != null && !target.equals(node.getLabel())) {
                                    throw stmtContinue;
                                }
                                // continue
                            } catch (final JexlException.Remove stmtRemove) {
                                String target = stmtRemove.getLabel();
                                if (target != null && !target.equals(node.getLabel())) {
                                    throw stmtRemove;
                                }
                                itemsIterator.remove();
                                i -= 1;
                                // and continue
                            }
                        }
                    }
                } finally {
                    // closeable iterator handling
                    closeIfSupported(itemsIterator);
                }
            }
        } finally {
            // restore lexical frame
            if (lexical) {
                block = block.pop();
            }
        }
        return result;
    }

    @Override
    protected Object visit(ASTForeachVar node, Object data) {
        return null;
    }

    @Override
    protected Object visit(ASTTryStatement node, Object data) {
        Object result = null;
        int num = node.jjtGetNumChildren();
        try {
            // execute try block
            result = node.jjtGetChild(0).jjtAccept(this, data);
        } catch (JexlException.Break stmtBreak) {
            String target = stmtBreak.getLabel();
            if (target == null || !target.equals(node.getLabel())) {
                throw stmtBreak;
            }
            // break
        } catch (JexlException.Continue e) {
            throw e;
        } catch (JexlException.Remove e) {
            throw e;
        } catch (JexlException.Return e) {
            throw e;
        } catch (JexlException.Yield e) {
            throw e;
        } catch(JexlException.Cancel e) {
            throw e;
        } catch (Throwable t) {
            boolean catched = false;
            Throwable ex = t;
            if (t instanceof JexlException && t.getCause() != null) {
                ex = t.getCause();
            }
            for (int i = 1; i < num; i++) {
                JexlNode cb = node.jjtGetChild(i);
                if (cb instanceof ASTCatchBlock) {
                    JexlNode catchVariable = cb.jjtGetNumChildren() > 1 ? (JexlNode) cb.jjtGetChild(0) : null;
                    if (catchVariable instanceof ASTMultiVar) {
                        List<Class> types = ((ASTMultiVar) catchVariable).getTypes();
                        for (Class type : types) {
                            if (type.isInstance(ex)) {
                                catched = true;
                                break;
                            }
                        }
                    } else if (catchVariable instanceof ASTVar) {
                        // Check exception catch type
                        Class type = ((ASTVar) catchVariable).getType();
                        if (type == null || type.isInstance(ex)) {
                            catched = true;
                        }
                    } else {
                        catched = true;
                    }
                }
                if (catched) {
                    cb.jjtAccept(this, ex);
                    break;
                }
            }
            // if there is no appropriate catch block just rethrow
            if (!catched) {
                throw t;
            }
        } finally {
            // execute finally block if any
            if (num > 1) {
                JexlNode fb = node.jjtGetChild(num - 1);
                if (!(fb instanceof ASTCatchBlock)) {
                    fb.jjtAccept(this, data);
                }
            }
        }
        return result;
    }

    @Override
    protected Object visit(ASTTryWithResourceStatement node, Object data) {
        cancelCheck(node);
        Object result = null;
        final int num = node.jjtGetNumChildren();
        try {
            ASTTryResource resReference = (ASTTryResource) node.jjtGetChild(0);
            // Last child is expression that returns the resource
            Object r = resReference.jjtGetChild(resReference.jjtGetNumChildren() - 1).jjtAccept(this, data);
            // get a resource manager for the resource via the introspector
            Object rman = operators.tryOverload(node, JexlOperator.TRY_WITH, r);
            if (JexlEngine.TRY_FAILED != rman) {
                r = rman;
            }

            ASTTryVar resDeclaration = resReference.jjtGetChild(0) instanceof ASTTryVar ? 
                (ASTTryVar) resReference.jjtGetChild(0) : 
                null;
            ASTIdentifier resVariable = resDeclaration != null ? (ASTIdentifier) resDeclaration.jjtGetChild(0) : null;
            final int symbol = resVariable != null ? resVariable.getSymbol() : -1;
            final boolean lexical = options.isLexical() && symbol >= 0;
            if (lexical) {
                // create lexical frame
                LexicalFrame locals = new LexicalFrame(frame, block);
                if (!defineVariable((ASTVar) resVariable, locals)) {
                    return redefinedVariable(node, resVariable.getName());
                }
                block = locals;
            }
            try {
                if (resReference.jjtGetNumChildren() == 2) {
                   // Set variable
                   resReference.jjtGetChild(0).jjtAccept(this, r);
                }
                try (ResourceManager rm = new ResourceManager(r)) {
                    // execute try block
                    JexlNode stmt = node.jjtGetChild(1);
                    result = stmt instanceof ASTInlinePropertyAssignment ? stmt.jjtAccept(this, r) : stmt.jjtAccept(this, data);
                }
            } finally {
                // restore lexical frame
                if (lexical) {
                    block = block.pop();
                }
            }
        } catch (JexlException.Break stmtBreak) {
            String target = stmtBreak.getLabel();
            if (target == null || !target.equals(node.getLabel())) {
                throw stmtBreak;
            }
            // break
        } catch (JexlException.Continue e) {
            throw e;
        } catch (JexlException.Remove e) {
            throw e;
        } catch (JexlException.Return e) {
            throw e;
        } catch (JexlException.Yield e) {
            throw e;
        } catch(JexlException.Cancel e) {
            throw e;
        } catch (Throwable t) {
            boolean catched = false;
            Throwable ex = t;
            if (t instanceof JexlException && t.getCause() != null) {
                ex = t.getCause();
            }
            for (int i = 2; i < num; i++) {
                JexlNode cb = node.jjtGetChild(i);
                if (cb instanceof ASTCatchBlock) {
                    JexlNode catchVariable = cb.jjtGetNumChildren() > 1 ? (JexlNode) cb.jjtGetChild(0) : null;
                    if (catchVariable instanceof ASTMultiVar) {
                        List<Class> types = ((ASTMultiVar) catchVariable).getTypes();
                        for (Class type : types) {
                            if (type.isInstance(ex)) {
                                catched = true;
                                break;
                            }
                        }
                    } else if (catchVariable instanceof ASTVar) {
                        // Check exception catch type
                        Class type = ((ASTVar) catchVariable).getType();
                        if (type == null || type.isInstance(ex)) {
                            catched = true;
                        }
                    } else {
                        catched = true;
                    }
                }
                if (catched) {
                    cb.jjtAccept(this, ex);
                    break;
                }
            }
            // if there is no appropriate catch block just rethrow
            if (!catched) {
                InterpreterBase.<RuntimeException>doThrow(t);
            }
        } finally {
            // execute finally block if any
            if (num > 2) {
                JexlNode fb = node.jjtGetChild(num - 1);
                if (!(fb instanceof ASTCatchBlock)) {
                    fb.jjtAccept(this, data);
                }
            }
        }
        return result;
    }

    @Override
    protected Object visit(ASTTryVar node, Object data) {
        ASTIdentifier variable = (ASTIdentifier) node.jjtGetChild(0);
        executeAssign(node, variable, data, null, null);
        return null;
    }

    @Override
    protected Object visit(ASTCatchBlock node, Object data) {
        int num = node.jjtGetNumChildren();
        ASTIdentifier catchVariable = num > 1 ? (ASTIdentifier) node.jjtGetChild(0) : null;
        if (catchVariable != null) {
            final int symbol = catchVariable.getSymbol();
            final boolean lexical = options.isLexical() && symbol >= 0;
            if (lexical) {
                // create lexical frame
                LexicalFrame locals = new LexicalFrame(frame, block);
                if (!defineVariable((ASTVar) catchVariable, locals)) {
                    return redefinedVariable(node, catchVariable.getName());
                }
                block = locals;
            }
            try {
                // Set catch variable
                executeAssign(node, catchVariable, data, null, null);

                // execute catch block
                node.jjtGetChild(1).jjtAccept(this, null);
            } finally {
                // restore lexical frame
                if (lexical) {
                    block = block.pop();
                }
            }
        } else {
            // just execute catch block
            node.jjtGetChild(0).jjtAccept(this, null);
        }
        return null;
    }

    @Override
    protected Object visit(ASTTryResource node, Object data) {
        return null;
    }

    @Override
    protected Object visit(ASTThrowStatement node, Object data) {
        cancelCheck(node);
        Object thrown = node.jjtGetChild(0).jjtAccept(this, data);
        if (thrown instanceof Throwable) {
            InterpreterBase.<RuntimeException>doThrow((Throwable) thrown);
        }
        throw new JexlException.Throw(detailedInfo(node), thrown);
    }

    @Override
    protected Object visit(ASTAssertStatement node, Object data) {
        if (isAssertions()) {
            cancelCheck(node);
            boolean test = arithmetic.toBoolean(node.jjtGetChild(0).jjtAccept(this, data));
            if (!test) {
                if (node.jjtGetNumChildren() > 1) {
                    Object val = node.jjtGetChild(1).jjtAccept(this, data);
                    throw new AssertionError(val);
                } else {
                    throw new AssertionError();
                }
            }
        }
        return null;
    }

    @Override
    protected Object visit(final ASTWhileStatement node, final Object data) {
        cancelCheck(node);
        Object result = null;
        /* first objectNode is the condition */
        final JexlNode condition = node.jjtGetChild(0);
        while (testPredicate(condition, condition.jjtAccept(this, data))) {
            cancelCheck(node);
            if (node.jjtGetNumChildren() > 1) {
                try {
                    // execute statement
                    result = node.jjtGetChild(1).jjtAccept(this, data);
                } catch (final JexlException.Break stmtBreak) {
                    String target = stmtBreak.getLabel();
                    if (target == null || target.equals(node.getLabel())) {
                        break;
                    } else {
                        throw stmtBreak;
                    }
                } catch (final JexlException.Continue stmtContinue) {
                    String target = stmtContinue.getLabel();
                    if (target != null && !target.equals(node.getLabel())) {
                        throw stmtContinue;
                    }
                    // continue
                }
            }
        }
        return result;
    }

    @Override
    protected Object visit(final ASTDoWhileStatement node, final Object data) {
        Object result = null;
        final int nc = node.jjtGetNumChildren();
        /* last objectNode is the condition */
        final JexlNode condition = node.jjtGetChild(nc - 1);
        do {
            cancelCheck(node);
            // execute statement
            if (node.jjtGetNumChildren() > 1) {
                try {
                    result = node.jjtGetChild(0).jjtAccept(this, data);
                } catch (final JexlException.Break stmtBreak) {
                    String target = stmtBreak.getLabel();
                    if (target == null || target.equals(node.getLabel())) {
                        break;
                    } else {
                        throw stmtBreak;
                    }
                } catch (final JexlException.Continue stmtContinue) {
                    String target = stmtContinue.getLabel();
                    if (target != null && !target.equals(node.getLabel())) {
                        throw stmtContinue;
                    }
                    // continue
                }
            }
        } while (testPredicate(condition, condition.jjtAccept(this, data)));

        return result;
    }


    @Override
    protected Object visit(final ASTSynchronizedStatement node, final Object data) {
        cancelCheck(node);
        Object result = null;
        /* first objectNode is the synchronization expression */
        final JexlNode expressionNode = node.jjtGetChild(0);
        try {
            synchronized (expressionNode.jjtAccept(this, data)) {
                // execute statement
                if (node.jjtGetNumChildren() > 1) {
                    result = node.jjtGetChild(1).jjtAccept(this, data);
                }
            }
        } catch (final JexlException.Break stmtBreak) {
            String target = stmtBreak.getLabel();
            if (target == null || !target.equals(node.getLabel())) {
                throw stmtBreak;
            }
            // break
        }
        return result;
    }

    @Override
    protected Object visit(final ASTSwitchStatement node, final Object data) {
        cancelCheck(node);
        final boolean lexical = options.isLexical();
        if (lexical) {
            // create lexical frame
            block = new LexicalFrame(frame, block);
        }
        try {
            Object result = null;
            /* first objectNode is the switch expression */
            Object left = node.jjtGetChild(0).jjtAccept(this, data);
            try {
                final int childCount = node.jjtGetNumChildren();
                boolean matched = false;
                Class scope = left != null ? left.getClass() : Void.class;
                int start = 0;
                // check all non default cases first
                l: for (int i = 1; i < childCount; i++) {
                    JexlNode child = node.jjtGetChild(i);
                    if (child instanceof ASTSwitchStatementCase) {
                        JexlNode labels = child.jjtGetChild(0);
                        if (labels.jjtGetChild(0) instanceof ASTVar) {
                            ASTVar caseVar = (ASTVar) labels.jjtGetChild(0);
                            Class type = caseVar.getType();
                            if (type.isAssignableFrom(left.getClass())) {
                                final int symbol = caseVar.getSymbol();
                                final boolean lxl = options.isLexical() && symbol >= 0;
                                if (lxl) {
                                    // create lexical frame
                                    LexicalFrame locals = new LexicalFrame(frame, block);
                                    if (!defineVariable((ASTVar) caseVar, locals)) {
                                        return redefinedVariable(node, caseVar.getName());
                                    }
                                    block = locals;
                                }
                                try {
                                    // Set case variable
                                    executeAssign(node, caseVar, left, null, null);
                                    boolean execute = true;
                                    if (labels.jjtGetNumChildren() > 1) {
                                        JexlNode cond = labels.jjtGetChild(1);
                                        execute = arithmetic.toBoolean(cond.jjtAccept(this, data));
                                    }
                                    if (execute) {
                                        // execute case block
                                        result = child.jjtAccept(this, data);
                                    }
                                } finally {
                                    // restore lexical frame
                                    if (lxl) {
                                        block = block.pop();
                                    }
                                }
                                // Execute fallthrough labeles
                                matched = true;
                                start = i+1;
                                break l;
                            }
                        } else {
                            // check all labels
                            for (int j = 0; j < labels.jjtGetNumChildren(); j++) {
                                JexlNode label = labels.jjtGetChild(j);
                                if (left == null) {
                                    if (label instanceof ASTNullLiteral) {
                                        matched = true;
                                        start = i;
                                        break l;
                                    }
                                } else {
                                    Object right = label instanceof ASTIdentifier ? 
                                        label.jjtAccept(this, scope) : 
                                        label.jjtAccept(this, data);
                                    try {
                                        Object caseMatched = operators.tryOverload(child, JexlOperator.EQ, left, right);
                                        if (caseMatched == JexlEngine.TRY_FAILED) {
                                            caseMatched = arithmetic.equals(left, right) ? Boolean.TRUE : Boolean.FALSE;
                                        }
                                        matched = arithmetic.toBoolean(caseMatched);
                                    } catch (ArithmeticException xrt) {
                                        throw createException(node, "== error", xrt);
                                    }
                                    if (matched) {
                                        start = i;
                                        break l;
                                    }
                                }
                            }
                        }
                    }
                }
                // otherwise jump to default case
                if (!matched) {
                    for (int i = 1; i < childCount; i++) {
                        JexlNode child = node.jjtGetChild(i);
                        if (child instanceof ASTSwitchStatementDefault) {
                            matched = true;
                            start = i;
                            break;
                        } else {
                            ASTSwitchCaseLabel labels = (ASTSwitchCaseLabel) child.jjtGetChild(0);
                            if (labels.isDefault()) {
                                matched = true;
                                start = i;
                                break;
                            }
                        }
                    }
                }
                // execute all cases starting from matched one
                if (matched) {
                    for (int i = start; i < childCount; i++) {
                        result = node.jjtGetChild(i).jjtAccept(this, data);
                    }
                }
            } catch (JexlException.Break stmtBreak) {
                String target = stmtBreak.getLabel();
                if (target != null && !target.equals(node.getLabel())) {
                    throw stmtBreak;
                }
                // break
            }
            return result;
        } finally {
            // restore lexical frame
            if (lexical) {
                block = block.pop();
            }
        }
    }

    @Override
    protected Object visit(final ASTSwitchStatementCase node, final Object data) {
        Object result = null;
        final int childCount = node.jjtGetNumChildren();
        for (int i = 1; i < childCount; i++) {
            cancelCheck(node);
            result = node.jjtGetChild(i).jjtAccept(this, data);
        }
        return result;
    }

    @Override
    protected Object visit(final ASTSwitchStatementDefault node, final Object data) {
        Object result = null;
        final int childCount = node.jjtGetNumChildren();
        for (int i = 0; i < childCount; i++) {
            cancelCheck(node);
            result = node.jjtGetChild(i).jjtAccept(this, data);
        }
        return result;
    }

    @Override
    protected Object visit(final ASTSwitchExpression node, final Object data) {
        /* first objectNode is the switch expression */
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        try {
            int childCount = node.jjtGetNumChildren();
            Class scope = left != null ? left.getClass() : Void.class;
            // check all cases first
            for (int i = 1; i < childCount; i++) {
                JexlNode child = node.jjtGetChild(i);
                if (child instanceof ASTSwitchExpressionCase) {
                    JexlNode labels = child.jjtGetChild(0);
                    if (labels.jjtGetChild(0) instanceof ASTVar) {
                        ASTVar caseVar = (ASTVar) labels.jjtGetChild(0);
                        Class type = caseVar.getType();
                        if (type.isAssignableFrom(left.getClass())) {
                            final int symbol = caseVar.getSymbol();
                            final boolean lexical = options.isLexical() && symbol >= 0;
                            if (lexical) {
                                // create lexical frame
                                LexicalFrame locals = new LexicalFrame(frame, block);
                                if (!defineVariable((ASTVar) caseVar, locals)) {
                                    return redefinedVariable(node, caseVar.getName());
                                }
                                block = locals;
                            }
                            try {
                                // Set case variable
                                executeAssign(node, caseVar, left, null, null);
                                boolean execute = true;
                                if (labels.jjtGetNumChildren() > 1) {
                                    JexlNode cond = labels.jjtGetChild(1);
                                    execute = arithmetic.toBoolean(cond.jjtAccept(this, data));
                                }
                                if (execute) {
                                    // execute case block
                                    return child.jjtAccept(this, data);
                                }
                            } finally {
                                // restore lexical frame
                                if (lexical) {
                                    block = block.pop();
                                }
                            }
                        }
                    } else {
                        boolean matched = false;
                        // check all labels
                        for (int j = 0; j < labels.jjtGetNumChildren(); j++) {
                            JexlNode label = labels.jjtGetChild(j);
                            if (left == null) {
                                if (label instanceof ASTNullLiteral)
                                    return child.jjtAccept(this, data); 
                            } else {
                                Object right = label instanceof ASTIdentifier ? label.jjtAccept(this, scope) : label.jjtAccept(this, data);
                                try {
                                    Object caseMatched = operators.tryOverload(child, JexlOperator.EQ, left, right);
                                    if (caseMatched == JexlEngine.TRY_FAILED) {
                                        caseMatched = arithmetic.equals(left, right) ? Boolean.TRUE : Boolean.FALSE;
                                    }
                                    matched = arithmetic.toBoolean(caseMatched);
                                } catch (ArithmeticException xrt) {
                                    throw createException(node, "== error", xrt);
                                }
                                if (matched) {
                                    return child.jjtAccept(this, data);
                                }
                            }
                        }
                    }
                }
            }
            // otherwise jump to default case
            for (int i = 1; i < childCount; i++) {
                JexlNode child = node.jjtGetChild(i);
                if (child instanceof ASTSwitchExpressionDefault) {
                    return child.jjtAccept(this, data);
                } else {
                    ASTSwitchCaseLabel labels = (ASTSwitchCaseLabel) child.jjtGetChild(0);
                    if (labels.isDefault()) {
                        return child.jjtAccept(this, data);
                    }
                }

            }
            return null;
        } catch (JexlException.Yield stmtYield) {
            return stmtYield.getValue();
        }
    }

    @Override
    protected Object visit(final ASTSwitchExpressionCase node, final Object data) {
        Object result = null;
        int childCount = node.jjtGetNumChildren();
        if (childCount > 1) {
            result = node.jjtGetChild(1).jjtAccept(this, data);
        }
        return result;
    }

    @Override
    protected Object visit(final ASTSwitchCaseLabel node, final Object data) {
        return null;
    }

    @Override
    protected Object visit(final ASTSwitchExpressionDefault node, final Object data) {
        Object result = null;
        int childCount = node.jjtGetNumChildren();
        if (childCount > 0) {
            result = node.jjtGetChild(0).jjtAccept(this, data);
        }
        return result;
    }

    @Override
    protected Object visit(final ASTAndNode node, final Object data) {
        /*
         * The pattern for exception mgmt is to let the child*.jjtAccept out of the try/catch loop so that if one fails,
         * the ex will traverse up to the interpreter. In cases where this is not convenient/possible, JexlException
         * must be caught explicitly and rethrown.
         */
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        try {
            final boolean leftValue = arithmetic.toBoolean(left);
            if (!leftValue) {
                return Boolean.FALSE;
            }
        } catch (final ArithmeticException xrt) {
            throw createException(node.jjtGetChild(0), "boolean coercion error", xrt);
        }
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final boolean rightValue = arithmetic.toBoolean(right);
            if (!rightValue) {
                return Boolean.FALSE;
            }
        } catch (final ArithmeticException xrt) {
            throw createException(node.jjtGetChild(1), "boolean coercion error", xrt);
        }
        return Boolean.TRUE;
    }

    @Override
    protected Object visit(final ASTOrNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        try {
            final boolean leftValue = arithmetic.toBoolean(left);
            if (leftValue) {
                return Boolean.TRUE;
            }
        } catch (final ArithmeticException xrt) {
            throw createException(node.jjtGetChild(0), "boolean coercion error", xrt);
        }
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final boolean rightValue = arithmetic.toBoolean(right);
            if (rightValue) {
                return Boolean.TRUE;
            }
        } catch (final ArithmeticException xrt) {
            throw createException(node.jjtGetChild(1), "boolean coercion error", xrt);
        }
        return Boolean.FALSE;
    }

    @Override
    protected Object visit(final ASTUnderscoreLiteral node, final Object data) {
        return null;
    }

    @Override
    protected Object visit(final ASTNullLiteral node, final Object data) {
        return node.getLiteral();
    }

    @Override
    protected Object visit(final ASTThisNode node, final Object data) {
        return context;
    }

    @Override
    protected Object visit(final ASTCurrentNode node, final Object data) {
        return current;
    }

    @Override
    protected Object visit(final ASTBooleanLiteral node, final Object data) {
        return node.getLiteral();
    }

    @Override
    protected Object visit(final ASTNumberLiteral node, final Object data) {
        if (data != null && node.isInteger()) {
            return getAttribute(data, node.getLiteral(), node);
        }
        return node.getLiteral();
    }

    @Override
    protected Object visit(final ASTStringLiteral node, final Object data) {
        if (data != null) {
            return getAttribute(data, node.getLiteral(), node);
        }
        return node.getLiteral();
    }

    @Override
    protected Object visit(final ASTRegexLiteral node, final Object data) {
        return node.getLiteral();
    }

    @Override
    protected Object visit(final ASTTextBlockLiteral node, final Object data) {
        return node.getLiteral();
    }

    @Override
    protected Object visit(final ASTStringBuilderLiteral node, final Object data) {
        final Object o = node.jjtGetChild(0).jjtAccept(this, data);
        return new StringBuilder(arithmetic.toString(o));
    }

    @Override
    protected Object visit(final ASTClassLiteral node, final Object data) {
        return node.getLiteral();
    }

    @Override
    protected Object visit(final ASTTypeLiteral node, final Object data) {
        return node.getType();
    }

    @Override
    protected Object visit(final ASTArrayLiteral node, final Object data) {
        int childCount = node.jjtGetNumChildren();
        JexlArithmetic.ArrayBuilder ab = arithmetic.arrayBuilder(childCount);
        boolean extended = node.isExtended();
        boolean immutable = node.isImmutable();
        final boolean cacheable = cache && immutable && node.isConstant();
        Object cached = cacheable ? node.jjtGetValue() : null;
        if (cached != null) {
            return cached;
        }
        for (int i = 0; i < childCount; i++) {
            cancelCheck(node);
            JexlNode child = node.jjtGetChild(i);
            if (child instanceof ASTEnumerationNode || child instanceof ASTEnumerationReference) {
                Iterator<?> it = (Iterator<?>) child.jjtAccept(this, data);
                if (it != null) {
                    try {
                        while (it.hasNext()) {
                            Object entry = it.next();
                            ab.add(entry);
                        }
                    } finally {
                        closeIfSupported(it);
                    }
                }
            } else {
                Object entry = child.jjtAccept(this, data);
                ab.add(entry);
            }
        }
        if (immutable) {
            Object result = ab.create(true);
            if (result instanceof List<?>) {
                result = Collections.unmodifiableList((List<?>) result);
            }
            if (cacheable) {
                node.jjtSetValue(result);
            }
            return result;
        } else {
            return ab.create(extended);
        }
    }

    @Override
    protected Object visit(final ASTSetOperand node, final Object data) {

        ArrayList<Object> result = new ArrayList<> ();

        int numChildren = node.jjtGetNumChildren();
        for (int i = 0; i < numChildren; i++) {
            cancelCheck(node);
            JexlNode child = node.jjtGetChild(i);
            if (child instanceof ASTEnumerationNode || child instanceof ASTEnumerationReference) {
                Iterator<?> it = (Iterator<?>) child.jjtAccept(this, data);
                if (it != null) {
                    try {
                        while (it.hasNext()) {
                            Object right = it.next();
                            result.add(right);
                        }
                    } finally {
                        closeIfSupported(it);
                    }
                }
            } else {
                Object right = child.jjtAccept(this, data);
                result.add(right);
            }
        }

        return result.toArray();
    }

    @Override
    protected Object visit(final ASTSetLiteral node, final Object data) {
        boolean immutable = node.isImmutable();
        final boolean cacheable = cache && immutable && node.isConstant();
        Object cached = cacheable ? node.jjtGetValue() : null;
        if (cached != null) {
            return cached;
        }
        int childCount = node.jjtGetNumChildren();
        boolean ordered = node.isOrdered();
        JexlArithmetic.SetBuilder mb = arithmetic.setBuilder(childCount, ordered);
        for (int i = 0; i < childCount; i++) {
            cancelCheck(node);
            JexlNode child = node.jjtGetChild(i);
            if (child instanceof ASTEnumerationNode || child instanceof ASTEnumerationReference) {
                Iterator<?> it = (Iterator<?>) child.jjtAccept(this, data);
                if (it != null) {
                    try {
                        while (it.hasNext()) {
                            Object entry = it.next();
                            mb.add(entry);
                        }
                    } finally {
                        closeIfSupported(it);
                    }
                }
            } else {
                Object entry = child.jjtAccept(this, data);
                mb.add(entry);
            }
        }
        Object result = mb.create();
        if (immutable) {
            if (result instanceof Set<?>) {
                result = Collections.unmodifiableSet((Set<?>) result);
            }
            if (cacheable) {
                node.jjtSetValue(result);
            }
        }
        return result;
    }

    @Override
    protected Object visit(final ASTMapLiteral node, final Object data) {
        boolean immutable = node.isImmutable();
        final boolean cacheable = cache && immutable && node.isConstant();
        Object cached = cacheable ? node.jjtGetValue() : null;
        if (cached != null) {
            return cached;
        }
        int childCount = node.jjtGetNumChildren();
        boolean ordered = node.isOrdered();
        JexlArithmetic.MapBuilder mb = arithmetic.mapBuilder(childCount, ordered);
        for (int i = 0; i < childCount; i++) {
            cancelCheck(node);
            JexlNode child = node.jjtGetChild(i);
            if (child instanceof ASTMapEntry) {
                Object[] entry = (Object[]) (child).jjtAccept(this, data);
                mb.put(entry[0], entry[1]);
            } else {

                Object iterableValue = (child).jjtAccept(this, data);
                if (iterableValue != null) {
                    // get an iterator for the collection/array etc via the introspector.
                    Object forEach = operators.tryOverload(node, JexlOperator.FOR_EACH_INDEXED, iterableValue);
                    Iterator<?> it = forEach instanceof Iterator ? 
                         (Iterator<?>) forEach : 
                         uberspect.getIndexedIterator(iterableValue);

                    int j = 0;
                    if (it != null) {
                        try {
                            while (it.hasNext()) {
                                Object value = it.next();
                                if (value instanceof Map.Entry<?,?>) {
                                    Map.Entry<?,?> entry = (Map.Entry<?,?>) value;
                                    mb.put(entry.getKey(), entry.getValue());
                                } else {
                                    mb.put(j, value);
                                }
                                j++;
                            }
                        } finally {
                            closeIfSupported(it);
                        }
                    }
                }
            }
        }
        if (immutable) {
            Object result = mb.create();
            if (result instanceof Map<?,?>) {
                result = Collections.unmodifiableMap((Map<?,?>) result);
            }
            if (cacheable) {
                node.jjtSetValue(result);
            }
            return result;
        } else {
            return mb.create();
        }
    }

    @Override
    protected Object visit(final ASTMapEntry node, final Object data) {
        final Object key = node.jjtGetChild(0).jjtAccept(this, data);
        final Object value = node.jjtGetChild(1).jjtAccept(this, data);
        return new Object[]{key, value};
    }

    @Override
    protected Object visit(final ASTMapEntryLiteral node, final Object data) {
        final Object key = node.jjtGetChild(0).jjtAccept(this, data);
        final Object value = node.jjtGetChild(1).jjtAccept(this, data);

        return arithmetic.createMapEntry(key, value);
    }

    @Override
    protected Object visit(final ASTMapEnumerationNode node, final Object data) {
        JexlNode valNode = node.jjtGetChild(0);
        Object iterableValue = valNode.jjtAccept(this, data);

        if (iterableValue != null) {
            Object forEach = operators.tryOverload(node, JexlOperator.FOR_EACH_INDEXED, iterableValue);
            Iterator<?> itemsIterator = forEach instanceof Iterator
                                   ? (Iterator<?>) forEach
                                   : uberspect.getIndexedIterator(iterableValue);
            return itemsIterator;
        } else {
            return null;
        }
    }

    @Override
    protected Object visit(final ASTInlinePropertyAssignment node, final Object data) {
        Object prev = current;
        current = data;
        try {
            int childCount = node.jjtGetNumChildren();
            for (int i = 0; i < childCount; i++) {
                cancelCheck(node);

                JexlNode p = node.jjtGetChild(i);

                if (p instanceof ASTInlinePropertyEntry) {

                   Object entry = p.jjtAccept(this, null);

                   if (entry instanceof Object[]) {
                       Object[] e = (Object[]) entry;

                       String name = String.valueOf(e[0]);
                       Object value = e[1];

                       setAttribute(data, name, value, p, JexlOperator.PROPERTY_SET);
                   } else if (entry instanceof Iterator<?>) {

                       int j = 0;
                       Iterator<?> it = (Iterator<?>) entry;

                       try {
                           while (it.hasNext()) {
                               Object value = it.next();
                               if (value instanceof Map.Entry<?,?>) {
                                   Map.Entry<?,?> e = (Map.Entry<?,?>) value;
                                   setAttribute(data, e.getKey(), e.getValue(), p, JexlOperator.PROPERTY_SET);

                               } else {
                                   setAttribute(data, j, value, p, JexlOperator.PROPERTY_SET);
                               }
                               j++;
                           }
                       } finally {
                           closeIfSupported(it);
                       }
                   }

                } else if (p instanceof ASTInlineFieldEntry) {

                   Object[] entry = (Object[]) p.jjtAccept(this, null);

                   String name = String.valueOf(entry[0]);
                   Object value = entry[1];

                   setField(data, name, value, p);

                } else if (p instanceof ASTInlinePropertyArrayEntry) {

                   Object[] entry = (Object[]) p.jjtAccept(this, null);

                   Object key = entry[0];
                   Object value = entry[1];

                   setAttribute(data, key, value, p, JexlOperator.ARRAY_SET);

                } else if (p instanceof ASTInlinePropertyNullEntry) {

                   ASTInlinePropertyNullEntry entry = (ASTInlinePropertyNullEntry) p;

                   JexlNode name = entry.jjtGetChild(0);
                   Object key = name instanceof ASTIdentifier ? ((ASTIdentifier) name).getName() : name.jjtAccept(this, null);
                   String property = String.valueOf(key);

                   Object value = getAttribute(data, property, p);

                   if (value == null) {
                      value = entry.jjtAccept(this, null);
                      setAttribute(data, property, value, p, JexlOperator.PROPERTY_SET);
                   }

                } else if (p instanceof ASTInlineFieldNullEntry) {

                   ASTInlineFieldNullEntry entry = (ASTInlineFieldNullEntry) p;

                   Object key = entry.jjtGetChild(0).jjtAccept(this, null);
                   String property = String.valueOf(key);

                   Object value = getField(data, property, p);

                   if (value == null) {
                      value = entry.jjtAccept(this, null);
                      setField(data, property, value, p);
                   }

                } else if (p instanceof ASTInlinePropertyArrayNullEntry) {

                   ASTInlinePropertyArrayNullEntry entry = (ASTInlinePropertyArrayNullEntry) p;

                   Object key = entry.jjtGetChild(0).jjtAccept(this, null);
                   Object value = getAttribute(data, key, p);

                   if (value == null) {
                      value = entry.jjtAccept(this, null);
                      setAttribute(data, key, value, p, JexlOperator.ARRAY_SET);
                   }

                } else if (p instanceof ASTInlinePropertyNEEntry) {

                   ASTInlinePropertyNEEntry entry = (ASTInlinePropertyNEEntry) p;

                   JexlNode name = entry.jjtGetChild(0);
                   Object key = name instanceof ASTIdentifier ? ((ASTIdentifier) name).getName() : name.jjtAccept(this, null);
                   String property = String.valueOf(key);

                   Object value = getAttribute(data, property, p);
                   Object right = entry.jjtAccept(this, null);

                   Object result = operators.tryOverload(entry, JexlOperator.EQ, value, right);
                   boolean equals = (result != JexlEngine.TRY_FAILED)
                              ? arithmetic.toBoolean(result)
                              : arithmetic.equals(value, right);
                   if (!equals) {
                      setAttribute(data, property, right, p, JexlOperator.PROPERTY_SET);
                   }

                } else if (p instanceof ASTInlineFieldNEEntry) {

                   ASTInlineFieldNEEntry entry = (ASTInlineFieldNEEntry) p;

                   Object key = entry.jjtGetChild(0).jjtAccept(this, null);
                   String property = String.valueOf(key);

                   Object value = getField(data, property, p);
                   Object right = entry.jjtAccept(this, null);

                   Object result = operators.tryOverload(entry, JexlOperator.EQ, value, right);
                   boolean equals = (result != JexlEngine.TRY_FAILED)
                              ? arithmetic.toBoolean(result)
                              : arithmetic.equals(value, right);
                   if (!equals) {
                      setField(data, property, right, p);
                   }

                } else if (p instanceof ASTInlinePropertyArrayNEEntry) {

                   ASTInlinePropertyArrayNEEntry entry = (ASTInlinePropertyArrayNEEntry) p;

                   Object key = entry.jjtGetChild(0).jjtAccept(this, null);
                   Object value = getAttribute(data, key, p);
                   Object right = entry.jjtAccept(this, null);

                   Object result = operators.tryOverload(entry, JexlOperator.EQ, value, right);
                   boolean equals = (result != JexlEngine.TRY_FAILED)
                              ? arithmetic.toBoolean(result)
                              : arithmetic.equals(value, right);
                   if (!equals) {
                      setAttribute(data, key, right, p, JexlOperator.ARRAY_SET);
                   }

                } else if (p instanceof ASTBlock) {

                   // ASTBlock
                   p.jjtAccept(this, null);

                } else {

                   // ASTReference 
                   p.jjtAccept(this, data);
                }
            }
        } finally {
            current = prev;
        }
        return data;
    }

    @Override
    protected Object visit(final ASTInlinePropertyArrayEntry node, final Object data) {
        final Object key = node.jjtGetChild(0).jjtAccept(this, data);
        final Object value = node.jjtGetChild(1).jjtAccept(this, data);

        return new Object[] {key, value};
    }

    @Override
    protected Object visit(final ASTInlinePropertyEntry node, final Object data) {

        int childCount = node.jjtGetNumChildren();
        if (childCount > 1) {
            JexlNode name = node.jjtGetChild(0);

            final Object key = name instanceof ASTIdentifier ? ((ASTIdentifier) name).getName() : name.jjtAccept(this, data);
            final Object value = node.jjtGetChild(1).jjtAccept(this, data);

            return new Object[] {key, value};
        } else {
            final Object iterableValue = node.jjtGetChild(0).jjtAccept(this, data);
            if (iterableValue != null) {
                 // get an iterator for the collection/array etc via the introspector.
                 Object forEach = operators.tryOverload(node, JexlOperator.FOR_EACH_INDEXED, iterableValue);
                 Iterator<?> it = forEach instanceof Iterator ? 
                      (Iterator<?>) forEach : 
                      uberspect.getIndexedIterator(iterableValue);
                 return it;
            }
            return null;
        }
    }

    @Override
    protected Object visit(final ASTInlineFieldEntry node, final Object data) {
        JexlNode name = node.jjtGetChild(0);

        final Object key = node.jjtGetChild(0).jjtAccept(this, data); 
        final Object value = node.jjtGetChild(1).jjtAccept(this, data);

        return new Object[] {key, value};
    }

    @Override
    protected Object visit(final ASTInlinePropertyNullEntry node, final Object data) {
        return node.jjtGetChild(1).jjtAccept(this, data);
    }

    @Override
    protected Object visit(final ASTInlineFieldNullEntry node, final Object data) {
        return node.jjtGetChild(1).jjtAccept(this, data);
    }

    @Override
    protected Object visit(final ASTInlinePropertyNEEntry node, final Object data) {
        return node.jjtGetChild(1).jjtAccept(this, data);
    }

    @Override
    protected Object visit(final ASTInlineFieldNEEntry node, final Object data) {
        return node.jjtGetChild(1).jjtAccept(this, data);
    }

    @Override
    protected Object visit(final ASTInlinePropertyArrayNullEntry node, final Object data) {
        return node.jjtGetChild(1).jjtAccept(this, data);
    }

    @Override
    protected Object visit(final ASTInlinePropertyArrayNEEntry node, final Object data) {
        return node.jjtGetChild(1).jjtAccept(this, data);
    }

    @Override
    protected Object visit(final ASTTernaryNode node, final Object data) {
        Object condition;
        try {
            condition = node.jjtGetChild(0).jjtAccept(this, data);
        } catch(final JexlException xany) {
            if (!(xany.getCause() instanceof JexlArithmetic.NullOperand)) {
                throw xany;
            }
            condition = null;
        }
        // ternary as in "x ? y : z"
        if (condition != null && arithmetic.toBoolean(condition)) {
            return node.jjtGetChild(1).jjtAccept(this, data);
        }
        if (node.jjtGetNumChildren() == 3) {
            return node.jjtGetChild(2).jjtAccept(this, data);
        }
        return null;
    }

    @Override
    protected Object visit(ASTElvisNode node, Object data) {
        Object condition;
        try {
            condition = node.jjtGetChild(0).jjtAccept(this, data);
        } catch(JexlException xany) {
            if (!(xany.getCause() instanceof JexlArithmetic.NullOperand)) {
                throw xany;
            }
            condition = null;
        }
        if (condition != null && arithmetic.toBoolean(condition)) {
            return condition;
        } else {
            return node.jjtGetChild(1).jjtAccept(this, data);
        }
    }

    @Override
    protected Object visit(final ASTNullpNode node, final Object data) {
        Object lhs;
        try {
            lhs = node.jjtGetChild(0).jjtAccept(this, data);
        } catch(final JexlException xany) {
            if (!(xany.getCause() instanceof JexlArithmetic.NullOperand)) {
                throw xany;
            }
            lhs = null;
        }
        // null elision as in "x ?? z"
        return lhs != null? lhs : node.jjtGetChild(1).jjtAccept(this, data);
    }

    @Override
    protected Object visit(final ASTSizeFunction node, final Object data) {
        try {
            final Object val = node.jjtGetChild(0).jjtAccept(this, data);
            return operators.size(node, val);
        } catch(final JexlException xany) {
            return 0;
        }
    }

    @Override
    protected Object visit(final ASTEmptyFunction node, final Object data) {
        try {
            final Object value = node.jjtGetChild(0).jjtAccept(this, data);
            return operators.empty(node, value);
        } catch(final JexlException xany) {
            return true;
        }
    }

    @Override
    protected Object visit(final ASTAwaitFunction node, final Object data) {
        try {
            final Object value = node.jjtGetChild(0).jjtAccept(this, data);
            Future f = (Future) arithmetic.cast(Future.class, value);
            return f != null ? f.get() : null;
        } catch(final ExecutionException xany) {
            throw createException(node, "get", xany);
        } catch(final InterruptedException x) {
            throw new JexlException.Cancel(detailedInfo(node));
        }
    }

    /**
     * Runs a node.
     * @param node the node
     * @param data the usual data
     * @return the return value
     */
    protected Object visitLexicalNode(final JexlNode node, final Object data) {
        block = new LexicalFrame(frame, null);
        try {
            return node.jjtAccept(this, data);
        } finally {
            block = block.pop();
        }
    }

    /**
     * Runs a closure.
     * @param closure the closure
     * @param data the usual data
     * @return the closure return value
     */
    protected Object runClosure(final Closure closure, final Object data) {
        final ASTJexlScript script = closure.getScript();
        // if empty script, nothing to evaluate
        final int numChildren = script.jjtGetNumChildren();
        if (numChildren == 0) {
            return null;
        }
        block = new LexicalFrame(frame, block).defineArgs();
        try {
            final JexlNode body = script instanceof ASTJexlLambda
                    ? script.jjtGetChild(numChildren - 1)
                    : script;
            return interpret(body);
        } finally {
            block = block.pop();
        }
    }

    @Override
    protected Object visit(final ASTJexlScript script, final Object data) {
        if (script instanceof ASTJexlLambda && !((ASTJexlLambda) script).isTopLevel()) {
            return Closure.create(this, (ASTJexlLambda) script);
        } else {
            block = new LexicalFrame(frame, block).defineArgs();
            try {
                final int numChildren = script.jjtGetNumChildren();
                Object result = null;
                for (int i = 0; i < numChildren; i++) {
                    final JexlNode child = script.jjtGetChild(i);
                    result = child.jjtAccept(this, data);
                    cancelCheck(child);
                }
                return result;
            } finally {
                block = block.pop();
            }
        }
    }

    @Override
    protected Object visit(final ASTEnclosedExpression node, final Object data) {
        try {
            return node.jjtGetChild(0).jjtAccept(this, data);
        } catch (final JexlException.Yield stmtYield) {
            return stmtYield.getValue();
        }
    }

    @Override
    protected Object visit(final ASTVar node, final Object data) {
        final int symbol = node.getSymbol();
        if ((options.isLexical() || node.isLexical()) && !defineVariable(node, block)) {
            return redefinedVariable(node, node.getName());
        }
        // if we have a var, we have a scope thus a frame
        boolean isFinal = block.isVariableFinal(symbol);
        if (isFinal) {
            throw createException(node, "can not redefine a final variable: " + node.getName());
        }
        // Adjust frame variable modifiers
        block.setModifiers(symbol, node.getType(), node.isLexical(), node.isConstant(), node.isRequired());
        // if we have a var, we have a scope thus a frame
        if (options.isLexical() || node.isLexical() || !frame.has(symbol)) {
            frame.set(symbol, null);
            return null;
        } else {
            return frame.get(symbol);
        }
    }

    @Override
    protected Object visit(final ASTExtVar node, final Object data) {
        return visit((ASTVar) node, data);
    }

    @Override
    protected Object visit(final ASTMultiVar node, final Object data) {
        return visit((ASTVar) node, data);
    }

    @Override
    protected Object visit(final ASTFunctionVar node, final Object data) {
        return visit((ASTVar) node, data);
    }

    @Override
    protected Object visit(final ASTIdentifier identifier, final Object data) {
        cancelCheck(identifier);
        return data != null
                ? getAttribute(data, identifier.getName(), identifier)
                : getVariable(frame, block, identifier);
    }

    @Override
    protected Object visit(final ASTArrayAccess node, final Object data) {
        // first objectNode is the identifier
        Object object = data;
        // can have multiple nodes - either an expression, integer literal or reference
        final int numChildren = node.jjtGetNumChildren();
        for (int i = 0; i < numChildren; i++) {
            final JexlNode nindex = node.jjtGetChild(i);
            if (object == null) {
                return unsolvableProperty(nindex, stringifyProperty(nindex), false, null);
            }
            final Object index = nindex.jjtAccept(this, null);
            cancelCheck(node);
            object = getAttribute(object, index, nindex);
        }
        return object;
    }

    @Override
    protected Object visit(ASTArrayAccessSafe node, Object data) {
        // first objectNode is the identifier
        Object object = data;
        // can have multiple nodes - either an expression, integer literal or reference
        int numChildren = node.jjtGetNumChildren();
        for (int i = 0; i < numChildren; i++) {
            JexlNode nindex = node.jjtGetChild(i);
            if (object == null) {
                return null;
            }
            Object index = nindex.jjtAccept(this, null);
            cancelCheck(node);
            object = getAttribute(object, index, nindex);
        }
        return object;
    }

    /**
     * Evaluates an access identifier based on the 2 main implementations;
     * static (name or numbered identifier) or dynamic (jxlt).
     * @param node the identifier access node
     * @return the evaluated identifier
     */
    private Object evalIdentifier(final ASTIdentifierAccess node) {
        if (node instanceof ASTIdentifierAccessJxlt) {
            final ASTIdentifierAccessJxlt accessJxlt = (ASTIdentifierAccessJxlt) node;
            final String src = node.getName();
            Throwable cause = null;
            TemplateEngine.TemplateExpression expr = (TemplateEngine.TemplateExpression) accessJxlt.getExpression();
            try {
                if (expr == null) {
                    final TemplateEngine jxlt = jexl.jxlt();
                    expr = jxlt.parseExpression(node.jexlInfo(), src, frame != null ? frame.getScope() : null);
                    accessJxlt.setExpression(expr);
                }
                if (expr != null) {
                    final Object name = expr.evaluate(context, frame, options);
                    if (name != null) {
                        final Integer id = ASTIdentifierAccess.parseIdentifier(name.toString());
                        return id != null ? id : name;
                    }
                }
            } catch (final JxltEngine.Exception xjxlt) {
                cause = xjxlt;
            }
            return node.isSafe() ? null : unsolvableProperty(node, src, true, cause);
        } else {
            return node.getIdentifier();
        }
    }

    @Override
    protected Object visit(final ASTIdentifierAccess node, final Object data) {
        if (data == null) {
            return null;
        }
        final Object id = evalIdentifier(node);
        return getAttribute(data, id, node);
    }

    @Override
    protected Object visit(final ASTFieldAccess node, final Object data) {
        if (data == null) {
            return null;
        }
        final Object id = node.getIdentifier();
        return getField(data, id, node);
    }

    @Override
    protected Object visit(final ASTAttributeReference node, final Object data) {
        return node.getName();
    }

    @Override
    protected Object visit(final ASTReference node, final Object data) {
        cancelCheck(node);
        final int numChildren = node.jjtGetNumChildren();
        final JexlNode parent = node.jjtGetParent();
        // pass first piece of data in and loop through children
        Object object = data;
        JexlNode objectNode = null;
        JexlNode ptyNode = null;
        StringBuilder ant = null;
        boolean antish = !(parent instanceof ASTReference) && options.isAntish();
        int v = 1;
        main:
        for (int c = 0; c < numChildren; c++) {
            objectNode = node.jjtGetChild(c);
            if (objectNode instanceof ASTMethodNode) {
                antish = false;
                if (object == null) {
                    // we may be performing a method call on an antish var
                    if (ant != null) {
                        final JexlNode child = objectNode.jjtGetChild(0);
                        if (child instanceof ASTIdentifierAccess) {
                            final int alen = ant.length();
                            ant.append('.');
                            ant.append(((ASTIdentifierAccess) child).getName());
                            object = context.get(ant.toString());
                            if (object != null) {
                                object = visit((ASTMethodNode) objectNode, object, context);
                                continue;
                            } else {
                                // remove method name from antish
                                ant.delete(alen, ant.length());
                                ptyNode = objectNode;
                            }
                        }
                    }
                    break;
                }
            } else if (objectNode instanceof ASTArrayAccess) {
                antish = false;
                if (object == null) {
                    ptyNode = objectNode;
                    break;
                }
            } else if (objectNode instanceof ASTFieldAccess) {
                antish = false;
                if (object == null) {
                    ptyNode = objectNode;
                    break;
                }
            } else if (objectNode instanceof ASTArrayAccessSafe) {
                antish = false;
                if (object == null) {
                    break;
                }
            } else if (objectNode instanceof ASTPipeNode) {
                antish = false;
                if (object == null) {
                    break;
                }
            } else if (objectNode instanceof ASTInlinePropertyAssignment) {
                antish = false;
                if (object == null) {
                    break;
                }
            }
            // attempt to evaluate the property within the object (visit(ASTIdentifierAccess node))
            object = objectNode.jjtAccept(this, object);
            cancelCheck(node);
            if (object != null) {
                // disallow mixing antish variable & bean with same root; avoid ambiguity
                antish = false;
            } else if (antish) {
                // create first from first node
                if (ant == null) {
                    // if we still have a null object, check for an antish variable
                    final JexlNode first = node.jjtGetChild(0);
                    if (first instanceof ASTIdentifier) {
                        final ASTIdentifier afirst = (ASTIdentifier) first;
                        ant = new StringBuilder(afirst.getName());
                        // skip the else...*
                    } else {
                        // not an identifier, not antish
                        ptyNode = objectNode;
                        break main;
                    }
                    // skip the first node case since it was trialed in jjtAccept above and returned null
                }
                // catch up to current node
                for (; v <= c; ++v) {
                    final JexlNode child = node.jjtGetChild(v);
                    if (child instanceof ASTIdentifierAccess) {
                        final ASTIdentifierAccess achild = (ASTIdentifierAccess) child;
                        if (achild.isSafe() || achild.isExpression()) {
                            break main;
                        }
                        ant.append('.');
                        ant.append(achild.getName());
                    } else {
                        // not an identifier, not antish
                        ptyNode = objectNode;
                        break main;
                    }
                }
                // solve antish
                object = context.get(ant.toString());
            } else if (c != numChildren - 1) {
                // only the last one may be null
                ptyNode = c == 0 && numChildren > 1 ? node.jjtGetChild(1) : objectNode;
                break; //
            }
        }
        // dealing with null
        if (object == null) {
            if (ptyNode != null) {
                if (ptyNode.isSafeLhs(isSafe())) {
                    return null;
                }
                if (ant != null) {
                    final String aname = ant.toString();
                    final boolean defined = isVariableDefined(frame, block, aname);
                    return unsolvableVariable(node, aname, !defined);
                }
                return unsolvableProperty(node,
                        stringifyProperty(ptyNode), ptyNode == objectNode, null);
            }
            if (antish) {
                if (node.isSafeLhs(isSafe())) {
                    return null;
                }
                final String aname = ant != null ? ant.toString() : "?";
                final boolean defined = isVariableDefined(frame, block, aname);
                // defined but null; arg of a strict operator?
                if (defined && !isStrictOperand(node)) {
                    return null;
                }
                return unsolvableVariable(node, aname, !defined);
            }
        }
        return object;
    }

    @Override
    protected Object visit(final ASTMultipleIdentifier node, final Object data) {
        return null;
    }

    @Override
    protected Object visit(final ASTMultipleAssignment node, final Object data) {
        cancelCheck(node);
        // Vector of identifiers to assign values to
        JexlNode identifiers = node.jjtGetChild(0);
        // Assignable values
        Object assignableValue = node.jjtGetChild(1).jjtAccept(this, data);
        return executeMultipleAssign(node, identifiers, assignableValue, data);
    }

    @Override
    protected Object visit(final ASTMultipleVarStatement node, final Object data) {
        cancelCheck(node);
        // Vector of identifiers to assign values to
        JexlNode identifiers = node.jjtGetChild(0);
        // Initialize variables
        final int num = identifiers.jjtGetNumChildren();
        for (int i = 0; i < num; i++) {
            JexlNode left = identifiers.jjtGetChild(i);
            left.jjtAccept(this, data);
        }
        // Assignable values
        Object assignableValue = node.jjtGetChild(1).jjtAccept(this, data);
        return executeMultipleAssign(node, identifiers, assignableValue, data);
    }

    /**
     * Executes a multiple assignment.
     * @param node        the node
     * @param identifiers the reference to assign to
     * @param value       the value expression to assign
     * @param data        the data
     * @return the left hand side
     */
    protected Object executeMultipleAssign(final JexlNode node, final JexlNode identifiers, final Object value, 
                                           final Object data) { // CSOFF: MethodLength
        Object result = null;
        final int num = identifiers.jjtGetNumChildren();
        // Use separate logic for maps and non-iterable objects for destructuring
        if (value instanceof Map<?,?>) {
            Map<?,?> assignableMap = (Map<?,?>) value;
            for (int i = 0; i < num; i++) {
                cancelCheck(node);
                JexlNode left = identifiers.jjtGetChild(i);
                if (left instanceof ASTIdentifier) {
                    ASTIdentifier var = (ASTIdentifier) left;
                    Object right = assignableMap.get(var.getName());
                    result = executeAssign(left, left, right, null, data);
                }
            }
        } else if (value != null) {
            Object forEach = operators.tryOverload(node, JexlOperator.FOR_EACH, value);
            Iterator<?> itemsIterator = forEach instanceof Iterator
                                    ? (Iterator<?>) forEach
                                    : uberspect.getIterator(value);
            if (itemsIterator != null) {
                try {
                    int i = -1;
                    while (itemsIterator.hasNext()) {
                        cancelCheck(node);
                        i += 1;
                        // Stop if we are out of variables to assign to
                        if (i == num) {
                            break;
                        }
                        // The value to assign
                        Object right = itemsIterator.next();
                        // The identifier to assign to
                        JexlNode left = identifiers.jjtGetChild(i);
                        if (left instanceof ASTIdentifier) {
                            result = executeAssign(left, left, right, null, data);
                        }
                    }
                    while (i + 1 < num) {
                        JexlNode left = identifiers.jjtGetChild(++i);
                        if (left instanceof ASTIdentifier) {
                            result = executeAssign(left, left, null, null, data);
                        }
                    }
                } finally {
                    //  closeable iterator handling
                    closeIfSupported(itemsIterator);
                }
            } else {
                for (int i = 0; i < num; i++) {
                    cancelCheck(node);
                    JexlNode left = identifiers.jjtGetChild(i);
                    if (left instanceof ASTIdentifier) {
                        ASTIdentifier var = (ASTIdentifier) left;
                        Object right = getAttribute(value, var.getName(), node);
                        result = executeAssign(left, left, right, null, data);
                    }
                }
            }
        } else {
            for (int i = 0; i < num; i++) {
                cancelCheck(node);
                JexlNode left = identifiers.jjtGetChild(i);
                if (left instanceof ASTIdentifier) {
                    result = executeAssign(left, left, null, null, data);
                }
            }
        }
        return result;
    }

    @Override
    protected Object visit(final ASTVarStatement node, final Object data) {
        cancelCheck(node);
        final int num = node.jjtGetNumChildren();
        Object value = null;
        for (int i = 0; i < num; i++) {
            value = node.jjtGetChild(i).jjtAccept(this, data);
        }
        return value;
    }

    @Override
    protected Object visit(final ASTInitialization node, final Object data) {
        cancelCheck(node);
        ASTVar left = (ASTVar) node.jjtGetChild(0);
        Object right = null;
        // First evaluate the right part
        if (node.jjtGetNumChildren() == 2) {
            right = node.jjtGetChild(1).jjtAccept(this, data);
        } else if (left.getType() == Boolean.TYPE) {
            right = Boolean.FALSE;
        } else if (left.getType() == Character.TYPE) {
            right = (char) 0;
        } else if (left.getType() == Byte.TYPE) {
            right = (byte) 0;
        } else if (left.getType() == Short.TYPE) {
            right = (short) 0;
        } else if (left.getType() == Integer.TYPE) {
            right = 0;
        } else if (left.getType() == Long.TYPE) {
            right = 0L;
        } else if (left.getType() == Float.TYPE) {
            right = 0.0f;
        } else if (left.getType() == Double.TYPE) {
            right = 0.0d;
        }
        // Then declare variable
        Object result = left.jjtAccept(this, data);
        // Initialize variable
        if (node.jjtGetNumChildren() == 2 || right != null) {
            return executeAssign(node, left, right, null, data);
        } else {
            return result;
        }
    }

    @Override
    protected Object visit(final ASTAssignment node, final Object data) {
        JexlNode left = node.jjtGetChild(0);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return executeAssign(node, left, right, null, data);
    }

    @Override
    protected Object visit(final ASTNullAssignment node, final Object data) {
        JexlNode left = node.jjtGetChild(0);
        Object value = left.jjtAccept(this, data);
        if (value != null) {
             return value;
        }
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return executeAssign(node, left, right, null, data);
    }

    @Override
    protected Object visit(final ASTNEAssignment node, final Object data) {
        JexlNode left = node.jjtGetChild(0);
        Object value = left.jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        Object result = operators.tryOverload(node, JexlOperator.EQ, value, right);
        boolean equals = (result != JexlEngine.TRY_FAILED)
                   ? arithmetic.toBoolean(result)
                   : arithmetic.equals(value, right);
        if (!equals) {
           return executeAssign(node, left, right, null, data);
        }
        return value;
    }

    @Override
    protected Object visit(final ASTSetAddNode node, final Object data) {
        final JexlNode left = node.jjtGetChild(0);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return executeAssign(node, left, right, JexlOperator.SELF_ADD, data);
    }

    @Override
    protected Object visit(final ASTSetSubNode node, final Object data) {
        final JexlNode left = node.jjtGetChild(0);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return executeAssign(node, left, right, JexlOperator.SELF_SUBTRACT, data);
    }

    @Override
    protected Object visit(final ASTSetMultNode node, final Object data) {
        final JexlNode left = node.jjtGetChild(0);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return executeAssign(node, left, right, JexlOperator.SELF_MULTIPLY, data);
    }

    @Override
    protected Object visit(final ASTSetDivNode node, final Object data) {
        final JexlNode left = node.jjtGetChild(0);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return executeAssign(node, left, right, JexlOperator.SELF_DIVIDE, data);
    }

    @Override
    protected Object visit(final ASTSetModNode node, final Object data) {
        final JexlNode left = node.jjtGetChild(0);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return executeAssign(node, left, right, JexlOperator.SELF_MOD, data);
    }

    @Override
    protected Object visit(final ASTSetAndNode node, final Object data) {
        final JexlNode left = node.jjtGetChild(0);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return executeAssign(node, left, right, JexlOperator.SELF_AND, data);
    }

    @Override
    protected Object visit(final ASTSetOrNode node, Object data) {
        final JexlNode left = node.jjtGetChild(0);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return executeAssign(node, left, right, JexlOperator.SELF_OR, data);
    }

    @Override
    protected Object visit(final ASTSetDiffNode node, Object data) {
        final JexlNode left = node.jjtGetChild(0);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return executeAssign(node, left, right, JexlOperator.SELF_DIFF, data);
    }

    @Override
    protected Object visit(final ASTSetXorNode node, final Object data) {
        final JexlNode left = node.jjtGetChild(0);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return executeAssign(node, left, right, JexlOperator.SELF_XOR, data);
    }

    @Override
    protected Object visit(final ASTSetShiftLeftNode node, final Object data) {
        final JexlNode left = node.jjtGetChild(0);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return executeAssign(node, left, right, JexlOperator.SELF_SHIFTLEFT, data);
    }

    @Override
    protected Object visit(final ASTSetShiftRightNode node, final Object data) {
        final JexlNode left = node.jjtGetChild(0);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return executeAssign(node, left, right, JexlOperator.SELF_SHIFTRIGHT, data);
    }

    @Override
    protected Object visit(final ASTSetShiftRightUnsignedNode node, final Object data) {
        final JexlNode left = node.jjtGetChild(0);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return executeAssign(node, left, right, JexlOperator.SELF_SHIFTRIGHTU, data);
    }

    @Override
    protected Object visit(final ASTIncrementGetNode node, final Object data) {
        final JexlNode left = node.jjtGetChild(0);
        return executeAssign(node, left, 1, JexlOperator.INCREMENT_AND_GET, data);
    }

    @Override
    protected Object visit(final ASTDecrementGetNode node, final Object data) {
        final JexlNode left = node.jjtGetChild(0);
        return executeAssign(node, left, 1, JexlOperator.DECREMENT_AND_GET, data);
    }

    @Override
    protected Object visit(final ASTGetIncrementNode node, final Object data) {
        JexlNode left = node.jjtGetChild(0);
        Object value = left.jjtAccept(this, data);
        executeAssign(node, left, 1, JexlOperator.GET_AND_INCREMENT, data);
        return value;
    }

    @Override
    protected Object visit(final ASTGetDecrementNode node, final Object data) {
        final JexlNode left = node.jjtGetChild(0);
        final Object value = left.jjtAccept(this, data);
        executeAssign(node, left, 1, JexlOperator.GET_AND_DECREMENT, data);
        return value;
    }

    /**
     * Executes an assignment with an optional side effect operator.
     * @param node     the node
     * @param left     the reference to assign to
     * @param right    the value expression to assign
     * @param assignop the assignment operator or null if simply assignment
     * @param data     the data
     * @return the left hand side
     */
    protected Object executeAssign(final JexlNode node, final JexlNode left, final Object right, 
                                   final JexlOperator assignop, final Object data) { // CSOFF: MethodLength
        cancelCheck(node);
        // left contains the reference to assign to
        final ASTIdentifier variable;
        Object object = null;
        final int symbol;
        // check var decl with assign is ok
        if (left instanceof ASTIdentifier) {
            variable = (ASTIdentifier) left;
            symbol = variable.getSymbol();
            if (symbol >= 0 && (variable.isLexical() || options.isLexical())) {
                if (variable.isShaded() && (variable.isLexical() || options.isLexicalShade())) {
                    return undefinedVariable(variable, variable.getName());
                }
            }
        } else {
            variable = null;
            symbol = -1;
        }
        boolean antish = options.isAntish();
        // 0: determine initial object & property:
        final int last = left.jjtGetNumChildren() - 1;
         // actual value to return, right in most cases
        Object actual = right;
         // a (var?) v = ... expression
        if (variable != null) {
            if (symbol >= 0) {
                // check we are not assigning a symbol itself
                if (last < 0) {
                    boolean isFinal = block.isVariableFinal(symbol);
                    if (isFinal && !(variable instanceof ASTVar || variable instanceof ASTExtVar)) {
                        throw createException(node, "can not assign a value to the final variable: " + variable.getName());
                    }

                    // check if we need to typecast result
                    Class type = block.typeof(symbol);
                    if (type != null) {
                        if (arithmetic.isStrict()) {
                            actual = arithmetic.implicitCast(type, actual);
                        } else {
                            actual = arithmetic.cast(type, actual);
                        }
                        if (type.isPrimitive() && actual == null) {
                            throw createException(node, "not null value required for: " + variable.getName());
                        }
                    }
                    boolean isRequired = block.isVariableRequired(symbol);
                    if (isRequired && actual == null) {
                        throw createException(node, "not null value required for: " + variable.getName());
                    }

                    if (assignop == null) {
                        // make the closure accessible to itself, ie capture the currently set variable after frame creation
                        if (actual instanceof Closure) {
                            final Closure closure = (Closure) actual;
                            // the variable scope must be the parent of the lambdas
                            closure.captureSelfIfRecursive(frame, symbol);
                        }
                        frame.set(symbol, actual);
                    } else {
                        // go through potential overload
                        final Object self = getVariable(frame, block, variable);
                        final Consumer<Object> f = r -> { 
                            // make the closure accessible to itself, ie capture the currently set variable after frame creation
                            if (r instanceof Closure) {
                                final Closure closure = (Closure) r;
                                // the variable scope must be the parent of the lambdas
                                closure.captureSelfIfRecursive(frame, symbol);
                            }
                            frame.set(symbol, r);
                        };
                        actual = assignop.getArity() == 1 ? operators.tryAssignOverload(node, assignop, f, self) :
                            operators.tryAssignOverload(node, assignop, f, self, actual);
                    }
                    return actual; // 1
                }
                object = getVariable(frame, block, variable);
                // top level is a symbol, can not be an antish var
                antish = false;
            } else {
                // check we are not assigning direct global
                final String name = variable.getName();
                if (last < 0) {
                    if (assignop == null) {
                        setContextVariable(node, name, right);
                    } else {
                        // go through potential overload
                        final Object self = context.get(name);
                        final Consumer<Object> f = r ->  setContextVariable(node, name, r);
                        actual = assignop.getArity() == 1 ? operators.tryAssignOverload(node, assignop, f, self) :
                            operators.tryAssignOverload(node, assignop, f, self, right);
                    }
                    return actual; // 2
                }
                object = context.get(name);
                // top level accesses object, can not be an antish var
                if (object != null) {
                    antish = false;
                }
            }
        } else if (left instanceof ASTIndirectNode) {
            if (assignop == null) {

                final Object self = left.jjtGetChild(0).jjtAccept(this, data);
                if (self == null) {
                    throw createException(left, "illegal assignment form *0");
                }

                if (self instanceof SetPointer) {
                    ((SetPointer) self).set(right);
                } else {
                    Object result = operators.indirectAssign(node, self, right);
                    if (result == JexlEngine.TRY_FAILED) {
                        throw createException(left, "illegal dereferenced assignment");
                    }
                }
                return right;
            } else {

                // Try with dereferenced object first
                final Object p = left.jjtAccept(this, data);
                if (p == null) {
                    throw createException(left, "illegal assignment form *0");
                }

                final Consumer<Object> assign = r -> {};

                Object result = assignop.getArity() == 1 ? operators.tryAssignOverload(node, assignop, assign, p) :
                            operators.tryAssignOverload(node, assignop, assign, p, right);

                if (result == p) {
                    return p;
                } else if (result != JexlEngine.TRY_FAILED) {

                    final Object self = left.jjtGetChild(0).jjtAccept(this, data);
                    if (self == null) {
                        throw createException(left, "illegal assignment form *0");
                    }

                    if (self instanceof SetPointer) {
                        ((SetPointer) self).set(result);
                    } else {
                        result = operators.indirectAssign(node, self, result);
                        if (result == JexlEngine.TRY_FAILED) {
                            throw createException(left, "illegal dereferenced assignment");
                        }
                    }
                }

                return right;
            }
        } else if (!(left instanceof ASTReference)) {
            throw createException(left, "illegal assignment form 0");
        }
        // 1: follow children till penultimate, resolve dot/array
        JexlNode objectNode = null;
        StringBuilder ant = null;
        int v = 1;
        // start at 1 if symbol
        main: for (int c = symbol >= 0 ? 1 : 0; c < last; ++c) {
            objectNode = left.jjtGetChild(c);
            object = objectNode.jjtAccept(this, object);
            if (object != null) {
                // disallow mixing antish variable & bean with same root; avoid ambiguity
                antish = false;
            } else if (antish) {
                // initialize if first time
                if (ant == null) {
                    final JexlNode first = left.jjtGetChild(0);
                    final ASTIdentifier firstId = first instanceof ASTIdentifier
                            ? (ASTIdentifier) first
                            : null;
                    if (firstId != null && firstId.getSymbol() < 0) {
                        ant = new StringBuilder(firstId.getName());
                    } else {
                        // ant remains null, object is null, stop solving
                        antish = false;
                        break main;
                    }
                }
                // catch up to current child
                for (; v <= c; ++v) {
                    final JexlNode child = left.jjtGetChild(v);
                    final ASTIdentifierAccess aid = child instanceof ASTIdentifierAccess
                            ? (ASTIdentifierAccess) child
                            : null;
                    // remain antish only if unsafe navigation
                    if (aid != null && aid.isGlobalVar()) {
                        ant.append('.');
                        ant.append(aid.getName());
                    } else {
                        antish = false;
                        break main;
                    }
                }
                // solve antish
                object = context.get(ant.toString());
            } else {
                throw createException(objectNode, "illegal assignment form");
            }
        }
        // 2: last objectNode will perform assignement in all cases
        JexlNode propertyNode = left.jjtGetChild(last);
        final Object property;

        if (propertyNode instanceof ASTFieldAccess) {
            final ASTIdentifierAccess propertyId = (ASTIdentifierAccess) propertyNode;
            property = propertyId.getIdentifier();
        } else if (propertyNode instanceof ASTIdentifierAccess) {
            final ASTIdentifierAccess propertyId = (ASTIdentifierAccess) propertyNode;
            // deal with creating/assignining antish variable
            if (antish && ant != null && object == null && propertyId.isGlobalVar()) {
                if (last > 0) {
                    ant.append('.');
                }
                ant.append(propertyId.getName());
                final String name = ant.toString();
                if (assignop == null) {
                    setContextVariable(propertyNode, name, right);
                } else {
                    final Object self = context.get(ant.toString());
                    final JexlNode pnode = propertyNode;
                    final Consumer<Object> assign = r -> setContextVariable(pnode, name, r);
                    actual = assignop.getArity() == 1 ? operators.tryAssignOverload(node, assignop, assign, self) :
                        operators.tryAssignOverload(node, assignop, assign, self, right);
                }
                return actual; // 3
            }
            // property of an object ?
            property = evalIdentifier(propertyId);
        } else if (propertyNode instanceof ASTArrayAccess) {
            // can have multiple nodes - either an expression, integer literal or reference
            final int numChildren = propertyNode.jjtGetNumChildren() - 1;
            for (int i = 0; i < numChildren; i++) {
                final JexlNode nindex = propertyNode.jjtGetChild(i);
                final Object index = nindex.jjtAccept(this, null);
                object = getAttribute(object, index, nindex);
            }
            propertyNode = propertyNode.jjtGetChild(numChildren);
            property = propertyNode.jjtAccept(this, null);
        } else {
            throw createException(objectNode, "illegal assignment form");
        }
        // we may have a null property as in map[null], no check needed.
        // we can not *have* a null object though.
        if (object == null) {
            // no object, we fail
            return propertyNode instanceof ASTFieldAccess ?
                unsolvableField(objectNode, "<null>.<?>", true, null) : 
                unsolvableProperty(objectNode, "<null>.<?>", true, null);
        }
        // 3: one before last, assign
        if (assignop == null) {
            if (propertyNode != null && propertyNode instanceof ASTFieldAccess) {
                setField(object, property, right, propertyNode);
            } else {
                final JexlOperator operator = propertyNode != null && propertyNode.jjtGetParent() instanceof ASTArrayAccess
                                              ? JexlOperator.ARRAY_SET : JexlOperator.PROPERTY_SET;
                setAttribute(object, property, right, propertyNode, operator);
            }
        } else {
            final Object self = propertyNode instanceof ASTFieldAccess ? 
                getField(object, property, propertyNode) :
                getAttribute(object, property, propertyNode);
            final Object o = object;
            final JexlNode n = propertyNode;
            final JexlOperator operator = propertyNode != null && propertyNode.jjtGetParent() instanceof ASTArrayAccess
                                         ? JexlOperator.ARRAY_SET : JexlOperator.PROPERTY_SET;
            final Consumer<Object> assign = (propertyNode != null && propertyNode instanceof ASTFieldAccess) ? 
                 r -> setField(o, property, r, n) :
                 r -> setAttribute(o, property, r, n, operator);

            actual = assignop.getArity() == 1 ? 
                operators.tryAssignOverload(node, assignop, assign, self) : 
                operators.tryAssignOverload(node, assignop, assign, self, right);
        }
        return actual;
    }

    @Override
    protected Object[] visit(ASTArguments node, Object data) {
        int childCount = node.jjtGetNumChildren();
        if (childCount > 0) {
            List<Object> av = new ArrayList<Object> (childCount);
            for (int i = 0; i < childCount; i++) {
                JexlNode child = node.jjtGetChild(i);
                if (child instanceof ASTEnumerationNode || child instanceof ASTEnumerationReference) {
                    Iterator<?> it = (Iterator<?>) child.jjtAccept(this, data);
                    if (it != null) {
                       try {
                           while (it.hasNext()) {
                               Object entry = it.next();
                               av.add(entry);
                           }
                       } finally {
                           closeIfSupported(it);
                       }
                    }
                } else {
                    Object entry = child.jjtAccept(this, data);
                    av.add(entry);
                }
            }
            return av.toArray();
        } else {
            return EMPTY_PARAMS;
        }
    }

    @Override
    protected Object visit(final ASTInnerConstructorNode node, Object data) {
        cancelCheck(node);
        String enclosingClass = data == null ? null : data.getClass().getCanonicalName();
        if (enclosingClass == null) {
            String tstr = data != null ? data.toString() : "?";
            return unsolvableMethod(node, tstr);
        }
        ASTArguments argNode = (ASTArguments) node.jjtGetChild(1);
        // get the ctor args
        Object[] argv = callArguments(data, false, (Object[]) argNode.jjtAccept(this, data));

        ASTIdentifier classNode = (ASTIdentifier) node.jjtGetChild(0);
        String target  = enclosingClass + "$" + classNode.getName();
        try {
            boolean narrow = false;
            JexlMethod ctor = null;
            while (true) {
                // try as stated
                ctor = uberspect.getConstructor(target, argv);
                if (ctor != null) {
                    break;
                }
                // if we did not find an exact method by name and we haven't tried yet,
                // attempt to narrow the parameters and if this succeeds, try again in next loop
                if (!narrow && arithmetic.narrowArguments(argv)) {
                    narrow = true;
                    continue;
                }
                // we are done trying
                break;
            }
            // we have either evaluated and returned or might have found a ctor
            if (ctor != null) {
                return ctor.invoke(target, argv);
            }
            final String tstr = Objects.toString(target, "?");			
            return unsolvableMethod(node, tstr);
        } catch (final JexlException xthru) {
            throw xthru;
        } catch (final Exception xany) {
            final String tstr = Objects.toString(target, "?");
            throw invocationException(node, tstr, xany);
        }
    }

    @Override
    protected Object visit(final ASTMethodReference node, Object data) {
        ASTIdentifier methodNode = (ASTIdentifier) node.jjtGetChild(0);
        String methodName = methodNode.getName();
        if (data == null) {
            return unsolvableMethod(methodNode, "<null>::" + methodName);
        }
        Object result = MethodReference.create(this, data, methodName);
        return result != null ? result : unsolvableMethod(methodNode, "::" + methodName);
    }

    @Override
    protected Object visit(final ASTMethodNode node, final Object data) {
        return visit(node, null, data);
    }

    /**
     * Execute a method call, ie syntactically written as name.call(...).
     * @param node the actual method call node
     * @param antish non-null when name.call is an antish variable
     * @param data the context
     * @return the method call result
     */
    private Object visit(final ASTMethodNode node, final Object antish, final Object data) {
        Object object = antish;
        // left contains the reference to the method
        final JexlNode methodNode = node.jjtGetChild(0);
        Object method;
        // 1: determine object and method or functor
        if (methodNode instanceof ASTIdentifierAccess) {
            method = methodNode;
            if (object == null) {
                object = data;
                if (object == null) {
                    // no object, we fail
                    return node.isSafeLhs(isSafe())
                        ? null
                        : unsolvableMethod(methodNode, "<null>.<?>(...)");
                }
            } else {
                // edge case of antish var used as functor
                method = object;
            }
        } else {
            method = methodNode.jjtAccept(this, data);
        }
        Object result = method;
        for (int a = 1; a < node.jjtGetNumChildren(); ++a) {
            if (result == null) {
                // no method, we fail// variable unknown in context and not a local
                return node.isSafeLhs(isSafe())
                        ? null
                        : unsolvableMethod(methodNode, "<?>.<null>(...)");
            }
            final ASTArguments argNode = (ASTArguments) node.jjtGetChild(a);
            result = call(node, object, result, argNode);
            object = result;
        }
        return result;
    }

    @Override
    protected Object visit(final ASTFunctionNode node, final Object data) {
        final ASTIdentifier functionNode = (ASTIdentifier) node.jjtGetChild(0);
        final String nsid = functionNode.getNamespace();
        final Object namespace = (nsid != null)? resolveNamespace(nsid, node) : context;
        final ASTArguments argNode = (ASTArguments) node.jjtGetChild(1);
        return call(node, namespace, functionNode, argNode);
    }

    /**
     * Calls a method (or function).
     * <p>
     * Method resolution is a follows:
     * 1 - attempt to find a method in the target passed as parameter;
     * 2 - if this fails, seeks a JexlScript or JexlMethod or a duck-callable* as a property of that target;
     * 3 - if this fails, narrow the arguments and try again 1
     * 4 - if this fails, seeks a context or arithmetic method with the proper name taking the target as first argument;
     * </p>
     * *duck-callable: an object where a "call" function exists
     *
     * @param node    the method node
     * @param target  the target of the method, what it should be invoked upon
     * @param funcNode the object carrying the method or function or the method identifier
     * @param argNode the node carrying the arguments
     * @return the result of the method invocation
     */
    protected Object call(final JexlNode node, final Object target, final Object funcNode, final ASTArguments argNode) {
        cancelCheck(node);
        final String methodName;
        boolean cacheable = cache;
        boolean isavar = false;
        Object functor = funcNode;
        // get the method name if identifier
        if (functor instanceof ASTIdentifier) {
            // function call, target is context or namespace (if there was one)
            final ASTIdentifier methodIdentifier = (ASTIdentifier) functor;
            final int symbol = methodIdentifier.getSymbol();
            methodName = methodIdentifier.getName();
            functor = null;
            // is it a global or local variable ?
            if (target == context) {
                if (frame != null && frame.has(symbol)) {
                    functor = frame.get(symbol);
                    isavar = functor != null;
                } else if (context.has(methodName)) {
                    functor = context.get(methodName);
                    isavar = functor != null;
                }
                // name is a variable, can't be cached
                cacheable &= !isavar;
            }
        } else if (functor instanceof ASTIdentifierAccess) {
            // a method call on target
            methodName = ((ASTIdentifierAccess) functor).getName();
            functor = null;
            cacheable = true;
        } else if (functor != null) {
            // ...(x)(y)
            methodName = null;
            cacheable = false;
        } else if (!node.isSafeLhs(isSafe())) {
            return unsolvableMethod(node, "?(...)");
        } else {
            // safe lhs
            return null;
        }

        // evaluate the arguments
        final Object[] argv = visit(argNode, null);

        // solving the call site
        final CallDispatcher call = new CallDispatcher(node, cacheable);
        try {
            // do we have a cached version method/function name ?
            final Object eval = call.tryEval(target, methodName, argv);
            if (JexlEngine.TRY_FAILED != eval) {
                return eval;
            }
            boolean functorp = false;
            boolean narrow = false;
            // pseudo loop to try acquiring methods without and with argument narrowing
            while (true) {
                call.narrow = narrow;
                // direct function or method call
                if (functor == null || functorp) {
                    // try a method or function from context
                    if (call.isTargetMethod(target, methodName, argv)) {
                        return call.eval(methodName);
                    }
                    if (target == context) {
                        // solve 'null' namespace
                        final Object namespace = resolveNamespace(null, node);
                        if (namespace != null
                            && namespace != context
                            && call.isTargetMethod(namespace, methodName, argv)) {
                            return call.eval(methodName);
                        }
                        // do not try context function since this was attempted
                        // 10 lines above...; solve as an arithmetic function
                        if (call.isArithmeticMethod(methodName, argv)) {
                            return call.eval(methodName);
                        }
                        // could not find a method, try as a property of a non-context target (performed once)
                    } else {
                        // try prepending target to arguments and look for
                        // applicable method in context...
                        final Object[] pargv = functionArguments(target, narrow, argv);
                        if (call.isContextMethod(methodName, pargv)) {
                            return call.eval(methodName);
                        }
                        // ...or arithmetic
                        if (call.isArithmeticMethod(methodName, pargv)) {
                            return call.eval(methodName);
                        }
                        // the method may also be a functor stored in a property of the target
                        if (!narrow) {
                            try {
                               functor = getAttribute(target, methodName);
                               functorp = functor != null;
                            } catch (UnsupportedOperationException eux) {
                               //
                            }
                        }
                    }
                }
                // this may happen without the above when we are chaining call like x(a)(b)
                // or when a var/symbol or antish var is used as a "function" name
                if (functor != null) {
                    // lambda, script or jexl method will do
                    if (functor instanceof JexlScript) {
                        JexlScript s = (JexlScript) functor;
                        boolean varArgs = s.isVarArgs();
                        if (!varArgs) {
                            String[] params = s.getUnboundParameters();
                            int paramCount = params != null ? params.length : 0;
                            int argCount = argv != null ? argv.length : 0;
                            if (argCount > paramCount) {
                                return unsolvableMethod(node, "(...)");
                            }
                        }
                        return s.execute(context, argv);
                    }
                    if (functor instanceof JexlMethod) {
                        return ((JexlMethod) functor).invoke(target, argv);
                    }
                    if (functor instanceof MethodReference) {
                        return ((MethodReference) functor).invoke(argv);
                    }
                    final String mCALL = "call";
                    // may be a generic callable, try a 'call' method
                    if (call.isTargetMethod(functor, mCALL, argv)) {
                        return call.eval(mCALL);
                    }
                    // functor is a var, may be method is a global one ?
                    if (isavar && target == context) {
                        if (call.isContextMethod(methodName, argv)) {
                            return call.eval(methodName);
                        }
                        if (call.isArithmeticMethod(methodName, argv)) {
                            return call.eval(methodName);
                        }
                    }
                    // try prepending functor to arguments and look for
                    // context or arithmetic function called 'call'
                    final Object[] pargv = functionArguments(functor, narrow, argv);
                    if (call.isContextMethod(mCALL, pargv)) {
                        return call.eval(mCALL);
                    }
                    if (call.isArithmeticMethod(mCALL, pargv)) {
                        return call.eval(mCALL);
                    }
                }
                // if we did not find an exact method by name and we haven't tried yet,
                // attempt to narrow the parameters and try again in next loop
                if (!narrow) {
                    arithmetic.narrowArguments(argv);
                    narrow = true;
                    // continue;
                } else {
                    break;
                }
            }
        } catch (JexlException.Method xmethod) {
            // ignore and handle at end; treat as an inner discover that fails
        } catch (final JexlException.TryFailed xany) {
            throw invocationException(node, methodName, xany.getCause());
        } catch (final JexlException xthru) {
            throw xthru;
        } catch (final Exception xany) {
            throw invocationException(node, methodName, xany);
        }
        // we have either evaluated and returned or no method was found
        return node.isSafeLhs(isSafe())
                ? null
                : unsolvableMethod(node, methodName, argv);
    }

    @Override
    protected Object visit(final ASTConstructorNode node, final Object data) {
        cancelCheck(node);
        // first child is class or class name
        final Object target = node.jjtGetChild(0).jjtAccept(this, data);
        // get the ctor args
        int argc = node.jjtGetNumChildren() - 1;
        Object[] argv = argc > 0 ? new Object[argc] : EMPTY_PARAMS;
        for (int i = 0; i < argc; i++) {
            argv[i] = node.jjtGetChild(i + 1).jjtAccept(this, data);
        }

        try {
            boolean cacheable = cache;
            // attempt to reuse last funcall cached in volatile JexlNode.value
            if (cacheable) {
                Object cached = node.jjtGetValue();
                if (cached instanceof Funcall) {
                    Object eval = ((Funcall) cached).tryInvoke(this, null, target, argv);
                    if (JexlEngine.TRY_FAILED != eval) {
                        return eval;
                    }
                }
            }
            boolean narrow = false;
            JexlMethod ctor = null;
            Funcall funcall = null;
            while (true) {
                // try as stated
                ctor = uberspect.getConstructor(target, argv);
                if (ctor != null) {
                    if (cacheable && ctor.isCacheable()) {
                        funcall = new Funcall(ctor, narrow);
                    }
                    break;
                }
                // try with prepending context as first argument
                Object[] nargv = callArguments(context, narrow, argv);
                ctor = uberspect.getConstructor(target, nargv);
                if (ctor != null) {
                    if (cacheable && ctor.isCacheable()) {
                        funcall = new ContextualCtor(ctor, narrow);
                    }
                    argv = nargv;
                    break;
                }
                // if we did not find an exact method by name and we haven't tried yet,
                // attempt to narrow the parameters and if this succeeds, try again in next loop
                if (!narrow && arithmetic.narrowArguments(argv)) {
                    narrow = true;
                    continue;
                }
                // we are done trying
                break;
            }
            // we have either evaluated and returned or might have found a ctor
            if (ctor != null) {
                Object eval = ctor.invoke(target, argv);
                // cache executor in volatile JexlNode.value
                if (funcall != null) {
                    node.jjtSetValue(funcall);
                }
                return eval;
            }
            final String tstr = Objects.toString(target, "?");
            return unsolvableMethod(node, tstr, argv);
        } catch (final JexlException xthru) {
            throw xthru;
        } catch (final Exception xany) {
            final String tstr = Objects.toString(target, "?");			
            throw invocationException(node, tstr, xany);
        }
    }

    @Override
    protected Object visit(final ASTQualifiedConstructorNode node, final Object data) {
        cancelCheck(node);
        // first child is class or class name
        final Class target = (Class) node.jjtGetChild(0).jjtAccept(this, data);
        // get the ctor args
        Object[] argv = (Object[]) node.jjtGetChild(1).jjtAccept(this, data);
        try {
            boolean cacheable = cache;
            // attempt to reuse last funcall cached in volatile JexlNode.value
            if (cacheable) {
                Object cached = node.jjtGetValue();
                if (cached instanceof Funcall) {
                    Object eval = ((Funcall) cached).tryInvoke(this, null, target, argv);
                    if (JexlEngine.TRY_FAILED != eval) {
                        return eval;
                    }
                }
            }
            boolean narrow = false;
            JexlMethod ctor = null;
            Funcall funcall = null;
            while (true) {
                // try as stated
                ctor = uberspect.getConstructor(target, argv);
                if (ctor != null) {
                    if (cacheable && ctor.isCacheable()) {
                        funcall = new Funcall(ctor, narrow);
                    }
                    break;
                }
                // try with prepending context as first argument
                Object[] nargv = callArguments(context, narrow, argv);
                ctor = uberspect.getConstructor(target, nargv);
                if (ctor != null) {
                    if (cacheable && ctor.isCacheable()) {
                        funcall = new ContextualCtor(ctor, narrow);
                    }
                    argv = nargv;
                    break;
                }
                // if we did not find an exact method by name and we haven't tried yet,
                // attempt to narrow the parameters and if this succeeds, try again in next loop
                if (!narrow && arithmetic.narrowArguments(argv)) {
                    narrow = true;
                    // continue;
                }
                // we are done trying
                break;
            }
            // we have either evaluated and returned or might have found a ctor
            if (ctor != null) {
                Object eval = ctor.invoke(target, argv);
                // cache executor in volatile JexlNode.value
                if (funcall != null) {
                    node.jjtSetValue(funcall);
                }
                return eval;
            }
            final String tstr = Objects.toString(target, "?");
            return unsolvableMethod(node, tstr, argv);
        } catch (final JexlException.Method xmethod) {
            throw xmethod;
        } catch (final Exception xany) {
            final String tstr = Objects.toString(target, "?");
            throw invocationException(node, tstr, xany);
        }
    }

    @Override
    protected Object visit(final ASTArrayConstructorNode node, Object data) {
        cancelCheck(node);
        // first child is class or class name
        final Class target = (Class) node.jjtGetChild(0).jjtAccept(this, data);
        // get the dimensions
        int argc = node.jjtGetNumChildren() - 1;
        int[] argv = new int[argc];
        for (int i = 0; i < argc; i++) {
            argv[i] = arithmetic.toInteger(node.jjtGetChild(i + 1).jjtAccept(this, data));
        }
        try {
            return Array.newInstance(target, argv);
        } catch (final Exception xany) {
            final String tstr = Objects.toString(target, "?");
            throw invocationException(node, tstr, xany);
        }
    }

    @Override
    protected Object visit(final ASTArrayOpenDimension node, final Object data) {
        return 0;
    }

    @Override
    protected Object visit(final ASTInitializedArrayConstructorNode node, final Object data) {
        cancelCheck(node);
        // first child is class or class name
        final Class target = (Class) node.jjtGetChild(0).jjtAccept(this, data);
        // get the length of the array
        int argc = node.jjtGetNumChildren() - 1;
        boolean comprehensions = false;
        for (int i = 0; i < argc; i++) {
            JexlNode child = node.jjtGetChild(i + 1);
            if (child instanceof ASTEnumerationNode || child instanceof ASTEnumerationReference) {
                comprehensions = true;
            }
        }
        try {
            if (comprehensions) {
                ArrayList<Object> result = new ArrayList<Object> (argc);
                for (int i = 0; i < argc; i++) {
                    JexlNode child = node.jjtGetChild(i + 1);
                    if (child instanceof ASTEnumerationNode || child instanceof ASTEnumerationReference) {
                        Iterator<?> it = (Iterator<?>) child.jjtAccept(this, data);
                        if (it != null) {
                            try {
                                while (it.hasNext()) {
                                    result.add(it.next());
                                }
                            } finally {
                                closeIfSupported(it);
                            }
                        }
                    } else {
                        result.add(child.jjtAccept(this, data));
                    }
                }
                int sz = result.size();
                Object array = Array.newInstance(target, sz);
                Class type = arithmetic.getWrapperClass(target);
                for (int i = 0; i < argc; i++) {
                    Object value = result.get(i);
                    if (!type.isInstance(value)) {
                        if (arithmetic.isStrict()) {
                            value = arithmetic.implicitCast(type, value);
                        } else {
                            value = arithmetic.cast(type, value);
                        }
                    }
                    Array.set(array, i, value);
                }
                return array;
            } else {
                Object result = Array.newInstance(target, argc);
                Class type = arithmetic.getWrapperClass(target);
                for (int i = 0; i < argc; i++) {
                    Object value = node.jjtGetChild(i + 1).jjtAccept(this, data);
                    if (!type.isInstance(value)) {
                        if (arithmetic.isStrict()) {
                            value = arithmetic.implicitCast(type, value);
                        } else {
                            value = arithmetic.cast(type, value);
                        }
                    }
                    Array.set(result, i, value);
                }
                return result;
            }
        } catch (final Exception xany) {
            final String tstr = Objects.toString(target, "?");			
            throw invocationException(node, tstr, xany);
        }
    }

    @Override
    protected Object visit(final ASTInitializedCollectionConstructorNode node, final Object data) {
        cancelCheck(node);
        // first child is class or class name
        final Class target = (Class) node.jjtGetChild(0).jjtAccept(this, data);
        if (!Collection.class.isAssignableFrom(target)) {
            throw createException(node, "Not a Collection", null);
        }
        try {
            JexlMethod ctor = uberspect.getConstructor(target, EMPTY_PARAMS);
            if (ctor != null) {
                Collection<Object> result = (Collection<Object>) ctor.invoke(target, EMPTY_PARAMS);
                // get the length of the collection
                int argc = node.jjtGetNumChildren() - 1;
                for (int i = 0; i < argc; i++) {
                    JexlNode child = node.jjtGetChild(i + 1);
                    if (child instanceof ASTEnumerationNode || child instanceof ASTEnumerationReference) {
                        Iterator<?> it = (Iterator<?>) child.jjtAccept(this, data);
                        if (it != null) {
                            try {
                                while (it.hasNext()) {
                                    result.add(it.next());
                                }
                            } finally {
                                closeIfSupported(it);
                            }
                        }
                    } else {
                        result.add(child.jjtAccept(this, data));
                    }
                }
                return result;
            }
            String tstr = target != null ? target.toString() : "?";
            return unsolvableMethod(node, tstr, EMPTY_PARAMS);
        } catch (Exception xany) {
            final String tstr = Objects.toString(target, "?");
            throw invocationException(node, tstr, xany);
        }
    }

    @Override
    protected Object visit(final ASTInitializedMapConstructorNode node, final Object data) {
        cancelCheck(node);
        // first child is class or class name
        final Class target = (Class) node.jjtGetChild(0).jjtAccept(this, data);
        if (!Map.class.isAssignableFrom(target)) {
            throw createException(node, "Not a Map", null);
        }
        try {
            JexlMethod ctor = uberspect.getConstructor(target, EMPTY_PARAMS);
            if (ctor != null) {
                Map<Object,Object> result = (Map<Object,Object>) ctor.invoke(target, EMPTY_PARAMS);
                // get the length of the map
                int argc = node.jjtGetNumChildren() - 1;
                for (int i = 0; i < argc; i++) {
                    JexlNode child = node.jjtGetChild(i + 1);
                    if (child instanceof ASTMapEntry) {
                        Object[] entry = (Object[]) (child).jjtAccept(this, data);
                        result.put(entry[0], entry[1]);
                    } else {
                        Iterator<Object> it = (Iterator<Object>) (child).jjtAccept(this, data);
                        if (it != null) {
                            try {
                                while (it.hasNext()) {
                                    Object value = it.next();
                                    if (value instanceof Map.Entry<?,?>) {
                                        Map.Entry<?,?> entry = (Map.Entry<?,?>) value;
                                        result.put(entry.getKey(), entry.getValue());
                                    } else {
                                        throw createException(node, "Not a Map.Entry", null);
                                    }
                                }
                            } finally {
                                closeIfSupported(it);
                            }
                        }
                    }
                }
                return result;
            }
            final String tstr = Objects.toString(target, "?");
            return unsolvableMethod(node, tstr, EMPTY_PARAMS);
        } catch (final Exception xany) {
            final String tstr = Objects.toString(target, "?");
            throw invocationException(node, tstr, xany);
        }
    }

    @Override
    protected Object visit(final ASTJxltLiteral node, final Object data) {
        final Object cache = node.getExpression();
        TemplateEngine.TemplateExpression tp;

        JexlInfo info = node.jexlInfo();
        if (this.block != null) {
            info = new JexlNode.Info(node, info);
        }

        if (cache instanceof TemplateEngine.TemplateExpression) {
            tp = (TemplateEngine.TemplateExpression) cache;
        } else {
            final TemplateEngine jxlt = jexl.jxlt();
            tp = jxlt.parseExpression(info, node.getLiteral(), frame != null ? frame.getScope() : null);
            node.setExpression(tp);
        }
        if (tp != null) {
            return tp.isDeferred() ? tp.prepare(context, frame, options, info) : tp.evaluate(context, frame, options);
        }
        return null;
    }

    @Override
    protected Object visit(final ASTAnnotation node, final Object data) {
        throw new UnsupportedOperationException(ASTAnnotation.class.getName() + ": Not supported.");
    }

    @Override
    protected Object visit(final ASTAnnotatedStatement node, final Object data) {
        return processAnnotation(node, 0, data);
    }

    /**
     * An annotated call.
     */
    public class AnnotatedCall implements Callable<Object> {
        /** The statement. */
        private final ASTAnnotatedStatement stmt;
        /** The child index. */
        private final int index;
        /** The data. */
        private final Object data;
        /** Tracking whether we processed the annotation. */
        private boolean processed;

        /**
         * Simple ctor.
         * @param astmt the statement
         * @param aindex the index
         * @param adata the data
         */
        AnnotatedCall(final ASTAnnotatedStatement astmt, final int aindex, final Object adata) {
            stmt = astmt;
            index = aindex;
            data = adata;
        }

        @Override
        public Object call() throws Exception {
            processed = true;
            try {
                return processAnnotation(stmt, index, data);
            } catch (JexlException.Return | JexlException.Break | JexlException.Continue xreturn) {
                return xreturn;
            } catch (JexlException.Yield | JexlException.Remove xreturn) {
                return xreturn;                
            }
        }

        /**
         * @return whether the statement has been processed
         */
        public boolean isProcessed() {
            return processed;
        }

        /**
         * @return the actual statement.
         */
        public Object getStatement() {
            return stmt;
        }
    }

    /**
     * Processes an annotated statement.
     * @param stmt the statement
     * @param index the index of the current annotation being processed
     * @param data the contextual data
     * @return  the result of the statement block evaluation
     */
    protected Object processAnnotation(final ASTAnnotatedStatement stmt, final int index, final Object data) {
        // are we evaluating the block ?
        final int last = stmt.jjtGetNumChildren() - 1;
        if (index == last) {
            final JexlNode cblock = stmt.jjtGetChild(last);
            // if the context has changed, might need a new arithmetic
            final JexlArithmetic jexla = arithmetic.options(context);
            if (jexla != arithmetic) {
                if (!arithmetic.getClass().equals(jexla.getClass())  && logger.isWarnEnabled()) {
                    logger.warn("expected arithmetic to be " + arithmetic.getClass().getSimpleName()
                            + ", got " + jexla.getClass().getSimpleName()
                    );
                }
                final JexlArithmetic old = arithmetic;
                try {
                    arithmetic = jexla;
                    return cblock.jjtAccept(this, data);
                } finally {
                    arithmetic = old;
                }
            } else {
                return cblock.jjtAccept(this, data);
            }
        }
        // tracking whether we processed the annotation
        final AnnotatedCall jstmt = new AnnotatedCall(stmt, index + 1, data);
        // the annotation node and name
        final ASTAnnotation anode = (ASTAnnotation) stmt.jjtGetChild(index);
        final String aname = anode.getName();
        // evaluate the arguments
        final Object[] argv = anode.jjtGetNumChildren() > 0
                        ? visit((ASTArguments) anode.jjtGetChild(0), null) : null;
        // wrap the future, will recurse through annotation processor
        Object result;
        try {
            result = processAnnotation(aname, argv, jstmt);
            // not processing an annotation is an error
            if (!jstmt.isProcessed()) {
                return annotationError(anode, aname, null);
            }
        } catch (final JexlException xany) {
            throw xany;
        } catch (final Exception xany) {
            return annotationError(anode, aname, xany);
        }
        // the caller may return a return, break or continue
        if (result instanceof JexlException) {
            throw (JexlException) result;
        }
        return result;
    }

    /**
     * Delegates the annotation processing to the JexlContext if it is an AnnotationProcessor.
     * @param annotation    the annotation name
     * @param args          the annotation arguments
     * @param stmt          the statement / block that was annotated
     * @return the result of statement.call()
     * @throws Exception if anything goes wrong
     */
    protected Object processAnnotation(final String annotation, final Object[] args, final Callable<Object> stmt) throws Exception {
                return context instanceof JexlContext.AnnotationProcessor
                ? ((JexlContext.AnnotationProcessor) context).processAnnotation(annotation, args, stmt)
                : stmt.call();
    }

    protected Iterator<?> prepareIndexedIterator(JexlNode node, Object iterableValue) {
        if (iterableValue != null) {
            Object forEach = operators.tryOverload(node, JexlOperator.FOR_EACH_INDEXED, iterableValue);
            Iterator<?> itemsIterator = forEach instanceof Iterator
                                    ? (Iterator<?>) forEach
                                    : uberspect.getIndexedIterator(iterableValue);
            return itemsIterator;
        }
        return null;
    }

    protected abstract class IteratorBase implements Iterator<Object>, AutoCloseable {

        protected final Iterator<?> itemsIterator;
        protected final JexlNode node;

        protected int i;

        protected IteratorBase(Iterator<?> iterator, JexlNode projection) {
            itemsIterator = iterator;
            node = projection;

            i = 0;
        }

        protected Object[] prepareArgs(ASTJexlScript lambda, Object data) {

            int argCount = lambda.getArgCount();
            boolean varArgs = lambda.isVarArgs();

            Object[] argv = null;

            if (argCount == 0) {
                argv = EMPTY_PARAMS;
            } else if (argCount == 1) {
                argv = new Object[] {data};
            } else if (!varArgs && data instanceof Object[]) {
                int len = ((Object[]) data).length;
                if (argCount > len) {
                    argv = new Object[len + 1];
                    argv[0] = i;
                    System.arraycopy(data, 0, argv, 1, len);
                } else if (argCount == len) {
                    argv = (Object[]) data;
                } else {
                    argv = new Object[] {i, data};
                }
            } else {
                argv = new Object[] {i, data};
            }

            return argv;
        }

        @Override
        public void close() {
            closeIfSupported(itemsIterator);
        }
    }

    public class ProjectionIterator extends IteratorBase {

        protected Map<Integer,Closure> scripts;

        protected ProjectionIterator(Iterator<?> iterator, JexlNode projection) {
            super(iterator, projection);
            scripts = new HashMap<Integer,Closure> ();
            i = -1;
            initClosure();
        }

        protected JexlNode getProjectionExpresssion(JexlNode node) {
            if (node instanceof ASTSimpleLambda && node.jjtGetNumChildren() == 1) {
                JexlNode expr = node.jjtGetChild(0);
                if (expr instanceof ASTJexlLambda || expr instanceof ASTCurrentNode) {
                    node = expr;
                }
            }
            return node;
        }

        protected void initClosure() {
            // can have multiple nodes
            int numChildren = node.jjtGetNumChildren();
            for (int i = 0; i < numChildren; i++) {
                JexlNode child = getProjectionExpresssion(node.jjtGetChild(i));
                if (child instanceof ASTJexlLambda) {
                    ASTJexlLambda script = (ASTJexlLambda) child;
                    scripts.put(i, new Closure(Interpreter.this, script));
                }
            }
        }

        protected Object evaluateProjection(int i, Object data) {
            Object prev = current;
            try {
                current = data;
                JexlNode child = getProjectionExpresssion(node.jjtGetChild(i));
                if (child instanceof ASTJexlLambda) {
                    Closure c = scripts.get(i);
                    Object[] argv = prepareArgs(c.getScript(), data);
                    return c.execute(null, argv);
                } else {
                    return child.jjtAccept(Interpreter.this, data);
                }
            } finally {
                current = prev;
            }
        }

        @Override
        public boolean hasNext() {
            return itemsIterator.hasNext();
        }

        @Override
        public Object next() {
            cancelCheck(node);
            Object data = itemsIterator.next();
            i += 1;
            // can have multiple nodes
            int numChildren = node.jjtGetNumChildren();
            if (numChildren == 1) {
                return evaluateProjection(0, data);
            } else {
                List<Object> value = new ArrayList(numChildren);
                for (int child = 0; child < numChildren; child++) {
                    value.add(evaluateProjection(child, data));
                }
                return Collections.unmodifiableList(value);
            }
        }

        @Override
        public void remove() {
            itemsIterator.remove();
        }
    }

    @Override
    protected Object visit(ASTProjectionNode node, Object data) {
        Iterator<?> itemsIterator = prepareIndexedIterator(node, data);
        return itemsIterator != null ? new ProjectionIterator(itemsIterator, node) : null;
    }

    public class MapProjectionIterator extends ProjectionIterator {

        protected MapProjectionIterator(Iterator<?> iterator, JexlNode projection) {
            super(iterator, projection);
        }

        @Override
        public Object next() {
            cancelCheck(node);
            Object data = itemsIterator.next();
            i += 1;

            int numChildren = node.jjtGetNumChildren();
            if (numChildren > 2) {
                LinkedHashMap<Object, Object> result = new LinkedHashMap();
                for (int j = 0; j < numChildren; j +=2 ) {
                   Object key = evaluateProjection(j, data);
                   Object value = evaluateProjection(j + 1, data);
                   result.put(key, value);
                }
                return result;
            } else {
                Object key = evaluateProjection(0, data);
                Object value = evaluateProjection(1, data);
                return new AbstractMap.SimpleImmutableEntry<Object,Object> (key, value);
           }
        }
    }

    @Override
    protected Object visit(ASTMapProjectionNode node, Object data) {
        Iterator<?> itemsIterator = prepareIndexedIterator(node, data);
        return itemsIterator != null ? new MapProjectionIterator(itemsIterator, node) : null;
    }

    public final class SelectionIterator extends IteratorBase {
        // filtering expression
        protected final Closure closure;
        // next available item of iterator
        protected Object nextItem;
        // whether the next item is available
        protected boolean hasNextItem;

        protected SelectionIterator(Iterator<?> iterator, ASTJexlLambda filter) {
            super(iterator, filter);
            closure = new Closure(Interpreter.this, filter);
        }

        protected void findNextItem() {
            if (!itemsIterator.hasNext()) {
                hasNextItem = false;
                nextItem = null;
            } else {
                Object data = null;
                boolean selected = false;
                Object prev = current;
                try {
                    do {
                        data = itemsIterator.next();
                        Object[] argv = prepareArgs((ASTJexlLambda) node, data);
                        current = data;
                        selected = arithmetic.toBoolean(closure.execute(null, argv));
                    } while (!selected && itemsIterator.hasNext());
                } finally {
                    current = prev;
                }
                if (selected) {
                    hasNextItem = true;
                    nextItem = data;
                }
            }
        }

        @Override
        public boolean hasNext() {
            if (!hasNextItem) {
                findNextItem();
            }
            return hasNextItem;
        }

        @Override
        public Object next() {
            cancelCheck(node);
            if (!hasNextItem) {
                findNextItem();
            }
            if (!hasNextItem) {
                throw new NoSuchElementException();
            }
            i += 1;
            hasNextItem = false;
            return nextItem;
        }

        @Override
        public void remove() {
            itemsIterator.remove();
        }
    }

    public final class StopCountIterator extends IteratorBase {
        // Stop counter
        protected final int limit;

        protected StopCountIterator(Iterator<?> iterator, JexlNode node, int stopCount) {
            super(iterator, node);
            limit = stopCount;
        }

        @Override
        public boolean hasNext() {
            return itemsIterator.hasNext() && i < limit;
        }

        @Override
        public Object next() {
            cancelCheck(node);
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            i += 1;
            return itemsIterator.next();
        }

        @Override
        public void remove() {
            itemsIterator.remove();
        }
    }

    public final class StartCountIterator extends IteratorBase {

        protected StartCountIterator(Iterator<?> iterator, JexlNode node, int startCount) {
            super(iterator, node);

            if (startCount > 0) {
                skipItems(startCount);
            }
        }

        protected void skipItems(int skipCount) {
            while (i < skipCount) {
                if (hasNext()) {
                    next();
                } else {
                    break;
                }
            }
        }

        @Override
        public boolean hasNext() {
            return itemsIterator.hasNext();
        }

        @Override
        public Object next() {
            cancelCheck(node);
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            i += 1;
            return itemsIterator.next();
        }

        @Override
        public void remove() {
            itemsIterator.remove();
        }
    }

    @Override
    protected Object visit(ASTSelectionNode node, Object data) {
        JexlNode child = node.jjtGetChild(0);

        if (child instanceof ASTStopCountNode) {
            int stopCount = (Integer) child.jjtAccept(this, null);
            Iterator<?> itemsIterator = prepareIndexedIterator(child, data);
            return itemsIterator != null ? new StopCountIterator(itemsIterator, node, stopCount) : null;
        } else if (child instanceof ASTStartCountNode) {
            int startCount = (Integer) child.jjtAccept(this, null);
            Iterator<?> itemsIterator = prepareIndexedIterator(child, data);
            return itemsIterator != null ? new StartCountIterator(itemsIterator, node, startCount) : null;
        }

        ASTJexlLambda script = (ASTJexlLambda) child;
        if (script instanceof ASTSimpleLambda && script.jjtGetNumChildren() == 1 && 
            script.jjtGetChild(0) instanceof ASTJexlLambda) {
           script = (ASTJexlLambda) script.jjtGetChild(0);
        }

        Iterator<?> itemsIterator = prepareIndexedIterator(child, data);
        return itemsIterator != null ? new SelectionIterator(itemsIterator, script) : null;
    }

    @Override
    protected Object visit(ASTStartCountNode node, Object data) {
        JexlNode child = node.jjtGetChild(0);
        return arithmetic.toInteger(child.jjtAccept(this, null));
    }

    @Override
    protected Object visit(ASTStopCountNode node, Object data) {
        JexlNode child = node.jjtGetChild(0);
        return arithmetic.toInteger(child.jjtAccept(this, null));
    }

    protected Object[] preparePipeLambdaArgs(ASTJexlLambda script, Object result, int i, Object value) {
        boolean varArgs = script.isVarArgs();
        int argCount = script.getArgCount();

        // Result
        Object[] argv = null;

        if (argCount == 0) {
            argv = EMPTY_PARAMS;
        } else if (argCount == 1) {
            argv = new Object[] {result};
        } else if (argCount == 2) {
            argv = new Object[] {result, value};
        } else if (argCount == 3) {
            argv = new Object[] {result, i, value};
        } else if (value instanceof Map.Entry<?,?>) {
            Map.Entry<?,?> entry = (Map.Entry<?,?>) value;
            argv = new Object[] {result, i, entry.getKey(), entry.getValue()};
        } else if (!varArgs && value instanceof Object[]) {
            int len = ((Object[]) value).length;
            if (argCount > len + 1) {
               argv = new Object[len + 2];
               argv[0] = result;
               argv[2] = i;
               System.arraycopy(value, 0, argv, 2, len);
            } else if (argCount == len + 1) {
               argv = new Object[len + 1];
               argv[0] = result;
               System.arraycopy(value, 0, argv, 1, len);
            } else {
               argv = new Object[] {result, i, value};
            }
        } else {
            argv = new Object[] {result, i, value};
        }
        return argv;
    }

    @Override
    protected Object visit(ASTPipeNode node, Object data) {
        JexlNode pipe = node.jjtGetChild(0);
        Object result = null;
        if (data instanceof Iterator<?>) {
            Iterator<?> itemsIterator = (Iterator) data;
            try {
                int i = 0;
                if (pipe instanceof ASTJexlLambda) {
                    ASTJexlLambda script = (ASTJexlLambda) pipe;
                    Closure closure = new Closure(this, script);

                    while (itemsIterator.hasNext()) {
                        Object value = itemsIterator.next();
                        Object prev = current;
                        try {
                            current = value;
                            result = closure.execute(null, preparePipeLambdaArgs(script, result, i, value));
                        } finally {
                            current = prev;
                        }
                        i += 1;
                    }
                } else {
                    while (itemsIterator.hasNext()) {
                        Object value = itemsIterator.next();
                        Object prev = current;
                        try {
                            current = value;
                            result = pipe.jjtAccept(this, null);
                        } finally {
                            current = prev;
                        }
                        i += 1;
                    }
                }
            } finally {
                closeIfSupported(itemsIterator);
            }
        } else if (data != null) {
            if (pipe instanceof ASTJexlLambda) {
                ASTJexlLambda script = (ASTJexlLambda) pipe;
                Closure closure = new Closure(this, script);
                Object prev = current;
                try {
                    current = data;
                    result = closure.execute(null, new Object[] {data});
                } finally {
                    current = prev;
                }
            } else {
                Object prev = current;
                try {
                    current = data;
                    result = pipe.jjtAccept(this, null);
                } finally {
                    current = prev;
                }
            }
        }
        return result;
    }

    protected Object createPredicate(JexlOperator operator, JexlNode node, boolean negate, boolean any, Object... operands) {
        return RelationalPredicate.create(arithmetic, operators, operator, node, negate, any, operands);
    }

}
