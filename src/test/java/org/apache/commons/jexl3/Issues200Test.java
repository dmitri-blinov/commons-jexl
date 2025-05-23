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


import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.jexl3.internal.TemplateDebugger;
import org.apache.commons.jexl3.introspection.JexlSandbox;
import org.apache.commons.logging.Log;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for reported issue between JEXL-200 and JEXL-299.
 */
@SuppressWarnings({"boxing", "UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class Issues200Test extends JexlTestCase {
    public Issues200Test() {
        super("Issues200Test", null);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        // ensure jul logging is only error to avoid warning in silent mode
        java.util.logging.Logger.getLogger(JexlEngine.class.getName()).setLevel(java.util.logging.Level.SEVERE);
    }

    public static class Eval {
        private JexlEngine jexl;

        public JexlScript fn(final String src) {
            return jexl.createScript(src);
        }

        void setJexl(final JexlEngine je) {
            jexl = je;
        }
    }

    @Test
    public void test200() throws Exception {
        final JexlContext jc = new MapContext();
        final Map<String, Object> funcs = new HashMap<>();
        final Eval eval = new Eval();
        funcs.put(null, eval);
        final JexlEngine jexl = new JexlBuilder().namespaces(funcs).create();
        eval.setJexl(jexl);
        final String src = "var f = fn(\'(x)->{x + 42}\'); f(y)";
        final JexlScript s200 = jexl.createScript(src, "y");
        Assert.assertEquals(142, s200.execute(jc, 100));
        Assert.assertEquals(52, s200.execute(jc, 10));
    }

    @Test
    public void test209a() throws Exception {
        final JexlContext jc = new MapContext();
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlScript e = jexl.createScript("var x = new('java.util.HashMap'); x.a = ()->{return 1}; x['a']()");
        final Object r = e.execute(jc);
        Assert.assertEquals(1, r);
    }

    @Test
    public void test209b() throws Exception {
        final JexlContext jc = new MapContext();
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlScript e = jexl.createScript("var x = new('java.util.HashMap'); x['a'] = ()->{return 1}; x.a()");
        final Object r = e.execute(jc);
        Assert.assertEquals(1, r);
    }

    public static class T210 {
        public void npe() {
            throw new NullPointerException("NPE210");
        }
    }

    @Test
    public void test210() throws Exception {
        final JexlContext jc = new MapContext();
        jc.set("v210", new T210());
        final JexlEngine jexl = new JexlBuilder().strict(false).silent(false).create();
        final JexlScript e = jexl.createScript("v210.npe()");
        try {
            e.execute(jc);
            Assert.fail("should have thrown an exception");
        } catch(final JexlException xjexl) {
            final Throwable th = xjexl.getCause();
            Assert.assertEquals("NPE210", th.getMessage());
        }
    }

    @Test
    public void test217() throws Exception {
        final JexlEvalContext jc = new JexlEvalContext();
        final JexlOptions options = jc.getEngineOptions();
        jc.set("foo", new int[]{0, 1, 2, 42});
        JexlEngine jexl;
        JexlScript e;
        Object r;
        jexl = new JexlBuilder().strict(false).silent(false).create();
        e = jexl.createScript("foo[3]");
        r = e.execute(jc);
        Assert.assertEquals(42, r);

        // cache and fail?
        jc.set("foo", new int[]{0, 1});
        options.setStrict(true);
        Assert.assertTrue(options.isStrict());
        try {
            r = e.execute(jc);
            Assert.fail("should have thrown an exception");
        } catch(final JexlException xjexl) {
            final Throwable th = xjexl.getCause();
            Assert.assertEquals(ArrayIndexOutOfBoundsException.class, th.getClass());
        }
        //
        options.setStrict(false);
        r = e.execute(jc);
        Assert.assertNull("oob adverted", r);
    }


    @Test
    public void test221() throws Exception {
        final JexlEvalContext jc = new JexlEvalContext();
        final Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        jc.set("map", map);
        final JexlEngine jexl = new JexlBuilder().cache(256).create();
        final JexlScript e = jexl.createScript("(x)->{ map[x] }");
        Object r;
        r = e.execute(jc, (Object) null);
        Assert.assertNull(r);
        r = e.execute(jc, (Object) null);
        Assert.assertNull(r);
        r = e.execute(jc, "one");
        Assert.assertEquals(1, r);
    }


    public static class JexlArithmetic224 extends JexlArithmetic {
        public JexlArithmetic224(final boolean astrict) {
            super(astrict);
        }

        protected Object nth(final Collection<?> c, int i) {
            if (c instanceof List) {
                // tell engine to use default
                return JexlEngine.TRY_FAILED;
            }
            for (final Object o : c) {
                if (i-- == 0) {
                    return o;
                }
            }
            return null;
        }

        public Object propertyGet(final Collection<?> c, final Number n) {
            return nth(c, n.intValue());
        }

        public Object arrayGet(final Collection<?> c, final Number n) {
            return nth(c, n.intValue());
        }

        public Object call(final Collection<?> c, final Number n) {
            if (c instanceof List) {
                return ((List) c).get(n.intValue());
            }
            return nth(c, n.intValue());
        }
    }

    @Test
    public void test224() throws Exception {
        final List<String> a0 = Arrays.asList("one", "two");
        final Set<String> a1 = new TreeSet<>(a0);
        final JexlContext jc = new MapContext();
        final JexlEngine jexl = new JexlBuilder().arithmetic(new JexlArithmetic224(true)).create();
        Object r;
        JexlScript e = jexl.createScript("(map, x)->{ map[x] }");
        r = e.execute(jc, a0, 1);
        Assert.assertEquals("two", r);
        r = e.execute(jc, a1, 1);
        Assert.assertEquals("two", r);
        e = jexl.createScript("(map)->{ map.1 }");
        r = e.execute(jc, a0);
        Assert.assertEquals("two", r);
        r = e.execute(jc, a1);
        Assert.assertEquals("two", r);
        e = jexl.createScript("(map, x)->{ map(x) }");
        r = e.execute(jc, a0, 1);
        Assert.assertEquals("two", r);
        r = e.execute(jc, a1, 1);
        Assert.assertEquals("two", r);
    }

    public static class Context225 extends MapContext {
        public String bar(){
            return "bar";
        }
    }

    @Test
    public void test225() throws Exception {
        final Context225 df = new Context225();
        final JexlEngine jexl = new JexlBuilder().create();

        final JexlExpression expression = jexl.createExpression("bar()");
        Assert.assertEquals("bar", expression.evaluate(df));
        final ObjectContext<Object> context = new ObjectContext<>(jexl, df);
        Assert.assertEquals("bar", expression.evaluate(context));
    }

    private static void handle(final ExecutorService pool, final JexlScript script, final Map<String, Object> payload) {
       pool.submit(() -> script.execute(new MapContext(payload)));
    }

    @Test
    public void test241() throws Exception {
        ExecutorService pool;
        final JexlScript script = new JexlBuilder().create().createScript("`${item}`");

        pool = Executors.newFixedThreadPool(4);

        final Map<String, Object> m1 = new HashMap<>();
        m1.put("item", "A");
        final Map<String, Object> m2 = new HashMap<>();
        m2.put("item", "B");

        handle(pool, script, m1);
        script.execute(new MapContext(m2));
        pool.shutdown();
    }

    @Test
    public void test242() throws Exception {
        final Double a = -40.05d;
        final Double b = -8.01d;
        final Double c = a + b;
        final JexlContext context = new MapContext();
        context.set("a", a);
        context.set("b", b);
        final JexlEngine JEXL_ENGINE = new JexlBuilder().strict(true).silent(true).create();
        final JexlExpression jsp = JEXL_ENGINE.createExpression("a + b");
        final Double e = (Double) jsp.evaluate(context);
        Assert.assertEquals(Double.doubleToLongBits(e) + " != " + Double.doubleToLongBits(c), c,
                e, 0.0);
        Assert.assertEquals(Double.doubleToLongBits(e) + " != " + Double.doubleToLongBits(c), a + b, e, 0.0);
    }


    @Test
    public void test243a() throws Exception {
        final JexlEngine jexl = new JexlBuilder().cache(32).create();
        final JexlScript script = jexl.createScript("while(true);");
        try {
            final JexlExpression expr = jexl.createExpression("while(true);");
            Assert.fail("should have failed!, expr do not allow 'while' statement");
        } catch (final JexlException xparse) {
            // ok
        }
    }

    public static class Foo245 {
        private Object bar;

        void setBar(final Object bar) {
            this.bar = bar;
        }

        public Object getBar() {
            return bar;
        }
    }

    @Test
    public void test245() throws Exception {
        final MapContext ctx = new MapContext();
        final Foo245 foo245 = new Foo245();
        ctx.set("foo", foo245);

        final JexlEngine engine = new JexlBuilder().strict(true).safe(false).silent(false).create();
        final JexlExpression foobar = engine.createExpression("foo.bar");
        final JexlExpression foobaz = engine.createExpression("foo.baz");
        final JexlExpression foobarbaz = engine.createExpression("foo.bar.baz");
        // add ambiguity with null & not-null
        final Object[] args = { null, 245 };
        for(final Object arg : args ){
            foo245.setBar(arg);
            // ok
            Assert.assertEquals(foo245.getBar(), foobar.evaluate(ctx));
            // fail level 1
            try {
                foobaz.evaluate(ctx);
                Assert.fail("foo.baz is not solvable, exception expected");
            } catch(final JexlException xp) {
                Assert.assertTrue(xp instanceof JexlException.Property);
            }
            // fail level 2
            try {
                foobarbaz.evaluate(ctx);
                Assert.fail("foo.bar.baz is not solvable, exception expected");
            } catch(final JexlException xp) {
                Assert.assertTrue(xp instanceof JexlException.Property);
            }
        }
    }

    @Test
    public void test256() throws Exception {
        final MapContext ctx = new MapContext() {
            @Override public void set(final String name, final Object value) {
                if ("java".equals(name)) {
                    throw new JexlException(null, "can not set " + name);
                }
                super.set(name, value);
            }
            @Override public Object get(final String name) {
                if ("java".equals(name)) {
                    return null;
                }
                return super.get(name);
            }
            @Override public boolean has(final String name) {
                if ("java".equals(name)) {
                    return false;
                }
                return super.has(name);
            }
        };
        ctx.set("java.version", 10);
        final JexlEngine engine = new JexlBuilder().strict(true).silent(false).create();
        JexlScript script;
        script = engine.createScript("java = 3");
        try {
             script.execute(ctx);
             Assert.fail("should have failed!");
        } catch(final JexlException xjexl) {
            // expected
        }
        script = engine.createScript("java.version");
        Assert.assertEquals(10, script.execute(ctx));
    }

    @Test
    public void test230() throws Exception {
        final JexlEngine jexl = new JexlBuilder().cache(4).create();
        final JexlContext ctxt = new MapContext();
        final int[] foo = {42};
        ctxt.set("fo o", foo);
        Object value;
        for (int l = 0; l < 2; ++l) {
            value = jexl.createExpression("fo\\ o[0]").evaluate(ctxt);
            Assert.assertEquals(42, value);
            value = jexl.createExpression("fo\\ o[0] = 43").evaluate(ctxt);
            Assert.assertEquals(43, value);
            value = jexl.createExpression("fo\\ o.0").evaluate(ctxt);
            Assert.assertEquals(43, value);
            value = jexl.createExpression("fo\\ o.0 = 42").evaluate(ctxt);
            Assert.assertEquals(42, value);
        }
    }

    @Test
    public void test265() throws Exception {
        final JexlEngine jexl = new JexlBuilder().cache(4).create();
        final JexlContext ctxt = new MapContext();
        ctxt.set("x", 42);
        Object result;
        JexlScript script;
        try {
            script = jexl.createScript("(true) ? x : abs(1)");
        } catch (final JexlException.Parsing xparse) {
            // ambiguous, parsing fails
        }
        script = jexl.createScript("(true) ? (x) : abs(2)");
        result = script.execute(ctxt);
        Assert.assertEquals(42, result);
        script = jexl.createScript("(true) ? x : (abs(3))");
        result = script.execute(ctxt);
        Assert.assertEquals(42, result);
        script = jexl.createScript("(!true) ? abs(4) : x");
        result = script.execute(ctxt);
        Assert.assertEquals(42, result);
    }


    /**
     * An iterator that implements Closeable (at least implements a close method).
     */
    public static class Iterator266 implements /*Closeable,*/ Iterator<Object> {
        private Iterator<Object> iterator;

        Iterator266(final Iterator<Object> ator) {
            iterator = ator;
        }

        @Override
        protected void finalize() throws Throwable {
            close();
            super.finalize();
        }

        //@Override
        public void close() {
            if (iterator != null) {
                Arithmetic266.closeIterator(this);
                iterator = null;
            }
        }

        @Override
        public boolean hasNext() {
            if (iterator == null) {
                return false;
            }
            final boolean n = iterator.hasNext();
            if (!n) {
                close();
            }
            return n;
        }

        @Override
        public Object next() {
            if (iterator == null) {
                throw new NoSuchElementException();
            }
            return iterator.next();
        }

        @Override
        public void remove() {
            if (iterator != null) {
                iterator.remove();
            }
        }
    }
    public static class Arithmetic266 extends JexlArithmetic {
        static final ThreadLocal<Deque<Iterator266>> TLS_FOREACH = new ThreadLocal<Deque<Iterator266>>() {
            @Override
            public Deque<Iterator266> initialValue() {
                return new LinkedList<>();
            }
        };
        public Arithmetic266(final boolean strict) {
            super(strict);
        }

        static void closeIterator(final Iterator266 i266) {
            final Deque<Iterator266> queue = TLS_FOREACH.get();
            if (queue != null) {
                queue.remove(i266);
            }
        }

        public Iterator<?> forEach(final Iterable<?> collection) {
            final Iterator266 it266 = new Iterator266((Iterator<Object>) collection.iterator());
            final Deque<Iterator266> queue = TLS_FOREACH.get();
            queue.addFirst(it266);
            return it266;
        }

        public Iterator<?> forEach(final Map<?,?> collection) {
            return forEach(collection.values());
        }

        public void remove() {
            final Deque<Iterator266> queue = TLS_FOREACH.get();
            final Iterator266 i266 = queue.getFirst();
            if (i266 != null) {
                i266.remove();
                throw new JexlException.Continue(null, null);
            } 
            throw new NoSuchElementException();
        }
    }

    @Test
    public void test266() throws Exception {
        Object result;
        JexlScript script;
        final JexlEngine jexl = new JexlBuilder().arithmetic(new Arithmetic266(true)).create();
        final JexlContext ctxt = new MapContext();

        final List<Integer> li = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5 ,6));
        ctxt.set("list", li);
        script = jexl.createScript("for (var item : list) { if (item <= 3) remove(); } return size(list)");
        result = script.execute(ctxt);
        Assert.assertEquals(3, result);
        Assert.assertEquals(3, li.size());

        final Map<String, Integer> msi = new HashMap<>();
        msi.put("a", 1);
        msi.put("b", 2);
        msi.put("c", 3);
        msi.put("d", 4);
        msi.put("e", 5);
        msi.put("f", 6);
        ctxt.set("map", msi);
        script = jexl.createScript("for (var item : map) { if (item <= 2) remove(); } return size(map)");
        result = script.execute(ctxt);
        Assert.assertEquals(4, result);
        Assert.assertEquals(4, msi.size());
    }

    @Test
    public void test267() throws Exception {
        Object result;
        JexlScript script;
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlContext ctxt = new MapContext();
        // API declared params
        script = jexl.createScript("x + y", "x", "y");
        result = script.execute(ctxt, 20, 22);
        Assert.assertEquals(42, result);
        // script declared params
        script = jexl.createScript("(x, y)->{ x + y}");
        result = script.execute(ctxt, 22, 20);
        Assert.assertEquals(42, result);
        // explicitly returning the lambda
        script = jexl.createScript("return (x, y)->{ x + y}");
        result = script.execute(ctxt);
        Assert.assertTrue(result instanceof JexlScript);
    }


    @Test
    public void test274() throws Exception {
        JexlEngine jexl = new JexlBuilder().strict(true).safe(true).stackOverflow(5).create();
        final JexlContext ctxt = new MapContext();
        JexlScript script= jexl.createScript("var f = (x)->{ x > 1? x * f(x - 1) : x }; f(a)", "a");
        Object result = script.execute(ctxt, 3);
        Assert.assertEquals(6, result);
        try {
            result = script.execute(ctxt, 32);
            Assert.fail("should have overflown");
        } catch(final JexlException.StackOverflow xstack) {
            // expected
            final String sxs = xstack.toString();
            Assert.assertTrue(sxs.contains("jexl"));
        }
        jexl = new JexlBuilder().strict(true).create();
        script= jexl.createScript("var f = (x)->{ x * f(x - 1) }; f(a)", "a");
        try {
            result = script.execute(ctxt, 32);
            Assert.fail("should have overflown");
        } catch(final JexlException.StackOverflow xstack) {
            // expected
            final String sxs = xstack.toString();
            Assert.assertTrue(sxs.contains("jvm"));
        }
    }

    @Test
    public void test275a() throws Exception {
        final JexlContext ctxt = new MapContext();
        ctxt.set("out", System.out);
        final JexlEngine jexl = new JexlBuilder().strict(true).safe(true).create();

        final JexlScript e = jexl.createScript("out.println(xyz)");
        try {
            final Object o = e.execute(ctxt);
            Assert.fail("should have thrown");
        } catch (final JexlException.Variable xvar) {
            Assert.assertEquals("xyz", xvar.getVariable());
        }
    }

    @Test
    public void test275b() throws Exception {
        final JexlContext ctxt = new MapContext();
        //ctxt.set("out", System.out);
        final JexlEngine jexl = new JexlBuilder().strict(true).safe(true).create();
        final JexlScript e = jexl.createScript("var xyz = xyz");
        try {
            final Object o = e.execute(ctxt);
            Assert.assertNull(o);
        } catch (final JexlException.Variable xvar) {
            Assert.fail("should not have thrown");
            // Assert.assertEquals("xyz", xvar.getVariable());
        }
    }

    @Test
    public void test275c() throws Exception {
        final JexlContext ctxt = new MapContext();
        //ctxt.set("out", System.out);
        final JexlEngine jexl = new JexlBuilder().strict(true).safe(true).silent(true).create();
        JexlScript e;
        Object r;
        e = jexl.createScript("(s, v)->{  var x = y ; 42; }");
        // wont make an error
        try {
            r = e.execute(ctxt, false, true);
            Assert.assertEquals(42, r);
        } catch (final JexlException.Variable xjexl) {
            Assert.fail("should not have thrown");
        }
    }

    @Test
    public void test275d() throws Exception {
        final JexlContext ctxt = new MapContext();
        ctxt.set("out", System.out);
        final JexlEngine jexl = new JexlBuilder().strict(true).safe(true).create();

        final JexlScript e = jexl.createScript("{ var xyz = 42 } out.println(xyz)");
        try {
            final Object o = e.execute(ctxt);
            Assert.assertNull(o);
        } catch (final JexlException.Variable xvar) {
            Assert.fail("should not have thrown" + xvar);
        }
    }

    @Test
    public void test278() throws Exception {
        final String[] srcs = new String[]{
            "return union x143('arg',5,6) ",
            "return union y143('arg',5,6)   ;",
            "return union\n z143('arg',5,6)   ;",
            "var f =()->{ return union 143 } foo[0]"
        };
        final Object[] ctls = new Object[]{
            "42","42","42", 42
        };
        final JexlEngine jexl = new JexlBuilder().cache(4).create();
        final JexlContext ctxt = new MapContext();
        final int[] foo = {42};
        ctxt.set("foo", foo);
        ctxt.set("union", "42");
        Object value;
        JexlScript jc;
        for(int i = 0; i < srcs.length; ++i) {
            String src = srcs[i];
            try {
                jc = jexl.createScript(src);
                Assert.fail("should have failed, " + (jc != null));
            } catch(final JexlException.Ambiguous xa) {
                final String str = xa.toString();
                Assert.assertTrue(str.contains("143"));
                src = xa.tryCleanSource(src);
            }
            jc = jexl.createScript(src);
            value = jc.execute(ctxt);
            Assert.assertEquals(src, ctls[i], value);
        }
    }

    public static class Context279 extends MapContext {
        public String identity(final String x) {
            return x;
        }
        public Number identity(final Number x) {
            return x;
        }
        public String[] spread(final String str) {
            if (str == null) {
                return null;
            }
             final String[] a = new String[str.length()];
             for(int i = 0; i < str.length(); ++i) {
                 a[i] = "" + str.charAt(i);
             }
             return a;
        }
    }

    @Test
    public void test279() throws Exception {
        final Log logger = null;//LogFactory.getLog(Issues200Test.class);
        Object result;
        JexlScript script;
        final JexlContext ctxt = new Context279();
        final String[] srcs = new String[]{
            "var z = null; identity(z[0]);",
             "var z = null; z.0;",
             "var z = null; z.foo();",
            "z['y']['z']",
            "z.y.any()",
             "identity(z.any())",
             "z[0]",
             "z.0",
             "z.foo()",
             "z.y[0]",
             "z.y[0].foo()",
             "z.y.0",
             "z.y.foo()",
             "var z = { 'y' : [42] }; z.y[1]",
             "var z = { 'y' : [42] }; z.y.1",
             "var z = { 'y' : [-42] }; z.y[1].foo()",
             "var z = { 'y' : [42] }; z.y.1.foo()",
             "var z = { 'y' : [null, null] }; z.y[1].foo()",
             "var z = { 'y' : [null, null] }; z.y.1.foo()"
        };
        for (int i = 0; i < 2; ++i) {
            for (final boolean strict : new boolean[]{true, false}) {
                final JexlEngine jexl = new JexlBuilder().safe(false).strict(strict).create();
                for (final String src : srcs) {
                    script = jexl.createScript(src);
                    try {
                        result = script.execute(ctxt);
                        if (strict) {
                            if (logger != null) {
                                logger.warn(ctxt.has("z") + ": " + src + ": no fail, " + result);
                            }
                            Assert.fail("should have failed: " + src);
                        }
                        // not reachable
                        Assert.assertNull("non-null result ?!", result);
                    } catch (final JexlException.Variable xvar) {
                        if (logger != null) {
                            logger.warn(ctxt.has("z") + ": " + src + ": fail, " + xvar);
                        }
                        if (!strict) {
                            Assert.fail(src + ", should not have thrown " + xvar);
                        } else {
                            Assert.assertTrue(src + ": " + xvar.toString(), xvar.toString().contains("z"));
                        }
                    } catch (final JexlException.Property xprop) {
                        if (logger != null) {
                            logger.warn(ctxt.has("z") + ": " + src + ": fail, " + xprop);
                        }
                        if (!strict) {
                            Assert.fail(src + ", should not have thrown " + xprop);
                        } else {
                            Assert.assertTrue(src + ": " + xprop.toString(), xprop.toString().contains("1"));
                        }
                    }
                }
            }
            ctxt.set("z.y", null);
        }
    }

    @Test
    public void test279b() throws Exception {
        Object result;
        JexlScript script;
        final JexlContext ctxt = new Context279();
        ctxt.set("ctxt", ctxt);
        final String src = "(x)->{ spread(x)[0].toString() }";
        final JexlEngine jexl = new JexlBuilder().safe(true).strict(true).create();
        script = jexl.createScript(src);
        result = script.execute(ctxt, "abc");
        Assert.assertEquals("a", result);
        result = null;
        try {
            result = script.execute(ctxt, (Object) null);
        } catch(final JexlException xany) {
            Assert.assertNotNull(xany.getMessage());
        }
        Assert.assertNull(result);
    }

    @Test
    public void test285() throws Exception {
        final List<String> out = new ArrayList<>(6);
        final JexlContext ctxt = new MapContext();
        ctxt.set("$out", out);
        final String src = "for(var b: ['g','h','i']) {\n"
                + "  var c = b;\n"
                + "  $out.add(c);\n"
                + "}\n"
                + " \n"
                + "for(var dc: ['j','k','l']) {\n"
                + "  $out.add(dc);\n"
                + "}"
                + " \n"
                + "$out.size()";

        final JexlFeatures features = new JexlFeatures();
        features.lexical(true);
        final JexlEngine jexl = new JexlBuilder()
                //.features(features)
                .safe(false).strict(true).lexical(true).create();
        final JexlScript script = jexl.createScript(src);
        final Object result = script.execute(ctxt, (Object) null);
        Assert.assertEquals(6, result);
        final List<String> ctl = Arrays.asList("g", "h", "i", "j", "k", "l");
        Assert.assertEquals(ctl, out);
    }

    @Test
    public void test285a() throws Exception {
        final List<String> out = new ArrayList<>(6);
        final JexlContext ctxt = new MapContext();
        ctxt.set("$out", out);
        final String src =
                  "for(var b: ['g','h','i']) { $out.add(b); }\n"
                + "for(b: ['j','k','l']) { $out.add(b);}\n"
                + "$out.size()";

        final JexlEngine jexl = new JexlBuilder().safe(false).strict(true).lexical(false).create();
        final JexlScript script = jexl.createScript(src);
        final Object result = script.execute(ctxt, (Object) null);
        Assert.assertEquals(6, result);
        final List<String> ctl = Arrays.asList("g", "h", "i", "j", "k", "l");
        Assert.assertEquals(ctl, out);
    }

    @Test
    public void test285b() throws Exception {
        final List<String> out = new ArrayList<>(6);
        final JexlContext ctxt = new MapContext();
        ctxt.set("$out", out);
        final String src =
                  "for(b: ['g','h','i']) { $out.add(b); }\n"
                + "for(var b: ['j','k','l']) { $out.add(b);}\n"
                + "$out.size()";

        final JexlEngine jexl = new JexlBuilder().safe(false).strict(true).create();
        final JexlScript script = jexl.createScript(src);
        final Object result = script.execute(ctxt, (Object) null);
        Assert.assertEquals(6, result);
        final List<String> ctl = Arrays.asList("g", "h", "i", "j", "k", "l");
        Assert.assertEquals(ctl, out);
    }

    @Test
    public void test286() {
        final String s286 = "var x = 0; for(x : 1..2){}; return x";
        final JexlEngine jexl = new JexlBuilder().strict(true).create();
        Assert.assertEquals(2, jexl.createScript(s286).execute(null));
    }

    @Test
    public void test287() {
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        final JexlEngine jexl = new JexlBuilder().strict(true).create();
        String src;
        JexlScript script;
        Object result;
        // declared, not defined
        src = "x = 1; if (false) var x = 2; x";
        script = jexl.createScript(src);
        result = script.execute(ctxt);
        Assert.assertEquals(1, result);
        // declared and defined
        src = "x = 1; if (true) var x = 2; x";
        script = jexl.createScript(src);
        result = script.execute(ctxt);
        Assert.assertEquals(2, result);
        // definition using shadowed global
        src = "x = 1; var x = x + 41; x";
        script = jexl.createScript(src);
        result = script.execute(ctxt);
        Assert.assertEquals(42, result);
        // definition using shadowed global
        options.setLexical(false);
        src = "(x)->{ if (x==1) { var y = 2; } else if (x==2) { var y = 3; }; y }";
        script = jexl.createScript(src);
        result = script.execute(ctxt, 1);
        Assert.assertEquals(2, result);
        result = script.execute(ctxt, 2);
        Assert.assertEquals(3, result);
        options.setStrict(true);
        try {
            result = script.execute(ctxt, 0);
            Assert.fail("should have failed!");
        } catch (final JexlException.Variable xvar) {
            Assert.assertTrue(xvar.getMessage().contains("y"));
        }
        options.setStrict(false);
        try {
            result = script.execute(ctxt, 0);
        } catch (final JexlException xvar) {
            Assert.fail("should not have failed! " + xvar);
        }
        Assert.assertNull(result);
    }

    @Test
    public void test290a() throws Exception {
        Object result;
        JexlScript script;
        final String[] srcs = new String[]{
            "(x)->{ x.nothing().toString() }",
            "(x)->{ x.toString().nothing() }",
            "(x)->{ x.nothing().nothing() }",
        };
        for (final boolean safe : new boolean[]{true, false}) {
            final JexlEngine jexl = new JexlBuilder().safe(safe).strict(true).create();
            for (final String src : srcs) {
                script = jexl.createScript(src);
                try {
                    result = script.execute(null, "abc");
                    if (!safe) {
                        Assert.fail("should have failed: " + src);
                    } else {
                        Assert.assertNull("non-null result ?!", result);
                    }
                } catch (final JexlException.Method xmethod) {
                    if (safe) {
                        Assert.fail(src + ", should not have thrown " + xmethod);
                    } else {
                        Assert.assertTrue(src + ": " + xmethod.toString(), xmethod.toString().contains("nothing"));
                    }
                }
            }
        }
    }

    @Test
    public void test290b() throws Exception {
        Object result;
        JexlScript script;
        final String[] srcs = new String[]{
            "(x)->{ x?.nothing()?.toString() }",
            "(x)->{ x.toString()?.nothing() }",
            "(x)->{ x?.nothing().nothing() }",};
        final JexlEngine jexl = new JexlBuilder().strict(true).create();
        for (final String src : srcs) {
            script = jexl.createScript(src);
            result = script.execute(null, "abc");
            Assert.assertNull(result);
        }
    }

    @Test
    public void test291() throws Exception {
        final String str = "{1:'one'}[1]";
        final JexlContext ctxt = new MapContext();
        final JexlEngine jexl = new JexlBuilder().create();
        JexlExpression e = jexl.createExpression(str);
        Object value = e.evaluate(ctxt);
        Assert.assertEquals("one", value);

        final JexlEngine sandboxedJexlEngine = new JexlBuilder().
                sandbox(new JexlSandbox(true)). // add a whitebox sandbox
                create();
         e = sandboxedJexlEngine.createExpression(str);
        value = e.evaluate(ctxt);
        Assert.assertEquals("one", value);
    }

    @Test
    public void testTemplate6565a() throws Exception {
        final JexlEngine jexl = new JexlBuilder().create();
        final JxltEngine jexlt = jexl.createJxltEngine();
        final String source =
            "$$ var res = '';\n" +
            "$$ var meta = session.data['METADATA'];\n" +
            "$$ if (meta) {\n" +
            "$$   var entry = meta['ID'];\n" +
            "$$   if (entry) {\n" +
            "$$     var value = session.data[entry];\n" +
            "$$     res = value?: '';\n" +
            "$$   }\n" +
            "$$ }\n" +
            "${res}\n";
        final JxltEngine.Template script = jexlt.createTemplate("$$", new StringReader(source));
        Assert.assertNotNull(script);
        final TemplateDebugger dbg = new TemplateDebugger();
        final String refactored = dbg.debug(script) ? dbg.toString() : "";
        Assert.assertNotNull(refactored);
        Assert.assertEquals(source, refactored);
    }

    @Test
    public void testTemplate6565b() throws Exception {
        final JexlEngine jexl = new JexlBuilder().create();
        final JxltEngine jexlt = jexl.createJxltEngine();
        final String source =
            "$$ var res = '';\n" +
            "$$ var meta = session.data['METADATA'];\n" +
            "$$ if (meta) {\n" +
            "$$   var entry = meta['ID'];\n" +
            "$$   if (entry) {\n" +
            "$$     var value = session.data[entry];\n" +
            "$$     res = value?: '';\n" +
            "${res}\n" +
            "$$   }\n" +
            "$$ }\n";
        final JxltEngine.Template script = jexlt.createTemplate("$$", new StringReader(source));
        Assert.assertNotNull(script);
        final TemplateDebugger dbg = new TemplateDebugger();
        final String refactored = dbg.debug(script) ? dbg.toString() : "";
        Assert.assertNotNull(refactored);
        Assert.assertEquals(source, refactored);
    }

    public static class Cls298 {
        int sz = 42;

        public int size() {
            return sz;
        }

        public int size(final int x) {
            return sz + x;
        }

        public boolean isEmpty() {
            return sz <= 0;
        }
    }

    @Test
    public void test298() throws Exception {
        final Cls298 c298 = new Cls298();
        final JexlContext ctxt = new MapContext();
        final JexlEngine jexl = new JexlBuilder().create();

        String str = "c.size()";
        JexlScript e = jexl.createScript(str, "c");
        Object value = e.execute(ctxt, c298);
        Assert.assertEquals(str, 42, value);

        str = "size c";
        e = jexl.createScript(str, "c");
        value = e.execute(ctxt, c298);
        Assert.assertEquals(str, 42, value);

        str = "c.size(127)";
        e = jexl.createScript(str, "c");
        value = e.execute(ctxt, c298);
        Assert.assertEquals(str, 169, value);

    }
}
