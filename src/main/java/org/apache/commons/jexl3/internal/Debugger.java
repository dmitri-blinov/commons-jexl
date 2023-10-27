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


import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.JexlFeatures;
import org.apache.commons.jexl3.JexlInfo;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.parser.ASTAddNode;
import org.apache.commons.jexl3.parser.ASTAndNode;
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
import org.apache.commons.jexl3.parser.ASTSimpleLambda;
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
import org.apache.commons.jexl3.parser.ASTFunctionStatement;
import org.apache.commons.jexl3.parser.ASTFunctionNode;
import org.apache.commons.jexl3.parser.ASTFunctionVar;
import org.apache.commons.jexl3.parser.ASTGENode;
import org.apache.commons.jexl3.parser.ASTGEPredicate;
import org.apache.commons.jexl3.parser.ASTGTNode;
import org.apache.commons.jexl3.parser.ASTGTPredicate;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTIdentifierAccess;
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
import org.apache.commons.jexl3.parser.ASTInlinePropertyArrayNEEntry;
import org.apache.commons.jexl3.parser.ASTInlinePropertyArrayNullEntry;
import org.apache.commons.jexl3.parser.ASTInlinePropertyEntry;
import org.apache.commons.jexl3.parser.ASTInlinePropertyNEEntry;
import org.apache.commons.jexl3.parser.ASTInlinePropertyNullEntry;
import org.apache.commons.jexl3.parser.ASTInnerConstructorNode;
import org.apache.commons.jexl3.parser.ASTIOFNode;
import org.apache.commons.jexl3.parser.ASTISNode;
import org.apache.commons.jexl3.parser.ASTMapLiteral;
import org.apache.commons.jexl3.parser.ASTIfStatement;
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
import org.apache.commons.jexl3.parser.ASTAnnotatedStatement;
import org.apache.commons.jexl3.parser.ASTAnnotation;
import org.apache.commons.jexl3.parser.ASTNullpNode;

import org.apache.commons.jexl3.parser.JexlNode;
import org.apache.commons.jexl3.parser.JexlParser;
import org.apache.commons.jexl3.parser.ParserVisitor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Locale;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import org.apache.commons.jexl3.parser.StringParser;

/**
 * Helps pinpoint the cause of problems in expressions that fail during evaluation.
 * <p>
 * It rebuilds an expression string from the tree and the start/end offsets of the cause in that string.
 * This implies that exceptions during evaluation do always carry the node that's causing the error.
 * </p>
 * @since 2.0
 */
public class Debugger extends ParserVisitor implements JexlInfo.Detail {
    /** The builder to compose messages. */
    protected final StringBuilder builder = new StringBuilder();
    /** The cause of the issue to debug. */
    protected JexlNode cause;
    /** The starting character location offset of the cause in the builder. */
    protected int start;
    /** The ending character location offset of the cause in the builder. */
    protected int end;
    /** The indentation level. */
    protected int indentLevel;
    /** Perform indentation?. */
    protected int indent = 2;
    /** accept() relative depth. */
    protected int depth = Integer.MAX_VALUE;
    /** Arrow symbol. */
    protected String arrow = "->";
    /** EOL. */
    protected String lf = "\n";
    /** Pragmas out. */
    protected boolean outputPragmas;

    /**
     * Creates a Debugger.
     */
    public Debugger() {
        // nothing to initialize
    }

    /**
     * Resets this debugger state.
     */
    public void reset() {
        builder.setLength(0);
        cause = null;
        start = 0;
        end = 0;
        indentLevel = 0;
        indent = 2;
        depth = Integer.MAX_VALUE;
    }

    /**
     * Tries (hard) to find the features used to parse a node.
     * @param node the node
     * @return the features or null
     */
    protected JexlFeatures getFeatures(JexlNode node) {
        JexlNode walk = node;
        while(walk != null) {
            if (walk instanceof ASTJexlScript) {
                ASTJexlScript script = (ASTJexlScript) walk;
                return script.getFeatures();
            }
            walk = walk.jjtGetParent();
        }
        return null;
    }

    /**
     * Sets the arrow style (fat or thin) depending on features.
     * @param node the node to start seeking features from.
     */
    protected void setArrowSymbol(JexlNode node) {
        JexlFeatures features = getFeatures(node);
        if (features != null && features.supportsFatArrow() && !features.supportsThinArrow()) {
            arrow = "=>";
        } else {
            arrow = "->";
        }
    }

    /**
     * Position the debugger on the root of an expression.
     * @param jscript the expression
     * @return true if the expression was a {@link Script} instance, false otherwise
     */
    public boolean debug(final JexlExpression jscript) {
        if (jscript instanceof Script) {
            Script script = (Script) jscript;
            return debug(script.script);
        }
        return false;
    }

    /**
     * Position the debugger on the root of a script.
     * @param jscript the script
     * @return true if the script was a {@link Script} instance, false otherwise
     */
    public boolean debug(final JexlScript jscript) {
        if (jscript instanceof Script) {
            Script script = (Script) jscript;
            return debug(script.script);
        }
        return false;
    }

    /**
     * Seeks the location of an error cause (a node) in an expression.
     * @param node the node to debug
     * @return true if the cause was located, false otherwise
     */
    public boolean debug(final JexlNode node) {
        return debug(node, true);
    }

    /**
     * Seeks the location of an error cause (a node) in an expression.
     * @param node the node to debug
     * @param r whether we should actively find the root node of the debugged node
     * @return true if the cause was located, false otherwise
     */
    public boolean debug(final JexlNode node, final boolean r) {
        start = 0;
        end = 0;
        indentLevel = 0;
        setArrowSymbol(node);
        if (node != null) {
            builder.setLength(0);
            cause = node;
            // make arg cause become the root cause
            JexlNode walk = node;
            if (r) {
                while (walk.jjtGetParent() != null) {
                    walk = walk.jjtGetParent();
                }
            }
            accept(walk, null);
        }
        return end > 0;
    }

    /**
     * @return The rebuilt expression
     */
    @Override
    public String toString() {
        return builder.toString();
    }

    /**
     * Rebuilds an expression from a JEXL node.
     * @param node the node to rebuilt from
     * @return the rebuilt expression
     * @since 3.0
     */
    public String data(final JexlNode node) {
        start = 0;
        end = 0;
        indentLevel = 0;
        setArrowSymbol(node);
        if (node != null) {
            builder.setLength(0);
            cause = node;
            accept(node, null);
        }
        return builder.toString();
    }

    /**
     * @return The starting offset location of the cause in the expression
     */
    @Override
    public int start() {
        return start;
    }

    /**
     * @return The end offset location of the cause in the expression
     */
    @Override
    public int end() {
        return end;
    }

    /**
     * Lets the debugger write out pragmas if any.
     * @param flag turn on or off
     * @return this debugger instance
     */
    public Debugger outputPragmas(boolean flag) {
        this.outputPragmas = flag;
        return this;
    }

    /**
     * Sets the indentation level.
     * @param level the number of spaces for indentation, none if less or equal to zero
     */
    public void setIndentation(final int level) {
        indentation(level);
    }

    /**
     * Sets the indentation level.
     * @param level the number of spaces for indentation, none if less or equal to zero
     * @return this debugger instance
     */
    public Debugger indentation(final int level) {
        indent = Math.max(level, 0);
        indentLevel = 0;
        return this;
    }

    /**
     * Sets this debugger relative maximum depth.
     * @param rdepth the maximum relative depth from the debugged node
     * @return this debugger instance
     */
    public Debugger depth(final int rdepth) {
        this.depth = rdepth;
        return this;
    }

