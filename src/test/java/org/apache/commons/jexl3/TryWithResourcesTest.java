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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.StringReader;
import org.junit.Assert;
import org.junit.Test;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Tests try-with-resources statement.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class TryWithResourcesTest extends JexlTestCase {

    public TryWithResourcesTest() {
        super("TryWithResourcesTest");
    }

    @Test
    public void testLastValue() throws Exception {
        JexlScript e = JEXL.createScript("try (r) {}");
        JexlContext jc = new MapContext();
        Object r = new StringReader("foo");
        jc.set("r", r);
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not last evaluated expression", r, o);
    }

    @Test
    public void testClosed() throws Exception {
        JexlEngine jexl = new JexlBuilder().strict(true).silent(false).create();
        JexlScript e = jexl.createScript("try (r) {}; r.read(); return 42");
        JexlContext jc = new MapContext();
        jc.set("r", new StringReader("foo"));
        Object o = null;
        try {
            o = e.execute(jc);
            Assert.fail("should have thrown");
        } catch (JexlException xjexl) {
            Assert.assertNull(o);
        }
    }

    public static class BadStream implements AutoCloseable {

        public BadStream() {
        }

        public BadStream(boolean fail) throws Exception {
            if (fail) 
                throw new Exception("Should not be created");
        }

        @Override
        public void close() throws Exception {
            throw new Exception("Should be ignored");
        }
    }

    @Test
    public void testFinallyAlwaysCalled() throws Exception {

        JexlContext jc = new MapContext();
        jc.set("r", new StringReader("foo"));

        JexlScript e = JEXL.createScript("try (r) {x = 1} finally {x = 42}");
        Object o = e.execute(jc);
        Assert.assertEquals(42, jc.get("x"));

        jc = new MapContext();
        jc.set("r", new StringReader("foo"));

        e = JEXL.createScript("try (r) {42/0} finally {x = 42}");
        try {
            o = e.execute(jc);
        } catch (Exception ex) {
            Assert.assertEquals(42, jc.get("x"));
        }

        jc = new MapContext();
        jc.set("r", new StringReader("foo"));

        e = JEXL.createScript("try (r) {42/0} catch (var e) {} finally {x = 42}");
        o = e.execute(jc);
        Assert.assertEquals(42, jc.get("x"));

        jc = new MapContext();
        jc.set("r", new BadStream());

        e = JEXL.createScript("try (r) {} finally {x = 42}");
        try {
            o = e.execute(jc);
            Assert.fail("should have thrown");
        } catch (Exception ex) {
            Assert.assertNull(o);
            Assert.assertEquals(42, jc.get("x"));
        }

        e = JEXL.createScript("try (var r = 42/0) {} finally {x = 42}");
        try {
            o = e.execute(jc);
            Assert.fail("should have thrown");
        } catch (Exception ex) {
            Assert.assertNull(o);
            Assert.assertEquals(42, jc.get("x"));
        }
    }

    @Test
    public void testCatch() throws Exception {
        JexlContext jc = new MapContext();
        jc.set("r", new StringReader("foo"));

        JexlScript e = JEXL.createScript("try (r) {42/0} catch (e) {}");
        jc = new MapContext();
        jc.set("r", new StringReader("foo"));
        Object o = e.execute(jc);
        Assert.assertTrue(jc.get("e") instanceof Exception);

        e = JEXL.createScript("try (r) {42/0} catch (var e) {x = e}");
        jc = new MapContext();
        jc.set("r", new StringReader("foo"));
        o = e.execute(jc);
        Assert.assertTrue(jc.get("x") instanceof Exception);

        jc = new MapContext();
        jc.set("r", new StringReader("foo"));
        e = JEXL.createScript("try (r) {return 42} catch (var e) {return 0}");
        o = e.execute(jc);
        Assert.assertEquals(42, o);

        e = JEXL.createScript("try (r) {return 42} catch(e) {}");
        jc = new MapContext();
        jc.set("r", new BadStream());
        o = e.execute(jc);
        Assert.assertNull(jc.get("e"));

        e = JEXL.createScript("try (r = 42/0) {} catch (var e) {x = e}");
        jc = new MapContext();
        o = e.execute(jc);
        Assert.assertTrue(jc.get("x") instanceof Exception);
    }

    @Test
    public void testBlindCatch() throws Exception {
        JexlScript e = JEXL.createScript("try (r = 42/0) {} catch {return 42}");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals(42, o);
    }

    @Test
    public void testReturn() throws Exception {
        JexlContext jc = new MapContext();
        jc.set("r", new StringReader("foo"));
        JexlScript e = JEXL.createScript("try (r) {return 1} finally {return 42}");
        Object o = e.execute(jc);
        Assert.assertEquals(42, o);

        jc = new MapContext();
        jc.set("r", new StringReader("foo"));
        e = JEXL.createScript("try (r) {42/0} catch(var e) {return 42}");
        o = e.execute(jc);
        Assert.assertEquals(42, o);

        jc = new MapContext();
        jc.set("r", new StringReader("foo"));
        e = JEXL.createScript("try (r) {42/0} catch(var e) {return 2} finally { return 42}");
        o = e.execute(jc);
        Assert.assertEquals(42, o);
    }

    @Test
    public void testBreakInsideTry() throws Exception {
        JexlEngine jexl = new JexlBuilder().safe(false).strict(true).lexical(false).create();
        JexlContext jc = new MapContext();
        jc.set("r", new StringReader("foo"));
        JexlScript e = jexl.createScript("for (var i : 42..43) try (r) {break} finally {}; i");
        Object o = e.execute(jc);
        Assert.assertEquals(42, o);

        jc = new MapContext();
        jc.set("r", new StringReader("foo"));
        e = jexl.createScript("for (var i : 42..43) try (r) {} finally {break}; i");
        o = e.execute(jc);
        Assert.assertEquals(42, o);

        jc = new MapContext();
        jc.set("r", new StringReader("foo"));
        e = jexl.createScript("for (var i : 42..43) try (r) {break} catch(var e) {}; i");
        o = e.execute(jc);
        Assert.assertEquals(42, o);

        jc = new MapContext();
        jc.set("r", new StringReader("foo"));
        e = jexl.createScript("for (var i : 42..43) try (r) {42/0} catch(var e) {break}; i");
        o = e.execute(jc);
        Assert.assertEquals(42, o);
    }

    @Test
    public void testContinueInsideTry() throws Exception {
        JexlContext jc = new MapContext();
        jc.set("r", new StringReader("foo"));
        JexlScript e = JEXL.createScript("var i = 0; while (true) { i+=1; try (r) {if (i < 42) continue else break} finally {}}; i");
        Object o = e.execute(jc);
        Assert.assertEquals(42, o);
    }

    @Test
    public void testLexical1() throws Exception {
        JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).create();
        JexlContext ctxt = new MapContext();
        JexlScript script = jexl.createScript("e = 1; try(var e = 42) {return e}");
        try {
            Object result = script.execute(ctxt);
            Assert.assertEquals(42, result);
        } catch (JexlException xany) {
            String ww = xany.toString();
            Assert.fail(ww);
        }
    }

    @Test
    public void testLexical2() throws Exception {
        JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).create();
        JexlContext ctxt = new MapContext();
        JexlScript script = jexl.createScript("e = 1; try(var e = 42) {}; return e");
        try {
            Object result = script.execute(ctxt);
            Assert.assertEquals(1, result);
        } catch (JexlException xany) {
            String ww = xany.toString();
            Assert.fail(ww);
        }
    }

    @Test
    public void testLexical3() throws Exception {
        JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).create();
        JexlContext ctxt = new MapContext();
        JexlScript script = jexl.createScript("e = 1; try(var e = 42) {} finally {return e}");
        try {
            Object result = script.execute(ctxt);
            Assert.assertEquals(1, result);
        } catch (JexlException xany) {
            String ww = xany.toString();
            Assert.fail(ww);
        }
    }

    @Test
    public void testLexical4() throws Exception {
        JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).create();
        JexlContext ctxt = new MapContext();
        JexlScript script = jexl.createScript("e = 42; try (var e = 1) {42/0} catch (var x) {return e}");
        try {
            Object result = script.execute(ctxt);
            Assert.assertEquals(42, result);
        } catch (JexlException xany) {
            String ww = xany.toString();
            Assert.fail(ww);
        }
    }

    @Test
    public void testTyped() throws Exception {
        JexlContext jc = new MapContext();
        jc.set("r", new StringReader("foo"));

        JexlScript e = JEXL.createScript("try (StringWriter r = new StringWriter()) { r.write('World'); return r.toString()}");
        Object o = e.execute(jc);
        Assert.assertEquals("World", o);
    }

    public class TestResource implements AutoCloseable {
        protected int x;
        protected int y;

        public int getX() {
           return x;
        }

        public void setX(int val) {
           x = val;
        }

        public int getY() {
           return y;
        }

        public void setY(int val) {
           y = val;
        }

        public void close() {
        }
    }

    @Test
    public void testInlinePropertyAssignment() throws Exception {
        JexlContext jc = new MapContext();
        TestResource r = new TestResource();
        jc.set("r", r);

        JexlScript e = JEXL.createScript("try (r) {x : 10, y : 20}");
        Object o = e.execute(jc);
        Assert.assertEquals(r.getX(), 10);
    }

}
