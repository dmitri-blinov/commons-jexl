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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for pragmas
 */
public class PragmaTest extends JexlTestCase {
    /**
     * Create a new test case.
     */
    public PragmaTest() {
        super("PragmaTest");
    }

    /**
     * Test creating a script from a string.
     */
    @Test
    @SuppressWarnings("AssertEqualsBetweenInconvertibleTypes")
    public void testPragmas() {
        final JexlScript script = JEXL.createScript("#pragma one 1\n#pragma the.very.hard 'truth'\n2;");
        Assert.assertNotNull(script);
        final Map<String, Object> pragmas = script.getPragmas();
        Assert.assertEquals(2, pragmas.size());
        Assert.assertEquals(1, pragmas.get("one"));
        Assert.assertEquals("truth", pragmas.get("the.very.hard"));
    }

    @Test
    @SuppressWarnings("AssertEqualsBetweenInconvertibleTypes")
    public void testJxltPragmas() {
        final JxltEngine engine = new JexlBuilder().create().createJxltEngine();
        final JxltEngine.Template tscript = engine.createTemplate("$$ #pragma one 1\n$$ #pragma the.very.hard 'truth'\n2;");
        Assert.assertNotNull(tscript);
        final Map<String, Object> pragmas = tscript.getPragmas();
        Assert.assertEquals(2, pragmas.size());
        Assert.assertEquals(1, pragmas.get("one"));
        Assert.assertEquals("truth", pragmas.get("the.very.hard"));
    }

    public static class SafeContext extends JexlEvalContext {
        // @Override
        public void processPragmas(final Map<String, Object> pragmas) {
            if (pragmas != null && !pragmas.isEmpty()) {
                final JexlOptions options = getEngineOptions();
                for (final Map.Entry<String, Object> pragma : pragmas.entrySet()) {
                    final String key = pragma.getKey();
                    final Object value = pragma.getValue();
                    if ("jexl.safe".equals(key) && value instanceof Boolean) {
                        options.setSafe((Boolean) value);
                    } else if ("jexl.strict".equals(key) && value instanceof Boolean) {
                        options.setStrict((Boolean) value);
                    } else if ("jexl.silent".equals(key) && value instanceof Boolean) {
                        options.setSilent((Boolean) value);
                    }
                }
            }
        }

        /**
         * Sleeps, called through scripts.
         * @param ms time to sleep in ms
         */
        public void sleep(final long ms) {
            try {
                Thread.sleep(ms);
            } catch (final InterruptedException e) {
                // ignore
            }
        }
    }

    @Test
    @SuppressWarnings("AssertEqualsBetweenInconvertibleTypes")
    public void testSafePragma() {
        SafeContext jc = new SafeContext();
        jc.set("foo", null);
        final JexlScript script = JEXL.createScript("#pragma jexl.safe true\nfoo.bar;");
        Assert.assertNotNull(script);
        jc.processPragmas(script.getPragmas());
        Object result = script.execute(jc);
        Assert.assertNull(result);
        jc = new SafeContext();
        jc.set("foo", null);
        try {
            script.execute(jc);
            Assert.fail("should have thrown");
        } catch (final JexlException xvar) {
            // ok, expected
        }
    }

    public static class ModuleContext extends MapContext {
        protected final Map<String, JexlScript> sources = new TreeMap<>();

        ModuleContext() {  }
        public Object script(String name) {
            return sources.get(name);
        }

        void script(String name, JexlScript script) { sources.put(name, script); }
    }

    public static class CachingModuleContext extends ModuleContext implements JexlContext.ModuleProcessor {
        private final ConcurrentMap<String, Object> modules = new ConcurrentHashMap<>();
        private final AtomicInteger count = new AtomicInteger(0);

        public int getCountCompute() {
            return count.get();
        }

        CachingModuleContext() {
        }

        @Override
        public Object processModule(JexlEngine engine, JexlInfo info, String name, final String body) {
            if (body.isEmpty()) {
                modules.remove(name);
                return null;
            }
            return modules.computeIfAbsent(name, (n) -> {
                Object module = engine.createExpression(info, body).evaluate(this);
                if (module instanceof JexlScript) {
                    module = ((JexlScript) module).execute(this);
                }
                count.incrementAndGet();
                return module;
            });
        }
    }