    /**
     * Checks if a child node is the cause to debug &amp; adds its representation to the rebuilt expression.
     * @param node the child node
     * @param data visitor pattern argument
     * @return visitor pattern value
     */
    protected Object accept(final JexlNode node, final Object data) {
        if (depth <= 0) {
            builder.append("...");
            return data;
        }
        if (node == cause) {
            start = builder.length();
        }
        depth -= 1;
        final Object value = node.jjtAccept(this, data);
        depth += 1;
        if (node == cause) {
            end = builder.length();
        }
        return value;
    }

    /**
     * Whether a node is a statement (vs an expression).
     * @param child the node
     * @return true if node is a statement
     */
    private static boolean isStatement(JexlNode child) {
        return child instanceof ASTJexlScript
                || child instanceof ASTBlock
                || child instanceof ASTIfStatement
                || child instanceof ASTForStatement
                || child instanceof ASTForeachStatement
                || child instanceof ASTFunctionStatement
                || child instanceof ASTWhileStatement
                || child instanceof ASTDoWhileStatement
                || child instanceof ASTTryStatement
                || child instanceof ASTTryWithResourceStatement
                || child instanceof ASTSwitchStatement
                || child instanceof ASTSynchronizedStatement
                || child instanceof ASTAnnotation;
    }

    /**
     * Whether a script or expression ends with a semicolumn.
     * @param cs the string
     * @return true if a semicolumn is the last non-whitespace character
     */
    private static boolean semicolTerminated(CharSequence cs) {
        for(int i = cs.length() - 1; i >= 0; --i) {
            char c = cs.charAt(i);
            if (c == ';') {
                return true;
            }
            if (!Character.isWhitespace(c)) {
                break;
            }
        }
        return false;
    }

    /**
     * Adds a statement node to the rebuilt expression.
     * @param child the child node
     * @param data  visitor pattern argument
     * @return visitor pattern value
     */
    protected Object acceptStatement(final JexlNode child, final Object data) {
        final JexlNode parent = child.jjtGetParent();
        if (indent > 0 && (parent instanceof ASTBlock || parent instanceof ASTJexlScript)) {
            for (int i = 0; i < indentLevel; ++i) {
                for(int s = 0; s < indent; ++s) {
                    builder.append(' ');
                }
            }
        }
        depth -= 1;
        final Object value = accept(child, data);
        depth += 1;
        // blocks, if, for & while don't need a ';' at end
        if (!isStatement(child) && !semicolTerminated(builder)) {
            builder.append(';');
            if (indent > 0) {
                builder.append('\n');
            } else {
                builder.append(' ');
            }
        }
        return value;
    }


    /**
     * Gets printable class name
     * @param c the type
     * @return class name value
     */
    protected String getClassName(Class c) {
        StringBuilder result = new StringBuilder();
        Class literal = c;
        int array = 0;
        while (literal.isArray()) {
            array++;
            literal = literal.getComponentType();
        }
        if (literal != null) {
            if (literal.isMemberClass()) {
                result.append(getClassName(literal.getEnclosingClass()));
                result.append(".");
            }
            result.append(literal.getSimpleName());
        }
        for (int i = 0; i < array; i++) {
            result.append("[]");
        }
        return result.toString();
    }

    /**
     * Checks if a terminal node is the cause to debug &amp; adds its representation to the rebuilt expression.
     * @param node  the child node
     * @param image the child node token image (optionally null)
     * @param data  visitor pattern argument
     * @return visitor pattern value
     */
    protected Object check(final JexlNode node, final String image, final Object data) {
        if (node == cause) {
            start = builder.length();
        }
        if (image != null) {
            builder.append(image);
        } else {
            builder.append(node.toString());
        }
        if (node == cause) {
            end = builder.length();
        }
        return data;
    }

    /**
     * Checks if the children of a node using infix notation is the cause to debug, adds their representation to the
     * rebuilt expression.
     * @param node  the child node
     * @param infix the child node token
     * @param paren whether the child should be parenthesized
     * @param data  visitor pattern argument
     * @return visitor pattern value
     */
    protected Object infixChildren(final JexlNode node, final String infix, final boolean paren, final Object data) {
        final int num = node.jjtGetNumChildren();
        if (paren) {
            builder.append('(');
        }
        for (int i = 0; i < num; ++i) {
            if (i > 0) {
                builder.append(infix);
            }
            accept(node.jjtGetChild(i), data);
        }
        if (paren) {
            builder.append(')');
        }
        return data;
    }

    /**
     * Checks if the child of a node using prefix notation is the cause to debug, adds their representation to the
     * rebuilt expression.
     * @param node   the node
     * @param prefix the node token
     * @param data   visitor pattern argument
     * @return visitor pattern value
     */
    protected Object prefixChild(final JexlNode node, final String prefix, final Object data) {
        final boolean paren = node.jjtGetChild(0).jjtGetNumChildren() > 1;
        builder.append(prefix);
        if (paren) {
            builder.append('(');
        }
        accept(node.jjtGetChild(0), data);
        if (paren) {
            builder.append(')');
        }
        return data;
    }

    /**
     * Checks if the child of a node using postfix notation is the cause to debug, adds their representation to the
     * rebuilt expression.
     * @param node   the node
     * @param suffix the node token
     * @param data   visitor pattern argument
     * @return visitor pattern value
     */
    protected Object postfixChild(JexlNode node, String suffix, Object data) {
        boolean paren = node.jjtGetChild(0).jjtGetNumChildren() > 1;
        if (paren) {
            builder.append('(');
        }
        accept(node.jjtGetChild(0), data);
        if (paren) {
            builder.append(')');
        }
        builder.append(suffix);
        return data;
    }

    @Override
    protected Object visit(final ASTAddNode node, final Object data) {
        return additiveNode(node, " + ", data);
    }

    @Override
    protected Object visit(final ASTSubNode node, final Object data) {
        return additiveNode(node, " - ", data);
    }

    /**
     * Rebuilds an additive expression.
     * @param node the node
     * @param op   the operator
     * @param data visitor pattern argument
     * @return visitor pattern value
     */
    protected Object additiveNode(final JexlNode node, final String op, final Object data) {
        // need parenthesis if not in operator precedence order
        final boolean paren = node.jjtGetParent() instanceof ASTMulNode
                || node.jjtGetParent() instanceof ASTDivNode
                || node.jjtGetParent() instanceof ASTModNode;
        final int num = node.jjtGetNumChildren();
        if (paren) {
            builder.append('(');
        }
        accept(node.jjtGetChild(0), data);
        for (int i = 1; i < num; ++i) {
            builder.append(op);
            accept(node.jjtGetChild(i), data);
        }
        if (paren) {
            builder.append(')');
        }
        return data;
    }

    /**
     * Rebuilds a shift expression.
     * @param node the node
     * @param op   the operator
     * @param data visitor pattern argument
     * @return visitor pattern value
     */
    protected Object shiftNode(JexlNode node, String op, Object data) {
        // need parenthesis if not in operator precedence order
        boolean paren = node.jjtGetParent() instanceof ASTAddNode
                || node.jjtGetParent() instanceof ASTSubNode;
        int num = node.jjtGetNumChildren();
        if (paren) {
            builder.append('(');
        }
        accept(node.jjtGetChild(0), data);
        for (int i = 1; i < num; ++i) {
            builder.append(op);
            accept(node.jjtGetChild(i), data);
        }
        if (paren) {
            builder.append(')');
        }
        return data;
    }

    @Override
    protected Object visit(final ASTAndNode node, final Object data) {
        return infixChildren(node, " && ", false, data);
    }

