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
package org.apache.commons.jexl3;

import org.apache.commons.jexl3.parser.JexlNode;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;


/**
 * Script debug base implementation.
 *
 */
public abstract class JexlDebugger implements JexlProbe {

    protected boolean enabled;

    protected Map<JexlInfo, String> sources;
    protected LongAdder frameId;

    public JexlDebugger() {
        sources = new ConcurrentHashMap<> ();
        frameId = new LongAdder();
    }

    protected static class StatementInfo {

        protected final JexlInfo stmt;
        protected final JexlNode node;

        protected Object result;
        protected Throwable exception;

        protected Map<String, VarInfo> locals = new LinkedHashMap<> ();
        protected Map<String, VarInfo> parameters = new LinkedHashMap<> ();
        protected Map<String, VarInfo> capturedVariables = new LinkedHashMap<> ();

        protected StatementInfo(final JexlInfo stmt, final JexlNode node) {
            this.stmt = stmt;
            this.node = node;
        }

        protected Map<String, VarInfo> getLocals() {
            return locals;
        }

        protected VarInfo getLocal(final String name) {
            return locals.get(name);
        }

        protected Map<String, VarInfo> getParameters() {
            return parameters;
        }

        protected VarInfo getParameter(final String name) {
            return parameters.get(name);
        }

        protected Map<String, VarInfo> getCapturedVariables() {
            return capturedVariables;
        }

        protected VarInfo getCapturedVar(final String name) {
            return capturedVariables.get(name);
        }

        protected JexlInfo getStatement() {
            return stmt;
        }

        protected JexlNode getNode() {
            return node;
        }

        protected Object getResult() {
            return result;
        }

        protected void captureFrame(JexlProbe.Frame frame) {
            captureVariables(frame.getLocals(), locals);
            captureVariables(frame.getParameters(), parameters);
            captureVariables(frame.getCapturedVariables(), capturedVariables);
        }

        protected void captureVariables(JexlProbe.Scope l, Map<String, VarInfo> vars) {
            String[] names = l.getNames();
            if (names != null) {
                for (String name : names) {
                    JexlProbe.Variable i = l.getVariableInfo(name);
                    if (i != null) {
                       vars.put(name, new VarInfo(name, i.getType(), i.isRequired(), i.isFinal(), l.getVariable(name)));
                    } else {
                       vars.put(name, null);
                    }
                }
            }
        }

        protected void setResult(Object value) {
            result = value;
        }

        protected Throwable getException() {
            return exception;
        }

        protected void setException(Throwable value) {
            exception = value;
        }

    }

    protected static class VarInfo {

        protected final String name;
        protected final Class type;
        protected final boolean isFinal;
        protected final boolean isRequired;

        protected final Object value;

        protected VarInfo(final String name, final Class type, final boolean isFinal, final boolean isRequired, final Object value) {
            this.name = name;
            this.type = type;
            this.isFinal = isFinal;
            this.isRequired = isRequired;
            this.value = value;
        }

        protected String getName() {
            return name;
        }

        protected Class getType() {
            return type;
        }

        protected Object getValue() {
            return value;
        }

        protected boolean isFinal() {
            return isFinal;
        }

        protected boolean isRequired() {
            return isRequired;
        }

    }

    public String getSource(JexlInfo script) {
        return sources.get(script);
    }

    // JexlProbe interface

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
    }

    @Override
    public long loadSource(JexlInfo script, String source) {
        sources.put(script, source);
        return sources.size();
    }

    @Override
    public long startScript(JexlInfo script) {
        frameId.increment();
        return frameId.longValue();
    }

    @Override
    public boolean endScript(Frame frame, Object result, Throwable any) {
        return true;
    }

    @Override
    public boolean startStatement(JexlInfo source, JexlNode node, Frame frame) {
        return true;
    }

    @Override
    public boolean endStatement(JexlInfo source, JexlNode node, Frame frame, Object result, Throwable any) {
        return true;
    }

}