    @Test public void testPragmaModuleNoCache() {
        ModuleContext ctxt = new ModuleContext();
        runPragmaModule(ctxt, null);
    }
    @Test public void testPragmaModuleCache() {
        CachingModuleContext ctxt = new CachingModuleContext();
        runPragmaModule(ctxt, ctxt);
    }
    void runPragmaModule(ModuleContext ctxt, CachingModuleContext cmCtxt) {
        ctxt.script("module0", JEXL.createScript("function f42(x) { 42 + x; } function f43(x) { 43 + x; }; { 'f42' : f42, 'f43' : f43 }"));
        ConcurrentMap<String, Object> modules = new ConcurrentHashMap<>();
        JexlScript script ;
        Object result ;
        script = JEXL.createScript("#pragma jexl.module.m0 \"script('module0')\"\n m0:f42(10);");
        result = script.execute(ctxt);
        Assert.assertEquals(52, result);
        if (cmCtxt != null) {
            Assert.assertEquals(1, cmCtxt.getCountCompute());
        }
        result = script.execute(ctxt);
        Assert.assertEquals(52, result);
        if (cmCtxt != null) {
            Assert.assertEquals(1, cmCtxt.getCountCompute());
        }
        script = JEXL.createScript("#pragma jexl.module.m0 \"script('module0')\"\n m0:f43(10);");
        result = script.execute(ctxt);
        Assert.assertEquals(53, result);
        if (cmCtxt != null) {
            Assert.assertEquals(1, cmCtxt.getCountCompute());
        }
        try {
            script = JEXL.createScript("#pragma jexl.module.m0 ''\n#pragma jexl.module.m0 \"fubar('module0')\"\n m0:f43(10);");
            result = script.execute(ctxt);
            Assert.fail("fubar sshoud fail");
        } catch(JexlException.Method xmethod) {
            Assert.assertEquals("fubar", xmethod.getMethod());
        }
    }


    public static class StaticSleeper {
        // precludes instantiation
        private StaticSleeper() {}

        public static void sleep(final long ms) {
            try {
                Thread.sleep(ms);
            } catch (final InterruptedException e) {
                // ignore
            }
        }
    }

    public static class Sleeper {
        public void sleep(final long ms) {
            try {
                Thread.sleep(ms);
            } catch (final InterruptedException e) {
                // ignore
            }
        }
    }

    @Test
    public void testImportPragmaValueSet() {
        String src =
                "#pragma jexl.import java.util\n"+
                "#pragma jexl.import java.io\n"+
                "#pragma jexl.import java.net\n"+
                "42";
        final JexlScript script = JEXL.createScript(src);
        Map<String, Object> pragmas = script.getPragmas();
        Object importz = pragmas.get("jexl.import");
        Assert.assertTrue(importz instanceof Set<?>);
        Set<String> importzz = (Set<String>) importz;
        Assert.assertTrue(importzz.contains("java.util"));
        Assert.assertTrue(importzz.contains("java.io"));
        Assert.assertTrue(importzz.contains("java.net"));
        Assert.assertEquals(3, importzz.size());
        String parsed = script.getParsedText();
        Assert.assertEquals(src, parsed);
    }

    @Test
    public void testImportPragmaDisabled() {
        String src =
                "#pragma jexl.import java.util\n"+
                        "#pragma jexl.import java.io\n"+
                        "#pragma jexl.import java.net\n"+
                        "42";
        JexlFeatures features = new JexlFeatures();
        features.importPragma(false);
        final JexlEngine jexl = new JexlBuilder().features(features).create();
        try {
            final JexlScript script = jexl.createScript(src);
        } catch(JexlException.Parsing xparse) {
            Assert.assertTrue(xparse.getMessage().contains("import pragma"));
        }
    }
    @Test
    public void testNamespacePragmaDisabled() {
        JexlFeatures features = new JexlFeatures();
        features.namespacePragma(false);
        final JexlEngine jexl = new JexlBuilder().features(features).create();
        try {
            final JexlScript src = jexl.createScript(
                    "#pragma jexl.namespace.sleeper " + StaticSleeper.class.getName() + "\n"
                            + "sleeper:sleep(100);"
                            + "42");
            Assert.fail("should have thrown syntax exception");
        } catch(JexlException.Parsing xparse) {
            Assert.assertTrue(xparse.getMessage().contains("namespace pragma"));
        }
    }
    @Test
    @SuppressWarnings("AssertEqualsBetweenInconvertibleTypes")
    public void testStaticNamespacePragma() {
        final JexlContext jc = new SafeContext();
        final JexlScript script = JEXL.createScript(
                "#pragma jexl.namespace.sleeper " + StaticSleeper.class.getName() + "\n"
                + "sleeper:sleep(100);"
                + "42");
        final Object result = script.execute(jc);
        Assert.assertEquals(42, result);
    }