    @Override
    protected Object visit(final ASTArrayAccess node, final Object data) {
        int num = node.jjtGetNumChildren();
        builder.append('[');
        for (int i = 0; i < num; ++i) {
            if (i > 0) {
                builder.append(',');
            }
            accept(node.jjtGetChild(i), data);
        }
        builder.append(']');
        return data;
    }

    @Override
    protected Object visit(final ASTArrayAccessSafe node, Object data) {
        int num = node.jjtGetNumChildren();
        builder.append("?[");
        for (int i = 0; i < num; ++i) {
            if (i > 0) {
                builder.append(',');
            }
            accept(node.jjtGetChild(i), data);
        }
        builder.append(']');
        return data;
    }

    @Override
    protected Object visit(final ASTArrayLiteral node, Object data) {
        int num = node.jjtGetNumChildren();

        if (node.isImmutable()) {
            builder.append("#");
        }

        builder.append("[ ");
        if (num > 0) {
            accept(node.jjtGetChild(0), data);
            for (int i = 1; i < num; ++i) {
                builder.append(", ");
                accept(node.jjtGetChild(i), data);
            }
        }
        if (node.isExtended()) {
            if (num > 0) {
                builder.append(",");
            }
            builder.append("...");
        }

        builder.append(" ]");
        return data;
    }

    @Override
    protected Object visit(final ASTRangeNode node, final Object data) {
        return infixChildren(node, " .. ", false, data);
    }

    @Override
    protected Object visit(final ASTAssignment node, final Object data) {
        return infixChildren(node, " = ", false, data);
    }

    @Override
    protected Object visit(final ASTNullAssignment node, final Object data) {
        return infixChildren(node, " ?= ", false, data);
    }

    @Override
    protected Object visit(final ASTNEAssignment node, final Object data) {
        return infixChildren(node, " := ", false, data);
    }

    @Override
    protected Object visit(final ASTVarStatement node, final Object data) {
        int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            if (i > 0) {
                builder.append(',');
            }
            accept(node.jjtGetChild(i), data);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTInitialization node, final Object data) {
        accept(node.jjtGetChild(0), data);
        if (node.jjtGetNumChildren() == 2) {
           builder.append(" = ");
           accept(node.jjtGetChild(1), data);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTMultipleIdentifier node, final Object data) {
        int num = node.jjtGetNumChildren();
        boolean isVarDeclare = node.jjtGetChild(0) instanceof ASTExtVar;

        if (isVarDeclare) {
            builder.append("var");
        }

        builder.append('(');
        for (int i = 0; i < num; ++i) {
            if (i > 0) {
                builder.append(',');
            }
            accept(node.jjtGetChild(i), data);
        }
        builder.append(")");
        return data;
    }

    @Override
    protected Object visit(final ASTMultipleAssignment node, final Object data) {
        accept(node.jjtGetChild(0), data);
        builder.append(" = ");
        accept(node.jjtGetChild(1), data);
        return data;
    }

    @Override
    protected Object visit(final ASTMultipleVarStatement node, final Object data) {
        accept(node.jjtGetChild(0), data);
        builder.append(" = ");
        accept(node.jjtGetChild(1), data);
        return data;
    }

    @Override
    protected Object visit(final ASTBitwiseAndNode node, final Object data) {
        return infixChildren(node, " & ", false, data);
    }

    @Override
    protected Object visit(final ASTShiftLeftNode node, final Object data) {
        return shiftNode(node, " << ", data);
    }

    @Override
    protected Object visit(final ASTShiftRightNode node, final Object data) {
        return shiftNode(node, " >> ", data);
    }

    @Override
    protected Object visit(final ASTShiftRightUnsignedNode node, final Object data) {
        return shiftNode(node, " >>> ", data);
    }


    @Override
    protected Object visit(final ASTBitwiseComplNode node, final Object data) {
        return prefixChild(node, "~", data);
    }

    @Override
    protected Object visit(final ASTBitwiseOrNode node, final Object data) {
        final boolean paren = node.jjtGetParent() instanceof ASTBitwiseDiffNode;
        return infixChildren(node, " | ", paren, data);
    }

    @Override
    protected Object visit(final ASTBitwiseDiffNode node, final Object data) {
        final boolean paren = node.jjtGetParent() instanceof ASTBitwiseXorNode;
        return infixChildren(node, " \\ ", paren, data);
    }

    @Override
    protected Object visit(final ASTBitwiseXorNode node, final Object data) {
        final boolean paren = node.jjtGetParent() instanceof ASTBitwiseAndNode;
        return infixChildren(node, " ^ ", paren, data);
    }

    @Override
    protected Object visit(final ASTBlock node, final Object data) {
        String label = node.getLabel();
        if (label != null) {
            builder.append(label);
            builder.append(" : ");
        }
        builder.append('{');
        if (indent > 0) {
            indentLevel += 1;
            builder.append('\n');
        } else {
            builder.append(' ');
        }
        final int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            final JexlNode child = node.jjtGetChild(i);
            acceptStatement(child, data);
        }
        if (indent > 0) {
            indentLevel -= 1;
            for (int i = 0; i < indentLevel; ++i) {
                for(int s = 0; s < indent; ++s) {
                    builder.append(' ');
                }
            }
        }
        builder.append('}');
        return data;
    }

    @Override
    protected Object visit(final ASTDivNode node, final Object data) {
        return infixChildren(node, " / ", false, data);
    }

