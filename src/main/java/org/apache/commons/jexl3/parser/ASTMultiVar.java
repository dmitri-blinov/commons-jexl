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

import java.util.List;
import java.util.ArrayList;

/**
 * Declares a mutitype local variable.
 */
public class ASTMultiVar extends ASTVar {

    /** The optional variable type. */
    private List<Class> mtype;

    void addType(Class c) {
        mtype.add(c);
    }

    public List<Class> getTypes() {
        return mtype;
    }

    public ASTMultiVar(int id) {
        super(id);
        mtype = new ArrayList<Class> ();
    }

    public ASTMultiVar(Parser p, int id) {
        super(p, id);
        mtype = new ArrayList<Class> ();
    }

    // Always final
    public boolean isFinal() {
        return true;
    }

    @Override
    public Object jjtAccept(ParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
}
