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
package org.apache.commons.jexl3.parser;

/**
 * Fully abstract to avoid public interface exposition.
 */
public abstract class ParserVisitor {
    /**
     * Unused, satisfy ParserVisitor interface.
     * @param node a node
     * @param data the data
     * @return does not return
     */
    protected final Object visit(final SimpleNode node, final Object data) {
        throw new UnsupportedOperationException(node.getClass().getSimpleName() + " : not supported yet.");
    }

    /**
     * Unused, should throw in Parser.
     * @param node a node
     * @param data the data
     * @return does not return
     */
    protected final Object visit(final ASTAmbiguous node, final Object data) {
        throw new UnsupportedOperationException("unexpected type of node");
    }

    protected abstract Object visit(ASTJexlScript node, Object data);

    protected abstract Object visit(ASTBlock node, Object data);

    protected abstract Object visit(ASTExpressionStatement node, Object data);

    protected abstract Object visit(ASTFunctionStatement node, Object data);

    protected abstract Object visit(ASTIfStatement node, Object data);

    protected abstract Object visit(ASTWhileStatement node, Object data);

    protected abstract Object visit(ASTDoWhileStatement node, Object data);

    protected abstract Object visit(ASTContinue node, Object data);

    protected abstract Object visit(ASTRemove node, Object data);

    protected abstract Object visit(ASTBreak node, Object data);

    protected abstract Object visit(ASTDelete node, Object data);

    protected abstract Object visit(ASTForStatement node, Object data);

    protected abstract Object visit(ASTForInitializationNode node, Object data);

    protected abstract Object visit(ASTForTerminationNode node, Object data);

    protected abstract Object visit(ASTForIncrementNode node, Object data);

    protected abstract Object visit(ASTForeachStatement node, Object data);

    protected abstract Object visit(ASTForeachVar node, Object data);

    protected abstract Object visit(ASTTryStatement node, Object data);

    protected abstract Object visit(ASTTryVar node, Object data);

    protected abstract Object visit(ASTCatchBlock node, Object data);

    protected abstract Object visit(ASTTryWithResourceStatement node, Object data);

    protected abstract Object visit(ASTTryResource node, Object data);

    protected abstract Object visit(ASTThrowStatement node, Object data);

    protected abstract Object visit(ASTAssertStatement node, Object data);

    protected abstract Object visit(ASTSynchronizedStatement node, Object data);

    protected abstract Object visit(ASTSwitchStatement node, Object data);

    protected abstract Object visit(ASTSwitchStatementCase node, Object data);

    protected abstract Object visit(ASTSwitchStatementDefault node, Object data);

    protected abstract Object visit(ASTReturnStatement node, Object data);

    protected abstract Object visit(ASTYieldStatement node, Object data);

    protected abstract Object visit(ASTMultipleAssignment node, Object data);

    protected abstract Object visit(ASTAssignment node, Object data);

    protected abstract Object visit(ASTNullAssignment node, Object data);

    protected abstract Object visit(ASTNEAssignment node, Object data);

    protected abstract Object visit(ASTMultipleVarStatement node, Object data);

    protected abstract Object visit(ASTVarStatement node, Object data);

    protected abstract Object visit(ASTInitialization node, Object data);

    protected abstract Object visit(ASTVar node, Object data);

    protected abstract Object visit(ASTExtVar node, Object data);

    protected abstract Object visit(ASTMultiVar node, Object data);

    protected abstract Object visit(ASTFunctionVar node, Object data);

    protected abstract Object visit(ASTReference node, Object data);

    protected abstract Object visit(ASTAttributeReference node, Object data);

    protected abstract Object visit(ASTTernaryNode node, Object data);

    protected abstract Object visit(ASTElvisNode node, Object data);

    protected abstract Object visit(ASTNullpNode node, Object data);

    protected abstract Object visit(ASTOrNode node, Object data);

    protected abstract Object visit(ASTAndNode node, Object data);

    protected abstract Object visit(ASTBitwiseOrNode node, Object data);

    protected abstract Object visit(ASTBitwiseDiffNode node, Object data);

    protected abstract Object visit(ASTBitwiseXorNode node, Object data);

    protected abstract Object visit(ASTBitwiseAndNode node, Object data);

    protected abstract Object visit(ASTISNode node, Object data);

    protected abstract Object visit(ASTNINode node, Object data);

    protected abstract Object visit(ASTEQNode node, Object data);

