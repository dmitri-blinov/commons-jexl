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

import java.util.Map;
import java.util.HashMap;
import java.util.BitSet;

/**
 * The set of symbols declared in a lexical scope.
 * <p>The symbol identifiers are determined by the functional scope.
 */
public class LexicalScope {
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
    /**
     * Number of bits in a long.
     */
    protected static final int LONGBITS = 64;
    /**
     * The mask of symbols in the frame.
     */
    protected long symbols = 0L;
    /**
     * Symbols after 64.
     */
    protected BitSet moreSymbols = null;
    /** Previous block. */
    protected final LexicalScope previous;
    /** The actual var modifiers. */
    private Map<Integer, VariableModifier> modifiers;
    /** The frame */
    protected final Frame frame;

    /**
     * Create a scope.
     */
    public LexicalScope() {
        this(null);
    }

    /**
     * Default ctor.
     * @param scope the previous scope
     */
    public LexicalScope(final LexicalScope scope) {
        this(null, scope);
    }

    /**
     * Create a scope.
     * @param frame the frame
     * @param scope the previous scope
     */
    public LexicalScope(final Frame frame, final LexicalScope scope) {
        if (frame != null) {
            this.frame = frame;
        } else {
            this.frame = scope != null ? scope.frame : null;
        }
        previous = scope;
    }

    /**
     * Frame copy ctor base.
     *
     * @param s  the symbols mask
     * @param ms the more symbols bitset
     */
    protected LexicalScope(final long s, final BitSet ms, final Frame frame, final LexicalScope pscope) {
        this(frame, pscope);
        symbols = s;
        moreSymbols = ms != null ? (BitSet) ms.clone() : null;
    }

    /**
     * Ensure more symbpls can be stored.
     *
     * @return the set of more symbols
     */
    protected final BitSet moreSymbols() {
        if (moreSymbols == null) {
            moreSymbols = new BitSet();
        }
        return moreSymbols;
    }

    /**
     * Checks whether a symbol has already been declared.
     *
     * @param symbol the symbol
     * @return true if declared, false otherwise
     */
    public boolean hasSymbol(final int symbol) {
        if (symbol < LONGBITS) {
            return (symbols & (1L << symbol)) != 0L;
        }
        return moreSymbols != null && moreSymbols.get(symbol - LONGBITS);
    }
    /**
     * Sets a variable modifiers.
     * @param r the offset in this frame
     * @param c the variable type
     * @param fin whether the variable is final
     * @param req whether the variable is required
     */
    public void setModifiers(int r, Class c, boolean fin, boolean req) {
        if (modifiers == null)
            modifiers = new HashMap<Integer, VariableModifier> ();
        modifiers.put(r, new VariableModifier(c, fin, req));
    }
    /**
     * Gets a symbol type.
     * @param s the offset in this frame
     * @return the type if any
     */
    public Class typeof(int s) {
        if (modifiers != null && modifiers.containsKey(s))
            return modifiers.get(s).getType();
        if (previous != null) 
            return previous.typeof(s);
        if (frame != null && frame.getScope() != null)
            return frame.getScope().getVariableType(s);
        return null;
    }
    /**
     * Returns if the local variable is declared final.
     * @param s the symbol index
     * @return true if final, false otherwise
     */
    public boolean isVariableFinal(int s) {
        if (modifiers != null && modifiers.containsKey(s))
            return modifiers.get(s).isFinal();
        if (previous != null)
            return previous.isVariableFinal(s);
        if (frame != null && frame.getScope() != null)
            return frame.getScope().isVariableFinal(s);
        return false;
    }
    /**
     * Returns if the local variable is declared non-null.
     * @param s the symbol index
     * @return true if non-null, false otherwise
     */
    public boolean isVariableRequired(int s) {
        if (modifiers != null && modifiers.containsKey(s))
            return modifiers.get(s).isRequired();
        if (previous != null)
            return previous.isVariableRequired(s);
        if (frame != null && frame.getScope() != null)
            return frame.getScope().isVariableRequired(s);
        return false;
    }
    /**
     * Adds a symbol in this scope.
     *
     * @param symbol the symbol index
     * @param c the variable type
     * @param fin whether the variable is final
     * @param req whether the variable is required
     * @return true if was not already declared, false if lexical clash (error)
     */
    public boolean addSymbol(int symbol, Class c, boolean fin, boolean req) {
        boolean result = addSymbol(symbol);
        if (result) {
            setModifiers(symbol, c, fin, req);
        }
        return result;
    }

    /**
     * Adds a symbol in this scope.
     *
     * @param symbol the symbol
     * @return true if registered, false if symbol was already registered
     */
    public boolean addSymbol(final int symbol) {
        if (symbol < LONGBITS) {
            if ((symbols & (1L << symbol)) != 0L) {
                return false;
            }
            symbols |= (1L << symbol);
        } else {
            final int s = symbol - LONGBITS;
            final BitSet ms = moreSymbols();
            if (ms.get(s)) {
                return false;
            }
            ms.set(s, true);
        }
        return true;
    }
    /**
     * Clear all symbols.
     *
     * @param cleanSymbol a (optional, may be null) functor to call for each cleaned symbol
     */
    public final void clearSymbols(final java.util.function.IntConsumer cleanSymbol) {
        // undefine symbols getting out of scope
        if (cleanSymbol != null) {
            long clean = symbols;
            while (clean != 0L) {
                final int s = Long.numberOfTrailingZeros(clean);
                clean &= ~(1L << s);
                cleanSymbol.accept(s);
            }
        }
        symbols = 0L;
        if (moreSymbols != null) {
            if (cleanSymbol != null) {
                for (int s = moreSymbols.nextSetBit(0); s != -1; s = moreSymbols.nextSetBit(s + 1)) {
                    cleanSymbol.accept(s + LONGBITS);
                }
            }
            moreSymbols.clear();
        }
    }

    /**
     * @return the number of symbols defined in this scope.
     */
    public int getSymbolCount() {
        return Long.bitCount(symbols) + (moreSymbols == null ? 0 : moreSymbols.cardinality());
    }

    public String toString() {
        return String.valueOf(symbols);
    }
}
