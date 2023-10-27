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
 * <p>The symbol identifiers are determined by the functional scope.</p>
 * <p>We use 2 bits per symbol s; bit (s*2)+0 sets the actual symbol as lexical (let), bit (s*2)+1 as a const.
 * There are actually only 2 used states: 1 and 3</p>
 */
public class LexicalScope {
    /**
     * A variable modifier, stores additional variable attributes.
     * @since 3.2
     */
    protected static final class VariableModifier implements Cloneable {
        /** The var 'lexical' modifier. */
        private final boolean isLexical;
        /** The var 'final' modifier. */
        private final boolean isFinal;
        /** The var 'required' modifier. */
        private final boolean isRequired;
        /** The var 'type' modifier. */
        private final Class type;

        /**
         * Creates a new variable modifier.
         * @param c the variable type
         * @param lex whether the variable is lexical
         * @param fin whether the variable is final
         * @param req whether the variable is required
         */
        protected VariableModifier(Class c, boolean lex, boolean fin, boolean req) {
            type = c;
            isLexical = lex;
            isFinal = fin;
            isRequired = req;
        }

        public Class getType() {
            return type;
        }

        public boolean isLexical() {
            return isLexical;
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
            return new VariableModifier(this.type, this.isLexical, this.isFinal, this.isRequired);
        }
    }
    /**
     * Number of bits in a long.
     */
    protected static final int BITS_PER_LONG = 64;
    /**
     * Bits per symbol.
     * let (b + 0) + const (b + 1).
     */
    protected static final int BITS_PER_SYMBOL = 2;
    /**
     * From a symbol number to a starting symbol bit number.
     */
    protected static final int SYMBOL_SHIFT = BITS_PER_SYMBOL - 1;
    /**
     * Bitmask for symbols.
     */
    protected static final long SYMBOL_MASK = (1L << (BITS_PER_SYMBOL - 1)) - 1; // 3, as 1+2, 2 bits
    /**
     * Number of symbols.
     */
    protected int count;
    /**
     * The mask of symbols in the scope.
     */
    protected long symbols;
    /**
     * Symbols after bit 64 (aka symbol 32 when 2 bits per symbol).
     */
    protected BitSet moreSymbols;
    /** 
     * Previous block. 
     */
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
        this((Frame) null, scope);
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
    protected LexicalScope(LexicalScope other, final LexicalScope pscope) {
        this(other.frame, pscope);
        this.symbols = other.symbols;
        BitSet otherMoreSymbols = other.moreSymbols;
        this.moreSymbols = otherMoreSymbols != null ? (BitSet) otherMoreSymbols.clone() : null;
        this.count = other.count;
    }

    /**
     * Ensure more symbpls can be stored.
     *
     * @return the set of more symbols
     */
    private BitSet moreBits() {
        if (moreSymbols == null) {
            moreSymbols = new BitSet();
        }
        return moreSymbols;
    }

    /**
     * Sets a variable modifiers.
     * @param r the offset in this frame
     * @param c the variable type
     * @param lex whether the variable is lexical
     * @param fin whether the variable is final
     * @param req whether the variable is required
     */
    public void setModifiers(int r, Class c, boolean lex, boolean fin, boolean req) {
        if (modifiers == null) {
            modifiers = new HashMap<Integer, VariableModifier> ();
        }
        modifiers.put(r, new VariableModifier(c, lex, fin, req));
    }
    /**
     * Gets a symbol type.
     * @param s the offset in this frame
     * @return the type if any
     */
    public Class typeof(int s) {
        if (modifiers != null && modifiers.containsKey(s)) {
            return modifiers.get(s).getType();
        }
        if (previous != null) {
            return previous.typeof(s);
        }
        if (frame != null && frame.getScope() != null) {
            return frame.getScope().getVariableType(s);
        }
        return null;
    }

    /**
     * Returns if the local variable is declared lexically scoped.
     * @param s the symbol index
     * @return true if lexical, false otherwise
     */
    public boolean isVariableLexical(int s) {
        if (modifiers != null && modifiers.containsKey(s)) {
            return modifiers.get(s).isLexical();
        }
        if (previous != null) {
            return previous.isVariableLexical(s);
        }
        if (frame != null && frame.getScope() != null) {
            return frame.getScope().isVariableLexical(s);
        }
        return false;
    }