    protected abstract Object visit(ASTNENode node, Object data);

    protected abstract Object visit(ASTLTNode node, Object data);

    protected abstract Object visit(ASTGTNode node, Object data);

    protected abstract Object visit(ASTLENode node, Object data);

    protected abstract Object visit(ASTGENode node, Object data);

    protected abstract Object visit(ASTERNode node, Object data);

    protected abstract Object visit(ASTNRNode node, Object data);

    protected abstract Object visit(ASTEQPredicate node, Object data);

    protected abstract Object visit(ASTNEPredicate node, Object data);

    protected abstract Object visit(ASTLTPredicate node, Object data);

    protected abstract Object visit(ASTGTPredicate node, Object data);

    protected abstract Object visit(ASTLEPredicate node, Object data);

    protected abstract Object visit(ASTGEPredicate node, Object data);

    protected abstract Object visit(ASTERPredicate node, Object data);

    protected abstract Object visit(ASTNRPredicate node, Object data);

    protected abstract Object visit(ASTSWPredicate node, Object data);

    protected abstract Object visit(ASTNSWPredicate node, Object data);

    protected abstract Object visit(ASTEWPredicate node, Object data);

    protected abstract Object visit(ASTNEWPredicate node, Object data);

    protected abstract Object visit(ASTSWNode node, Object data);

    protected abstract Object visit(ASTNSWNode node, Object data);

    protected abstract Object visit(ASTEWNode node, Object data);

    protected abstract Object visit(ASTNEWNode node, Object data);

    protected abstract Object visit(ASTIOFNode node, Object data);

    protected abstract Object visit(ASTNIOFNode node, Object data);

    protected abstract Object visit(ASTAddNode node, Object data);

    protected abstract Object visit(ASTSubNode node, Object data);

    protected abstract Object visit(ASTMulNode node, Object data);

    protected abstract Object visit(ASTDivNode node, Object data);

    protected abstract Object visit(ASTModNode node, Object data);

    protected abstract Object visit(ASTShiftLeftNode node, Object data);

    protected abstract Object visit(ASTShiftRightNode node, Object data);

    protected abstract Object visit(ASTShiftRightUnsignedNode node, Object data);

    protected abstract Object visit(ASTUnaryMinusNode node, Object data);

    protected abstract Object visit(ASTUnaryPlusNode node, Object data);

    protected abstract Object visit(ASTIncrementGetNode node, Object data);

    protected abstract Object visit(ASTDecrementGetNode node, Object data);

    protected abstract Object visit(ASTGetIncrementNode node, Object data);

    protected abstract Object visit(ASTGetDecrementNode node, Object data);

    protected abstract Object visit(ASTIndirectNode node, Object data);

    protected abstract Object visit(ASTPointerNode node, Object data);

    protected abstract Object visit(ASTBitwiseComplNode node, Object data);

    protected abstract Object visit(ASTNotNode node, Object data);

    protected abstract Object visit(ASTCastNode node, Object data);

    protected abstract Object visit(ASTIdentifier node, Object data);

    protected abstract Object visit(ASTMultipleIdentifier node, Object data);

    protected abstract Object visit(ASTUnderscoreLiteral node, Object data);

    protected abstract Object visit(ASTNullLiteral node, Object data);

    protected abstract Object visit(ASTThisNode node, Object data);

    protected abstract Object visit(ASTCurrentNode node, Object data);

    protected abstract Object visit(ASTBooleanLiteral node, Object data);

    protected abstract Object visit(ASTNumberLiteral node, Object data);

    protected abstract Object visit(ASTStringLiteral node, Object data);

    protected abstract Object visit(ASTRegexLiteral node, Object data);

    protected abstract Object visit(ASTTextBlockLiteral node, Object data);

    protected abstract Object visit(ASTStringBuilderLiteral node, Object data);

    protected abstract Object visit(ASTClassLiteral node, Object data);

    protected abstract Object visit(ASTTypeLiteral node, Object data);

    protected abstract Object visit(ASTSetLiteral node, Object data);

    protected abstract Object visit(ASTSetOperand node, Object data);

    protected abstract Object visit(ASTArrayLiteral node, Object data);

    protected abstract Object visit(ASTRangeNode node, Object data);

    protected abstract Object visit(ASTMapLiteral node, Object data);

    protected abstract Object visit(ASTMapEntry node, Object data);