    @Test
    @SuppressWarnings("AssertEqualsBetweenInconvertibleTypes")
    public void testStatictNamespacePragmaCtl() {
        final Map<String, Object> ns = Collections.singletonMap("sleeper", StaticSleeper.class.getName());
        final JexlEngine jexl = new JexlBuilder().namespaces(ns).create();
        final JexlContext jc = new SafeContext();
        final JexlScript script = jexl.createScript(
                "sleeper:sleep(100);"
                + "42");
        final Object result = script.execute(jc);
        Assert.assertEquals(42, result);
    }

    @Test
    @SuppressWarnings("AssertEqualsBetweenInconvertibleTypes")
    public void testNamespacePragma() {
        final JexlContext jc = new SafeContext();
        final String src =
                "#pragma jexl.namespace.sleeper " + Sleeper.class.getName() + "\n"
                        + "sleeper:sleep(100);\n"
                        + "42;\n";
        final JexlScript script = JEXL.createScript(src);
        final Object result = script.execute(jc);
        Assert.assertEquals(42, result);
        String parsed = script.getParsedText();
        Assert.assertEquals(src, parsed);
    }

    @Test
    @SuppressWarnings("AssertEqualsBetweenInconvertibleTypes")
    public void testNamespacePragmaCtl() {
        final Map<String, Object> ns = Collections.singletonMap("sleeper", Sleeper.class.getName());
        final JexlEngine jexl = new JexlBuilder().namespaces(ns).create();
        final JexlContext jc = new SafeContext();
        final JexlScript script = jexl.createScript(
                "sleeper:sleep(100);"
                + "42");
        final Object result = script.execute(jc);
        Assert.assertEquals(42, result);
    }

    @Test public void test354() {
        Map<String, Number> values = new TreeMap<>();
        values.put("1", 1);
        values.put("+1", 1);
        values.put("-1", -1);
        values.put("1l", 1L);
        values.put("+1l", 1L);
        values.put("-1l", -1L);
        values.put("10h", BigInteger.valueOf(10));
        values.put("-11h", BigInteger.valueOf(-11));
        values.put("+12h", BigInteger.valueOf(12));
        values.put("0xa", 0xa);
        values.put("+0xa", 0xa);
        values.put("-0xa", -0xa);
        values.put("0xacl", 0xacL);
        values.put("+0xadl", 0xadL);
        values.put("-0xafl", -0xafL);
        values.put("1d", 1d);
        values.put("-1d", -1d);
        values.put("+1d", 1d);
        values.put("1f", 1f);
        values.put("-1f", -1f);
        values.put("+1f", 1f);
        values.put("1B", new BigDecimal(1));
        values.put("-1B", new BigDecimal(-1));
        values.put("+1B", new BigDecimal(1));
        values.put("-42424242424242424242424242424242", new BigInteger("-42424242424242424242424242424242"));
        values.put("+42424242424242424242424242424242", new BigInteger("+42424242424242424242424242424242"));
        values.put("42424242424242424242424242424242", new BigInteger("42424242424242424242424242424242"));
        JexlEngine jexl = new JexlBuilder().safe(true).create();
        for(Map.Entry<String, Number> e : values.entrySet()) {
            String text = "#pragma number " + e.getKey();
            JexlScript script = jexl.createScript(text);
            Assert.assertNotNull(script);
            Map<String, Object> pragmas = script.getPragmas();
            Assert.assertNotNull(pragmas);
            Assert.assertEquals(e.getKey(), e.getValue(), pragmas.get("number"));
        }
    }
}