    @Override
    protected Object visit(final ASTEmptyFunction node, final Object data) {
        builder.append("empty ");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(final ASTAwaitFunction node, final Object data) {
        builder.append("await ");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(final ASTISNode node, final Object data) {
        return infixChildren(node, " === ", false, data);
    }

    @Override
    protected Object visit(final ASTNINode node, final Object data) {
        return infixChildren(node, " !== ", false, data);
    }

    @Override
    protected Object visit(final ASTEQNode node, final Object data) {
        return infixChildren(node, " == ", false, data);
    }

    @Override
    protected Object visit(final ASTEQPredicate node, final Object data) {
        builder.append("== ");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(final ASTERNode node, final Object data) {
        return infixChildren(node, " =~ ", false, data);
    }

    @Override
    protected Object visit(final ASTERPredicate node, final Object data) {
        builder.append("=~ ");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(final ASTSWNode node, final Object data) {
        return infixChildren(node, " =^ ", false, data);
    }

    @Override
    protected Object visit(final ASTSWPredicate node, final Object data) {
        builder.append("=^ ");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(final ASTEWNode node, final Object data) {
        return infixChildren(node, " =$ ", false, data);
    }

    @Override
    protected Object visit(final ASTEWPredicate node, final Object data) {
        builder.append("=$ ");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(final ASTNSWNode node, final Object data) {
        return infixChildren(node, " !^ ", false, data);
    }

    @Override
    protected Object visit(final ASTNSWPredicate node, final Object data) {
        builder.append("!^ ");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(final ASTNEWNode node, final Object data) {
        return infixChildren(node, " !$ ", false, data);
    }

    @Override
    protected Object visit(final ASTNEWPredicate node, final Object data) {
        builder.append("!$ ");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(final ASTContinue node, final Object data) {
        builder.append("continue");
        String label = node.getLabel();
        if (label != null) {
            builder.append(' ');
            builder.append(label);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTRemove node, final Object data) {
        builder.append("remove");
        String label = node.getLabel();
        if (label != null) {
            builder.append(' ');
            builder.append(label);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTBreak node, final Object data) {
        builder.append("break");
        String label = node.getLabel();
        if (label != null) {
            builder.append(' ');
            builder.append(label);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTDelete node, final Object data) {
        builder.append("delete ");

        accept(node.jjtGetChild(0), data);

        return data;
    }

    @Override
    protected Object visit(final ASTForStatement node, final Object data) {
        String label = node.getLabel();
        if (label != null) {
            builder.append(label);
            builder.append(" : ");
        }
        builder.append("for(");
        accept(node.jjtGetChild(0), data);
        builder.append(" ; ");
        accept(node.jjtGetChild(1), data);
        builder.append(" ; ");
        accept(node.jjtGetChild(2), data);
        builder.append(") ");
        if (node.jjtGetNumChildren() > 3) {
            acceptStatement(node.jjtGetChild(3), data);
        } else {
            builder.append(';');
        }
        return data;
    }

    @Override
    protected Object visit(final ASTForInitializationNode node, final Object data) {
        int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            if (i > 0) {
                builder.append(',');
            }
            JexlNode child = node.jjtGetChild(i);
            accept(child, data);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTForTerminationNode node, final Object data) {
        if (node.jjtGetNumChildren() > 0) {
            accept(node.jjtGetChild(0), data);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTForIncrementNode node, final Object data) {
        int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            if (i > 0) {
                builder.append(',');
            }
            JexlNode child = node.jjtGetChild(i);
            accept(child, data);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTForeachStatement node, final Object data) {
        String label = node.getLabel();
        if (label != null) {
            builder.append(label);
            builder.append(" : ");
        }
        builder.append("for(");
        accept(node.jjtGetChild(0), data);
        builder.append(" : ");
        accept(node.jjtGetChild(1), data);
        builder.append(") ");
        if (node.jjtGetNumChildren() > 2) {
            acceptStatement(node.jjtGetChild(2), data);
        } else {
            builder.append(';');
        }
        return data;
    }

    @Override
    protected Object visit(final ASTForeachVar node, final Object data) {
        accept(node.jjtGetChild(0), data);
        if (node.jjtGetNumChildren() > 1) {
            builder.append(", ");
            accept(node.jjtGetChild(1), data);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTTryStatement node, final Object data) {
        int num = node.jjtGetNumChildren();
        builder.append("try ");
        accept(node.jjtGetChild(0), data);
        for (int i = 1; i < num; ++i) {
            JexlNode child = node.jjtGetChild(i);
            if (!(child instanceof ASTCatchBlock)) {
                builder.append(" finally ");
            }
            accept(child, data);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTTryVar node, final Object data) {
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(final ASTCatchBlock node, final Object data) {
        int num = node.jjtGetNumChildren();
        builder.append(" catch ");
        if (num > 1) {
            builder.append("(");
            accept(node.jjtGetChild(0), data);
            builder.append(") ");
        }
        accept(node.jjtGetChild(num - 1), data);
        return data;
    }

    @Override
    protected Object visit(final ASTTryWithResourceStatement node, final Object data) {
        int num = node.jjtGetNumChildren();
        builder.append("try ");
        builder.append("(");
        accept(node.jjtGetChild(0), data);
        builder.append(")");
        accept(node.jjtGetChild(1), data);
        for (int i = 2; i < num; ++i) {
            JexlNode child = node.jjtGetChild(i);
            if (!(child instanceof ASTCatchBlock)) {
                builder.append(" finally ");
            }
            accept(child, data);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTTryResource node, final Object data) {
        accept(node.jjtGetChild(0), data);
        if (node.jjtGetNumChildren() > 1) {
            builder.append("=");
            accept(node.jjtGetChild(1), data);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTGENode node, final Object data) {
        return infixChildren(node, " >= ", false, data);
    }

    @Override
    protected Object visit(final ASTGEPredicate node, final Object data) {
        builder.append(">= ");
        accept(node.jjtGetChild(0), data);
        return data;

    }

    @Override
    protected Object visit(final ASTGTNode node, final Object data) {
        return infixChildren(node, " > ", false, data);
    }

    @Override
    protected Object visit(final ASTGTPredicate node, final Object data) {
        builder.append("> ");
        accept(node.jjtGetChild(0), data);
        return data;

    }

    /** Checks identifiers that contain spaces or punctuation
     * (but underscore, at-sign, sharp-sign and dollar).
     */
    protected static final Pattern QUOTED_IDENTIFIER =
            Pattern.compile("\\s|\\p{Punct}&&[^@#$_]");

    /**
     * Checks whether an identifier should be quoted or not.
     * @param str the identifier
     * @return true if needing quotes, false otherwise
     */
    protected boolean needQuotes(final String str) {
        return QUOTED_IDENTIFIER.matcher(str).find()
                || "size".equals(str)
                || "empty".equals(str);
    }

    @Override
    protected Object visit(final ASTIdentifier node, final Object data) {
        final String ns = node.getNamespace();
        final String image = StringParser.escapeIdentifier(node.getName());
        if (ns == null) {
            return check(node, image, data);
        }
        final String nsid = StringParser.escapeIdentifier(ns) + ":" + image;
        return check(node, nsid, data);
    }

    @Override
    protected Object visit(final ASTIdentifierAccess node, final Object data) {
        builder.append(node.isSafe() ? "?." : ".");
        final String image = node.getName();
        if (node.isExpression()) {
            builder.append('`');
            builder.append(image.replace("`", "\\`"));
            builder.append('`');
        } else if (needQuotes(image)) {
            // quote it
            builder.append('\'');
            builder.append(image.replace("'", "\\'"));
            builder.append('\'');
        } else {
            builder.append(image);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTFieldAccess node, final Object data) {
        builder.append(".@");
        final String image = node.getName();
        builder.append(image);
        return data;
    }

    @Override
    protected Object visit(final ASTExpressionStatement node, final Object data) {
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(final ASTFunctionStatement node, final Object data) {
        // Last node is function
        ASTJexlScript script = (ASTJexlScript) node.jjtGetChild(1);
        if (script.getScope().isStatic()) {
            builder.append("static ");
        }
        Class type = script.getScope().getReturnType();
        if (type != null) {
            builder.append(getClassName(type));
        } else {
            builder.append("function ");
        }
        // Name
        accept(node.jjtGetChild(0), data);
        // Function
        accept(script, data);
        return data;
    }

    @Override
    protected Object visit(final ASTIfStatement node, final Object data) {
        final int numChildren = node.jjtGetNumChildren();
        // if (...) ...
        builder.append("if (");
        accept(node.jjtGetChild(0), data);
        builder.append(") ");
        acceptStatement(node.jjtGetChild(1), data);
        // else...
        if (numChildren > 2) {
            builder.append(" else ");
            acceptStatement(node.jjtGetChild(2), data);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTNumberLiteral node, final Object data) {
        return check(node, node.toString(), data);
    }

    /**
     * A pseudo visitor for parameters.
     * @param p the parameter name
     * @param data the visitor argument
     * @return the parameter name to use
     */
    protected String visitParameter(final String p, final Object data) {
        return p;
    }

    /**
     * A formatter for default values of parameters.
     * @param data the data to format
     * @return the formatted value
     */
    protected String formatDefaultValue(Object data) {
        if (data instanceof String) {
            String img = ((String) data).replace("'", "\\'");
            return "'" + img + "'";
        } else if (data instanceof Pattern) {
            String img = data.toString().replace("/", "\\/");
            return "~/" + img + "/";
        } else if (data instanceof BigDecimal) {
            DecimalFormat BIGDF = new DecimalFormat("0.0b", new DecimalFormatSymbols(Locale.ENGLISH));
            return BIGDF.format((BigDecimal) data);
        } else if (data instanceof Number && Double.isNaN(((Number) data).doubleValue())) {
            return "NaN";
        } else {
            StringBuilder strb = new StringBuilder(String.valueOf(data));
            if (data instanceof Float) {
                strb.append('f');
            } else if (data instanceof Double) {
                strb.append('d');
            } else if (data instanceof BigInteger) {
                strb.append('h');
            } else if (data instanceof Long) {
                strb.append('l');
            }
            return strb.toString();
        }
    }

    private static  boolean isLambdaExpr(ASTJexlLambda lambda) {
        return lambda.jjtGetNumChildren() == 1 && !isStatement(lambda.jjtGetChild(0));
    }

    /**
     * Stringifies the pragmas.
     * @param builder where to stringify
     * @param pragmas the pragmas, may be null
     */
    private static void writePragmas(StringBuilder builder, Map<String, Object> pragmas) {
        if (pragmas != null) {
            for (final Map.Entry<String, Object> pragma : pragmas.entrySet()) {
                final String key = pragma.getKey();
                final Object value = pragma.getValue();
                final Set<Object> values = value instanceof Set<?>
                    ? (Set<Object>) value
                    : Collections.singleton(value);
                for (final Object pragmaValue : values) {
                    builder.append("#pragma ");
                    builder.append(key);
                    builder.append(' ');
                    builder.append(pragmaValue.toString());
                    builder.append('\n');
                }
            }
        }

    }

    @Override
    protected Object visit(final ASTJexlScript node, final Object arg) {
        if (outputPragmas) {
            writePragmas(builder, node.getPragmas());
        }
        Object data = arg;
        // if single expression lambda
        boolean expr = false;
        // if lambda, produce parameters
        if (node instanceof ASTSimpleLambda) {
            expr = true;
        } else if (node instanceof ASTJexlLambda) {
            if (node.jjtGetNumChildren() == 1) {
               JexlNode child = node.jjtGetChild(0);
               if (!(child instanceof ASTBlock)) {
                   expr = true;
               }
            }

            JexlNode parent = node.jjtGetParent();
            boolean function = parent instanceof ASTFunctionStatement;

            Scope scope = node.getScope();

            String[] params = node.getParameters();
            boolean varSyntax = false;
            if (params != null) {
                for (String param : params) {
                    int symbol = scope.getSymbol(param);
                    Class type = scope.getVariableType(symbol);
                    boolean isLexical = scope.isVariableLexical(symbol);
                    boolean isFinal = scope.isVariableFinal(symbol);
                    boolean isRequired = scope.isVariableRequired(symbol);
                    Object value = scope.getVariableValue(symbol);
                    if (isLexical || isFinal || isRequired || type != null || value != null) {
                        varSyntax = true;
                        break;
                    }
                }
            }
            Class retType = scope.getReturnType();

            // use lambda syntax if not assigned
            boolean named = (parent instanceof ASTAssignment || parent instanceof ASTNullAssignment) && !expr;

            if (retType != null) {
                builder.append(getClassName(retType));
            } else if (named) {
                builder.append("function");
            }

            boolean parens = named || function || params == null || params.length != 1 
                             || node.isVarArgs() || varSyntax || retType != null;

            if (parens) {
                builder.append('(');
            }

            if (params != null && params.length > 0) {
                for (int p = 0; p < params.length; ++p) {
                    if (p > 0) {
                        builder.append(", ");
                    }
                    String param = params[p];
                    int symbol = scope.getSymbol(param);
                    boolean isLexical = scope.isVariableLexical(symbol);
                    boolean isFinal = scope.isVariableFinal(symbol);

                    if (isLexical) {
                        if (isFinal) {
                            builder.append("const ");
                        } else {
                            builder.append("let ");
                        }
                    } else {
                        if (isFinal) {
                            builder.append("final ");
                        }
                        if (varSyntax) {
                            Class type = scope.getVariableType(symbol);
                            if (type == null) {
                                builder.append("var ");
                            } else {
                                builder.append(getClassName(type)).append(" ");
                            }
                        }
                        boolean isRequired = scope.isVariableRequired(symbol);
                        if (isRequired) {
                            builder.append("&");
                        }
                    }
                    builder.append(visitParameter(param, data));
                    Object value = scope.getVariableValue(symbol);
                    if (value != null) {
                        builder.append(" = ");
                        builder.append(formatDefaultValue(value));
                    }
                }
                if (node.isVarArgs()) {
                    builder.append("...");
                }
            }

            if (parens) {
                builder.append(')');
            }

            if (named) {
                builder.append(' ');
            } else {
                if (expr) {
                    builder.append(arrow);
                } else if (!function) {
                    builder.append(arrow);
                }
            }
        }
        // no parameters or done with them
        final int num = node.jjtGetNumChildren();
        if (num == 1 && (expr || !(node instanceof ASTJexlLambda))) {
            return accept(node.jjtGetChild(0), data);
        } else {
            for (int i = 0; i < num; ++i) {
                final JexlNode child = node.jjtGetChild(i);
                acceptStatement(child, data);
            }
        }
        return data;
    }

    @Override
    protected Object visit(final ASTLENode node, final Object data) {
        return infixChildren(node, " <= ", false, data);
    }

    @Override
    protected Object visit(final ASTLEPredicate node, final Object data) {
        builder.append("<= ");
        accept(node.jjtGetChild(0), data);
        return data;

    }

    @Override
    protected Object visit(final ASTLTNode node, final Object data) {
        return infixChildren(node, " < ", false, data);
    }

    @Override
    protected Object visit(final ASTLTPredicate node, final Object data) {
        builder.append("< ");
        accept(node.jjtGetChild(0), data);
        return data;

    }

    @Override
    protected Object visit(final ASTIOFNode node, final Object data) {
        return infixChildren(node, " instanceof ", false, data);
    }

    @Override
    protected Object visit(final ASTNIOFNode node, final Object data) {
        return infixChildren(node, " !instanceof ", false, data);
    }

    @Override
    protected Object visit(final ASTMapEntry node, final Object data) {
        accept(node.jjtGetChild(0), data);
        builder.append(" : ");
        accept(node.jjtGetChild(1), data);
        return data;
    }

    @Override
    protected Object visit(final ASTMapEntryLiteral node, final Object data) {
        builder.append("[");
        accept(node.jjtGetChild(0), data);
        builder.append(" : ");
        accept(node.jjtGetChild(1), data);
        builder.append("]");
        return data;
    }

    @Override
    protected Object visit(final ASTMapEnumerationNode node, final Object data) {
        return prefixChild(node, "*:", data);
    }

    @Override
    protected Object visit(final ASTSetLiteral node, final Object data) {
        if (node.isImmutable()) {
            builder.append("#");
        }
        builder.append("{ ");
        final int num = node.jjtGetNumChildren();
        if (num > 0) {
            accept(node.jjtGetChild(0), data);
            for (int i = 1; i < num; ++i) {
                builder.append(",");
                accept(node.jjtGetChild(i), data);
            }
        }
        if (node.isOrdered()) {
            if (num > 0) {
                builder.append(",");
            }
            builder.append("...");
        }
        builder.append(" }");
        return data;
    }

    @Override
    protected Object visit(final ASTSetOperand node, final Object data) {
        builder.append("?");
        if (!node.isAny()) {
            builder.append("?");
        }
        builder.append("(");
        int num = node.jjtGetNumChildren();
        if (num > 0) {
            accept(node.jjtGetChild(0), data);
            for (int i = 1; i < num; ++i) {
                builder.append(",");
                accept(node.jjtGetChild(i), data);
            }
        }
        builder.append(")");
        return data;
    }

    @Override
    protected Object visit(final ASTMapLiteral node, final Object data) {
        if (node.isImmutable()) {
            builder.append("#");
        }
        builder.append("{ ");
        final int num = node.jjtGetNumChildren();
        if (num > 0) {
            accept(node.jjtGetChild(0), data);
            for (int i = 1; i < num; ++i) {
                builder.append(",");
                accept(node.jjtGetChild(i), data);
            }
        } else {
            builder.append(':');
        }
        if (node.isOrdered()) {
            if (num > 0) {
                builder.append(",");
            }
            builder.append("...");
        }
        builder.append(" }");
        return data;
    }

    @Override
    protected Object visit(ASTInlinePropertyArrayEntry node, Object data) {
        builder.append("[");
        accept(node.jjtGetChild(0), data);
        builder.append("] : ");
        accept(node.jjtGetChild(1), data);
        return data;
    }

    @Override
    protected Object visit(ASTInlinePropertyArrayNullEntry node, Object data) {
        builder.append("[");
        accept(node.jjtGetChild(0), data);
        builder.append("] ?: ");
        accept(node.jjtGetChild(1), data);
        return data;
    }

    @Override
    protected Object visit(ASTInlinePropertyArrayNEEntry node, Object data) {
        builder.append("[");
        accept(node.jjtGetChild(0), data);
        builder.append("] =: ");
        accept(node.jjtGetChild(1), data);
        return data;
    }

    @Override
    protected Object visit(ASTInlinePropertyEntry node, Object data) {
        int num = node.jjtGetNumChildren();
        if (num > 1) {
            accept(node.jjtGetChild(0), data);
            builder.append(" : ");
            accept(node.jjtGetChild(1), data);
        } else {
            builder.append(" * : ");
            accept(node.jjtGetChild(0), data);
        }
        return data;
    }

    @Override
    protected Object visit(ASTInlinePropertyNullEntry node, Object data) {
        accept(node.jjtGetChild(0), data);
        builder.append(" ?: ");
        accept(node.jjtGetChild(1), data);
        return data;
    }

    @Override
    protected Object visit(ASTInlinePropertyNEEntry node, Object data) {
        accept(node.jjtGetChild(0), data);
        builder.append(" =: ");
        accept(node.jjtGetChild(1), data);
        return data;
    }

    @Override
    protected Object visit(ASTInlineFieldEntry node, Object data) {
        accept(node.jjtGetChild(0), data);
        builder.append(" : ");
        accept(node.jjtGetChild(1), data);
        return data;
    }

    @Override
    protected Object visit(ASTInlineFieldNullEntry node, Object data) {
        accept(node.jjtGetChild(0), data);
        builder.append(" ?: ");
        accept(node.jjtGetChild(1), data);
        return data;
    }

    @Override
    protected Object visit(ASTInlineFieldNEEntry node, Object data) {
        accept(node.jjtGetChild(0), data);
        builder.append(" =: ");
        accept(node.jjtGetChild(1), data);
        return data;
    }

    @Override
    protected Object visit(ASTInlinePropertyAssignment node, Object data) {
        int num = node.jjtGetNumChildren();
        builder.append("{ ");
        for (int i = 0; i < num; ++i) {
            if (i > 0) {
                builder.append(",");
            }
            accept(node.jjtGetChild(i), data);
        }
        builder.append(" }");
        return data;
    }

    @Override
    protected Object visit(final ASTConstructorNode node, final Object data) {
        final int num = node.jjtGetNumChildren();
        builder.append("new(");
        if (num > 0) {
            accept(node.jjtGetChild(0), data);
            for (int i = 1; i < num; ++i) {
                builder.append(", ");
                accept(node.jjtGetChild(i), data);
            }
        }
        builder.append(")");
        return data;
    }

    @Override
    protected Object visit(final ASTQualifiedConstructorNode node, final Object data) {
        int num = node.jjtGetNumChildren();
        builder.append("new ");
        accept(node.jjtGetChild(0), data);
        accept(node.jjtGetChild(1), data);
        return data;
    }

    @Override
    protected Object visit(final ASTArrayConstructorNode node, final Object data) {
        int num = node.jjtGetNumChildren();
        builder.append("new ");
        accept(node.jjtGetChild(0), data);
        for (int i = 1; i < num; ++i) {
            builder.append("[");
            accept(node.jjtGetChild(i), data);
            builder.append("]");
        }
        return data;
    }

    @Override
    protected Object visit(final ASTArrayOpenDimension node, final Object data) {
        return data;
    }

    @Override
    protected Object visit(final ASTInitializedArrayConstructorNode node, final Object data) {
        int num = node.jjtGetNumChildren();
        builder.append("new ");
        accept(node.jjtGetChild(0), data);
        builder.append("[]");
        builder.append("{");
        for (int i = 1; i < num; ++i) {
            if (i > 1) {
                builder.append(", ");
            }
            accept(node.jjtGetChild(i), data);
        }
        builder.append("}");
        return data;
    }

    @Override
    protected Object visit(final ASTInitializedCollectionConstructorNode node, final Object data) {
        int num = node.jjtGetNumChildren();
        builder.append("new ");
        accept(node.jjtGetChild(0), data);
        builder.append("{");
        for (int i = 1; i < num; ++i) {
            if (i > 1) {
                builder.append(", ");
            }
            accept(node.jjtGetChild(i), data);
        }
        builder.append("}");
        return data;
    }

    @Override
    protected Object visit(final ASTInitializedMapConstructorNode node, final Object data) {
        int num = node.jjtGetNumChildren();
        builder.append("new ");
        accept(node.jjtGetChild(0), data);
        builder.append("{");
        for (int i = 1; i < num; ++i) {
            if (i > 1) {
                builder.append(", ");
            }
            accept(node.jjtGetChild(i), data);
        }
        builder.append("}");
        return data;
    }

    @Override
    protected Object visit(final ASTFunctionNode node, final Object data) {
        final int num = node.jjtGetNumChildren();
        if (num == 3) {
            accept(node.jjtGetChild(0), data);
            builder.append(":");
            accept(node.jjtGetChild(1), data);
            accept(node.jjtGetChild(2), data);
        } else if (num == 2) {
            accept(node.jjtGetChild(0), data);
            accept(node.jjtGetChild(1), data);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTMethodNode node, final Object data) {
        final int num = node.jjtGetNumChildren();
        if (num == 2) {
            accept(node.jjtGetChild(0), data);
            accept(node.jjtGetChild(1), data);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTMethodReference node, final Object data) {
        builder.append("::");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(final ASTInnerConstructorNode node, final Object data) {
        builder.append(".new ");
        int num = node.jjtGetNumChildren();
        if (num == 2) {
            accept(node.jjtGetChild(0), data);
            accept(node.jjtGetChild(1), data);
        }
        return data;
    }

    @Override
    protected Object visit(ASTArguments node, Object data) {
        final int num = node.jjtGetNumChildren();
        builder.append("(");
        if (num > 0) {
            accept(node.jjtGetChild(0), data);
            for (int i = 1; i < num; ++i) {
                builder.append(", ");
                accept(node.jjtGetChild(i), data);
            }
        }
        builder.append(")");
        return data;
    }

    @Override
    protected Object visit(final ASTModNode node, final Object data) {
        return infixChildren(node, " % ", false, data);
    }

    @Override
    protected Object visit(final ASTMulNode node, final Object data) {
        return infixChildren(node, " * ", false, data);
    }

    @Override
    protected Object visit(final ASTNENode node, final Object data) {
        return infixChildren(node, " != ", false, data);
    }

    @Override
    protected Object visit(final ASTNEPredicate node, final Object data) {
        builder.append("!= ");
        accept(node.jjtGetChild(0), data);
        return data;

    }

    @Override
    protected Object visit(final ASTNRNode node, final Object data) {
        return infixChildren(node, " !~ ", false, data);
    }

    @Override
    protected Object visit(final ASTNRPredicate node, final Object data) {
        builder.append("!~ ");
        accept(node.jjtGetChild(0), data);
        return data;

    }

    @Override
    protected Object visit(final ASTNotNode node, final Object data) {
        builder.append("!");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(final ASTCastNode node, final Object data) {
        builder.append("(");
        accept(node.jjtGetChild(0), data);
        builder.append(")");
        accept(node.jjtGetChild(1), data);
        return data;
    }

    @Override
    protected Object visit(final ASTNullLiteral node, final Object data) {
        builder.append(node.toString());
        return data;
    }

    @Override
    protected Object visit(final ASTUnderscoreLiteral node, final Object data) {
        check(node, "_", data);
        return data;
    }

    @Override
    protected Object visit(final ASTOrNode node, final Object data) {
        // need parenthesis if not in operator precedence order
        final boolean paren = node.jjtGetParent() instanceof ASTAndNode;
        return infixChildren(node, " || ", paren, data);
    }

    @Override
    protected Object visit(final ASTReference node, final Object data) {
        final int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            accept(node.jjtGetChild(i), data);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTAttributeReference node, final Object data) {
        builder.append('@');
        builder.append(node.getName());
        return data;
    }

    @Override
    protected Object visit(final ASTEnclosedExpression node, final Object data) {
        JexlNode first = node.jjtGetChild(0);
        builder.append('(');
        accept(first, data);
        builder.append(')');
        return data;
    }

    @Override
    protected Object visit(final ASTReturnStatement node, final Object data) {
        builder.append("return ");
        int num = node.jjtGetNumChildren();
        if (num > 0) {
            accept(node.jjtGetChild(0), data);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTYieldStatement node, final Object data) {
        builder.append("yield ");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(final ASTThrowStatement node, final Object data) {
        builder.append("throw ");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(final ASTAssertStatement node, final Object data) {
        builder.append("assert ");
        accept(node.jjtGetChild(0), data);
        int num = node.jjtGetNumChildren();
        if (num > 1) {
            builder.append(" : ");
            accept(node.jjtGetChild(1), data);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTSizeFunction node, final Object data) {
        builder.append("size ");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(final ASTStringLiteral node, Object data) {
        String img = StringParser.escapeString(node.getLiteral(), '\'');
        return this.check(node, img, data);
    }

    @Override
    protected Object visit(final ASTJxltLiteral node, Object data) {
        String img = StringParser.escapeString(node.getLiteral(), '`');
        return this.check(node, img, data);
    }

    @Override
    protected Object visit(final ASTTextBlockLiteral node, final Object data) {
        String img = node.toString();
        return check(node, img, data);
    }

    @Override
    protected Object visit(final ASTStringBuilderLiteral node, final Object data) {
        accept(node.jjtGetChild(0), data);
        builder.append("...");
        return data;
    }

    @Override
    protected Object visit(final ASTRegexLiteral node, final Object data) {
        String img = StringParser.escapeString(node.toString(), '/');
        return check(node, "~" + img, data);
    }

    @Override
    protected Object visit(final ASTClassLiteral node, final Object data) {
        builder.append(node.toString());
        builder.append(".class");
        return data;
    }

    @Override
    protected Object visit(final ASTTypeLiteral node, final Object data) {
        builder.append(node.toString());
        return data;
    }

    @Override
    protected Object visit(final ASTTernaryNode node, final Object data) {
        accept(node.jjtGetChild(0), data);
        builder.append("? ");
        accept(node.jjtGetChild(1), data);
        if (node.jjtGetNumChildren() > 2) {
            builder.append(" : ");
            accept(node.jjtGetChild(2), data);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTElvisNode node, final Object data) {
        accept(node.jjtGetChild(0), data);
        builder.append("?: ");
        accept(node.jjtGetChild(1), data);
        return data;
    }

    @Override
    protected Object visit(final ASTNullpNode node, final Object data) {
        accept(node.jjtGetChild(0), data);
        builder.append("??");
        accept(node.jjtGetChild(1), data);
        return data;
    }

    @Override
    protected Object visit(final ASTBooleanLiteral node, final Object data) {
        builder.append(node.toString());
        return data;
    }

    @Override
    protected Object visit(final ASTThisNode node, final Object data) {
        check(node, "this", data);
        return data;
    }

    @Override
    protected Object visit(final ASTCurrentNode node, final Object data) {
        check(node, "@", data);
        return data;
    }

    @Override
    protected Object visit(final ASTUnaryMinusNode node, final Object data) {
        return prefixChild(node, "-", data);
    }

    @Override
    protected Object visit(final ASTUnaryPlusNode node, final Object data) {
        return prefixChild(node, "+", data);
    }

    @Override
    protected Object visit(final ASTIncrementGetNode node, final Object data) {
        return prefixChild(node, "++", data);
    }

    @Override
    protected Object visit(final ASTDecrementGetNode node, final Object data) {
        return prefixChild(node, "--", data);
    }

    @Override
    protected Object visit(final ASTGetIncrementNode node, final Object data) {
        return postfixChild(node, "++", data);
    }

    @Override
    protected Object visit(final ASTGetDecrementNode node, final Object data) {
        return postfixChild(node, "--", data);
    }

    @Override
    protected Object visit(final ASTIndirectNode node, final Object data) {
        return prefixChild(node, "*", data);
    }

    @Override
    protected Object visit(final ASTPointerNode node, final Object data) {
        builder.append("&");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(final ASTEnumerationNode node, final Object data) {
        int num = node.jjtGetNumChildren();
        if (num == 1) {
            builder.append("...");
            accept(node.jjtGetChild(0), data);
            return data;
        } else {
            builder.append("...(");
            accept(node.jjtGetChild(0), data);
            builder.append(':');
            accept(node.jjtGetChild(1), data);
            builder.append(")");
            return data;
        }
    }

    @Override
    protected Object visit(final ASTEnumerationReference node, final Object data) {
        int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            accept(node.jjtGetChild(i), data);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTVar node, final Object data) {
        if (node.isLexical()) {
            if (node.isConstant()) {
                builder.append("const ");
            } else {
                builder.append("let ");
            }
        } else {
            if (node.isConstant()) {
                builder.append("final ");
            }
            Class type = node.getType();
            if (type == null) {
                builder.append("var ");
            } else {
                builder.append(getClassName(type)).append(" ");
            }
            if (node.isRequired()) {
                builder.append("&");
            }
        }
        check(node, node.getName(), data);
        return data;
    }

    @Override
    protected Object visit(final ASTFunctionVar node, final Object data) {
        Class type = node.getType();
        if (type != null) {
            builder.append(getClassName(type)).append(" ");
        }
        check(node, node.getName(), data);
        return data;
    }

    @Override
    protected Object visit(final ASTMultiVar node, final Object data) {
        boolean first = true;
        for (Class type : node.getTypes()) {
            if (!first) {
                builder.append(" | ");
            }
            builder.append(getClassName(type)).append(" ");
            first = false;
        }
        check(node, node.getName(), data);
        return data;
    }

    @Override
    protected Object visit(final ASTExtVar node, final Object data) {
        check(node, node.getName(), data);
        return data;
    }

    @Override
    protected Object visit(final ASTWhileStatement node, final Object data) {
        String label = node.getLabel();
        if (label != null) {
            builder.append(label);
            builder.append(" : ");
        }
        builder.append("while (");
        accept(node.jjtGetChild(0), data);
        builder.append(") ");
        if (node.jjtGetNumChildren() > 1) {
            acceptStatement(node.jjtGetChild(1), data);
        } else {
            builder.append(';');
        }
        return data;
    }

    @Override
    protected Object visit(final ASTDoWhileStatement node, final Object data) {
        String label = node.getLabel();
        if (label != null) {
            builder.append(label);
            builder.append(" : ");
        }
        builder.append("do ");
        final int nc = node.jjtGetNumChildren();
        if (nc > 1) {
            acceptStatement(node.jjtGetChild(0), data);
        } else {
            builder.append(";");
        }
        builder.append(" while (");
        accept(node.jjtGetChild(nc - 1), data);
        builder.append(")");
        return data;
    }

    @Override
    protected Object visit(final ASTSynchronizedStatement node, final Object data) {
        builder.append("synchronized (");
        accept(node.jjtGetChild(0), data);
        builder.append(") ");
        if (node.jjtGetNumChildren() > 1) {
            acceptStatement(node.jjtGetChild(1), data);
        } else {
            builder.append(';');
        }
        return data;
    }

    @Override
    protected Object visit(final ASTSwitchStatement node, final Object data) {
        String label = node.getLabel();
        if (label != null) {
            builder.append(label);
            builder.append(" : ");
        }
        builder.append("switch (");
        accept(node.jjtGetChild(0), data);
        builder.append(") {");
        for (int i = 1; i < node.jjtGetNumChildren(); i++) {
            accept(node.jjtGetChild(i), data);
        }
        builder.append("}");
        return data;
    }

    @Override
    protected Object visit(final ASTSwitchStatementCase node, final Object data) {
        builder.append("case ");
        accept(node.jjtGetChild(0), data);
        builder.append(" : ");
        if (node.jjtGetNumChildren() > 1) {
            for (int i = 1; i < node.jjtGetNumChildren(); i++) {
                acceptStatement(node.jjtGetChild(i), data);
            }
        } else {
            builder.append(';');
        }
        return data;
    }

    @Override
    protected Object visit(final ASTSwitchStatementDefault node, final Object data) {
        builder.append("default : ");
        if (node.jjtGetNumChildren() > 0) {
            for (int i = 0; i < node.jjtGetNumChildren(); i++) {
                acceptStatement(node.jjtGetChild(i), data);
            }
        } else {
            builder.append(';');
        }
        return data;
    }

    @Override
    protected Object visit(final ASTSwitchExpression node, final Object data) {
        builder.append("switch (");
        accept(node.jjtGetChild(0), data);
        builder.append(") {");
        for (int i = 1; i < node.jjtGetNumChildren(); i++) {
            accept(node.jjtGetChild(i), data);
        }
        builder.append("}");
        return data;
    }

    @Override
    protected Object visit(final ASTSwitchExpressionCase node, final Object data) {
        builder.append("case ");
        accept(node.jjtGetChild(0), data);
        builder.append(" -> ");
        if (node.jjtGetNumChildren() > 1) {
            acceptStatement(node.jjtGetChild(1), data);
        } else {
            builder.append(';');
        }
        return data;
    }

    @Override
    protected Object visit(final ASTSwitchCaseLabel node, final Object data) {
        if (node.jjtGetChild(0) instanceof ASTVar) {
            accept(node.jjtGetChild(0), data);
            if (node.jjtGetNumChildren() > 1) {
                builder.append(" when ");
                accept(node.jjtGetChild(1), data);
            }
        } else {
            for (int i = 0; i < node.jjtGetNumChildren(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                accept(node.jjtGetChild(i), data);
            }

            if (node.isDefault())
                builder.append(", default");
        }

        return data;
    }

    @Override
    protected Object visit(final ASTSwitchExpressionDefault node, final Object data) {
        builder.append("default -> ");
        if (node.jjtGetNumChildren() > 0) {
            acceptStatement(node.jjtGetChild(0), data);
        } else {
            builder.append(';');
        }
        return data;
    }

    @Override
    protected Object visit(final ASTSetAddNode node, final Object data) {
        return infixChildren(node, " += ", false, data);
    }

    @Override
    protected Object visit(final ASTSetSubNode node, final Object data) {
        return infixChildren(node, " -= ", false, data);
    }

    @Override
    protected Object visit(final ASTSetMultNode node, final Object data) {
        return infixChildren(node, " *= ", false, data);
    }

    @Override
    protected Object visit(final ASTSetDivNode node, final Object data) {
        return infixChildren(node, " /= ", false, data);
    }

    @Override
    protected Object visit(final ASTSetModNode node, final Object data) {
        return infixChildren(node, " %= ", false, data);
    }

    @Override
    protected Object visit(final ASTSetAndNode node, final Object data) {
        return infixChildren(node, " &= ", false, data);
    }

    @Override
    protected Object visit(final ASTSetOrNode node, final Object data) {
        return infixChildren(node, " |= ", false, data);
    }

    @Override
    protected Object visit(final ASTSetDiffNode node, final Object data) {
        return infixChildren(node, " \\= ", false, data);
    }

    @Override
    protected Object visit(final ASTSetXorNode node, final Object data) {
        return infixChildren(node, " ^= ", false, data);
    }

    @Override
    protected Object visit(final ASTSetShiftRightNode node, final Object data) {
        return infixChildren(node, " >>= ", false, data);
    }

    @Override
    protected Object visit(final ASTSetShiftRightUnsignedNode node, final Object data) {
        return infixChildren(node, " >>>= ", false, data);
    }

    @Override
    protected Object visit(final ASTSetShiftLeftNode node, final Object data) {
        return infixChildren(node, " <<= ", false, data);
    }

    @Override
    protected Object visit(final ASTAnnotation node, final Object data) {
        final int num = node.jjtGetNumChildren();
        builder.append('@');
        builder.append(node.getName());
        if (num > 0) {
            accept(node.jjtGetChild(0), data); // zut
        }
        return null;
    }

    @Override
    protected Object visit(final ASTAnnotatedStatement node, final Object data) {
        final int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            if (i > 0) {
                builder.append(' ');
            }
            final JexlNode child = node.jjtGetChild(i);
            acceptStatement(child, data);
        }
        return data;
    }
    @Override
    protected Object visit(ASTProjectionNode node, Object data) {
        int num = node.jjtGetNumChildren();
        builder.append(".{");
        for (int i = 0; i < num; ++i) {
            if (i > 0) {
                builder.append(',');
            }
            accept(node.jjtGetChild(i), data);
        }
        builder.append('}');
        return data;
    }

    @Override
    protected Object visit(ASTMapProjectionNode node, Object data) {
        builder.append(".{");
        int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; i += 2) {
            if (i > 0) {
                builder.append(',');
            }
            accept(node.jjtGetChild(i), data);
            builder.append(':');
            accept(node.jjtGetChild(i+1), data);
        }
        builder.append('}');
        return data;
    }

    @Override
    protected Object visit(ASTSelectionNode node, Object data) {
        builder.append(".[");
        accept(node.jjtGetChild(0), data);
        builder.append(']');
        return data;
    }

    @Override
    protected Object visit(ASTStartCountNode node, Object data) {
        return prefixChild(node, ">", data);
    }

    @Override
    protected Object visit(ASTStopCountNode node, Object data) {
        return prefixChild(node, "<", data);
    }

    @Override
    protected Object visit(ASTPipeNode node, Object data) {
        builder.append(".(");
        accept(node.jjtGetChild(0), data);
        builder.append(')');
        return data;
    }
}
