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
package org.apache.commons.jexl3.parser;

public final class ASTBooleanLiteral extends JexlNode implements JexlNode.Constant<Boolean> {
    private Boolean literal = null;

    ASTBooleanLiteral(int id) {
        super(id);
    }

    ASTBooleanLiteral(Parser p, int id) {
        super(p, id);
    }

    @Override
    public String toString() {
        return String.valueOf(literal);
    }

    @Override
    public Boolean getLiteral() {
        return literal;
    }

    @Override
    protected boolean isConstant(boolean literal) {
        return true;
    }

    void setLiteral(Boolean literal) {
        this.literal = literal;
    }

    @Override
    public Object jjtAccept(ParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
}
