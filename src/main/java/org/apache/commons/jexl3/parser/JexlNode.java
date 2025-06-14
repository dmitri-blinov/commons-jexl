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

import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.JexlCache;
import org.apache.commons.jexl3.JexlInfo;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlPropertyGet;
import org.apache.commons.jexl3.introspection.JexlPropertySet;

/**
 * Base class for parser nodes - holds an 'image' of the token for later use.
 *
 * @since 2.0
 */
public abstract class JexlNode extends SimpleNode implements JexlCache.Reference {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    // line + column encoded: up to 4096 columns (ie 20 bits for line + 12 bits for column)
    private int lc = -1;

    /**
     * A marker interface for constants.
     * @param <T> the literal type
     */
    public interface Constant<T> {
        T getLiteral();
    }

    public JexlNode(final int id) {
        super(id);
    }

    public JexlNode(final Parser p, final int id) {
        super(p, id);
    }

    public void jjtSetFirstToken(final Token t) {
        // 0xc = 12, 12 bits -> 4096
        // 0xfff, 12 bits mask
        this.lc = (t.beginLine << 0xc) | (0xfff & t.beginColumn);
    }

    public void jjtSetLastToken(final Token t) {
        // nothing
    }

    public int getLine() {
        return this.lc >>> 0xc;
    }

    public int getColumn() {
        return this.lc & 0xfff;
    }

    /**
     * Gets the associated JexlInfo instance.
     *
     * @return the info
     */
    public JexlInfo jexlInfo() {
        return jexlInfo(null, null);
    }

    /**
     * Gets the associated JexlInfo instance.
     *
     * @param name the source name
     * @param name the source path
     * @return the info
     */
    public JexlInfo jexlInfo(String name, String path) {
        JexlInfo info = null;
        JexlNode node = this;
        while (node != null) {
            if (node.jjtGetValue() instanceof JexlInfo) {
                info = (JexlInfo) node.jjtGetValue();
                break;
            }
            node = node.jjtGetParent();
        }
        if (lc >= 0) {
            final int c = lc & 0xfff;
            final int l = lc >> 0xc;
            // at least an info with line/column number
            return info != null? info.at(info.getLine() + l - 1, c) : new JexlInfo(name, path, l, c);
        }
        // weird though; no jjSetFirstToken(...) ever called?
        return info;
    }

    /**
     * Marker interface for cachable function calls.
     */
    public interface Funcall {}

    /**
     * Clears any cached value of type JexlProperty{G,S}et or JexlMethod.
     * <p>
     * This is called when the engine detects the evaluation of a script occurs with a class loader
     * different that the one that created it.</p>
     */
    public void clearCache() {
        final Object value = jjtGetValue();
        if (value instanceof JexlPropertyGet
            || value instanceof JexlPropertySet
            || value instanceof JexlMethod
            || value instanceof Funcall
            || value instanceof Class  ) {
            jjtSetValue(null);
        }
        for (int n = 0; n < jjtGetNumChildren(); ++n) {
            jjtGetChild(n).clearCache();
        }
    }

    /**
     * Checks whether this node is an operator that accepts a null argument
     * even when arithmetic is in strict mode.
     * The default cases are equals and not equals.
     *
     * @return true if node accepts null arguments, false otherwise
     */
    public boolean isStrictOperator(JexlArithmetic arithmetic) {
        return OperatorController.INSTANCE.isStrict(arithmetic, this);
    }

    /**
     * Whether this node is a constant node.
     * <p>Its value can not change after the first evaluation and can be cached
     * indefinitely.</p>
     *
     * @return true if constant, false otherwise
     */
    public boolean isConstant() {
        return isConstant(this instanceof JexlNode.Constant<?>);
    }

