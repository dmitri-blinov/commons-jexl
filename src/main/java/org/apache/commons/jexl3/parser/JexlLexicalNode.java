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

import org.apache.commons.jexl3.internal.LexicalScope;

/**
 * Base class for AST nodes behaving as lexical units.
 * @since 3.2
 */
public class JexlLexicalNode extends JexlNode implements JexlParser.LexicalUnit {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /** The local lexical scope, local information about let/const. */
    private LexicalScope locals;

    public JexlLexicalNode(final int id) {
        super(id);
    }

    public JexlLexicalNode(final Parser p, final int id) {
        super(p, id);
    }

    @Override
    public boolean declareSymbol(final int symbol) {
        if (locals == null) {
            locals  = new LexicalScope();
        }
        return locals.addSymbol(symbol);
    }

    @Override
    public boolean declareSymbol(final int symbol, final Class c, final boolean lex, final boolean fin, final boolean req) {
        if (locals == null) {
            locals  = new LexicalScope();
        }
        return locals.addSymbol(symbol, c, lex, fin, req);
    }

    @Override
    public int getSymbolCount() {
        return locals == null? 0 : locals.getSymbolCount();
    }

    @Override
    public boolean hasSymbol(final int symbol) {
        return locals != null && locals.hasSymbol(symbol);
    }

    @Override
    public boolean isSymbolLexical(int symbol) {
        return locals != null && locals.isVariableLexical(symbol);
    }

    @Override
    public boolean isSymbolFinal(int symbol) {
        return locals != null && locals.isVariableFinal(symbol);
    }

    @Override
    public LexicalScope getLexicalScope() {
        return locals;
    }
}
