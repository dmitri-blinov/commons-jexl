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
package org.apache.commons.jexl3.internal;

import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.JexlScript;
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
import org.apache.commons.jexl3.parser.ASTIfStatement;
import org.apache.commons.jexl3.parser.ASTIOFNode;
import org.apache.commons.jexl3.parser.ASTISNode;
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
import org.apache.commons.jexl3.parser.ParserVisitor;

/**
 * Fully abstract to avoid public interface exposition.
 */
public class ScriptVisitor extends ParserVisitor {
    /**
     * Visits all AST constituents of a JEXL expression.
     * @param jscript the expression
     * @param data some data context
     * @return the visit result or null if jscript was not a Script implementation
     */
    public Object visitExpression (final JexlExpression jscript, final Object data) {
        if (jscript instanceof Script) {
            return ((Script) jscript).getScript().jjtAccept(this, data);
        }
        return null;
    }

    /**
     * Visits all AST constituents of a JEXL script.
     * @param jscript the expression
     * @param data some data context
     * @return the visit result or null if jscript was not a Script implementation
     */
    public Object visitScript(final JexlScript jscript, final Object data) {
        if (jscript instanceof Script) {
            return ((Script) jscript).getScript().jjtAccept(this, data);
        }
        return null;
    }

    /**
     * Visits a node.
     * Default implementation visits all its children.
     * @param node the node to visit
     * @param data visitor pattern argument
     * @return visitor pattern value
     */
    protected Object visitNode(final JexlNode node, final Object data) {
        return node.childrenAccept(this, data);
    }

    @Override
    protected Object visit(final ASTJexlScript node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTBlock node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTExpressionStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTFunctionStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTIfStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTWhileStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTDoWhileStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTContinue node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTRemove node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTBreak node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTDelete node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTForStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTForInitializationNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTForTerminationNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTForIncrementNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTForeachStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTForeachVar node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTReturnStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTYieldStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTTryStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTTryVar node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTCatchBlock node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTTryWithResourceStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTTryResource node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTThrowStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTAssertStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTSynchronizedStatement node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSwitchStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSwitchStatementCase node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSwitchStatementDefault node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTMultipleAssignment node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTMultipleVarStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTVarStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTAssignment node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNullAssignment node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNEAssignment node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTInitialization node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTVar node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTExtVar node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTMultiVar node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTFunctionVar node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTAttributeReference node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTReference node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTTernaryNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTElvisNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNullpNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTOrNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTAndNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTBitwiseOrNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTBitwiseDiffNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTBitwiseXorNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTBitwiseAndNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTISNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNINode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTEQNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTEQPredicate node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNENode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNEPredicate node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTLTNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTLTPredicate node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTGTNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTGTPredicate node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTLENode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTLEPredicate node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTGENode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTGEPredicate node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTERNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTERPredicate node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNRNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNRPredicate node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSWNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSWPredicate node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNSWNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNSWPredicate node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTEWNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTEWPredicate node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNEWNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNEWPredicate node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTIOFNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNIOFNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTAddNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSubNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTMulNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTDivNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTModNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTShiftLeftNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTShiftRightNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTShiftRightUnsignedNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTUnaryMinusNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTUnaryPlusNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTIncrementGetNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTDecrementGetNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTGetIncrementNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTGetDecrementNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTIndirectNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTPointerNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTBitwiseComplNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNotNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTCastNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTEnumerationNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTEnumerationReference node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTIdentifier node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTMultipleIdentifier node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTUnderscoreLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNullLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTThisNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTCurrentNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTBooleanLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNumberLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTStringLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTRegexLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTTextBlockLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTStringBuilderLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTClassLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTTypeLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetOperand node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTArrayLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTRangeNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTMapLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTMapEntry node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTMapEntryLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTMapEnumerationNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTInlinePropertyAssignment node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTInlinePropertyArrayEntry node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTInlinePropertyArrayNullEntry node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTInlinePropertyArrayNEEntry node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTInlinePropertyEntry node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTInlinePropertyNullEntry node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTInlinePropertyNEEntry node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTInlineFieldEntry node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTInlineFieldNullEntry node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTInlineFieldNEEntry node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTEmptyFunction node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTAwaitFunction node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSizeFunction node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTFunctionNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTMethodNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTMethodReference node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTInnerConstructorNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTConstructorNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTQualifiedConstructorNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTArrayConstructorNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTArrayOpenDimension node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTInitializedArrayConstructorNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTInitializedCollectionConstructorNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTInitializedMapConstructorNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSwitchExpression node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSwitchExpressionCase node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSwitchCaseLabel node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSwitchExpressionDefault node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTArrayAccess node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTArrayAccessSafe node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTIdentifierAccess node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTFieldAccess node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTArguments node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTEnclosedExpression node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetAddNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetSubNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetMultNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetDivNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetModNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetAndNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetOrNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetDiffNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetXorNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetShiftLeftNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetShiftRightUnsignedNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetShiftRightNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTJxltLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTAnnotation node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTAnnotatedStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTProjectionNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTMapProjectionNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSelectionNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTStartCountNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTStopCountNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTPipeNode node, final Object data) {
        return visitNode(node, data);
    }

}