    protected abstract Object visit(ASTMapEntryLiteral node, Object data);

    protected abstract Object visit(ASTMapEnumerationNode node, Object data);

    protected abstract Object visit(ASTInlinePropertyAssignment node, Object data);

    protected abstract Object visit(ASTInlinePropertyArrayEntry node, Object data);

    protected abstract Object visit(ASTInlinePropertyArrayNullEntry node, Object data);

    protected abstract Object visit(ASTInlinePropertyArrayNEEntry node, Object data);

    protected abstract Object visit(ASTInlinePropertyEntry node, Object data);

    protected abstract Object visit(ASTInlinePropertyNullEntry node, Object data);

    protected abstract Object visit(ASTInlinePropertyNEEntry node, Object data);

    protected abstract Object visit(ASTInlineFieldEntry node, Object data);

    protected abstract Object visit(ASTInlineFieldNullEntry node, Object data);

    protected abstract Object visit(ASTInlineFieldNEEntry node, Object data);

    protected abstract Object visit(ASTEmptyFunction node, Object data);

    protected abstract Object visit(ASTAwaitFunction node, Object data);

    protected abstract Object visit(ASTSizeFunction node, Object data);

    protected abstract Object visit(ASTFunctionNode node, Object data);

    protected abstract Object visit(ASTMethodNode node, Object data);

    protected abstract Object visit(ASTMethodReference node, Object data);

    protected abstract Object visit(ASTInnerConstructorNode node, Object data);

    protected abstract Object visit(ASTConstructorNode node, Object data);

    protected abstract Object visit(ASTQualifiedConstructorNode node, Object data);

    protected abstract Object visit(ASTArrayConstructorNode node, Object data);

    protected abstract Object visit(ASTArrayOpenDimension node, Object data);

    protected abstract Object visit(ASTInitializedArrayConstructorNode node, Object data);

    protected abstract Object visit(ASTInitializedCollectionConstructorNode node, Object data);

    protected abstract Object visit(ASTInitializedMapConstructorNode node, Object data);

    protected abstract Object visit(ASTSwitchCaseLabel node, Object data);

    protected abstract Object visit(ASTSwitchExpression node, Object data);

    protected abstract Object visit(ASTSwitchExpressionCase node, Object data);

    protected abstract Object visit(ASTSwitchExpressionDefault node, Object data);

    protected abstract Object visit(ASTArrayAccess node, Object data);

    protected abstract Object visit(ASTArrayAccessSafe node, Object data);

    protected abstract Object visit(ASTIdentifierAccess node, Object data);

    protected abstract Object visit(ASTFieldAccess node, Object data);

    protected abstract Object visit(ASTArguments node, Object data);

    protected abstract Object visit(ASTEnclosedExpression node, Object data);

    protected abstract Object visit(ASTSetAddNode node, Object data);

    protected abstract Object visit(ASTSetSubNode node, Object data);

    protected abstract Object visit(ASTSetMultNode node, Object data);

    protected abstract Object visit(ASTSetDivNode node, Object data);

    protected abstract Object visit(ASTSetModNode node, Object data);

    protected abstract Object visit(ASTSetAndNode node, Object data);

    protected abstract Object visit(ASTSetOrNode node, Object data);

    protected abstract Object visit(ASTSetDiffNode node, Object data);

    protected abstract Object visit(ASTSetXorNode node, Object data);

    protected abstract Object visit(ASTSetShiftLeftNode node, final Object data);

    protected abstract Object visit(ASTSetShiftRightNode node, final Object data);

    protected abstract Object visit(ASTSetShiftRightUnsignedNode node, final Object data);

    protected abstract Object visit(ASTJxltLiteral node, Object data);

    protected abstract Object visit(ASTAnnotation node, Object data);

    protected abstract Object visit(ASTAnnotatedStatement node, Object data);

    protected abstract Object visit(ASTEnumerationNode node, Object data);

    protected abstract Object visit(ASTEnumerationReference node, Object data);

    protected abstract Object visit(ASTProjectionNode node, Object data);

    protected abstract Object visit(ASTMapProjectionNode node, Object data);

    protected abstract Object visit(ASTSelectionNode node, Object data);

    protected abstract Object visit(ASTStartCountNode node, Object data);

    protected abstract Object visit(ASTStopCountNode node, Object data);

    protected abstract Object visit(ASTPipeNode node, Object data);
}
