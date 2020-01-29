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
 * A resumable interpreter of JEXL syntax to support generators.
 *
 * @since 3.2
 */
public class ResumableInterpreter extends Interpreter {
    /** If interpreter is suspended. */
    protected boolean suspended;

    protected class InterpreterState {
        /** Statement pointer. */
        protected int index;
        /** Statement argument. */
        protected Object value;
        /** Statement completed. */
        protected boolean completed;

        public InterpreterState(int i) {
            this(i, null);
        }

        public InterpreterState(int i, Object value) {
            this.index = i;
            this.value = value;
        }

        public int getIndex() {
            return index;
        }

        public void incIndex() {
            index++;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object v) {
            value = v;
        }

        public void setCompleted() {
            completed = true;
        }

        public boolean isCompleted() {
            return completed;
        }

    }

    /** resumable interpreter state. */
    protected final Map<JexlNode,InterpreterState> state;

    /**
     * Creates an interpreter.
     * @param engine   the engine creating this interpreter
     * @param aContext the evaluation context, global variables, methods and functions
     * @param opts     the evaluation options, flags modifying evaluation behavior
     * @param eFrame   the evaluation frame, arguments and local variables
     */
    protected ResumableInterpreter(Engine engine, JexlOptions opts, JexlContext aContext, Frame eFrame) {
        super(engine, opts, aContext, eFrame);
        state = new HashMap<JexlNode,InterpreterState> ();
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
    @Override
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
            if (!suspended) {
                synchronized (this) {
                    if (functors != null) {
                        for (Object functor : functors.values()) {
                            closeIfSupported(functor);
                        }
                        functors.clear();
                        functors = null;
                    }
                }
                state.clear();
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

    @Override
    protected Object visit(ASTExpressionStatement node, Object data) {
        suspended = false;
        return super.visit(node, data);
    }

    @Override
    protected Object visit(ASTFunctionStatement node, Object data) {
        suspended = false;
        return super.visit(node, data);
    }

    @Override
    protected Object visit(ASTIfStatement node, Object data) {
        cancelCheck(node);
        try {
            Object condition = null;
            InterpreterState st = state.get(node);
            if (st == null) {
                st = new InterpreterState(0);
                state.put(node, st);
                suspended = false;
                condition = node.jjtGetChild(0).jjtAccept(this, null);
            } else {
                condition = st.getValue();
                if (condition == null) {
                    suspended = false;
                    return null;
                }
            }
            if (arithmetic.toBoolean(condition)) {
                st.setValue(Boolean.TRUE);
                // first objectNode is true statement
                JexlNode child = node.jjtGetChild(1);
                try {
                    return child.jjtAccept(this, null);
                } catch (JexlException.Yield stmtYield) {
                    if (child instanceof ASTYieldStatement) {
                        if (suspended) {
                            st.setValue(null);
                        }
                    }
                    throw stmtYield;
                }
            }
            if (node.jjtGetNumChildren() > 2) {
                st.setValue(Boolean.FALSE);
                // if there is an else, execute it.
                JexlNode child = node.jjtGetChild(2);
                try {
                    return child.jjtAccept(this, null);
                } catch (JexlException.Yield stmtYield) {
                    if (child instanceof ASTYieldStatement) {
                        if (suspended) {
                            st.setValue(null);
                        }
                    }
                    throw stmtYield;
                }
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
        } finally {
            if (!suspended) {
                state.remove(node);
            }
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
        int start = 0;
        InterpreterState st = state.get(node);
        if (st == null) {
            st = new InterpreterState(0);
            state.put(node, st);
            suspended = false;
        } else {
            start = st.getIndex();
        }
        try {
            final int numChildren = node.jjtGetNumChildren();
            for (int i = start; i < numChildren; i++) {
                cancelCheck(node);
                JexlNode statement = node.jjtGetChild(i);
                try {
                    result = statement.jjtAccept(this, data);
                    st.incIndex();
                } catch (JexlException.Yield stmtYield) {
                    if (statement instanceof ASTYieldStatement) {
                        if (suspended) {
                            st.incIndex();
                        }
                    }
                    throw stmtYield;
                } catch (JexlException.Break stmtBreak) {
                    String target = stmtBreak.getLabel();
                    if (target != null && target.equals(node.getLabel())) {
                        break;
                    } else {
                        throw stmtBreak;
                    }
                }
            }
            // Evaluated empty block
            suspended = false;
        } finally {
            if (!suspended) {
                state.remove(node);
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
        try {
            if (!suspended) {
                block = new LexicalFrame(frame, block);
            }
            return visitBlock(node, data);
        } finally {
            if (!suspended) {
                block = block.pop();
            }
        }
    }

    @Override
    protected Object visit(ASTReturnStatement node, Object data) {
        suspended = false;
        return super.visit(node, data);
    }

    @Override
    protected Object visit(ASTYieldStatement node, Object data) {
        try {
            return super.visit(node, data);
        } finally {
            suspended = node.isReturn();
        }
    }

    @Override
    protected Object visit(ASTContinue node, Object data) {
        suspended = false;
        return super.visit(node, data);
    }

    @Override
    protected Object visit(ASTRemove node, Object data) {
        suspended = false;
        return super.visit(node, data);
    }

    @Override
    protected Object visit(ASTBreak node, Object data) {
        suspended = false;
        return super.visit(node, data);
    }

    @Override
    protected Object visit(ASTForStatement node, Object data) {
        cancelCheck(node);
        final boolean lexical = options.isLexical();
        if (lexical && !suspended) {
              // create lexical frame
              block = new LexicalFrame(frame, block);
        }
        try {
            final int numChildren = node.jjtGetNumChildren();
            JexlNode expressionNode = node.jjtGetChild(1);
            boolean when = false;
            boolean doBody = true;
            Object result = null;
            InterpreterState st = null;
            try {
                st = state.get(node);
                if (st == null) {
                    st = new InterpreterState(0);
                    state.put(node, st);
                    suspended = false;
                    // Initialize for-loop
                    result = node.jjtGetChild(0).jjtAccept(this, data);
                    when = (Boolean) expressionNode.jjtAccept(this, data);
                } else {
                    when = true;
                    doBody = !st.isCompleted();
                }
                /* third objectNode is the statement to execute */
                JexlNode statement = node.jjtGetNumChildren() > 3 ? node.jjtGetChild(3) : null;

                while (when) {
                    cancelCheck(node);
                    // Execute loop body
                    if (statement != null && doBody) {
                        try {
                            result = statement.jjtAccept(this, data);
                        } catch (JexlException.Yield stmtYield) {
                            if (statement instanceof ASTYieldStatement) {
                                if (suspended) {
                                    st.setCompleted();
                                }
                            }
                            throw stmtYield;
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
                    } else {
                        suspended = false;
                        doBody = true;
                    }
                    // for-increment node
                    result = node.jjtGetChild(2).jjtAccept(this, data);
                    // for0termination check
                    when = (Boolean) expressionNode.jjtAccept(this, data);
                }
                return result;
            } finally {
                if (!suspended) {
                    state.remove(node);
                }
            }
        } finally {
            if (lexical && !suspended) {
                // restore lexical frame
                block = block.pop();
            }
        }
    }

    @Override
    protected Object visit(ASTForeachStatement node, Object data) {
        cancelCheck(node);
        Object result = null;
        /* first objectNode is the loop variable */
        ASTForeachVar loopReference = (ASTForeachVar) node.jjtGetChild(0);
        ASTIdentifier loopVariable = (ASTIdentifier) loopReference.jjtGetChild(0);
        ASTIdentifier loopValueVariable = loopReference.jjtGetNumChildren() > 1 ? (ASTIdentifier) loopReference.jjtGetChild(1) : null;
        final boolean lexical = options.isLexical();

        if (lexical && !suspended) {
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
            Iterator<?> itemsIterator = null;
            int i = -1;

            InterpreterState st = null;
            boolean when = false;
            boolean doBody = true;
            try {
                st = state.get(node);
                if (st == null) {
                    suspended = false;

                    /* second objectNode is the variable to iterate */
                    Object iterableValue = node.jjtGetChild(1).jjtAccept(this, data);
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
                    st = new InterpreterState(i, itemsIterator);
                    state.put(node, st);
                } else {
                    when = true;
                    doBody = !st.isCompleted();
                    itemsIterator = (Iterator<?>) st.getValue();
                    i = st.getIndex();
                }
                if (itemsIterator != null) {
                    try {
                        /* third objectNode is the statement to execute */
                        JexlNode statement = node.jjtGetNumChildren() >= 3 ? node.jjtGetChild(2) : null;
                        while (when || itemsIterator.hasNext()) {
                            if (!when) {
                                cancelCheck(node);
                                i += 1;
                                // increment context counter
                                st.incIndex();
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
                            }
                            if (statement != null && doBody) {
                                try {
                                    // execute statement
                                    result = statement.jjtAccept(this, data);
                                } catch (JexlException.Yield stmtYield) {
                                    if (statement instanceof ASTYieldStatement) {
                                        if (suspended) {
                                            st.setCompleted();
                                        }
                                    }
                                    throw stmtYield;
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
                            // back to normal execution
                            doBody = true;
                            when = false;
                        }
                    } finally {
                        // closeable iterator handling
                        closeIfSupported(itemsIterator);
                    }
                }

            } finally {
                if (!suspended) {
                    state.remove(node);
                }
            }
        } finally {
            // restore lexical frame
            if (lexical && !suspended)
                block = block.pop();
        }
        return result;
    }

    @Override
    protected Object visit(ASTTryStatement node, Object data) {
        suspended = false;
        return super.visit(node, data);
    }

    @Override
    protected Object visit(ASTTryWithResourceStatement node, Object data) {
        suspended = false;
        return super.visit(node, data);
    }

    @Override
    protected Object visit(ASTThrowStatement node, Object data) {
        suspended = false;
        return super.visit(node, data);
    }

    @Override
    protected Object visit(ASTAssertStatement node, Object data) {
        suspended = false;
        return super.visit(node, data);
    }

    @Override
    protected Object visit(ASTWhileStatement node, Object data) {
        cancelCheck(node);
        Object result = null;
        /* first objectNode is the expression */
        JexlNode expressionNode = node.jjtGetChild(0);

        Object condition = null;
        InterpreterState st = state.get(node);
        if (st == null) {
            condition = expressionNode.jjtAccept(this, data);
            st = new InterpreterState(0, condition);
            state.put(node, st);
            suspended = false;
        } else {
            condition = st.getValue();
        }
        try {
            while (arithmetic.toBoolean(condition)) {
                cancelCheck(node);
                if (node.jjtGetNumChildren() > 1) {
                    JexlNode child = node.jjtGetChild(1);
                    try {
                        // execute statement
                        result = child.jjtAccept(this, data);
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
                } else {
                    suspended = false;
                }
                // Reevaluate while condition
                condition = expressionNode.jjtAccept(this, data);
            }
        } finally {
            if (!suspended) {
                state.remove(node);
            }
        }
        return result;
    }

    @Override
    protected Object visit(ASTDoWhileStatement node, Object data) {
        Object result = null;
        /* last objectNode is the expression */
        JexlNode expressionNode = node.jjtGetChild(node.jjtGetNumChildren() - 1);
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
            } else {
                suspended = false;
            }
        } while (arithmetic.toBoolean(expressionNode.jjtAccept(this, data)));

        return result;
    }

    @Override
    protected Object visit(ASTSynchronizedStatement node, Object data) {
        suspended = false;
        return super.visit(node, data);
    }

    @Override
    protected Object visit(ASTSwitchStatement node, Object data) {
        cancelCheck(node);
        final boolean lexical = options.isLexical();
        if (lexical && !suspended) {
            // create lexical frame
            block = new LexicalFrame(frame, block);
        }
        try {
            int start = 0;
            final int childCount = node.jjtGetNumChildren();

            InterpreterState st = state.get(node);
            if (st == null) {
                suspended = false;

                boolean matched = false;
                /* first objectNode is the switch expression */
                Object left = node.jjtGetChild(0).jjtAccept(this, data);
                Class scope = left != null ? left.getClass() : Void.class;
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

                st = new InterpreterState(start);
                state.put(node, st);
            } else {
                start = st.getIndex();
            }

            try {
                Object result = null;
                // execute all cases starting from matched one
                if (start > 0) {
                    try {
                        for (int i = start; i < childCount; i++) {
                            JexlNode child = node.jjtGetChild(i);
                            result = child.jjtAccept(this, data);
                            st.incIndex();
                        }
                    } catch (JexlException.Break stmtBreak) {
                        String target = stmtBreak.getLabel();
                        if (target != null && !target.equals(node.getLabel())) {
                            throw stmtBreak;
                        }
                        // break
                    }
                } else {
                    suspended = false;
                }
                return result;
            } finally {
                if (!suspended) {
                    state.remove(node);
                }
            }
        } finally {
            // restore lexical frame
            if (lexical && !suspended)
                block = block.pop();
        }
    }

    @Override
    protected Object visit(ASTSwitchStatementCase node, Object data) {
        int start = 1;
        InterpreterState st = state.get(node);
        if (st == null) {
            suspended = false;
            st = new InterpreterState(start);
            state.put(node, st);
        } else {
            start = st.getIndex();
        }
        try {
            Object result = null;
            final int childCount = node.jjtGetNumChildren();
            for (int i = start; i < childCount; i++) {
                cancelCheck(node);
                JexlNode statement = node.jjtGetChild(i);
                try {
                    result = statement.jjtAccept(this, data);
                    st.incIndex();
                } catch (JexlException.Yield stmtYield) {
                    if (statement instanceof ASTYieldStatement) {
                        if (suspended) {
                            st.incIndex();
                        }
                    }
                    throw stmtYield;
                }
            }
            suspended = false;
            return result;
        } finally {
            if (!suspended) {
                state.remove(node);
            }
        }
    }

    @Override
    protected Object visit(ASTSwitchStatementDefault node, Object data) {
        int start = 0;
        InterpreterState st = state.get(node);
        if (st == null) {
            suspended = false;
            st = new InterpreterState(start);
            state.put(node, st);
        } else {
            start = st.getIndex();
        }
        try {
            Object result = null;
            final int childCount = node.jjtGetNumChildren();
            for (int i = start; i < childCount; i++) {
                cancelCheck(node);
                JexlNode statement = node.jjtGetChild(i);
                try {
                    result = statement.jjtAccept(this, data);
                } catch (JexlException.Yield stmtYield) {
                    if (statement instanceof ASTYieldStatement) {
                        if (suspended) {
                            st.incIndex();
                        }
                    }
                    throw stmtYield;
                }
                st.incIndex();
            }
            suspended = false;
            return result;
        } finally {
            if (!suspended) {
                state.remove(node);
            }
        }
    }


    @Override
    protected Object visit(ASTMultipleAssignment node, Object data) {
        suspended = false;
        return super.visit(node, data);
    }

    @Override
    protected Object visit(ASTMultipleInitialization node, Object data) {
        suspended = false;
        return super.visit(node, data);
    }

    @Override
    protected Object visit(ASTInitialization node, Object data) {
        suspended = false;
        return super.visit(node, data);
    }

}
