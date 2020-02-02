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
//CSOFF: FileLength
package org.apache.commons.jexl3.internal;

import java.util.Iterator;
import java.util.concurrent.Callable;

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
import org.apache.commons.jexl3.introspection.JexlPropertyGet;

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
import org.apache.commons.jexl3.parser.ASTBitwiseAndNode;
import org.apache.commons.jexl3.parser.ASTBitwiseComplNode;
import org.apache.commons.jexl3.parser.ASTBitwiseOrNode;
import org.apache.commons.jexl3.parser.ASTBitwiseXorNode;
import org.apache.commons.jexl3.parser.ASTBlock;
import org.apache.commons.jexl3.parser.ASTBreak;
import org.apache.commons.jexl3.parser.ASTCastNode;
import org.apache.commons.jexl3.parser.ASTCatchBlock;
import org.apache.commons.jexl3.parser.ASTClassLiteral;
import org.apache.commons.jexl3.parser.ASTConstructorNode;
import org.apache.commons.jexl3.parser.ASTContinue;
import org.apache.commons.jexl3.parser.ASTCurrentNode;
import org.apache.commons.jexl3.parser.ASTDecrementNode;
import org.apache.commons.jexl3.parser.ASTDecrementPostfixNode;
import org.apache.commons.jexl3.parser.ASTDivNode;
import org.apache.commons.jexl3.parser.ASTDoWhileStatement;
import org.apache.commons.jexl3.parser.ASTEQNode;
import org.apache.commons.jexl3.parser.ASTERNode;
import org.apache.commons.jexl3.parser.ASTEWNode;
import org.apache.commons.jexl3.parser.ASTElvisNode;
import org.apache.commons.jexl3.parser.ASTEmptyFunction;
import org.apache.commons.jexl3.parser.ASTEnumerationNode;
import org.apache.commons.jexl3.parser.ASTEnumerationReference;
import org.apache.commons.jexl3.parser.ASTExpressionStatement;
import org.apache.commons.jexl3.parser.ASTExtVar;
import org.apache.commons.jexl3.parser.ASTFalseNode;
import org.apache.commons.jexl3.parser.ASTForStatement;
import org.apache.commons.jexl3.parser.ASTForInitializationNode;
import org.apache.commons.jexl3.parser.ASTForTerminationNode;
import org.apache.commons.jexl3.parser.ASTForIncrementNode;
import org.apache.commons.jexl3.parser.ASTForeachStatement;
import org.apache.commons.jexl3.parser.ASTForeachVar;
import org.apache.commons.jexl3.parser.ASTFunctionNode;
import org.apache.commons.jexl3.parser.ASTFunctionStatement;
import org.apache.commons.jexl3.parser.ASTGENode;
import org.apache.commons.jexl3.parser.ASTGTNode;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTIdentifierAccess;
import org.apache.commons.jexl3.parser.ASTIdentifierAccessJxlt;
import org.apache.commons.jexl3.parser.ASTIncrementNode;
import org.apache.commons.jexl3.parser.ASTIncrementPostfixNode;
import org.apache.commons.jexl3.parser.ASTIndirectNode;
import org.apache.commons.jexl3.parser.ASTInitialization;
import org.apache.commons.jexl3.parser.ASTInitializedArrayConstructorNode;
import org.apache.commons.jexl3.parser.ASTInitializedCollectionConstructorNode;
import org.apache.commons.jexl3.parser.ASTInitializedMapConstructorNode;
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
import org.apache.commons.jexl3.parser.ASTLTNode;
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
import org.apache.commons.jexl3.parser.ASTMultipleInitialization;
import org.apache.commons.jexl3.parser.ASTMultiVar;
import org.apache.commons.jexl3.parser.ASTNEAssignment;
import org.apache.commons.jexl3.parser.ASTNENode;
import org.apache.commons.jexl3.parser.ASTNEWNode;
import org.apache.commons.jexl3.parser.ASTNINode;
import org.apache.commons.jexl3.parser.ASTNIOFNode;
import org.apache.commons.jexl3.parser.ASTNRNode;
import org.apache.commons.jexl3.parser.ASTNSWNode;
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
import org.apache.commons.jexl3.parser.ASTSelectionNode;
import org.apache.commons.jexl3.parser.ASTSetAddNode;
import org.apache.commons.jexl3.parser.ASTSetAndNode;
import org.apache.commons.jexl3.parser.ASTSetDivNode;
import org.apache.commons.jexl3.parser.ASTSetLiteral;
import org.apache.commons.jexl3.parser.ASTSetModNode;
import org.apache.commons.jexl3.parser.ASTSetMultNode;
import org.apache.commons.jexl3.parser.ASTSetOperand;
import org.apache.commons.jexl3.parser.ASTSetOrNode;
import org.apache.commons.jexl3.parser.ASTSetSubNode;
import org.apache.commons.jexl3.parser.ASTSetShlNode;
import org.apache.commons.jexl3.parser.ASTSetSarNode;
import org.apache.commons.jexl3.parser.ASTSetShrNode;
import org.apache.commons.jexl3.parser.ASTSetXorNode;
import org.apache.commons.jexl3.parser.ASTShiftLeftNode;
import org.apache.commons.jexl3.parser.ASTShiftRightNode;
import org.apache.commons.jexl3.parser.ASTShiftRightUnsignedNode;
import org.apache.commons.jexl3.parser.ASTSimpleLambda;
import org.apache.commons.jexl3.parser.ASTSizeFunction;
import org.apache.commons.jexl3.parser.ASTStartCountNode;
import org.apache.commons.jexl3.parser.ASTStopCountNode;
import org.apache.commons.jexl3.parser.ASTStringLiteral;
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
import org.apache.commons.jexl3.parser.ASTThisNode;
import org.apache.commons.jexl3.parser.ASTThrowStatement;
import org.apache.commons.jexl3.parser.ASTTrueNode;
import org.apache.commons.jexl3.parser.ASTTryStatement;
import org.apache.commons.jexl3.parser.ASTTryVar;
import org.apache.commons.jexl3.parser.ASTTryWithResourceStatement;
import org.apache.commons.jexl3.parser.ASTTryResource;
import org.apache.commons.jexl3.parser.ASTTypeLiteral;
import org.apache.commons.jexl3.parser.ASTUnaryMinusNode;
import org.apache.commons.jexl3.parser.ASTUnaryPlusNode;
import org.apache.commons.jexl3.parser.ASTUnderscoreLiteral;
import org.apache.commons.jexl3.parser.ASTVar;
import org.apache.commons.jexl3.parser.ASTWhileStatement;
import org.apache.commons.jexl3.parser.ASTYieldStatement;
import org.apache.commons.jexl3.parser.JexlNode;
import org.apache.commons.jexl3.parser.Node;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.AbstractMap;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

import java.lang.reflect.Array;
/**
 * An interpreter of JEXL syntax.
 *
 * @since 2.0
 */
public class Interpreter extends InterpreterBase {
    /** Frame height. */
    protected int fp = 0;
    /** Symbol values. */
    protected final Frame frame;
    /** Block micro-frames. */
    protected LexicalFrame block = null;
    /** Current evaluation target. */
    protected Object current = null;

    /**
     * The thread local interpreter.
     */
    protected static final java.lang.ThreadLocal<Interpreter> INTER =
                       new java.lang.ThreadLocal<Interpreter>();

    /**
     * Creates an interpreter.
     * @param engine   the engine creating this interpreter
     * @param aContext the evaluation context, global variables, methods and functions
     * @param opts     the evaluation options, flags modifying evaluation behavior
     * @param eFrame   the evaluation frame, arguments and local variables
     */
    protected Interpreter(Engine engine, JexlOptions opts, JexlContext aContext, Frame eFrame) {
        this(engine, opts, aContext, eFrame, null);
    }

    /**
     * Creates an interpreter.
     * @param engine   the engine creating this interpreter
     * @param aContext the evaluation context, global variables, methods and functions
     * @param opts     the evaluation options, flags modifying evaluation behavior
     * @param eFrame   the evaluation frame, arguments and local variables
     * @param current  the current evaluation object
     */
    protected Interpreter(Engine engine, JexlOptions opts, JexlContext aContext, Frame eFrame, Object current) {
        super(engine, opts, aContext);
        this.frame = eFrame;
        this.current = current;
    }

    /**
     * Copy constructor.
     * @param ii  the interpreter to copy
     * @param jexla the arithmetic instance to use (or null)
     */
    protected Interpreter(Interpreter ii, JexlArithmetic jexla) {
        super(ii, jexla);
        frame = ii.frame;
        block = ii.block != null? new LexicalFrame(ii.block) : null;
    }

