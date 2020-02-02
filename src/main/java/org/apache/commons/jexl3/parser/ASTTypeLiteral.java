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

public final class ASTTypeLiteral extends ASTClassLiteral {

    /** The actual literal value; the inherited 'value' member may host a cached getter. */

    private Class type;
    private int array;

    ASTTypeLiteral(int id) {
        super(id);
    }

    ASTTypeLiteral(Parser p, int id) {
        super(p, id);
    }

    @Override
    public String toString() {
        if (array == 0)
            return super.toString();
        StringBuilder result = new StringBuilder();
        result.append(super.toString());
        for (int i = 0; i < array; i++)
            result.append("[]");
        return result.toString();
    }

    public Class getType() {
        return type;
    }

    void setLiteral(Class literal) {
        super.setLiteral(literal);
        type = literal;
        array = 0;
    }

    void setArray() {
        array++;
        type = JexlParser.arrayType(type);
    }

    public int getArray() {
        return array;
    }

    @Override
    public Object jjtAccept(ParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
}
