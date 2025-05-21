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

import org.apache.commons.jexl3.JxltEngine;

public final class ASTJxltLiteral extends JexlNode {
    /** Serial uid.*/
    private static final long serialVersionUID = 1L;
    /** The actual literal value. */
    private String literal;
    /** The expression (parsed). */
    private transient JxltEngine.Expression jxltExpression = null;

    ASTJxltLiteral(final int id) {
        super(id);
    }

    public JxltEngine.Expression getExpression() {
        return jxltExpression;
    }

    /**
     * Gets the literal value.
     * @return the string literal
     */
    public String getLiteral() {
        return this.literal;
    }

    @Override
    public Object jjtAccept(final ParserVisitor visitor, final Object data) {
        return visitor.visit(this, data);
    }

    public void setExpression(final JxltEngine.Expression e) {
        this.jxltExpression = e;
    }

    void setLiteral(final String literal) {
        this.literal = literal;
    }

    @Override
    public String toString() {
        return this.literal;
    }
}