    /**
     * Swaps the current thread local interpreter.
     * @param inter the interpreter or null
     * @return the previous thread local interpreter
     */
    protected Interpreter putThreadInterpreter(Interpreter inter) {
        Interpreter pinter = INTER.get();
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
    public Object interpret(JexlNode node) {
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
                throw new JexlException.StackOverflow(node.jexlInfo(), "jexl (" + jexl.stackOverflow + ")", null);
            }
            cancelCheck(node);
            return node.jjtAccept(this, null);
        } catch (StackOverflowError xstack) {
            JexlException xjexl = new JexlException.StackOverflow(node.jexlInfo(), "jvm", xstack);
            if (!isSilent()) {
                throw xjexl.clean();
            }
            if (logger.isWarnEnabled()) {
                logger.warn(xjexl.getMessage(), xjexl.getCause());
            }
        } catch (JexlException.Return xreturn) {
            return xreturn.getValue();
        } catch (JexlException.Yield xyield) {
            return xyield.getValue();
        } catch (JexlException.Cancel xcancel) {
            // cancelled |= Thread.interrupted();
            cancelled.weakCompareAndSet(false, Thread.interrupted());
            if (isCancellable()) {
                throw xcancel.clean();
            }
        } catch (JexlException xjexl) {
            if (!isSilent()) {
                throw xjexl.clean();
            }
            if (logger.isWarnEnabled()) {
                logger.warn(xjexl.getMessage(), xjexl.getCause());
            }
        } finally {
            synchronized(this) {
                if (functors != null) {
                    for (Object functor : functors.values()) {
                        closeIfSupported(functor);
                    }
                    functors.clear();
                    functors = null;
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
    public Object getAttribute(Object object, Object attribute) {
        return getAttribute(object, attribute, null);
    }

    /**
     * Sets an attribute of an object.
     *
     * @param object    to set the value to
     * @param attribute the attribute of the object, e.g. an index (1, 0, 2) or key for a map
     * @param value     the value to assign to the object's attribute
     */
    public void setAttribute(Object object, Object attribute, Object value) {
        setAttribute(object, attribute, value, null, JexlOperator.PROPERTY_SET);
    }

    @Override
    protected Object visit(ASTAddNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.ADD, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.add(left, right);
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "+ error", xrt);
        }
    }

    @Override
    protected Object visit(ASTSubNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.SUBTRACT, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.subtract(left, right);
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "- error", xrt);
        }
    }

    @Override
    protected Object visit(ASTMulNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.MULTIPLY, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.multiply(left, right);
        } catch (ArithmeticException xrt) {
            JexlNode xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, "* error", xrt);
        }
    }

    @Override
    protected Object visit(ASTDivNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.DIVIDE, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.divide(left, right);
        } catch (ArithmeticException xrt) {
            if (!arithmetic.isStrict()) {
                return 0.0d;
            }
            JexlNode xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, "/ error", xrt);
        }
    }

    @Override
    protected Object visit(ASTModNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.MOD, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.mod(left, right);
        } catch (ArithmeticException xrt) {
            if (!arithmetic.isStrict()) {
                return 0.0d;
            }
            JexlNode xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, "% error", xrt);
        }
    }

    @Override
    protected Object visit(ASTShiftLeftNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.SHL, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.leftShift(left, right);
        } catch (ArithmeticException xrt) {
            JexlNode xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, "<< error", xrt);
        }
    }

    @Override
    protected Object visit(ASTShiftRightNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.SAR, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.rightShift(left, right);
        } catch (ArithmeticException xrt) {
            JexlNode xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, ">> error", xrt);
        }
    }

    @Override
    protected Object visit(ASTShiftRightUnsignedNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.SHR, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.rightShiftUnsigned(left, right);
        } catch (ArithmeticException xrt) {
            JexlNode xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, ">>> error", xrt);
        }
    }

    @Override
    protected Object visit(ASTBitwiseAndNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.AND, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.and(left, right);
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "& error", xrt);
        }
    }

    @Override
    protected Object visit(ASTBitwiseOrNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.OR, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.or(left, right);
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "| error", xrt);
        }
    }

    @Override
    protected Object visit(ASTBitwiseXorNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.XOR, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.xor(left, right);
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "^ error", xrt);
        }
    }

    @Override
    protected Object visit(ASTISNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return left == right;
    }

    @Override
    protected Object visit(ASTNINode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
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
                            if (ok && any || !ok && !any)
                                return ok;
                        }
                    } finally {
                        closeIfSupported(it);
                    }
                }
            } else {
                Object right = child.jjtAccept(this, data);
                Boolean ok = p.test(right);
                if (ok && any || !ok && !any)
                    return ok;
            }
        }
        return !any;
    }

    @Override
    protected Object visit(ASTEQNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        JexlNode operand = node.jjtGetChild(1);
        if (operand instanceof ASTSetOperand) {
            return checkSetOperand(node, (ASTSetOperand) operand, left, data, (right) -> {
                try {
                    Object result = operators.tryOverload(node, JexlOperator.EQ, left, right);
                    return result != JexlEngine.TRY_FAILED
                           ? arithmetic.toBoolean(result) ? Boolean.TRUE : Boolean.FALSE
                           : arithmetic.equals(left, right) ? Boolean.TRUE : Boolean.FALSE;
                } catch (ArithmeticException xrt) {
                    throw new JexlException(node, "== error", xrt);
                }
            });
        } else {
            Object right = operand.jjtAccept(this, data);
            try {
                Object result = operators.tryOverload(node, JexlOperator.EQ, left, right);
                return result != JexlEngine.TRY_FAILED
                       ? arithmetic.toBoolean(result) ? Boolean.TRUE : Boolean.FALSE
                       : arithmetic.equals(left, right) ? Boolean.TRUE : Boolean.FALSE;
            } catch (ArithmeticException xrt) {
                throw new JexlException(node, "== error", xrt);
            }
        }
    }

    @Override
    protected Object visit(ASTNENode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        JexlNode operand = node.jjtGetChild(1);
        if (operand instanceof ASTSetOperand) {
            return checkSetOperand(node, (ASTSetOperand) operand, left, data, (right) -> {
                try {
                    Object result = operators.tryOverload(node, JexlOperator.EQ, left, right);
                    return result != JexlEngine.TRY_FAILED
                           ? arithmetic.toBoolean(result) ? Boolean.FALSE : Boolean.TRUE
                           : arithmetic.equals(left, right) ? Boolean.FALSE : Boolean.TRUE;
                } catch (ArithmeticException xrt) {
                    throw new JexlException(node, "!= error", xrt);
                }
            });
        } else {
            Object right = operand.jjtAccept(this, data);
            try {
                Object result = operators.tryOverload(node, JexlOperator.EQ, left, right);
                return result != JexlEngine.TRY_FAILED
                       ? arithmetic.toBoolean(result) ? Boolean.FALSE : Boolean.TRUE
                       : arithmetic.equals(left, right) ? Boolean.FALSE : Boolean.TRUE;
            } catch (ArithmeticException xrt) {
                JexlNode xnode = findNullOperand(xrt, node, left, right);
                throw new JexlException(xnode, "!= error", xrt);
            }
        }
    }

    @Override
    protected Object visit(ASTGENode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        JexlNode operand = node.jjtGetChild(1);
        if (operand instanceof ASTSetOperand) {
            return checkSetOperand(node, (ASTSetOperand) operand, left, data, (right) -> {
                try {
                    Object result = operators.tryOverload(node, JexlOperator.GTE, left, right);
                    return result != JexlEngine.TRY_FAILED
                           ? arithmetic.toBoolean(result) ? Boolean.TRUE : Boolean.FALSE
                           : arithmetic.greaterThanOrEqual(left, right) ? Boolean.TRUE : Boolean.FALSE;
                } catch (ArithmeticException xrt) {
                    throw new JexlException(node, ">= error", xrt);
                }
            });
        } else {
            Object right = operand.jjtAccept(this, data);
            try {
                Object result = operators.tryOverload(node, JexlOperator.GTE, left, right);
                return result != JexlEngine.TRY_FAILED
                       ? arithmetic.toBoolean(result) ? Boolean.TRUE : Boolean.FALSE
                       : arithmetic.greaterThanOrEqual(left, right) ? Boolean.TRUE : Boolean.FALSE;
            } catch (ArithmeticException xrt) {
                throw new JexlException(node, ">= error", xrt);
            }
        }
    }

    @Override
    protected Object visit(ASTGTNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        JexlNode operand = node.jjtGetChild(1);
        if (operand instanceof ASTSetOperand) {
            return checkSetOperand(node, (ASTSetOperand) operand, left, data, (right) -> {
                try {
                    Object result = operators.tryOverload(node, JexlOperator.GT, left, right);
                    return result != JexlEngine.TRY_FAILED
                           ? arithmetic.toBoolean(result) ? Boolean.TRUE : Boolean.FALSE
                           : arithmetic.greaterThan(left, right) ? Boolean.TRUE : Boolean.FALSE;
                } catch (ArithmeticException xrt) {
                    throw new JexlException(node, "> error", xrt);
                }
            });
        } else {
            Object right = operand.jjtAccept(this, data);
            try {
                Object result = operators.tryOverload(node, JexlOperator.GT, left, right);
                return result != JexlEngine.TRY_FAILED
                       ? arithmetic.toBoolean(result) ? Boolean.TRUE : Boolean.FALSE
                       : arithmetic.greaterThan(left, right) ? Boolean.TRUE : Boolean.FALSE;
            } catch (ArithmeticException xrt) {
                throw new JexlException(node, "> error", xrt);
            }
        }
    }

    @Override
    protected Object visit(ASTLENode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        JexlNode operand = node.jjtGetChild(1);
        if (operand instanceof ASTSetOperand) {
            return checkSetOperand(node, (ASTSetOperand) operand, left, data, (right) -> {
                try {
                    Object result = operators.tryOverload(node, JexlOperator.LTE, left, right);
                    return result != JexlEngine.TRY_FAILED
                           ? arithmetic.toBoolean(result) ? Boolean.TRUE : Boolean.FALSE
                           : arithmetic.lessThanOrEqual(left, right) ? Boolean.TRUE : Boolean.FALSE;
                } catch (ArithmeticException xrt) {
                    throw new JexlException(node, "<= error", xrt);
                }
            });
        } else {
            Object right = operand.jjtAccept(this, data);
            try {
                Object result = operators.tryOverload(node, JexlOperator.LTE, left, right);
                return result != JexlEngine.TRY_FAILED
                       ? arithmetic.toBoolean(result) ? Boolean.TRUE : Boolean.FALSE
                       : arithmetic.lessThanOrEqual(left, right) ? Boolean.TRUE : Boolean.FALSE;
            } catch (ArithmeticException xrt) {
                throw new JexlException(node, "<= error", xrt);
            }
        }
    }

    @Override
    protected Object visit(ASTLTNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        JexlNode operand = node.jjtGetChild(1);
        if (operand instanceof ASTSetOperand) {
            return checkSetOperand(node, (ASTSetOperand) operand, left, data, (right) -> {
                try {
                    Object result = operators.tryOverload(node, JexlOperator.LT, left, right);
                    return result != JexlEngine.TRY_FAILED
                           ? arithmetic.toBoolean(result) ? Boolean.TRUE : Boolean.FALSE
                           : arithmetic.lessThan(left, right) ? Boolean.TRUE : Boolean.FALSE;
                } catch (ArithmeticException xrt) {
                    throw new JexlException(node, "< error", xrt);
                }
            });
        } else {
            Object right = operand.jjtAccept(this, data);
            try {
                Object result = operators.tryOverload(node, JexlOperator.LT, left, right);
                return result != JexlEngine.TRY_FAILED
                       ? arithmetic.toBoolean(result) ? Boolean.TRUE : Boolean.FALSE
                       : arithmetic.lessThan(left, right) ? Boolean.TRUE : Boolean.FALSE;
            } catch (ArithmeticException xrt) {
                throw new JexlException(node, "< error", xrt);
            }
        }
    }

    @Override
    protected Object visit(ASTSWNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        JexlNode operand = node.jjtGetChild(1);
        if (operand instanceof ASTSetOperand) {
            return checkSetOperand(node, (ASTSetOperand) operand, left, data, (right) -> {
                return operators.startsWith(node, "^=", left, right) ? Boolean.TRUE : Boolean.FALSE;
            });
        } else {
            Object right = operand.jjtAccept(this, data);
            return operators.startsWith(node, "^=", left, right) ? Boolean.TRUE : Boolean.FALSE;
        }
    }

    @Override
    protected Object visit(ASTNSWNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        JexlNode operand = node.jjtGetChild(1);
        if (operand instanceof ASTSetOperand) {
            return checkSetOperand(node, (ASTSetOperand) operand, left, data, (right) -> {
                return operators.startsWith(node, "^!", left, right) ? Boolean.FALSE : Boolean.TRUE;
            });
        } else {
            Object right = operand.jjtAccept(this, data);
            return operators.startsWith(node, "^!", left, right) ? Boolean.FALSE : Boolean.TRUE;
        }
    }

    @Override
    protected Object visit(ASTEWNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        JexlNode operand = node.jjtGetChild(1);
        if (operand instanceof ASTSetOperand) {
            return checkSetOperand(node, (ASTSetOperand) operand, left, data, (right) -> {
                return operators.endsWith(node, "$=", left, right) ? Boolean.TRUE : Boolean.FALSE;
            });
        } else {
            Object right = operand.jjtAccept(this, data);
            return operators.endsWith(node, "$=", left, right) ? Boolean.TRUE : Boolean.FALSE;
        }
    }

    @Override
    protected Object visit(ASTNEWNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        JexlNode operand = node.jjtGetChild(1);
        if (operand instanceof ASTSetOperand) {
            return checkSetOperand(node, (ASTSetOperand) operand, left, data, (right) -> {
                return operators.endsWith(node, "$!", left, right) ? Boolean.FALSE : Boolean.TRUE;
            });
        } else {
            Object right = operand.jjtAccept(this, data);
            return operators.endsWith(node, "$!", left, right) ? Boolean.FALSE : Boolean.TRUE;
        }
    }

    @Override
    protected Object visit(ASTERNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        JexlNode operand = node.jjtGetChild(1);
        if (operand instanceof ASTSetOperand) {
            return checkSetOperand(node, (ASTSetOperand) operand, left, data, (right) -> {
                return operators.contains(node, "=~", right, left) ? Boolean.TRUE : Boolean.FALSE;
            });
        } else {
            Object right = operand.jjtAccept(this, data);
            return operators.contains(node, "=~", right, left) ? Boolean.TRUE : Boolean.FALSE;
        }
    }

    @Override
    protected Object visit(ASTNRNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        JexlNode operand = node.jjtGetChild(1);
        if (operand instanceof ASTSetOperand) {
            return checkSetOperand(node, (ASTSetOperand) operand, left, data, (right) -> {
                return operators.contains(node, "!~", right, left) ? Boolean.FALSE : Boolean.TRUE;
            });
        } else {
            Object right = operand.jjtAccept(this, data);
            return operators.contains(node, "!~", right, left) ? Boolean.FALSE : Boolean.TRUE;
        }
    }

    @Override
    protected Object visit(ASTIOFNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
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
    protected Object visit(ASTNIOFNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
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
    protected Object visit(ASTRangeNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.createRange(left, right);
        } catch (ArithmeticException xrt) {
            JexlNode xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, ".. error", xrt);
        }
    }

    @Override
    protected Object visit(ASTUnaryMinusNode node, Object data) {
        // use cached value if literal
        Object value = node.jjtGetValue();
        if (value != null && !(value instanceof JexlMethod)) {
            return value;
        }
        JexlNode valNode = node.jjtGetChild(0);
        Object val = valNode.jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.NEGATE, val);
            if (result != JexlEngine.TRY_FAILED) {
                return result;
            }
            Object number = arithmetic.negate(val);
            // attempt to recoerce to literal class
            if ((number instanceof Number)) {
                // cache if number literal and negate is idempotent
                if (valNode instanceof ASTNumberLiteral) {
                    number = arithmetic.narrowNumber((Number) number, ((ASTNumberLiteral) valNode).getLiteralClass());
                    if (arithmetic.isNegateStable()) {
                        node.jjtSetValue(number);
                    }
                }
            }
            return number;
        } catch (ArithmeticException xrt) {
            throw new JexlException(valNode, "- error", xrt);
        }
    }

    @Override
    protected Object visit(ASTUnaryPlusNode node, Object data) {
        // use cached value if literal
        Object value = node.jjtGetValue();
        if (value != null && !(value instanceof JexlMethod)) {
            return value;
        }
        JexlNode valNode = node.jjtGetChild(0);
        Object val = valNode.jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.POSITIVIZE, val);
            if (result != JexlEngine.TRY_FAILED) {
                return result;
            }
            Object number = arithmetic.positivize(val);
            if (valNode instanceof ASTNumberLiteral
                && number instanceof Number
                && arithmetic.isPositivizeStable()) {
                node.jjtSetValue(number);
            }
            return number;
        } catch (ArithmeticException xrt) {
            throw new JexlException(valNode, "- error", xrt);
        }
    }

    @Override
    protected Object visit(ASTIndirectNode node, Object data) {
        Object val = node.jjtGetChild(0).jjtAccept(this, data);
        if (val == null) {
            if (isStrictEngine()) {
                throw new JexlException(node, "Null dereference", null);
            } else {
                return null;
            }
        }
        if (val instanceof GetPointer)
            return ((GetPointer) val).get();

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
    protected Object visit(ASTPointerNode node, Object data) {
        JexlNode left = node.jjtGetChild(0);
        if (left instanceof ASTIdentifier) {
            ASTIdentifier var = (ASTIdentifier) left;
            if (data != null) {
                return new PropertyPointer(var, data, var.getName());
            } else {
                int symbol = var.getSymbol();
                if (symbol >= 0) {
                    return var.isFinal() ? new FinalVarPointer(var) : new VarPointer(var);
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
                    throw new JexlException(objectNode, "illegal address");
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
                throw new JexlException(objectNode, "illegal pointer form");
            }
        }
    }

    @Override
    protected Object visit(ASTBitwiseComplNode node, Object data) {
        Object arg = node.jjtGetChild(0).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.COMPLEMENT, arg);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.complement(arg);
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "~ error", xrt);
        }
    }

    @Override
    protected Object visit(ASTNotNode node, Object data) {
        Object val = node.jjtGetChild(0).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.NOT, val);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.not(val);
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "! error", xrt);
        }
    }

    @Override
    protected Object visit(ASTCastNode node, Object data) {
        // Type
        ASTTypeLiteral type = (ASTTypeLiteral) node.jjtGetChild(0);
        Class c = type.getType();
        // Value
        Object val = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.cast(c, val);
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "cast error", xrt);
        }
    }

    @Override
    protected Object visit(ASTEnumerationReference node, Object data) {
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
    protected Object visit(ASTEnumerationNode node, Object data) {
        JexlNode valNode = node.jjtGetChild(0);
        if (valNode instanceof ASTSimpleLambda) {
            ASTJexlLambda generator = (ASTJexlLambda) valNode;
            return new GeneratorIterator(generator);
        } else {
            Object iterableValue = valNode.jjtAccept(this, data);
            if (iterableValue != null) {
                Object forEach = operators.tryOverload(node, JexlOperator.FOR_EACH_INDEXED, iterableValue);
                Iterator<?> itemsIterator = forEach instanceof Iterator ? (Iterator<?>) forEach : uberspect.getIndexedIterator(iterableValue);
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
            generator = jexl.createResumableInterpreter(context, scope, options);
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
            if (i == -1)
                prepareNextValue();
            return nextValue;
        }

        @Override
        public Object next() {
            cancelCheck(node);
            if (!hasNext())
                throw new NoSuchElementException();
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
    protected Object visit(ASTExpressionStatement node, Object data) {
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
    protected Object visit(ASTFunctionStatement node, Object data) {
        cancelCheck(node);
        // Declare variable
        JexlNode left = node.jjtGetChild(0);
        left.jjtAccept(this, data);
        // Create function
        Object right = Closure.create(this, (ASTJexlLambda) node.jjtGetChild(1));
        return executeAssign(node, left, right, null, data);
    }

    @Override
    protected Object visit(ASTIfStatement node, Object data) {
        cancelCheck(node);
        try {
            Object condition = node.jjtGetChild(0).jjtAccept(this, null);
            if (arithmetic.toBoolean(condition)) {
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
            throw new JexlException(node.jjtGetChild(0), "if error", xrt);
        }
    }

    /**
     * Base visitation for blocks.
     * @param node the block
     * @param data the usual data
     * @return the result of the last expression evaluation
     */
    private Object visitBlock(ASTBlock node, Object data) {
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
    protected Object visit(ASTBlock node, Object data) {
        int cnt = node.getSymbolCount();
        if (!options.isLexical() || cnt <= 0) {
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
    protected Object visit(ASTReturnStatement node, Object data) {
        cancelCheck(node);
        final int numChildren = node.jjtGetNumChildren();
        Object val = numChildren > 0 ? node.jjtGetChild(0).jjtAccept(this, data) : null;
        throw new JexlException.Return(node, null, val);
    }

    @Override
    protected Object visit(ASTYieldStatement node, Object data) {
        cancelCheck(node);
        Object val = node.jjtGetChild(0).jjtAccept(this, data);
        throw new JexlException.Yield(node, null, val);
    }

    @Override
    protected Object visit(ASTContinue node, Object data) {
        cancelCheck(node);
        throw new JexlException.Continue(node, node.getLabel());
    }

    @Override
    protected Object visit(ASTRemove node, Object data) {
        cancelCheck(node);
        throw new JexlException.Remove(node, node.getLabel());
    }

    @Override
    protected Object visit(ASTBreak node, Object data) {
        cancelCheck(node);
        throw new JexlException.Break(node, node.getLabel());
    }

    @Override
    protected Object visit(ASTForStatement node, Object data) {
        cancelCheck(node);
        final boolean lexical = options.isLexical();
        if (lexical) {
              // create lexical frame
              block = new LexicalFrame(frame, block);
        }
        try {
            final int numChildren = node.jjtGetNumChildren();
            // Initialize for-loop
            Object result = node.jjtGetChild(0).jjtAccept(this, data);
            boolean when = false;
            /* third objectNode is the statement to execute */
            JexlNode statement = node.jjtGetNumChildren() > 3 ? node.jjtGetChild(3) : null;
            while (when = (Boolean) node.jjtGetChild(1).jjtAccept(this, data)) {
                cancelCheck(node);
                // Execute loop body
                if (statement != null) {
                    try {
                        result = statement.jjtAccept(this, data);
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
                result = node.jjtGetChild(2).jjtAccept(this, data);
            }
            return result;
        } finally {
              // restore lexical frame
              if (lexical)
                  block = block.pop();
        }
    }

    @Override
    protected Object visit(ASTForInitializationNode node, Object data) {
        Object result = null;
        final int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            cancelCheck(node);
            result = node.jjtGetChild(i).jjtAccept(this, data);
        }
        return result;
    }

    @Override
    protected Object visit(ASTForTerminationNode node, Object data) {
        if (node.jjtGetNumChildren() > 0) {
            return arithmetic.toBoolean(node.jjtGetChild(0).jjtAccept(this, data));
        } else {
            return Boolean.TRUE;
        }
    }

    @Override
    protected Object visit(ASTForIncrementNode node, Object data) {
        Object result = null;
        final int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            cancelCheck(node);
            result = node.jjtGetChild(i).jjtAccept(this, data);
        }
        return result;
    }

    @Override
    protected Object visit(ASTForeachStatement node, Object data) {
        cancelCheck(node);
        Object result = null;
        /* first objectNode is the loop variable */
        ASTForeachVar loopReference = (ASTForeachVar) node.jjtGetChild(0);
        ASTIdentifier loopVariable = (ASTIdentifier) loopReference.jjtGetChild(0);
        ASTIdentifier loopValueVariable = loopReference.jjtGetNumChildren() > 1 ? (ASTIdentifier) loopReference.jjtGetChild(1) : null;
        final boolean lexical = options.isLexical();// && node.getSymbolCount() > 0;
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
            Object iterableValue = node.jjtGetChild(1).jjtAccept(this, data);
            Iterator<?> itemsIterator = null;
            // make sure there is a value to iterate on
            if (iterableValue != null) {
                if (loopValueVariable != null) {
                    // get an iterator for the collection/array etc via the introspector.
                    Object forEach = operators.tryOverload(node, JexlOperator.FOR_EACH_INDEXED, iterableValue);
                    itemsIterator = forEach instanceof Iterator ? (Iterator<?>) forEach : uberspect.getIndexedIterator(iterableValue);
                } else {
                    // get an iterator for the collection/array etc via the introspector.
                    Object forEach = operators.tryOverload(node, JexlOperator.FOR_EACH, iterableValue);
                    itemsIterator = forEach instanceof Iterator ? (Iterator<?>) forEach : uberspect.getIterator(iterableValue);
                }
            }
            if (itemsIterator != null) {
                int i = -1;
                try {
                    /* third objectNode is the statement to execute */
                    JexlNode statement = node.jjtGetNumChildren() >= 3 ? node.jjtGetChild(2) : null;
                    while (itemsIterator.hasNext()) {
                        cancelCheck(node);
                        i += 1;
                        // set loopVariable to value of iterator
                        Object value = itemsIterator.next();
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
                                // continue
                            } catch (JexlException.Remove stmtRemove) {
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
            if (lexical)
                block = block.pop();
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
            if (t instanceof JexlException && t.getCause() instanceof ArithmeticException) {
                ex = t.getCause();
            }
            for (int i = 1; i < num; i++) {
                JexlNode cb = node.jjtGetChild(i);
                if (cb instanceof ASTCatchBlock) {
                    JexlNode catchVariable = (JexlNode) cb.jjtGetChild(0);
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
                if (!(fb instanceof ASTCatchBlock))
                    fb.jjtAccept(this, data);
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
            if (JexlEngine.TRY_FAILED != rman)
                r = rman;

            ASTTryVar resDeclaration = resReference.jjtGetChild(0) instanceof ASTTryVar ? (ASTTryVar) resReference.jjtGetChild(0) : null;
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
                    result = node.jjtGetChild(1).jjtAccept(this, data);
                }
            } finally {
                // restore lexical frame
                if (lexical)
                    block = block.pop();
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
            if (t instanceof JexlException && t.getCause() instanceof ArithmeticException) {
                ex = t.getCause();
            }
            for (int i = 2; i < num; i++) {
                JexlNode cb = node.jjtGetChild(i);
                if (cb instanceof ASTCatchBlock) {
                    JexlNode catchVariable = (JexlNode) cb.jjtGetChild(0);
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
                if (!(fb instanceof ASTCatchBlock))
                    fb.jjtAccept(this, data);
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
        ASTIdentifier catchVariable = (ASTIdentifier) node.jjtGetChild(0);
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
            if (lexical)
                block = block.pop();
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
        Object val = node.jjtGetChild(0).jjtAccept(this, data);
        if (val instanceof Throwable)
            InterpreterBase.<RuntimeException>doThrow((Throwable) val);
        String message = arithmetic.toString(val);
        if (message != null) {
            throw new RuntimeException(message);
        } else {
            throw new RuntimeException();
        }
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
    protected Object visit(ASTWhileStatement node, Object data) {
        cancelCheck(node);
        Object result = null;
        /* first objectNode is the condition */
        Node condition = node.jjtGetChild(0);
        while (arithmetic.toBoolean(condition.jjtAccept(this, data))) {
            cancelCheck(node);
            if (node.jjtGetNumChildren() > 1) {
                try {
                    // execute statement
                    result = node.jjtGetChild(1).jjtAccept(this, data);
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
                    // continue
                }
            }
        }
        return result;
    }

    @Override
    protected Object visit(ASTDoWhileStatement node, Object data) {
        Object result = null;
        /* last objectNode is the expression */
        Node expressionNode = node.jjtGetChild(node.jjtGetNumChildren() - 1);
        do {
            cancelCheck(node);
            // execute statement
            if (node.jjtGetNumChildren() > 1) {
                try {
                    result = node.jjtGetChild(0).jjtAccept(this, data);
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
                    // continue
                }
            }
        } while (arithmetic.toBoolean(expressionNode.jjtAccept(this, data)));

        return result;
    }


    @Override
    protected Object visit(ASTSynchronizedStatement node, Object data) {
        cancelCheck(node);
        Object result = null;
        /* first objectNode is the synchronization expression */
        Node expressionNode = node.jjtGetChild(0);
        try {
            synchronized (expressionNode.jjtAccept(this, data)) {
                // execute statement
                if (node.jjtGetNumChildren() > 1)
                    result = node.jjtGetChild(1).jjtAccept(this, data);
            }
        } catch (JexlException.Break stmtBreak) {
            String target = stmtBreak.getLabel();
            if (target == null || !target.equals(node.getLabel())) {
                throw stmtBreak;
            }
            // break
        }
        return result;
    }

    @Override
    protected Object visit(ASTSwitchStatement node, Object data) {
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
                        // check all labels
                        for (int j = 0; j < labels.jjtGetNumChildren(); j++) {
                            JexlNode label = labels.jjtGetChild(j);
                            Object right = label instanceof ASTIdentifier ? label.jjtAccept(this, scope) : label.jjtAccept(this, data);
                            try {
                                Object caseMatched = operators.tryOverload(child, JexlOperator.EQ, left, right);
                                if (caseMatched == JexlEngine.TRY_FAILED)
                                    caseMatched = arithmetic.equals(left, right) ? Boolean.TRUE : Boolean.FALSE;
                                matched = arithmetic.toBoolean(caseMatched);
                            } catch (ArithmeticException xrt) {
                                throw new JexlException(node, "== error", xrt);
                            }
                            if (matched) {
                                start = i;
                                break l;
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
                        }
                    }
                }
                // execute all cases starting from matched one
                if (matched) {
                    for (int i = start; i < childCount; i++) {
                        JexlNode child = node.jjtGetChild(i);
                        result = child.jjtAccept(this, data);
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
            if (lexical)
                block = block.pop();
        }
    }

    @Override
    protected Object visit(ASTSwitchStatementCase node, Object data) {
        Object result = null;
        final int childCount = node.jjtGetNumChildren();
        for (int i = 1; i < childCount; i++) {
            cancelCheck(node);
            result = node.jjtGetChild(i).jjtAccept(this, data);
        }
        return result;
    }

    @Override
    protected Object visit(ASTSwitchStatementDefault node, Object data) {
        Object result = null;
        final int childCount = node.jjtGetNumChildren();
        for (int i = 0; i < childCount; i++) {
            cancelCheck(node);
            result = node.jjtGetChild(i).jjtAccept(this, data);
        }
        return result;
    }

    @Override
    protected Object visit(ASTSwitchExpression node, Object data) {
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
                    boolean matched = false;
                    // check all labels
                    for (int j = 0; j < labels.jjtGetNumChildren(); j++) {
                        JexlNode label = labels.jjtGetChild(j);
                        Object right = label instanceof ASTIdentifier ? label.jjtAccept(this, scope) : label.jjtAccept(this, data);
                        try {
                            Object caseMatched = operators.tryOverload(child, JexlOperator.EQ, left, right);
                            if (caseMatched == JexlEngine.TRY_FAILED)
                                caseMatched = arithmetic.equals(left, right) ? Boolean.TRUE : Boolean.FALSE;
                            matched = arithmetic.toBoolean(caseMatched);
                        } catch (ArithmeticException xrt) {
                            throw new JexlException(node, "== error", xrt);
                        }
                        if (matched) {
                            return child.jjtAccept(this, data);
                        }
                    }
                }
            }
            // otherwise jump to default case
            for (int i = 1; i < childCount; i++) {
                JexlNode child = node.jjtGetChild(i);
                if (child instanceof ASTSwitchExpressionDefault) {
                    return child.jjtAccept(this, data);
                }
            }
            return null;
        } catch (JexlException.Yield stmtYield) {
            return stmtYield.getValue();
        }
    }

    @Override
    protected Object visit(ASTSwitchExpressionCase node, Object data) {
        Object result = null;
        int childCount = node.jjtGetNumChildren();
        if (childCount > 1)
            result = node.jjtGetChild(1).jjtAccept(this, data);
        return result;
    }

    @Override
    protected Object visit(ASTSwitchCaseLabel node, Object data) {
        return null;
    }

    @Override
    protected Object visit(ASTSwitchExpressionDefault node, Object data) {
        Object result = null;
        int childCount = node.jjtGetNumChildren();
        if (childCount > 0)
            result = node.jjtGetChild(0).jjtAccept(this, data);
        return result;
    }

    @Override
    protected Object visit(ASTAndNode node, Object data) {
        /**
         * The pattern for exception mgmt is to let the child*.jjtAccept out of the try/catch loop so that if one fails,
         * the ex will traverse up to the interpreter. In cases where this is not convenient/possible, JexlException
         * must be caught explicitly and rethrown.
         */
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        try {
            boolean leftValue = arithmetic.toBoolean(left);
            if (!leftValue) {
                return Boolean.FALSE;
            }
        } catch (ArithmeticException xrt) {
            throw new JexlException(node.jjtGetChild(0), "boolean coercion error", xrt);
        }
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            boolean rightValue = arithmetic.toBoolean(right);
            if (!rightValue) {
                return Boolean.FALSE;
            }
        } catch (ArithmeticException xrt) {
            throw new JexlException(node.jjtGetChild(1), "boolean coercion error", xrt);
        }
        return Boolean.TRUE;
    }

    @Override
    protected Object visit(ASTOrNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        try {
            boolean leftValue = arithmetic.toBoolean(left);
            if (leftValue) {
                return Boolean.TRUE;
            }
        } catch (ArithmeticException xrt) {
            throw new JexlException(node.jjtGetChild(0), "boolean coercion error", xrt);
        }
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            boolean rightValue = arithmetic.toBoolean(right);
            if (rightValue) {
                return Boolean.TRUE;
            }
        } catch (ArithmeticException xrt) {
            throw new JexlException(node.jjtGetChild(1), "boolean coercion error", xrt);
        }
        return Boolean.FALSE;
    }

    @Override
    protected Object visit(ASTUnderscoreLiteral node, Object data) {
        return null;
    }

    @Override
    protected Object visit(ASTNullLiteral node, Object data) {
        return null;
    }

    @Override
    protected Object visit(ASTThisNode node, Object data) {
        return context;
    }

    @Override
    protected Object visit(ASTCurrentNode node, Object data) {
        return current;
    }

    @Override
    protected Object visit(ASTTrueNode node, Object data) {
        return Boolean.TRUE;
    }

    @Override
    protected Object visit(ASTFalseNode node, Object data) {
        return Boolean.FALSE;
    }

    @Override
    protected Object visit(ASTNumberLiteral node, Object data) {
        if (data != null && node.isInteger()) {
            return getAttribute(data, node.getLiteral(), node);
        }
        return node.getLiteral();
    }

    @Override
    protected Object visit(ASTStringLiteral node, Object data) {
        if (data != null) {
            return getAttribute(data, node.getLiteral(), node);
        }
        return node.getLiteral();
    }

    @Override
    protected Object visit(ASTRegexLiteral node, Object data) {
        return node.getLiteral();
    }

    @Override
    protected Object visit(ASTClassLiteral node, Object data) {
        return node.getLiteral();
    }

    @Override
    protected Object visit(ASTTypeLiteral node, Object data) {
        return node.getType();
    }

    @Override
    protected Object visit(ASTArrayLiteral node, Object data) {
        int childCount = node.jjtGetNumChildren();
        JexlArithmetic.ArrayBuilder ab = arithmetic.arrayBuilder(childCount);
        boolean extended = node.isExtended();
        boolean immutable = node.isImmutable();
        final boolean cacheable = cache && immutable && node.isConstant();
        Object cached = cacheable ? node.jjtGetValue() : null;
        if (cached != null)
            return cached;

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
            if (result instanceof List<?>)
                result = Collections.unmodifiableList((List<?>) result);
            if (cacheable)
                node.jjtSetValue(result);
            return result;
        } else {
            return ab.create(extended);
        }
    }

    @Override
    protected Object visit(ASTSetOperand node, Object data) {
        return null;
    }

    @Override
    protected Object visit(ASTSetLiteral node, Object data) {
        boolean immutable = node.isImmutable();
        final boolean cacheable = cache && immutable && node.isConstant();
        Object cached = cacheable ? node.jjtGetValue() : null;
        if (cached != null)
            return cached;
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
            if (result instanceof Set<?>)
                result = Collections.unmodifiableSet((Set<?>) result);
            if (cacheable)
                node.jjtSetValue(result);
        }
        return result;
    }

    @Override
    protected Object visit(ASTMapLiteral node, Object data) {
        boolean immutable = node.isImmutable();
        final boolean cacheable = cache && immutable && node.isConstant();
        Object cached = cacheable ? node.jjtGetValue() : null;
        if (cached != null)
            return cached;
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
                Iterator<Object> it = (Iterator<Object>) (child).jjtAccept(this, data);
                int j = 0;
                if (it != null) {
                    try {
                        while (it.hasNext()) {
                            Object value = it.next();
                            if (value instanceof Map.Entry<?,?>) {
                                Map.Entry<?,?> entry = (Map.Entry<?,?>) value;
                                mb.put(entry.getKey(), entry.getValue());
                            } else {
                                mb.put(i, value);
                            }
                            i++;
                        }
                    } finally {
                        closeIfSupported(it);
                    }
                }
            }
        }
        if (immutable) {
            Object result = mb.create();
            if (result instanceof Map<?,?>)
                result = Collections.unmodifiableMap((Map<?,?>) result);
            if (cacheable)
                node.jjtSetValue(result);
            return result;
        } else {
            return mb.create();
        }
    }

    @Override
    protected Object visit(ASTMapEntry node, Object data) {
        Object key = node.jjtGetChild(0).jjtAccept(this, data);
        Object value = node.jjtGetChild(1).jjtAccept(this, data);
        return new Object[]{key, value};
    }

    @Override
    protected Object visit(ASTMapEntryLiteral node, Object data) {
        Object key = node.jjtGetChild(0).jjtAccept(this, data);
        Object value = node.jjtGetChild(1).jjtAccept(this, data);

        return arithmetic.createMapEntry(key, value);
    }

    @Override
    protected Object visit(ASTMapEnumerationNode node, Object data) {
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
    protected Object visit(ASTInlinePropertyAssignment node, Object data) {
        Object prev = current;
        current = data;
        try {
            int childCount = node.jjtGetNumChildren();
            for (int i = 0; i < childCount; i++) {
                cancelCheck(node);

                JexlNode p = node.jjtGetChild(i);

                if (p instanceof ASTInlinePropertyEntry) {

                   Object[] entry = (Object[]) p.jjtAccept(this, null);

                   String name = String.valueOf(entry[0]);
                   Object value = entry[1];

                   setAttribute(data, name, value, p, JexlOperator.PROPERTY_SET);

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

                } else if (p instanceof ASTInlinePropertyArrayNullEntry) {

                   ASTInlinePropertyArrayNullEntry entry = (ASTInlinePropertyArrayNullEntry) p;

                   Object key = entry.jjtGetChild(0).jjtAccept(this, null);
                   Object value = getAttribute(data, key, p);

                   if (value == null) {
                      value = entry.jjtAccept(this, null);
                      setAttribute(data, key, value, p, JexlOperator.ARRAY_SET);
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
    protected Object visit(ASTInlinePropertyArrayEntry node, Object data) {

        Object key = node.jjtGetChild(0).jjtAccept(this, data);
        Object value = node.jjtGetChild(1).jjtAccept(this, data);

        return new Object[] {key, value};
    }

    @Override
    protected Object visit(ASTInlinePropertyEntry node, Object data) {
        JexlNode name = node.jjtGetChild(0);

        Object key = name instanceof ASTIdentifier ? ((ASTIdentifier) name).getName() : name.jjtAccept(this, data);
        Object value = node.jjtGetChild(1).jjtAccept(this, data);

        return new Object[] {key, value};
    }

    @Override
    protected Object visit(ASTInlinePropertyNullEntry node, Object data) {
        return node.jjtGetChild(1).jjtAccept(this, data);
    }

    @Override
    protected Object visit(ASTInlinePropertyNEEntry node, Object data) {
        return node.jjtGetChild(1).jjtAccept(this, data);
    }

    @Override
    protected Object visit(ASTInlinePropertyArrayNullEntry node, Object data) {
        return node.jjtGetChild(1).jjtAccept(this, data);
    }

    @Override
    protected Object visit(ASTInlinePropertyArrayNEEntry node, Object data) {
        return node.jjtGetChild(1).jjtAccept(this, data);
    }

    @Override
    protected Object visit(ASTTernaryNode node, Object data) {
        Object condition;
        try {
            condition = node.jjtGetChild(0).jjtAccept(this, data);
        } catch(JexlException xany) {
            if (!(xany.getCause() instanceof JexlArithmetic.NullOperand)) {
                throw xany;
            }
            condition = null;
        }
        // ternary as in "x ? y : z"
        if (condition != null && arithmetic.toBoolean(condition))
            return node.jjtGetChild(1).jjtAccept(this, data);
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
    protected Object visit(ASTNullpNode node, Object data) {
        Object lhs;
        try {
            lhs = node.jjtGetChild(0).jjtAccept(this, data);
        } catch(JexlException xany) {
            if (!(xany.getCause() instanceof JexlArithmetic.NullOperand)) {
                throw xany;
            }
            lhs = null;
        }
        // null elision as in "x ?? z"
        return lhs != null? lhs : node.jjtGetChild(1).jjtAccept(this, data);
    }

    @Override
    protected Object visit(ASTSizeFunction node, Object data) {
        try {
            Object val = node.jjtGetChild(0).jjtAccept(this, data);
            return operators.size(node, val);
        } catch(JexlException xany) {
            return 0;
        }
    }

    @Override
    protected Object visit(ASTEmptyFunction node, Object data) {
        try {
            Object value = node.jjtGetChild(0).jjtAccept(this, data);
            return operators.empty(node, value);
        } catch(JexlException xany) {
            return true;
        }
    }

    /**
     * Runs a node.
     * @param node the node
     * @param data the usual data
     * @return the return value
     */
    protected Object visitLexicalNode(JexlNode node, Object data) {
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
    protected Object runClosure(Closure closure, Object data) {
        ASTJexlScript script = closure.getScript();
        block = new LexicalFrame(frame, block).defineArgs();
        try {
            JexlNode body = script.jjtGetChild(script.jjtGetNumChildren() - 1);
            return interpret(body);
        } finally {
            block = block.pop();
        }
    }

    @Override
    protected Object visit(ASTJexlScript script, Object data) {
        if (script instanceof ASTJexlLambda && !((ASTJexlLambda) script).isTopLevel()) {
            return Closure.create(this, (ASTJexlLambda) script);
        } else {
            block = new LexicalFrame(frame, block).defineArgs();
            try {
                final int numChildren = script.jjtGetNumChildren();
                Object result = null;
                for (int i = 0; i < numChildren; i++) {
                    JexlNode child = script.jjtGetChild(i);
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
    protected Object visit(ASTEnclosedExpression node, Object data) {
        try {
            return node.jjtGetChild(0).jjtAccept(this, data);
        } catch (JexlException.Yield stmtYield) {
            return stmtYield.getValue();
        }
    }

    @Override
    protected Object visit(ASTVar node, Object data) {
        int symbol = node.getSymbol();
        if (options.isLexical() && !defineVariable(node, block)) {
            return redefinedVariable(node, node.getName());
        }
        // if we have a var, we have a scope thus a frame
        boolean isFinal = block.isVariableFinal(symbol);
        if (isFinal) {
            throw new JexlException(node, "can not redefine a final variable: " + node.getName());
        }
        // Adjust frame variable modifiers
        block.setModifiers(symbol, node.getType(), node.isFinal(), node.isRequired());
        // if we have a var, we have a scope thus a frame
        if (options.isLexical() || !frame.has(symbol)) {
            frame.set(symbol, null);
            return null;
        } else {
            return frame.get(symbol);
        }
    }

    @Override
    protected Object visit(ASTExtVar node, Object data) {
        return visit((ASTVar) node, data);
    }

    @Override
    protected Object visit(ASTMultiVar node, Object data) {
        return visit((ASTVar) node, data);
    }

    @Override
    protected Object visit(ASTIdentifier identifier, Object data) {
        cancelCheck(identifier);
        return data != null
                ? getAttribute(data, identifier.getName(), identifier)
                : getVariable(frame, block, identifier);
    }

    @Override
    protected Object visit(ASTArrayAccess node, Object data) {
        // first objectNode is the identifier
        Object object = data;
        // can have multiple nodes - either an expression, integer literal or reference
        int numChildren = node.jjtGetNumChildren();
        for (int i = 0; i < numChildren; i++) {
            JexlNode nindex = node.jjtGetChild(i);
            if (object == null) {
                return unsolvableProperty(nindex, stringifyProperty(nindex), false, null);
            }
            Object index = nindex.jjtAccept(this, null);
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
    private Object evalIdentifier(ASTIdentifierAccess node) {
        if (node instanceof ASTIdentifierAccessJxlt) {
            final ASTIdentifierAccessJxlt accessJxlt = (ASTIdentifierAccessJxlt) node;
            final String src = node.getName();
            Throwable cause = null;
            TemplateEngine.TemplateExpression expr = (TemplateEngine.TemplateExpression) accessJxlt.getExpression();
            try {
                if (expr == null) {
                    TemplateEngine jxlt = jexl.jxlt();
                    expr = jxlt.parseExpression(node.jexlInfo(), src, frame != null ? frame.getScope() : null);
                    accessJxlt.setExpression(expr);
                }
                if (expr != null) {
                    Object name = expr.evaluate(frame, context);
                    if (name != null) {
                        Integer id = ASTIdentifierAccess.parseIdentifier(name.toString());
                        return id != null ? id : name;
                    }
                }
            } catch (JxltEngine.Exception xjxlt) {
                cause = xjxlt;
            }
            return node.isSafe() ? null : unsolvableProperty(node, src, true, cause);
        } else {
            return node.getIdentifier();
        }
    }

    @Override
    protected Object visit(ASTIdentifierAccess node, Object data) {
        if (data == null) {
            return null;
        }
        Object id = evalIdentifier(node);
        return getAttribute(data, id, node);
    }

    @Override
    protected Object visit(ASTAttributeReference node, Object data) {
        if (data == null) {
            return null;
        }
        Object id = node.getName();
        return getAttribute(data, id, node);
    }

    @Override
    protected Object visit(final ASTReference node, Object data) {
        cancelCheck(node);
        final int numChildren = node.jjtGetNumChildren();
        final JexlNode parent = node.jjtGetParent();
        // pass first piece of data in and loop through children
        Object object = data;
        JexlNode objectNode = null;
        JexlNode ptyNode = null;
        StringBuilder ant = null;
        boolean antish = !(parent instanceof ASTReference);
        int v = 1;
        main:
        for (int c = 0; c < numChildren; c++) {
            objectNode = node.jjtGetChild(c);
            if (objectNode instanceof ASTMethodNode) {
                antish = false;
                if (object == null) {
                    // we may be performing a method call on an antish var
                    if (ant != null) {
                        JexlNode child = objectNode.jjtGetChild(0);
                        if (child instanceof ASTIdentifierAccess) {
                            int alen = ant.length();
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
                    JexlNode first = node.jjtGetChild(0);
                    if (first instanceof ASTIdentifier) {
                        ASTIdentifier afirst = (ASTIdentifier) first;
                        ant = new StringBuilder(afirst.getName());
                        // skip the else...*
                    } else {
                        // not an identifier, not antish
                        ptyNode = objectNode;
                        break main;
                    }
                    // *... and continue
                    if (!options.isAntish()) {
                        antish = false;
                        continue;
                    }
                    // skip the first node case since it was trialed in jjtAccept above and returned null
                    if (c == 0) {
                        continue;
                    }
                }
                // catch up to current node
                for (; v <= c; ++v) {
                    JexlNode child = node.jjtGetChild(v);
                    if (child instanceof ASTIdentifierAccess) {
                        ASTIdentifierAccess achild = (ASTIdentifierAccess) child;
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
                ptyNode = objectNode;
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
                    String aname = ant.toString();
                    boolean undefined = !(context.has(aname));
                    return unsolvableVariable(node, aname, undefined);
                }
                return unsolvableProperty(node,
                        stringifyProperty(ptyNode), ptyNode == objectNode, null);
            }
            if (antish) {
                if (node.isSafeLhs(isSafe())) {
                    return null;
                }
                String aname = ant != null ? ant.toString() : "?";
                boolean defined = context.has(aname);
                if (defined && !arithmetic.isStrict()) {
                    return null;
                }
                if (!defined || !(node.jjtGetParent() instanceof ASTExpressionStatement)) {
                    return unsolvableVariable(node, aname, !defined);
                }
            }
        }
        return object;
    }

    @Override
    protected Object visit(ASTMultipleIdentifier node, Object data) {
        return null;
    }

    @Override
    protected Object visit(ASTMultipleAssignment node, Object data) {
        cancelCheck(node);
        // Vector of identifiers to assign values to
        JexlNode identifiers = node.jjtGetChild(0);
        // Assignable values
        Object assignableValue = node.jjtGetChild(1).jjtAccept(this, data);
        return executeMultipleAssign(node, identifiers, assignableValue, data);
    }

    @Override
    protected Object visit(ASTMultipleInitialization node, Object data) {
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
    protected Object executeMultipleAssign(JexlNode node, JexlNode identifiers, Object value, Object data) { // CSOFF: MethodLength
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
                        if (i == num)
                            break;
                        // The value to assign
                        Object right = itemsIterator.next();
                        // The identifier to assign to
                        JexlNode left = identifiers.jjtGetChild(i);
                        if (left instanceof ASTIdentifier)
                            result = executeAssign(left, left, right, null, data);
                    }
                    while (i + 1 < num) {
                        JexlNode left = identifiers.jjtGetChild(++i);
                        if (left instanceof ASTIdentifier)
                            result = executeAssign(left, left, null, null, data);
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
                if (left instanceof ASTIdentifier)
                    result = executeAssign(left, left, null, null, data);
            }
        }
        return result;
    }

    @Override
    protected Object visit(ASTInitialization node, Object data) {
        cancelCheck(node);
        JexlNode left = node.jjtGetChild(0);
        if (node.jjtGetNumChildren() == 2) {
           // First evaluate the right part
           Object right = node.jjtGetChild(1).jjtAccept(this, data);
           // Then declare variable
           left.jjtAccept(this, data);
           // Initialize variable
           return executeAssign(node, left, right, null, data);
        } else {
           // Just declare variable
           return left.jjtAccept(this, data);
        }
    }

    @Override
    protected Object visit(ASTAssignment node, Object data) {
        JexlNode left = node.jjtGetChild(0);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return executeAssign(node, left, right, null, data);
    }

    @Override
    protected Object visit(ASTNullAssignment node, Object data) {
        JexlNode left = node.jjtGetChild(0);
        Object value = left.jjtAccept(this, data);
        if (value != null) {
             return value;
        }
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return executeAssign(node, left, right, null, data);
    }

    @Override
    protected Object visit(ASTNEAssignment node, Object data) {
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
    protected Object visit(ASTSetAddNode node, Object data) {
        JexlNode left = node.jjtGetChild(0);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return executeAssign(node, left, right, JexlOperator.SELF_ADD, data);
    }

    @Override
    protected Object visit(ASTSetSubNode node, Object data) {
        JexlNode left = node.jjtGetChild(0);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return executeAssign(node, left, right, JexlOperator.SELF_SUBTRACT, data);
    }

    @Override
    protected Object visit(ASTSetMultNode node, Object data) {
        JexlNode left = node.jjtGetChild(0);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return executeAssign(node, left, right, JexlOperator.SELF_MULTIPLY, data);
    }

    @Override
    protected Object visit(ASTSetDivNode node, Object data) {
        JexlNode left = node.jjtGetChild(0);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return executeAssign(node, left, right, JexlOperator.SELF_DIVIDE, data);
    }

    @Override
    protected Object visit(ASTSetModNode node, Object data) {
        JexlNode left = node.jjtGetChild(0);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return executeAssign(node, left, right, JexlOperator.SELF_MOD, data);
    }

    @Override
    protected Object visit(ASTSetAndNode node, Object data) {
        JexlNode left = node.jjtGetChild(0);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return executeAssign(node, left, right, JexlOperator.SELF_AND, data);
    }

    @Override
    protected Object visit(ASTSetOrNode node, Object data) {
        JexlNode left = node.jjtGetChild(0);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return executeAssign(node, left, right, JexlOperator.SELF_OR, data);
    }

    @Override
    protected Object visit(ASTSetXorNode node, Object data) {
        JexlNode left = node.jjtGetChild(0);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return executeAssign(node, left, right, JexlOperator.SELF_XOR, data);
    }

    @Override
    protected Object visit(ASTSetShlNode node, Object data) {
        JexlNode left = node.jjtGetChild(0);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return executeAssign(node, left, right, JexlOperator.SELF_SHL, data);
    }

    @Override
    protected Object visit(ASTSetSarNode node, Object data) {
        JexlNode left = node.jjtGetChild(0);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return executeAssign(node, left, right, JexlOperator.SELF_SAR, data);
    }

    @Override
    protected Object visit(ASTSetShrNode node, Object data) {
        JexlNode left = node.jjtGetChild(0);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return executeAssign(node, left, right, JexlOperator.SELF_SHR, data);
    }

    @Override
    protected Object visit(ASTIncrementNode node, Object data) {
        JexlNode left = node.jjtGetChild(0);
        return executeAssign(node, left, 1, JexlOperator.INCREMENT, data);
    }

    @Override
    protected Object visit(ASTDecrementNode node, Object data) {
        JexlNode left = node.jjtGetChild(0);
        return executeAssign(node, left, 1, JexlOperator.DECREMENT, data);
    }

    @Override
    protected Object visit(ASTIncrementPostfixNode node, Object data) {
        JexlNode left = node.jjtGetChild(0);
        Object value = left.jjtAccept(this, data);
        executeAssign(node, left, 1, JexlOperator.INCREMENT, data);
        return value;
    }

    @Override
    protected Object visit(ASTDecrementPostfixNode node, Object data) {
        JexlNode left = node.jjtGetChild(0);
        Object value = left.jjtAccept(this, data);
        executeAssign(node, left, 1, JexlOperator.DECREMENT, data);
        return value;
    }

    /**
     * Executes an assignment with an optional side-effect operator.
     * @param node     the node
     * @param left     the reference to assign to
     * @param right    the value expression to assign
     * @param assignop the assignment operator or null if simply assignment
     * @param data     the data
     * @return the left hand side
     */
    protected Object executeAssign(JexlNode node, JexlNode left, Object right, JexlOperator assignop, Object data) { // CSOFF: MethodLength
        cancelCheck(node);
        // left contains the reference to assign to
        ASTIdentifier var = null;
        Object object = null;
        int symbol = -1;
        // check var decl with assign is ok
        if (left instanceof ASTIdentifier) {
            var = (ASTIdentifier) left;
            symbol = var.getSymbol();
            if (symbol >= 0 && options.isLexical()) {
                if (options.isLexicalShade() && var.isShaded()) {
                    return undefinedVariable(var, var.getName());
                }
            }
        }
        boolean antish = options.isAntish();
        // 0: determine initial object & property:
        final int last = left.jjtGetNumChildren() - 1;
        // a (var?) v = ... expression
        if (var != null) {
            if (symbol >= 0) {
                // check we are not assigning a symbol itself
                if (last < 0) {
                    boolean isFinal = block.isVariableFinal(symbol);
                    if (isFinal && !(var instanceof ASTVar || var instanceof ASTExtVar)) {
                        throw new JexlException(node, "can not assign a value to the final variable: " + var.getName());
                    }
                    if (assignop != null) {
                        Object self = getVariable(frame, block, var);
                        right = assignop.getArity() == 1 ? operators.tryAssignOverload(node, assignop, self) :
                            operators.tryAssignOverload(node, assignop, self, right);
                        if (right == JexlOperator.ASSIGN) {
                            return self;
                        }
                    }
                    // check if we need to typecast result
                    Class type = block.typeof(symbol);
                    if (type != null) {
                        if (arithmetic.isStrict()) {
                            right = arithmetic.implicitCast(type, right);
                        } else {
                            right = arithmetic.cast(type, right);
                        }
                        if (type.isPrimitive() && right == null)
                            throw new JexlException(node, "not null value required for: " + var.getName());
                    }
                    boolean isRequired = block.isVariableRequired(symbol);
                    if (isRequired && right == null)
                        throw new JexlException(node, "not null value required for: " + var.getName());

                    frame.set(symbol, right);
                    // make the closure accessible to itself, ie capture the currently set variable after frame creation
                    if (right instanceof Closure) {
                          ((Closure) right).setCaptured(symbol, right);
                    }
                    return right; // 1
                }
                object = getVariable(frame, block, var);
                // top level is a symbol, can not be an antish var
                antish = false;
            } else {
                // check we are not assigning direct global
                if (last < 0) {
                    if (assignop != null) {
                        Object self = context.get(var.getName());
                        right = assignop.getArity() == 1 ? operators.tryAssignOverload(node, assignop, self) :
                            operators.tryAssignOverload(node, assignop, self, right);
                        if (right == JexlOperator.ASSIGN) {
                            return self;
                        }
                    }
                    setContextVariable(node, var.getName(), right);
                    return right; // 2
                }
                object = context.get(var.getName());
                // top level accesses object, can not be an antish var
                if (object != null) {
                    antish = false;
                }
            }
        } else if (left instanceof ASTIndirectNode) {
            if (assignop == null) {
                Object self = left.jjtGetChild(0).jjtAccept(this, data);
                if (self == null)
                    throw new JexlException(left, "illegal assignment form *0");
                if (self instanceof SetPointer) {
                    ((SetPointer) self).set(right);
                } else {
                    Object result = operators.indirectAssign(node, self, right);
                    if (result == JexlEngine.TRY_FAILED)
                        throw new JexlException(left, "illegal dereferenced assignment");
                }
                return right;
            } else {
                Object self = left.jjtAccept(this, data);
                if (self == null)
                    throw new JexlException(left, "illegal assignment form *0");
                Object result = operators.tryAssignOverload(node, assignop, self, right);
                if (result == JexlOperator.ASSIGN) {
                    return self;
                } else if (result != JexlEngine.TRY_FAILED) {
                    self = left.jjtGetChild(0).jjtAccept(this, data);
                    if (self == null)
                        throw new JexlException(left, "illegal assignment form *0");
                    if (self instanceof SetPointer) {
                        ((SetPointer) self).set(result);
                    } else {
                        result = operators.indirectAssign(node, self, result);
                        if (result == JexlEngine.TRY_FAILED)
                            throw new JexlException(left, "illegal dereferenced assignment");
                    }
                }
                return right;
            }
        } else if (!(left instanceof ASTReference)) {
            throw new JexlException(left, "illegal assignment form 0");
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
                    JexlNode first = left.jjtGetChild(0);
                    ASTIdentifier firstId = first instanceof ASTIdentifier
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
                    JexlNode child = left.jjtGetChild(v);
                    ASTIdentifierAccess aid = child instanceof ASTIdentifierAccess
                            ? (ASTIdentifierAccess) child
                            : null;
                    // remain antish only if unsafe navigation
                    if (aid != null && !aid.isSafe() && !aid.isExpression()) {
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
                throw new JexlException(objectNode, "illegal assignment form");
            }
        }
        // 2: last objectNode will perform assignement in all cases
        Object property = null;
        JexlNode propertyNode = left.jjtGetChild(last);
        ASTIdentifierAccess propertyId = propertyNode instanceof ASTIdentifierAccess
                ? (ASTIdentifierAccess) propertyNode
                : null;
        if (propertyId != null) {
            // deal with creating/assignining antish variable
            if (antish && ant != null && object == null && !propertyId.isSafe() && !propertyId.isExpression()) {
                if (last > 0) {
                    ant.append('.');
                }
                ant.append(propertyId.getName());
                if (assignop != null) {
                    Object self = context.get(ant.toString());
                    right = assignop.getArity() == 1 ? operators.tryAssignOverload(node, assignop, self) :
                        operators.tryAssignOverload(node, assignop, self, right);
                    if (right == JexlOperator.ASSIGN) {
                        return self;
                    }
                }
                setContextVariable(propertyNode, ant.toString(), right);
                return right; // 3
            }
            // property of an object ?
            property = evalIdentifier(propertyId);
        } else if (propertyNode instanceof ASTArrayAccess) {
            // can have multiple nodes - either an expression, integer literal or reference
            int numChildren = propertyNode.jjtGetNumChildren() - 1;
            for (int i = 0; i < numChildren; i++) {
                JexlNode nindex = propertyNode.jjtGetChild(i);
                Object index = nindex.jjtAccept(this, null);
                object = getAttribute(object, index, nindex);
            }
            propertyNode = propertyNode.jjtGetChild(numChildren);
            property = propertyNode.jjtAccept(this, null);
        } else {
            throw new JexlException(objectNode, "illegal assignment form");
        }
        // we may have a null property as in map[null], no check needed.
        // we can not *have* a null object though.
        if (object == null) {
            // no object, we fail
            return unsolvableProperty(objectNode, "<null>.<?>", true, null);
        }
        // 3: one before last, assign
        if (assignop != null) {
            Object self = getAttribute(object, property, propertyNode);
            right = assignop.getArity() == 1 ? operators.tryAssignOverload(node, assignop, self) :
                operators.tryAssignOverload(node, assignop, self, right);
            if (right == JexlOperator.ASSIGN) {
                return self;
            }
        }

        final JexlOperator operator = propertyNode != null && propertyNode.jjtGetParent() instanceof ASTArrayAccess
                                      ? JexlOperator.ARRAY_SET : JexlOperator.PROPERTY_SET;

        setAttribute(object, property, right, propertyNode, operator);
        return right; // 4
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
        if (isCancelled()) {
            throw new JexlException.Cancel(node);
        }

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
            String tstr = target != null ? target.toString() : "?";
            return unsolvableMethod(node, tstr);
        } catch (JexlException xthru) {
            throw xthru;
        } catch (Exception xany) {
            String tstr = target != null ? target.toString() : "?";
            throw invocationException(node, tstr, xany);
        }
    }

    @Override
    protected Object visit(final ASTMethodReference node, Object data) {
        ASTIdentifier methodNode = (ASTIdentifier) node.jjtGetChild(0);
        String methodName = methodNode.getName();
        if (data == null)
            return unsolvableMethod(methodNode, "<null>::" + methodName);
        Object result = MethodReference.create(this, data, methodName);
        return result != null ? result : unsolvableMethod(methodNode, "::" + methodName);
    }

    @Override
    protected Object visit(final ASTMethodNode node, Object data) {
        return visit(node, null, data);
    }

    /**
     * Execute a method call, ie syntactically written as name.call(...).
     * @param node the actual method call node
     * @param object non null when name.call is an antish variable
     * @param data the context
     * @return the method call result
     */
    private Object visit(final ASTMethodNode node, Object object, Object data) {
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
            ASTArguments argNode = (ASTArguments) node.jjtGetChild(a);
            result = call(node, object, result, argNode);
            object = result;
        }
        return result;
    }

    @Override
    protected Object visit(ASTFunctionNode node, Object data) {
        ASTIdentifier functionNode = (ASTIdentifier) node.jjtGetChild(0);
        String nsid = functionNode.getNamespace();
        Object namespace = (nsid != null)? resolveNamespace(nsid, node) : context;
        ASTArguments argNode = (ASTArguments) node.jjtGetChild(1);
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
     * @param functor the object carrying the method or function or the method identifier
     * @param argNode the node carrying the arguments
     * @return the result of the method invocation
     */
    protected Object call(final JexlNode node, Object target, Object functor, final ASTArguments argNode) {
        cancelCheck(node);
        // evaluate the arguments
        final Object[] argv = visit(argNode, null);
        // get the method name if identifier
        final int symbol;
        final String methodName;
        boolean cacheable = cache;
        boolean isavar = false;
        if (functor instanceof ASTIdentifier) {
            // function call, target is context or namespace (if there was one)
            ASTIdentifier methodIdentifier = (ASTIdentifier) functor;
            symbol = methodIdentifier.getSymbol();
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
                // name is a variable, cant be cached
                cacheable &= !isavar;
            }
        } else if (functor instanceof ASTIdentifierAccess) {
            // a method call on target
            methodName = ((ASTIdentifierAccess) functor).getName();
            symbol = -1;
            functor = null;
            cacheable = true;
        } else if (functor != null) {
            // ...(x)(y)
            symbol = -1 - 1; // -2;
            methodName = null;
            cacheable = false;
        } else if (!node.isSafeLhs(isSafe())) {
            return unsolvableMethod(node, "?(...)");
        } else {
            // safe lhs
            return null;
        }

        // solving the call site
        CallDispatcher call = new CallDispatcher(node, cacheable);
        try {
            // do we have a  cached version method/function name ?
            Object eval = call.tryEval(target, methodName, argv);
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
                        Object namespace = resolveNamespace(null, node);
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
                        Object[] pargv = functionArguments(target, narrow, argv);
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
                            if (argCount > paramCount)
                                return unsolvableMethod(node, "(...)");
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
                    Object[] pargv = functionArguments(functor, narrow, argv);
                    if (call.isContextMethod(mCALL, pargv)) {
                        return call.eval(mCALL);
                    }
                    if (call.isArithmeticMethod(mCALL, pargv)) {
                        return call.eval(mCALL);
                    }
                }
                // if we did not find an exact method by name and we haven't tried yet,
                // attempt to narrow the parameters and if this succeeds, try again in next loop
                if (!narrow && arithmetic.narrowArguments(argv)) {
                    narrow = true;
                    // continue;
                } else {
                    break;
                }
            }
            // we have either evaluated and returned or no method was found
            return node.isSafeLhs(isSafe())
                    ? null
                    : unsolvableMethod(node, methodName, argv);
        } catch (JexlException.TryFailed xany) {
            throw invocationException(node, methodName, xany.getCause());
        } catch (JexlException xthru) {
            throw xthru;
        } catch (Exception xany) {
            throw invocationException(node, methodName, xany);
        }
    }

    @Override
    protected Object visit(ASTConstructorNode node, Object data) {
        if (isCancelled()) {
            throw new JexlException.Cancel(node);
        }
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
            String tstr = target != null ? target.toString() : "?";
            return unsolvableMethod(node, tstr, argv);
        } catch (JexlException xthru) {
            throw xthru;
        } catch (Exception xany) {
            String tstr = target != null ? target.toString() : "?";
            throw invocationException(node, tstr, xany);
        }
    }

    @Override
    protected Object visit(ASTQualifiedConstructorNode node, Object data) {
        if (isCancelled()) {
            throw new JexlException.Cancel(node);
        }
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
            String tstr = target != null ? target.toString() : "?";
            return unsolvableMethod(node, tstr, argv);
        } catch (JexlException.Method xmethod) {
            throw xmethod;
        } catch (Exception xany) {
            String tstr = target != null ? target.toString() : "?";
            throw invocationException(node, tstr, xany);
        }
    }

    @Override
    protected Object visit(ASTArrayConstructorNode node, Object data) {
        if (isCancelled()) {
            throw new JexlException.Cancel(node);
        }
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
        } catch (Exception xany) {
            String tstr = target != null ? target.toString() : "?";
            throw invocationException(node, tstr, xany);
        }
    }

    @Override
    protected Object visit(ASTArrayOpenDimension node, Object data) {
        return 0;
    }

    @Override
    protected Object visit(ASTInitializedArrayConstructorNode node, Object data) {
        if (isCancelled()) {
            throw new JexlException.Cancel(node);
        }
        // first child is class or class name
        final Class target = (Class) node.jjtGetChild(0).jjtAccept(this, data);
        // get the length of the array
        int argc = node.jjtGetNumChildren() - 1;
        boolean comprehensions = false;
        for (int i = 0; i < argc; i++) {
            JexlNode child = node.jjtGetChild(i + 1);
            if (child instanceof ASTEnumerationNode || child instanceof ASTEnumerationReference)
                comprehensions = true;
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
        } catch (Exception xany) {
            String tstr = target != null ? target.toString() : "?";
            throw invocationException(node, tstr, xany);
        }
    }

    @Override
    protected Object visit(ASTInitializedCollectionConstructorNode node, Object data) {
        if (isCancelled()) {
            throw new JexlException.Cancel(node);
        }
        // first child is class or class name
        final Class target = (Class) node.jjtGetChild(0).jjtAccept(this, data);
        if (!Collection.class.isAssignableFrom(target))
            throw new JexlException(node, "Not a Collection", null);
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
            String tstr = target != null ? target.toString() : "?";
            throw invocationException(node, tstr, xany);
        }
    }

    @Override
    protected Object visit(ASTInitializedMapConstructorNode node, Object data) {
        if (isCancelled()) {
            throw new JexlException.Cancel(node);
        }
        // first child is class or class name
        final Class target = (Class) node.jjtGetChild(0).jjtAccept(this, data);
        if (!Map.class.isAssignableFrom(target))
            throw new JexlException(node, "Not a Map", null);
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
                                        throw new JexlException(node, "Not a Map.Entry", null);
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
            String tstr = target != null ? target.toString() : "?";
            return unsolvableMethod(node, tstr, EMPTY_PARAMS);
        } catch (Exception xany) {
            String tstr = target != null ? target.toString() : "?";
            throw invocationException(node, tstr, xany);
        }
    }

    @Override
    protected Object visit(ASTJxltLiteral node, Object data) {
        TemplateEngine.TemplateExpression tp = (TemplateEngine.TemplateExpression) node.jjtGetValue();
        if (tp == null) {
            TemplateEngine jxlt = jexl.jxlt();
            JexlInfo info = node.jexlInfo();
            if (this.block != null) {
                info = new JexlNode.Info(node, info);
            }
            tp = jxlt.parseExpression(info, node.getLiteral(), frame != null ? frame.getScope() : null);
            node.jjtSetValue(tp);
        }
        if (tp != null) {
            return tp.evaluate(frame, context);
        }
        return null;
    }

    @Override
    protected Object visit(ASTAnnotation node, Object data) {
        throw new UnsupportedOperationException(ASTAnnotation.class.getName() + ": Not supported.");
    }

    @Override
    protected Object visit(ASTAnnotatedStatement node, Object data) {
        return processAnnotation(node, 0, data);
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
            JexlNode cblock = stmt.jjtGetChild(last);
            // if the context has changed, might need a new arithmetic
            final JexlArithmetic jexla = arithmetic.options(context);
            if (jexla != arithmetic) {
                if (!arithmetic.getClass().equals(jexla.getClass())) {
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
        final boolean[] processed = new boolean[]{false};
        final Callable<Object> jstmt = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                processed[0] = true;
                try {
                    return processAnnotation(stmt, index + 1, data);
                } catch (JexlException.Return xreturn) {
                    return xreturn;
                } catch (JexlException.Yield xyield) {
                    return xyield;
                } catch (JexlException.Break xbreak) {
                    return xbreak;
                } catch (JexlException.Continue xcontinue) {
                    return xcontinue;
                } catch (JexlException.Remove xremove) {
                    return xremove;
                }
            }
        };
        // the annotation node and name
        final ASTAnnotation anode = (ASTAnnotation) stmt.jjtGetChild(index);
        final String aname = anode.getName();
        // evaluate the arguments
        Object[] argv = anode.jjtGetNumChildren() > 0
                        ? visit((ASTArguments) anode.jjtGetChild(0), null) : null;
        // wrap the future, will recurse through annotation processor
        Object result;
        try {
            result = processAnnotation(aname, argv, jstmt);
            // not processing an annotation is an error
            if (!processed[0]) {
                return annotationError(anode, aname, null);
            }
        } catch (JexlException xany) {
            throw xany;
        } catch (Exception xany) {
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
    protected Object processAnnotation(String annotation, Object[] args, Callable<Object> stmt) throws Exception {
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
                if (expr instanceof ASTJexlLambda || expr instanceof ASTCurrentNode)
                    node = expr;
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
            Object key = evaluateProjection(0, data);
            Object value = evaluateProjection(1, data);
            return new AbstractMap.SimpleImmutableEntry<Object,Object> (key, value);
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
            if (!hasNextItem)
                findNextItem();
            return hasNextItem;
        }

        @Override
        public Object next() {
            cancelCheck(node);
            if (!hasNextItem)
                findNextItem();
            if (!hasNextItem)
                throw new NoSuchElementException();
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
            if (!hasNext())
                throw new NoSuchElementException();
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

            if (startCount > 0)
                skipItems(startCount);
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
            if (!hasNext())
                throw new NoSuchElementException();
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
        if (script instanceof ASTSimpleLambda && script.jjtGetNumChildren() == 1 && script.jjtGetChild(0) instanceof ASTJexlLambda)
           script = (ASTJexlLambda) script.jjtGetChild(0);

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

    @Override
    protected Object visit(ASTPipeNode node, Object data) {

        Object result = null;
        JexlNode pipe = node.jjtGetChild(0);

        if (data instanceof Iterator<?>) {

            Iterator<?> itemsIterator = (Iterator) data;

            try {
                int i = 0;
                if (pipe instanceof ASTJexlLambda) {

                    ASTJexlLambda script = (ASTJexlLambda) pipe;

                    Closure closure = new Closure(this, script);

                    boolean varArgs = script.isVarArgs();
                    int argCount = script.getArgCount();

                    while (itemsIterator.hasNext()) {
                        Object value = itemsIterator.next();

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

                        Object prev = current;
                        try {
                            current = value;
                            result = closure.execute(null, argv);
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
                Object[] argv = {data};
                Object prev = current;
                try {
                    current = data;
                    result = closure.execute(null, argv);
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

}
