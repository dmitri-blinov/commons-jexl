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

import java.util.BitSet;

/**
 * The set of symbols declared in a lexical scope.
 * <p>The symbol identifiers are determined by the functional scope.
 */
public final class LexicalScope {
    /**
     * A variable modifier, stores additional variable attributes.
     * @since 3.2
     */
    protected static final class VariableModifier {
        /** The var 'final' modifier. */
        private final boolean isFinal;
        /** The var 'required' modifier. */
        private final boolean isRequired;
        /** The var 'type' modifier. */
        private final Class type;

        /**
         * Creates a new variable modifier.
         * @param c the variable type
         * @param fin whether the variable is final
         * @param req whether the variable is required
         */
        protected VariableModifier(Class c, boolean fin, boolean req) {
            type = c;
            isFinal = fin;
            isRequired = req;
        }

        public Class getType() {
            return type;
        }

        public boolean isFinal() {
            return isFinal;
        }

        public boolean isRequired() {
            return isRequired;
        }

        /**
         * Creates a clone of this modifier.
         * @return new modifier
        */
        public VariableModifier clone() {
            return new VariableModifier(this.type, this.isFinal, this.isRequired);
        }
    }
    /** Number of bits in a long. */
    protected static final int LONGBITS = 64;
    /** The mask of symbols in the frame. */
    protected long symbols = 0L;
    /** Symbols after 64. */
    protected BitSet moreSymbols = null;
    /** Previous block. */
    protected final LexicalScope previous;
    /** The actual var modifiers. */
    private final VariableModifier[] modifiers;
    /** The frame */
    private final Frame frame;

    /**
     * Default ctor.
     * @param scope the previous scope
     */
    public LexicalScope(LexicalScope scope) {
        this(null, scope);
    }

    /**
     * Create a scope.
     * @param frame the current execution frame
     * @param scope the previous scope
     */
    public LexicalScope(Frame frame, LexicalScope scope) {
        if (frame != null) {
            this.frame = frame;
            Integer stackSize = frame.getStackSize();
            modifiers = stackSize != null ? new VariableModifier[stackSize] : null;
            int argc = frame.getScope().getArgCount();
            for(int a  = 0; a < argc; ++a) {
                declareSymbol(a);
            }
        } else {
            this.frame = scope != null ? scope.frame : null;
            modifiers = null;
        }
        previous = scope;
    }

    /**
     * Ensure more symbpls can be stored.
     * @return the set of more symbols
     */
    final BitSet moreSymbols() {
        if (moreSymbols == null) {
            moreSymbols = new BitSet();
        }
        return moreSymbols;
    }
    
    /**
     * Checks whether a symbol has already been declared.
     * @param symbol the symbol
     * @return true if declared, false otherwise
     */
    public boolean hasSymbol(int symbol) {
        if (symbol < LONGBITS) {
            return (symbols & (1L << symbol)) != 0L;
        } else {
            return moreSymbols == null ? false : moreSymbols.get(symbol - LONGBITS);
        }
    }
    /**
     * Sets a variable modifiers.
     * @param r the offset in this frame
     * @param c the variable type
     * @param fin whether the variable is final
     * @param req whether the variable is required
     */
    public void setModifiers(int r, Class c, boolean fin, boolean req) {
        modifiers[r] = new VariableModifier(c, fin, req);
    }
    /**
     * Gets a symbol type.
     * @param s the offset in this frame
     * @return the type if any
     */
    public Class typeof(int s) {
        return modifiers != null && modifiers[s] != null ? modifiers[s].getType() : frame.getScope().getVariableType(s);
    }
    /**
     * Returns if the local variable is declared final.
     * @param s the symbol index
     * @return true if final, false otherwise
     */
    public boolean isVariableFinal(int s) {
        return modifiers != null && modifiers[s] != null ? modifiers[s].isFinal() : frame.getScope().isVariableFinal(s);
    }
    /**
     * Returns if the local variable is declared non-null.
     * @param s the symbol index
     * @return true if non-null, false otherwise
     */
    public boolean isVariableRequired(int s) {
        return modifiers != null && modifiers[s] != null ? modifiers[s].isRequired() : frame.getScope().isVariableRequired(s);
    }
    /**
     * Declares a local symbol.
     *
     * @param symbol the symbol index
     * @return true if was not already declared, false if lexical clash (error)
     */
    public boolean declareSymbol(int symbol) {
        LexicalScope walk = previous;
        while (walk != null) {
            if (walk.hasSymbol(symbol)) {
                return false;
            }
            walk = walk.previous;
        }
        if (symbol < LONGBITS) {
            if ((symbols & (1L << symbol)) != 0L) {
                return false;
            }
            symbols |= (1L << symbol);
        } else {
            int s = symbol - LONGBITS;
            BitSet ms = moreSymbols();
            if (ms.get(s)) {
                return false;
            }
            ms.set(s, true);
        }
        return true;
    }
    /**
     * @return the number of symbols defined in this scope.
     */
    public int getSymbolCount() {
        return Long.bitCount(symbols) + (moreSymbols == null? 0 : moreSymbols.cardinality());
    }
}