    protected boolean isConstant(final boolean literal) {
        if (literal) {
            for (int n = 0; n < jjtGetNumChildren(); ++n) {
                final JexlNode child = jjtGetChild(n);
                if (child instanceof ASTReference) {
                    final boolean is = child.isConstant(true);
                    if (!is) {
                        return false;
                    }
                } else if (child instanceof ASTMapEntry) {
                    final boolean is = child.isConstant(true);
                    if (!is) {
                        return false;
                    }
                } else if (!child.isConstant()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Whether this node is an immutable node Its value can not change after the first evaluation and can be cached
     * indefinitely.
     *
     * @return true if immutable, false otherwise
     */
    public boolean isImmutable() {
        return this instanceof JexlNode.Constant<?>;
    }

    /**
     * Whether this node is a left value.
     * @return true if node is assignable, false otherwise
     */
    public boolean isLeftValue() {
        if (isImmutable())
            return false;
        JexlNode walk = this;
        do {
            if (walk instanceof ASTMultipleIdentifier) {
                int nc = walk.jjtGetNumChildren();
                for (int i = 0; i < nc; i++) {
                    JexlNode left = walk.jjtGetChild(i);
                    if (left instanceof ASTUnderscoreLiteral)
                        continue;
                    if (!left.isLeftValue())
                        return false;
                }
                return true;
            }
            if ((walk instanceof ASTIdentifier && ((ASTIdentifier) walk).isConstant())
                || (walk instanceof ASTIdentifierAccess && ((ASTIdentifierAccess) walk).isSafe())) {
                return false;
            }
            if (walk instanceof ASTIdentifier
                || walk instanceof ASTIdentifierAccess
                || walk instanceof ASTArrayAccess
                || walk instanceof ASTIndirectNode) {
                return true;
            }
            final int nc = walk.jjtGetNumChildren() - 1;
            if (nc < 0) {
                return walk.jjtGetParent() instanceof ASTReference;
            }
            walk = walk.jjtGetChild(nc);
        } while (walk != null);
        return false;
    }

    /**
     * @return true if this node looks like a global var
     */
    public boolean isGlobalVar() {
        if (this instanceof ASTVar) {
            return false;
        }
        if (this instanceof ASTIdentifier) {
            return ((ASTIdentifier) this).getSymbol() < 0;
        }
        final int nc = this.jjtGetNumChildren() - 1;
        if (nc >= 0) {
            final JexlNode first = this.jjtGetChild(0);
            return first.isGlobalVar();
        }
        if (jjtGetParent() instanceof ASTReference) {
            return true;
        }
        return false;
    }

    /**
     * Whether this node is the left-hand side of a safe access identifier as in.
     * For instance, in 'x?.y' , 'x' is safe.
     * @param safe whether the engine is in safe-navigation mode
     * @return true if safe lhs, false otherwise
     */
    public boolean isSafeLhs(final boolean safe) {
        if (this instanceof ASTReference) {
            return jjtGetChild(0).isSafeLhs(safe);
        }
        if (this instanceof ASTMethodNode) {
            if (this.jjtGetNumChildren() > 1
                    && this.jjtGetChild(0) instanceof ASTIdentifierAccess
                    && (((ASTIdentifierAccess) this.jjtGetChild(0)).isSafe() || safe)) {
                return true;
            }
        }
        final JexlNode parent = this.jjtGetParent();
        if (parent == null) {
            return false;
        }
        // find this node in its parent
        final int nsiblings = parent.jjtGetNumChildren();
        int rhs = -1;
        for(int s = 0; s < nsiblings; ++s) {
            final JexlNode sibling = parent.jjtGetChild(s);
            if (sibling == this) {
                // the next chid offset of this nodes parent
                rhs = s + 1;
                break;
            }
        }
        // seek next child in parent
        if (rhs >= 0 && rhs < nsiblings) {
            JexlNode rsibling = parent.jjtGetChild(rhs);
            if (rsibling instanceof ASTMethodNode || rsibling instanceof ASTFunctionNode) {
                rsibling = rsibling.jjtGetChild(0);
            }
            if (rsibling instanceof ASTIdentifierAccess
                && (((ASTIdentifierAccess) rsibling).isSafe() || safe)) {
                return true;
            }
            if (rsibling instanceof ASTPipeNode) {
                return true;
            }
            if (rsibling instanceof ASTArrayAccessSafe || (rsibling instanceof ASTArrayAccess && safe)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object getCache() {
        return jjtGetValue();
    }

    @Override
    public void setCache(Object cache) {
        jjtSetValue(cache);
    }

    /**
     * An info bound to its node.
     * <p>Used to parse expressions for templates.
     */
    public static class Info extends JexlInfo {
        JexlNode node = null;

        /**
         * Default ctor.
         * @param jnode the node
         */
        public Info(final JexlNode jnode) {
            this(jnode, jnode.jexlInfo());
        }

        /**
         * Copy ctor.
         * @param jnode the node
         * @param info the
         */
        public Info(final JexlNode jnode, final JexlInfo info) {
            this(jnode, info.getName(), info.getPath(), info.getLine(), info.getColumn());
        }

        /**
         * Full detail ctor.
         * @param jnode the node
         * @param name the file name
         * @param l the line
         * @param c the column
         */
        private Info(final JexlNode jnode, final String name, final String p, final int l, final int c) {
            super(name, p, l, c);
            node = jnode;
        }

        /**
         * @return the node this info is bound to
         */
        public JexlNode getNode() {
            return node;
        }

        @Override
        public JexlInfo at(final int l, final int c) {
            return new Info(node, getName(), getPath(), l, c);
        }

        @Override
        public JexlInfo detach() {
            node = null;
            return this;
        }
    }
}
