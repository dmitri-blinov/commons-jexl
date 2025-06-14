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
package org.apache.commons.jexl3;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;

import org.apache.commons.jexl3.parser.JexlNode;

/**
 * Tests for jexl probe interface
 * @since 4.0
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ProbeTest extends JexlTestCase {

    public ProbeTest() {
        super("ProbeTest");
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

    protected static class JexlProbeTest implements JexlProbe {

        protected long frameId;
        protected long sourceId;

        protected List<StatementInfo> stmts = new ArrayList<StatementInfo> ();
        protected Map<JexlInfo, String> sources = new HashMap<> ();

        protected JexlInfo script;
        protected Object result;
        protected Throwable exception;

        protected JexlInfo getScript() {
            return script;
        }

        protected Object getResult() {
            return result;
        }

        protected Throwable getException() {
            return exception;
        }

        protected Map<JexlInfo, String> getSources() {
            return sources;
        }

        protected String getSource(JexlInfo script) {
            return sources.get(script);
        }

        protected List<StatementInfo> getStatements() {
            return stmts;
        }

        protected StatementInfo getStatement(int i) {
            return stmts.get(i);
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public long loadSource(JexlInfo script, String source) {
            sources.put(script, source);
            return ++sourceId;
        }

        @Override
        public long startScript(JexlInfo script) {
            this.script = script;
            return ++frameId;
        }

        @Override
        public boolean endScript(Frame frame, Object result, Throwable any) {
            this.result = result;
            this.exception = any;

            return true;
        }

        @Override
        public boolean startStatement(JexlInfo source, JexlNode node, Frame frame) {
            stmts.add(new StatementInfo(source, node));
            return true;
        }

        @Override
        public boolean endStatement(JexlInfo source, JexlNode node, Frame frame, Object result, Throwable any) {
            int pos = stmts.size() - 1;
            StatementInfo stmt = stmts.get(pos);

            stmt.setResult(result);
            stmt.setException(any);
            stmt.captureFrame(frame);

            return true;
        }
    }

    @Test
    public void testConstant() throws Exception {

        JexlProbeTest probe = new JexlProbeTest();
        JexlEngine jexl = new JexlBuilder().probe(probe).create();

        JexlContext jc = new MapContext();
        JexlInfo i = new JexlInfo("42.jexl", 1, 1);
        Object o = jexl.createScript(i, "42").execute(jc);

        Assert.assertEquals(42, o);
        Assert.assertEquals(42, probe.getResult());
        Assert.assertEquals(2, probe.getStatements().size());
        Assert.assertEquals("42", probe.getSource(i));
    }

    @Test
    public void testArithmetic() throws Exception {

        JexlProbeTest probe = new JexlProbeTest();
        JexlEngine jexl = new JexlBuilder().probe(probe).create();

        JexlContext jc = new MapContext();
        JexlInfo i = new JexlInfo("42.jexl", 1, 1);
        Object o = jexl.createScript(i, "40+2").execute(jc);

        Assert.assertEquals(42, o);
        Assert.assertEquals(42, probe.getResult());
        Assert.assertEquals(2, probe.getStatements().size());
        Assert.assertEquals("40+2", probe.getSource(i));
    }

    @Test
    public void testVar() throws Exception {

        JexlProbeTest probe = new JexlProbeTest();
        JexlEngine jexl = new JexlBuilder().probe(probe).create();

        JexlContext jc = new MapContext();
        JexlInfo i = new JexlInfo("42.jexl", 1, 1);
        Object o = jexl.createScript(i, "var a = 40+2").execute(jc);

        Assert.assertEquals(42, o);
        Assert.assertEquals(42, probe.getResult());
        Assert.assertEquals(3, probe.getStatements().size());
        Assert.assertEquals(1, probe.getStatement(2).getLocals().size());
        Assert.assertEquals("a", probe.getStatement(2).getLocal("a").getName());
        Assert.assertEquals(42, probe.getStatement(2).getLocal("a").getValue());
    }

    @Test
    public void testMultiVar() throws Exception {

        JexlProbeTest probe = new JexlProbeTest();
        JexlEngine jexl = new JexlBuilder().probe(probe).create();

        JexlContext jc = new MapContext();
        JexlInfo i = new JexlInfo("42.jexl", 1, 1);
        Object o = jexl.createScript(i, "var a = 40+2, b = 43").execute(jc);

        Assert.assertEquals(43, o);
        Assert.assertEquals(43, probe.getResult());
        Assert.assertEquals(4, probe.getStatements().size());
        Assert.assertEquals(2, probe.getStatement(3).getLocals().size());
        Assert.assertEquals("a", probe.getStatement(3).getLocal("a").getName());
        Assert.assertEquals(42, probe.getStatement(3).getLocal("a").getValue());
        Assert.assertEquals("b", probe.getStatement(3).getLocal("b").getName());
        Assert.assertEquals(43, probe.getStatement(3).getLocal("b").getValue());
    }

    @Test
    public void testScopedVar() throws Exception {

        JexlProbeTest probe = new JexlProbeTest();
        JexlEngine jexl = new JexlBuilder().probe(probe).create();

        JexlContext jc = new MapContext();
        JexlInfo i = new JexlInfo("42.jexl", 1, 1);
        Object o = jexl.createScript(i, "final String &a = '42'").execute(jc);

        Assert.assertEquals("42", o);
        Assert.assertEquals("42", probe.getResult());
        Assert.assertEquals(3, probe.getStatements().size());
        Assert.assertEquals(1, probe.getStatement(2).getLocals().size());
        Assert.assertEquals("a", probe.getStatement(2).getLocal("a").getName());
        Assert.assertEquals("42", probe.getStatement(2).getLocal("a").getValue());
        Assert.assertEquals(String.class, probe.getStatement(2).getLocal("a").getType());
        Assert.assertTrue(probe.getStatement(2).getLocal("a").isFinal());
        Assert.assertTrue(probe.getStatement(2).getLocal("a").isRequired());
    }

    @Test
    public void testMultiStatement() throws Exception {

        JexlProbeTest probe = new JexlProbeTest();
        JexlEngine jexl = new JexlBuilder().probe(probe).create();

        JexlContext jc = new MapContext();
        JexlInfo i = new JexlInfo("42.jexl", 1, 1);
        Object o = jexl.createScript(i, "var a = 40+2; a = 43").execute(jc);

        Assert.assertEquals(43, o);
        Assert.assertEquals(43, probe.getResult());
        Assert.assertEquals(4, probe.getStatements().size());
        Assert.assertEquals(1, probe.getStatement(2).getLocals().size());
        Assert.assertEquals(1, probe.getStatement(3).getLocals().size());
        Assert.assertEquals("a", probe.getStatement(2).getLocal("a").getName());
        Assert.assertEquals(42, probe.getStatement(2).getLocal("a").getValue());
        Assert.assertEquals("a", probe.getStatement(3).getLocal("a").getName());
        Assert.assertEquals(43, probe.getStatement(3).getLocal("a").getValue());
    }

    @Test
    public void testParameters() throws Exception {

        JexlProbeTest probe = new JexlProbeTest();
        JexlEngine jexl = new JexlBuilder().probe(probe).create();

        JexlContext jc = new MapContext();
        JexlInfo i = new JexlInfo("42.jexl", 1, 1);
        Object o = jexl.createScript(i, "function (x) { 42 }").execute(jc);

        Assert.assertEquals(42, o);
        Assert.assertEquals(42, probe.getResult());
        Assert.assertEquals(3, probe.getStatements().size());
        Assert.assertEquals(0, probe.getStatement(2).getLocals().size());
        Assert.assertEquals(1, probe.getStatement(2).getParameters().size());
        Assert.assertEquals("x", probe.getStatement(2).getParameter("x").getName());
        Assert.assertEquals(null, probe.getStatement(2).getParameter("x").getValue());
    }

    @Test
    public void testParametersValues() throws Exception {

        JexlProbeTest probe = new JexlProbeTest();
        JexlEngine jexl = new JexlBuilder().probe(probe).create();

        JexlContext jc = new MapContext();
        JexlInfo i = new JexlInfo("42.jexl", 1, 1);
        Object o = jexl.createScript(i, "function (x) { 40 + x }").execute(jc, 2);

        Assert.assertEquals(42, o);
        Assert.assertEquals(42, probe.getResult());
        Assert.assertEquals(3, probe.getStatements().size());
        Assert.assertEquals(0, probe.getStatement(2).getLocals().size());
        Assert.assertEquals(1, probe.getStatement(2).getParameters().size());
        Assert.assertEquals("x", probe.getStatement(2).getParameter("x").getName());
        Assert.assertEquals(2, probe.getStatement(2).getParameter("x").getValue());
    }

}
