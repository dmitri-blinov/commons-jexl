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

import org.apache.commons.jexl3.JexlOptions;
import org.apache.commons.jexl3.JexlEngine;

import java.math.MathContext;

/**
 * A basic implementation of JexlOptions.
 * <p>Thread safety is only guaranteed if no modifications (call set* method)
 * occurs in different threads; note however that using the 'callable()' method to
 * allow a script to run as such will use a copy.
 */
public class Options implements JexlOptions {
    /** The asertions bit. */
    protected static final int ASSERTIONS = 7;
    /** The local shade bit. */
    protected static final int SHADE = 6;
    /** The antish var bit. */
    protected static final int ANTISH = 5;
    /** The lexical scope bit. */
    protected static final int LEXICAL = 4;
    /** The safe bit. */
    protected static final int SAFE = 3;
    /** The silent bit. */
    protected static final int SILENT = 2;
    /** The strict bit. */
    protected static final int STRICT = 1;
    /** The cancellable bit. */
    protected static final int CANCELLABLE = 0;
    /** The flags names ordered. */
    private static final String[] NAMES = {
        "cancellable", "strict", "silent", "safe", "lexical", "antish", "lexicalShade"
    };
    /** Default mask .*/
    protected static int DEFAULT = 1 /*<< CANCELLABLE*/ | 1 << STRICT | 1 << ANTISH | 1 << SAFE;
    /** The arithmetic math context. */
    private MathContext mathContext = null;
    /** The arithmetic math scale. */
    private int mathScale = Integer.MIN_VALUE;
    /** The arithmetic strict math flag. */
    private boolean strictArithmetic = true;
    /** The default flags, all but safe. */
    private int flags = DEFAULT;

    /**
     * Sets the value of a flag in a mask.
     * @param ordinal the flag ordinal
     * @param mask the flags mask
     * @param value true or false
     * @return the new flags mask value
     */
    protected static int set(int ordinal, int mask, boolean value) {
        return value? mask | (1 << ordinal) : mask & ~(1 << ordinal);
    }

    /**
     * Checks the value of a flag in the mask.
     * @param ordinal the flag ordinal
     * @param mask the flags mask
     * @return the mask value with this flag or-ed in
     */
    protected static boolean isSet(int ordinal, int mask) {
        return (mask & 1 << ordinal) != 0;
    }

    /**
     * Default ctor.
     */
    public Options() {}

    /**
     * Sets the default flags.
     * <p>Used by tests to force default options.
     * @param flags the flags to set
     */
    public static void setDefaultFlags(String...flags) {
        DEFAULT = parseFlags(DEFAULT, flags);
    }

    /**
     * Parses flags by name.
     * <p>A '+flag' or 'flag' will set flag as true, '-flag' set as false.
     * The possible flag names are:
     * cancellable, strict, silent, safe, lexical, antish, lexicalShade
     * @param mask the initial mask state
     * @param flags the flags to set
     * @return the flag mask updated
     */
    public static int parseFlags(int mask, String...flags) {
        for(String name : flags) {
            boolean b = true;
            if (name.charAt(0) == '+') {
                name = name.substring(1);
            } else if (name.charAt(0) == '-') {
                name = name.substring(1);
                b = false;
            }
            for(int flag = 0; flag < NAMES.length; ++flag) {
                if (NAMES[flag].equals(name)) {
                    if (b) {
                        mask |= (1 << flag);
                    } else {
                        mask &= ~(1 << flag);
                    }
                    break;
                }
            }
        }
        return mask;
    }

    @Override
    public Options set(JexlEngine jexl) {
        if (jexl instanceof Engine) {
            set(((Engine) jexl).options);
        } else {
            mathContext = jexl.getArithmetic().getMathContext();
            mathScale = jexl.getArithmetic().getMathScale();
            strictArithmetic = jexl.getArithmetic().isStrict();
            set(STRICT, flags, jexl.isStrict());
            set(SILENT, flags, jexl.isSilent());
            set(SAFE, flags, jexl.isSafe());
            set(CANCELLABLE, flags, jexl.isCancellable());
            set(ASSERTIONS, flags, jexl.isAssertions());
        }
        return this;
    }

    @Override
    public Options set(JexlOptions opts) {
        if (opts instanceof Options) {
            Options src = (Options) opts;
            mathContext = src.mathContext;
            mathScale = src.mathScale;
            strictArithmetic = src.strictArithmetic;
            flags = src.flags;
        } else {
            mathContext = opts.getMathContext();
            mathScale = opts.getMathScale();
            strictArithmetic = opts.isStrict();
            int mask = DEFAULT;
            mask = set(STRICT, mask, opts.isStrict());
            mask = set(SILENT, mask, opts.isSilent());
            mask = set(SAFE, mask, opts.isSafe());
            mask = set(CANCELLABLE, mask, opts.isCancellable());
            mask = set(ASSERTIONS, mask, opts.isAssertions());
            mask = set(LEXICAL, mask, opts.isLexical());
            mask = set(SHADE, mask, opts.isLexicalShade());
            mask = set(ANTISH, mask, opts.isAntish());
            flags = mask;
        }
        return this;
    }

    @Override
    public Options copy() {
        return new Options().set(this);
    }

    @Override
    public void setAntish(boolean flag) {
        flags = set(ANTISH, flags, flag);
    }

    @Override
    public void setMathContext(MathContext mcontext) {
        this.mathContext = mcontext;
    }

    @Override
    public void setMathScale(int mscale) {
        this.mathScale = mscale;
    }

    @Override
    public void setStrictArithmetic(boolean stricta) {
        this.strictArithmetic = stricta;
    }

    @Override
    public void setStrict(boolean flag) {
        flags = set(STRICT, flags, flag);
    }

    @Override
    public void setSafe(boolean flag) {
        flags = set(SAFE, flags, flag);
    }

    @Override
    public void setSilent(boolean flag) {
        flags = set(SILENT, flags, flag);
    }

    @Override
    public void setCancellable(boolean flag) {
        flags = set(CANCELLABLE, flags, flag);
    }

    @Override
    public void setAssertions(boolean flag) {
        flags = set(ASSERTIONS, flags, flag);
    }

    @Override
    public void setLexical(boolean flag) {
        flags = set(LEXICAL, flags, flag);
    }

    @Override
    public void setLexicalShade(boolean flag) {
        flags = set(SHADE, flags, flag);
    }

    @Override
    public boolean isAntish() {
        return isSet(ANTISH, flags);
    }

    @Override
    public boolean isSilent() {
        return isSet(SILENT, flags);
    }

    @Override
    public boolean isStrict() {
        return isSet(STRICT, flags);
    }

    @Override
    public boolean isSafe() {
        return isSet(SAFE, flags);
    }

    @Override
    public boolean isCancellable() {
        return isSet(CANCELLABLE, flags);
    }

    @Override
    public boolean isAssertions() {
        return isSet(ASSERTIONS, flags);
    }

    @Override
    public boolean isLexical() {
        return isSet(LEXICAL, flags);
    }

    @Override
    public boolean isLexicalShade() {
        return isSet(SHADE, flags);
    }

    @Override
    public boolean isStrictArithmetic() {
        return strictArithmetic;
    }

    @Override
    public MathContext getMathContext() {
        return mathContext;
    }

    @Override
    public int getMathScale() {
        return mathScale;
    }

}
