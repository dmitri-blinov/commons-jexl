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


import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlOptions;
import org.apache.commons.jexl3.parser.ASTArrayAccess;
import org.apache.commons.jexl3.parser.ASTAssignment;
import org.apache.commons.jexl3.parser.ASTEQNode;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTNENode;
import org.apache.commons.jexl3.parser.ASTNullpNode;
import org.apache.commons.jexl3.parser.ASTReference;
import org.apache.commons.jexl3.parser.ASTTernaryNode;
import org.apache.commons.jexl3.parser.JexlNode;

/**
 * An Engine that behaves like JEXL 3.2, bugs included.
 */
public class Engine32 extends Engine {
    public Engine32(final JexlBuilder conf) {
        super(conf);
    }

    public Engine32() {
        super();
    }

    /**
     * Static delegation of isTernaryProtected.
     * @param ii the interpreter (unused)
     * @param node the node
     * @return true if node is navigation-safe, false otherwise
     */
    static boolean isTernaryProtected(Interpreter ii, JexlNode node) {
        for (JexlNode walk = node.jjtGetParent(); walk != null; walk = walk.jjtGetParent()) {
            // protect only the condition part of the ternary
            if (walk instanceof ASTTernaryNode
                    || walk instanceof ASTNullpNode
                    || walk instanceof ASTEQNode
                    || walk instanceof ASTNENode) {
                return node == walk.jjtGetChild(0);
            }
            if (!(walk instanceof ASTReference || walk instanceof ASTArrayAccess)) {
                break;
            }
            node = walk;
        }
        return false;
    }

    /**
     * Static delegation of getVariable.
     * @param ii the interpreter
     * @param frame the frame
     * @param block the scope
     * @param identifier the variable identifier
     * @return the variable value
     */
    static Object getVariable(Interpreter ii, Frame frame, LexicalScope block, ASTIdentifier identifier) {
        int symbol = identifier.getSymbol();
        // if we have a symbol, we have a scope thus a frame
        if (ii.options.isLexicalShade() && identifier.isShaded()) {
            return ii.undefinedVariable(identifier, identifier.getName());
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
        Object value = ii.context.get(name);
        if (value == null && !ii.context.has(name)) {
            boolean ignore = (ii.isSafe()
                    && (symbol >= 0
                    || identifier.jjtGetParent() instanceof ASTAssignment))
                    || (identifier.jjtGetParent() instanceof ASTReference);
            if (!ignore) {
                return ii.unsolvableVariable(identifier, name, true); // undefined
            }
        }
        return value;
    }

    @Override
    protected Interpreter createInterpreter(final JexlContext context, final Frame frame, final JexlOptions opts) {
        return new Interpreter(this, opts, context, frame) {
            @Override
            protected boolean isStrictOperand(JexlNode node) {
                return false;
            }

            @Override
            protected boolean isTernaryProtected( JexlNode node) {
                return Engine32.isTernaryProtected(this, node);
            }

            @Override
            protected Object getVariable(Frame frame, LexicalScope block, ASTIdentifier identifier) {
                return Engine32.getVariable(this, frame, block, identifier);
            }
        };
    }

    @Override
    protected Interpreter createTemplateInterpreter(TemplateInterpreter.Arguments args) {
        return new TemplateInterpreter(args) {
            @Override
            protected boolean isStrictOperand(JexlNode node) {
                return false;
            }

            @Override
            protected boolean isTernaryProtected( JexlNode node) {
                return Engine32.isTernaryProtected(this, node);
            }

            @Override
            protected Object getVariable(Frame frame, LexicalScope block, ASTIdentifier identifier) {
                return Engine32.getVariable(this, frame, block, identifier);
            }
        };
    }
}