    /**
     * Returns if the local variable is declared final.
     * @param s the symbol index
     * @return true if final, false otherwise
     */
    public boolean isVariableFinal(int s) {
        if (modifiers != null && modifiers.containsKey(s)) {
            return modifiers.get(s).isFinal();
        }
        if (previous != null) {
            return previous.isVariableFinal(s);
        }
        if (frame != null && frame.getScope() != null) {
            return frame.getScope().isVariableFinal(s);
        }
        return false;
    }

    /**
     * Returns if the local variable is declared non-null.
     * @param s the symbol index
     * @return true if non-null, false otherwise
     */
    public boolean isVariableRequired(int s) {
        if (modifiers != null && modifiers.containsKey(s)) {
            return modifiers.get(s).isRequired();
        }
        if (previous != null) {
            return previous.isVariableRequired(s);
        }
        if (frame != null && frame.getScope() != null) {
            return frame.getScope().isVariableRequired(s);
        }
        return false;
    }

 
   /**
     * Whether a given bit (not symbol) is set.
     * @param bit the bit
     * @return true if set
     */
    private boolean isSet(final int bit) {
        if (bit < BITS_PER_LONG) {
            return (symbols & (1L << bit)) != 0L;
        }
        return moreSymbols != null && moreSymbols.get(bit - BITS_PER_LONG);
    }

    /**
     * Sets a given bit (not symbol).
     * @param bit the bit
     * @return true if it was actually set, false if it was set before
     */
    private boolean set(final int bit) {
        if (bit < BITS_PER_LONG) {
            if ((symbols & (1L << bit)) != 0L) {
                return false;
            }
            symbols |= (1L << bit);
        } else {
            final int bit64 = bit - BITS_PER_LONG;
            final BitSet ms = moreBits();
            if (ms.get(bit64)) {
                return false;
            }
            ms.set(bit64, true);
        }
        return true;
    }

    /**
     * Adds a symbol in this scope.
     *
     * @param symbol the symbol index
     * @param c the variable type
     * @param lex whether the variable is lexical
     * @param fin whether the variable is final
     * @param req whether the variable is required
     * @return true if was not already declared, false if lexical clash (error)
     */
    public boolean addSymbol(int symbol, Class c, boolean lex, boolean fin, boolean req) {
        boolean result = addSymbol(symbol);
        if (result) {
            setModifiers(symbol, c, lex, fin, req);
        }
        return result;
    }

    public boolean hasSymbol(final int symbol) {
        final int bit = symbol << SYMBOL_SHIFT;
        return isSet(bit);
    }

    /**
     * Checks whether a symbol is declared as a constant.
     *
     * @param symbol the symbol
     * @return true if declared as constant, false otherwise
     */
    public boolean isConstant(final int symbol) {
        final int bit = (symbol << SYMBOL_SHIFT) | 1;
        return isSet(bit);
    }

    /**
     * Adds a symbol in this scope.
     *
     * @param symbol the symbol
     * @return true if registered, false if symbol was already registered
     */
    public boolean addSymbol(final int symbol) {
        final int bit = (symbol << SYMBOL_SHIFT) ;
        if (set(bit)) {
            count += 1;
            return true;
        }
        return false;
    }

    /**
     * Adds a constant in this scope.
     *
     * @param symbol the symbol
     * @return true if registered, false if symbol was already registered
     */
    public boolean addConstant(final int symbol) {
        final int letb = (symbol << SYMBOL_SHIFT) ;
        if (!isSet(letb)) {
            throw new IllegalStateException("const not declared as symbol " + symbol);
        }
        final int bit = (symbol << SYMBOL_SHIFT) | 1;
        return set(bit);
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
                final int bit = Long.numberOfTrailingZeros(clean);
                final int s = bit >> SYMBOL_SHIFT;
                cleanSymbol.accept(s);
                // call clean for symbol definition (3 as a mask for 2 bits,1+2)
                clean &= ~(SYMBOL_MASK << bit);
            }
            // step by bits per symbol
            int bit = moreSymbols != null ? moreSymbols.nextSetBit(0) : -1;
            while (bit >= 0) {
                final int s = (bit + BITS_PER_LONG) >> SYMBOL_SHIFT;
                cleanSymbol.accept(s);
                bit = moreSymbols.nextSetBit(bit + BITS_PER_SYMBOL);
            }
        }
        // internal cleansing
        symbols = 0L;
        count = 0;
        if (moreSymbols != null) {
            moreSymbols.clear();
        }
    }

    /**
     * @return the number of symbols defined in this scope.
     */
    public int getSymbolCount() {
        return count;
    }

    public String toString() {
        return String.valueOf(symbols);
    }
}
