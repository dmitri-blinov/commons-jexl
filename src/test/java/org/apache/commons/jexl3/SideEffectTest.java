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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import org.apache.commons.jexl3.junit.Asserter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * Tests for array access operator []
 *
 * @since 2.0
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class SideEffectTest extends JexlTestCase {

    private Asserter asserter;

    public SideEffectTest() {
        super("SideEffectTest");
    }

    @Override
    @Before
    public void setUp() {
        asserter = new Asserter(JEXL);
    }

    @Test
    public void testSideEffectVar() throws Exception {
        final Map<String,Object> context = asserter.getVariables();
        final Integer i41 = Integer.valueOf(4141);
        final Object foo = i41;

        context.put("foo", foo);
        asserter.assertExpression("foo += 2", i41 + 2);
        Assert.assertEquals(context.get("foo"), i41 + 2);

        context.put("foo", foo);
        asserter.assertExpression("foo -= 2", i41 - 2);
        Assert.assertEquals(context.get("foo"), i41 - 2);

        context.put("foo", foo);
        asserter.assertExpression("foo *= 2", i41 * 2);
        Assert.assertEquals(context.get("foo"), i41 * 2);

        context.put("foo", foo);
        asserter.assertExpression("foo /= 2", i41 / 2);
        Assert.assertEquals(context.get("foo"), i41 / 2);

        context.put("foo", foo);
        asserter.assertExpression("foo %= 2", i41 % 2);
        Assert.assertEquals(context.get("foo"), i41 % 2);

        context.put("foo", foo);
        asserter.assertExpression("foo &= 3", (long) (i41 & 3));
        Assert.assertEquals(context.get("foo"), (long)(i41 & 3));

        context.put("foo", foo);
        asserter.assertExpression("foo |= 2", (long)(i41 | 2));
        Assert.assertEquals(context.get("foo"), (long)(i41 | 2));

        context.put("foo", foo);
        asserter.assertExpression("foo ^= 2", (long)(i41 ^ 2));
        Assert.assertEquals(context.get("foo"), (long)(i41 ^ 2));

        context.put("foo", foo);
        asserter.assertExpression("foo \\= 2", (long)(i41 & ~2));
        Assert.assertEquals(context.get("foo"), (long)(i41 & ~2));

        context.put("foo", foo);
        asserter.assertExpression("foo <<= 2", i41 << 2);
        Assert.assertEquals(context.get("foo"), i41 << 2);

        context.put("foo", foo);
        asserter.assertExpression("foo >>= 2", i41 >> 2);
        Assert.assertEquals(context.get("foo"), i41 >> 2);

        context.put("foo", foo);
        asserter.assertExpression("foo >>>= 2", i41 >>> 2);
        Assert.assertEquals(context.get("foo"), i41 >>> 2);
    }

    @Test
    public void testSideEffectVarDots() throws Exception {
        final Map<String,Object> context = asserter.getVariables();
        final Integer i41 = Integer.valueOf(4141);
        final Object foo = i41;

        context.put("foo.bar.quux", foo);
        asserter.assertExpression("foo.bar.quux += 2", i41 + 2);
        Assert.assertEquals(context.get("foo.bar.quux"), i41 + 2);

        context.put("foo.bar.quux", foo);
        asserter.assertExpression("foo.bar.quux -= 2", i41 - 2);
        Assert.assertEquals(context.get("foo.bar.quux"), i41 - 2);

        context.put("foo.bar.quux", foo);
        asserter.assertExpression("foo.bar.quux *= 2", i41 * 2);
        Assert.assertEquals(context.get("foo.bar.quux"), i41 * 2);

        context.put("foo.bar.quux", foo);
        asserter.assertExpression("foo.bar.quux /= 2", i41 / 2);
        Assert.assertEquals(context.get("foo.bar.quux"), i41 / 2);

        context.put("foo.bar.quux", foo);
        asserter.assertExpression("foo.bar.quux %= 2", i41 % 2);
        Assert.assertEquals(context.get("foo.bar.quux"), i41 % 2);

        context.put("foo.bar.quux", foo);
        asserter.assertExpression("foo.bar.quux &= 3", (long) (i41 & 3));
        Assert.assertEquals(context.get("foo.bar.quux"), (long)(i41 & 3));

        context.put("foo.bar.quux", foo);
        asserter.assertExpression("foo.bar.quux |= 2", (long)(i41 | 2));
        Assert.assertEquals(context.get("foo.bar.quux"), (long)(i41 | 2));

        context.put("foo.bar.quux", foo);
        asserter.assertExpression("foo.bar.quux ^= 2", (long)(i41 ^ 2));
        Assert.assertEquals(context.get("foo.bar.quux"), (long)(i41 ^ 2));
    }

    @Test
    public void testSideEffectArray() throws Exception {
        final Integer i41 = Integer.valueOf(4141);
        final Integer i42 = Integer.valueOf(42);
        final Integer i43 = Integer.valueOf(43);
        final String s42 = "fourty-two";
        final String s43 = "fourty-three";
        final Object[] foo = new Object[3];
        foo[1] = i42;
        foo[2] = i43;
        asserter.setVariable("foo", foo);
        foo[0] = i41;
        asserter.assertExpression("foo[0] += 2", i41 + 2);
        Assert.assertEquals(foo[0], i41 + 2);
        foo[0] = i41;
        asserter.assertExpression("foo[0] -= 2", i41 - 2);
        Assert.assertEquals(foo[0], i41 - 2);
        foo[0] = i41;
        asserter.assertExpression("foo[0] *= 2", i41 * 2);
        Assert.assertEquals(foo[0], i41 * 2);
        foo[0] = i41;
        asserter.assertExpression("foo[0] /= 2", i41 / 2);
        Assert.assertEquals(foo[0], i41 / 2);
        foo[0] = i41;
        asserter.assertExpression("foo[0] %= 2", i41 % 2);
        Assert.assertEquals(foo[0], i41 % 2);
        foo[0] = i41;
        asserter.assertExpression("foo[0] &= 3", (long) (i41 & 3));
        Assert.assertEquals(foo[0], (long)(i41 & 3));
        foo[0] = i41;
        asserter.assertExpression("foo[0] |= 2", (long)(i41 | 2));
        Assert.assertEquals(foo[0], (long)(i41 | 2));
        foo[0] = i41;
        asserter.assertExpression("foo[0] ^= 2", (long)(i41 ^ 2));
        Assert.assertEquals(foo[0], (long)(i41 ^ 2));
    }

    @Test
    public void testSideEffectDotArray() throws Exception {
        final Integer i41 = Integer.valueOf(4141);
        final Integer i42 = Integer.valueOf(42);
        final Integer i43 = Integer.valueOf(43);
        final String s42 = "fourty-two";
        final String s43 = "fourty-three";
        final Object[] foo = new Object[3];
        foo[1] = i42;
        foo[2] = i43;
        asserter.setVariable("foo", foo);
        foo[0] = i41;
        asserter.assertExpression("foo.0 += 2", i41 + 2);
        Assert.assertEquals(foo[0], i41 + 2);
        foo[0] = i41;
        asserter.assertExpression("foo.0 -= 2", i41 - 2);
        Assert.assertEquals(foo[0], i41 - 2);
        foo[0] = i41;
        asserter.assertExpression("foo.0 *= 2", i41 * 2);
        Assert.assertEquals(foo[0], i41 * 2);
        foo[0] = i41;
        asserter.assertExpression("foo.0 /= 2", i41 / 2);
        Assert.assertEquals(foo[0], i41 / 2);
        foo[0] = i41;
        asserter.assertExpression("foo.0 %= 2", i41 % 2);
        Assert.assertEquals(foo[0], i41 % 2);
        foo[0] = i41;
        asserter.assertExpression("foo.0 &= 3", (long) (i41 & 3));
        Assert.assertEquals(foo[0], (long)(i41 & 3));
        foo[0] = i41;
        asserter.assertExpression("foo.0 |= 2", (long)(i41 | 2));
        Assert.assertEquals(foo[0], (long)(i41 | 2));
        foo[0] = i41;
        asserter.assertExpression("foo.0 ^= 2", (long)(i41 ^ 2));
        Assert.assertEquals(foo[0], (long)(i41 ^ 2));
    }

    @Test
    public void testSideEffectAntishArray() throws Exception {
        final Integer i41 = Integer.valueOf(4141);
        final Integer i42 = Integer.valueOf(42);
        final Integer i43 = Integer.valueOf(43);
        final Object[] foo = new Object[3];
        foo[1] = i42;
        foo[2] = i43;
        asserter.setVariable("foo.bar", foo);
        foo[0] = i41;
        asserter.assertExpression("foo.bar[0] += 2", i41 + 2);
        Assert.assertEquals(foo[0], i41 + 2);
        foo[0] = i41;
        asserter.assertExpression("foo.bar[0] -= 2", i41 - 2);
        Assert.assertEquals(foo[0], i41 - 2);
        foo[0] = i41;
        asserter.assertExpression("foo.bar[0] *= 2", i41 * 2);
        Assert.assertEquals(foo[0], i41 * 2);
        foo[0] = i41;
        asserter.assertExpression("foo.bar[0] /= 2", i41 / 2);
        Assert.assertEquals(foo[0], i41 / 2);
        foo[0] = i41;
        asserter.assertExpression("foo.bar[0] %= 2", i41 % 2);
        Assert.assertEquals(foo[0], i41 % 2);
        foo[0] = i41;
        asserter.assertExpression("foo.bar[0] &= 3", (long) (i41 & 3));
        Assert.assertEquals(foo[0], (long)(i41 & 3));
        foo[0] = i41;
        asserter.assertExpression("foo.bar[0] |= 2", (long)(i41 | 2));
        Assert.assertEquals(foo[0], (long)(i41 | 2));
        foo[0] = i41;
        asserter.assertExpression("foo.bar[0] ^= 2", (long)(i41 ^ 2));
        Assert.assertEquals(foo[0], (long)(i41 ^ 2));
    }

    public static class Foo {
        int value;
        Foo(final int v) {
            value = v;
        }

        @Override
        public String toString() {
            return Integer.toString(value);
        }

        public void setValue(final long v) {
            value = (int) v;
        }
        public int getValue() {
            return value;
        }

        public void setBar(final int x, final long v) {
            value = (int) v + x;
        }

        public int getBar(final int x) {
            return value + x;
        }
    }

    @Test
    public void testSideEffectBean() throws Exception {
        final Integer i41 = Integer.valueOf(4141);
        final Foo foo = new Foo(0);
        asserter.setVariable("foo", foo);
        foo.value = i41;
        asserter.assertExpression("foo.value += 2", i41 + 2);
        Assert.assertEquals(foo.value, i41 + 2);
        foo.value = i41;
        asserter.assertExpression("foo.value -= 2", i41 - 2);
        Assert.assertEquals(foo.value, i41 - 2);
        foo.value = i41;
        asserter.assertExpression("foo.value *= 2", i41 * 2);
        Assert.assertEquals(foo.value, i41 * 2);
        foo.value = i41;
        asserter.assertExpression("foo.value /= 2", i41 / 2);
        Assert.assertEquals(foo.value, i41 / 2);
        foo.value = i41;
        asserter.assertExpression("foo.value %= 2", i41 % 2);
        Assert.assertEquals(foo.value, i41 % 2);
        foo.value = i41;
        asserter.assertExpression("foo.value &= 3", (long) (i41 & 3));
        Assert.assertEquals(foo.value, i41 & 3);
        foo.value = i41;
        asserter.assertExpression("foo.value |= 2", (long)(i41 | 2));
        Assert.assertEquals(foo.value, i41 | 2);
        foo.value = i41;
        asserter.assertExpression("foo.value ^= 2", (long)(i41 ^ 2));
        Assert.assertEquals(foo.value, i41 ^ 2);
    }

    @Test
    public void testSideEffectBeanContainer() throws Exception {
        final Integer i41 = Integer.valueOf(4141);
        final Foo foo = new Foo(0);
        asserter.setVariable("foo", foo);
        foo.value = i41;
        asserter.assertExpression("foo.bar[0] += 2", i41 + 2);
        Assert.assertEquals(foo.value, i41 + 2);
        foo.value = i41;
        asserter.assertExpression("foo.bar[1] += 2", i41 + 3);
        Assert.assertEquals(foo.value, i41 + 4);
        foo.value = i41;
        asserter.assertExpression("foo.bar[0] -= 2", i41 - 2);
        Assert.assertEquals(foo.value, i41 - 2);
        foo.value = i41;
        asserter.assertExpression("foo.bar[0] *= 2", i41 * 2);
        Assert.assertEquals(foo.value, i41 * 2);
        foo.value = i41;
        asserter.assertExpression("foo.bar[0] /= 2", i41 / 2);
        Assert.assertEquals(foo.value, i41 / 2);
        foo.value = i41;
        asserter.assertExpression("foo.bar[0] %= 2", i41 % 2);
        Assert.assertEquals(foo.value, i41 % 2);
        foo.value = i41;
        asserter.assertExpression("foo.bar[0] &= 3", (long) (i41 & 3));
        Assert.assertEquals(foo.value, i41 & 3);
        foo.value = i41;
        asserter.assertExpression("foo.bar[0] |= 2", (long)(i41 | 2));
        Assert.assertEquals(foo.value, i41 | 2);
        foo.value = i41;
        asserter.assertExpression("foo.bar[0] ^= 2", (long)(i41 ^ 2));
        Assert.assertEquals(foo.value, i41 ^ 2);
    }

    @Test
    public void testArithmeticSelf() throws Exception {
        final JexlEngine jexl = new JexlBuilder().cache(64).arithmetic(new SelfArithmetic(false)).create();
        final JexlContext jc = null;
        runSelfOverload(jexl, jc);
        runSelfOverload(jexl, jc);
    }

    @Test
    public void testArithmeticSelfNoCache() throws Exception {
        final JexlEngine jexl = new JexlBuilder().cache(0).arithmetic(new SelfArithmetic(false)).create();
        final JexlContext jc = null;
        runSelfOverload(jexl, jc);
    }

    protected void runSelfOverload(final JexlEngine jexl, final JexlContext jc) {
        JexlScript script;
        Object result;
        script = jexl.createScript("(x, y)->{ x += y }");
        result = script.execute(jc, 3115, 15);
        Assert.assertEquals(3115 + 15,  result);
        final Var v0 = new Var(3115);
        result = script.execute(jc, v0, new Var(15));
        Assert.assertEquals(result, v0);
        Assert.assertEquals(3115 + 15,  v0.value);

        script = jexl.createScript("(x, y)->{ x -= y}");
        result = script.execute(jc, 3115, 15);
        Assert.assertEquals(3115 - 15,  result);
        final Var v1 = new Var(3115);
        result = script.execute(jc, v1, new Var(15));
        Assert.assertNotEquals(result, v1); // not a real side effect
        Assert.assertEquals(3115 - 15,  ((Var) result).value);

        script = jexl.createScript("(x, y)->{ x *= y }");
        result = script.execute(jc, 3115, 15);
        Assert.assertEquals(3115 * 15,  result);
        final Var v2 = new Var(3115);
        result = script.execute(jc, v2, new Var(15));
        Assert.assertEquals(result, v2);
        Assert.assertEquals(3115 * 15,  v2.value);

        script = jexl.createScript("(x, y)->{ x /= y }");
        result = script.execute(jc, 3115, 15);
        Assert.assertEquals(3115 / 15,  result);
        final Var v3 = new Var(3115);
        result = script.execute(jc, v3, new Var(15));
        Assert.assertEquals(result, v3);
        Assert.assertEquals(3115 / 15,  v3.value);

        script = jexl.createScript("(x, y)->{ x %= y }");
        result = script.execute(jc, 3115, 15);
        Assert.assertEquals(3115 % 15,  result);
        final Var v4 = new Var(3115);
        result = script.execute(jc, v4, new Var(15));
        Assert.assertEquals(result, v4);
        Assert.assertEquals(3115 % 15,  v4.value);

        script = jexl.createScript("(x, y)->{ x &= y }");
        result = script.execute(jc, 3115, 15);
        Assert.assertEquals(3115L & 15,  result);
        final Var v5 = new Var(3115);
        result = script.execute(jc, v5, new Var(15));
        Assert.assertEquals(result, v5);
        Assert.assertEquals(3115 & 15,  v5.value);

        script = jexl.createScript("(x, y)->{ x |= y }");
        result = script.execute(jc, 3115, 15);
        Assert.assertEquals(3115L | 15,  result);
        final Var v6 = new Var(3115);
        result = script.execute(jc, v6, new Var(15));
        Assert.assertEquals(result, v6);
        Assert.assertEquals(3115L | 15,  v6.value);

        script = jexl.createScript("(x, y)->{ x ^= y }");
        result = script.execute(jc, 3115, 15);
        Assert.assertEquals(3115L ^ 15,  result);
        final Var v7 = new Var(3115);
        result = script.execute(jc, v7, new Var(15));
        Assert.assertEquals(result, v7);
        Assert.assertEquals(3115L ^ 15,  v7.value);
    }


    @Test
    public void testOverrideGetSet() throws Exception {
        final JexlEngine jexl = new JexlBuilder().cache(64).arithmetic(new SelfArithmetic(false)).create();
        final JexlContext jc = null;

        JexlScript script;
        Object result;
        final Var v0 = new Var(3115);
        script = jexl.createScript("(x)->{ x.value}");
        result = script.execute(jc, v0);
        Assert.assertEquals(3115, result);
        script = jexl.createScript("(x)->{ x['VALUE']}");
        result = script.execute(jc, v0);
        Assert.assertEquals(3115, result);
        script = jexl.createScript("(x,y)->{ x.value = y}");
        result = script.execute(jc, v0, 42);
        Assert.assertEquals(42, result);
        script = jexl.createScript("(x,y)->{ x['VALUE'] = y}");
        result = script.execute(jc, v0, 169);
        Assert.assertEquals(169, result);
    }

    public static class Var {
        int value;

        Var(final int v) {
            value = v;
        }

        @Override
        public String toString() {
            return Integer.toString(value);
        }
    }

    // an arithmetic that performs side effects
    public static class SelfArithmetic extends JexlArithmetic {
        public SelfArithmetic(final boolean strict) {
            super(strict);
        }

        public Object propertyGet(final Var var, final String property) {
            return "value".equals(property)? var.value : JexlEngine.TRY_FAILED;
        }

        public Object propertySet(final Var var, final String property, final int v) {
            return "value".equals(property)? var.value = v : JexlEngine.TRY_FAILED;
        }

        public Object arrayGet(final Var var, final String property) {
            return "VALUE".equals(property)? var.value : JexlEngine.TRY_FAILED;
        }

        public Object arraySet(final Var var, final String property, final int v) {
            return "VALUE".equals(property)? var.value = v : JexlEngine.TRY_FAILED;
        }

        public JexlOperator selfAdd(final Var lhs, final Var rhs) {
            lhs.value += rhs.value;
            return JexlOperator.ASSIGN;
        }

        // for kicks, this one does not side effect but overloads nevertheless
        public Var selfSubtract(final Var lhs, final Var rhs) {
            return new Var(lhs.value - rhs.value);
        }

        public JexlOperator selfDivide(final Var lhs, final Var rhs) {
            lhs.value /= rhs.value;
            return JexlOperator.ASSIGN;
        }

        public JexlOperator selfMultiply(final Var lhs, final Var rhs) {
            lhs.value *= rhs.value;
            return JexlOperator.ASSIGN;
        }

        public JexlOperator selfMod(final Var lhs, final Var rhs) {
            lhs.value %= rhs.value;
            return JexlOperator.ASSIGN;
        }

        public Var and(final Var lhs, final Var rhs) {
            return new Var(lhs.value & rhs.value);
        }

        public JexlOperator selfAnd(final Var lhs, final Var rhs) {
            lhs.value &= rhs.value;
            return JexlOperator.ASSIGN;
        }

        public Var or(final Var lhs, final Var rhs) {
            return new Var(lhs.value | rhs.value);
        }

        public JexlOperator selfOr(final Var lhs, final Var rhs) {
            lhs.value |= rhs.value;
            return JexlOperator.ASSIGN;
        }

        public Var xor(final Var lhs, final Var rhs) {
            return new Var(lhs.value ^ rhs.value);
        }

        public JexlOperator selfXor(final Var lhs, final Var rhs) {
            lhs.value ^= rhs.value;
            return JexlOperator.ASSIGN;
        }
    }

    /**
     * An arithmetic that implements 2 selfAdd methods.
     */
    public static class Arithmetic246 extends JexlArithmetic {
        public Arithmetic246(final boolean astrict) {
            super(astrict);
        }

        public JexlOperator selfAdd(final Collection<String> c, final String item) throws IOException {
            c.add(item);
            return JexlOperator.ASSIGN;
        }

        public JexlOperator selfAdd(final Appendable c, final String item) throws IOException {
            c.append(item);
            return JexlOperator.ASSIGN;
        }

        @Override
        public Object add(final Object right, final Object left) {
            return super.add(left, right);
        }
    }

   public static class Arithmetic246b extends Arithmetic246 {
        public Arithmetic246b(final boolean astrict) {
            super(astrict);
        }

        public Object selfAdd(final Object c, final String item) throws IOException {
            if (c == null) {
                return new ArrayList<>(Collections.singletonList(item));
            }
            if (c instanceof Appendable) {
                ((Appendable) c).append(item);
                return JexlOperator.ASSIGN;
            }
            return JexlEngine.TRY_FAILED;
        }
    }

    @Test
    public void test246() throws Exception {
        run246(new Arithmetic246(true));
    }

    @Test
    public void test246b() throws Exception {
        run246(new Arithmetic246b(true));
    }

    private void run246(final JexlArithmetic j246) throws Exception {
        final Log log246 = LogFactory.getLog(SideEffectTest.class);
        // quiesce the logger
        final java.util.logging.Logger ll246 = java.util.logging.LogManager.getLogManager().getLogger(SideEffectTest.class.getName());
       // ll246.setLevel(Level.WARNING);
        final JexlEngine jexl = new JexlBuilder().arithmetic(j246).cache(32).debug(true).logger(log246).create();
        final JexlScript script = jexl.createScript("z += x", "x");
        final MapContext ctx = new MapContext();
        List<String> z = new ArrayList<>(1);

        // no ambiguous, std case
        ctx.set("z", z);
        Object zz = script.execute(ctx, "42");
        Assert.assertSame(zz, z);
        Assert.assertEquals(1, z.size());
        z.clear();

        boolean t246 = false;
        // call with null
        try {
            script.execute(ctx, "42");
            zz = ctx.get("z");
            Assert.assertTrue(zz instanceof List<?>);
            z = (List<String>) zz;
            Assert.assertEquals(1, z.size());
        } catch(JexlException | ArithmeticException xjexl) {
            t246 = true;
            Assert.assertEquals(j246.getClass(), Arithmetic246.class);
        }
        ctx.clear();

        // a non ambiguous call still succeeds
        ctx.set("z", z);
        zz = script.execute(ctx, "-42");
        Assert.assertSame(zz, z);
        Assert.assertEquals(t246? 1 : 2, z.size());
    }

    // an arithmetic that performs side effects
    public static class Arithmetic248 extends JexlArithmetic {
        public Arithmetic248(final boolean strict) {
            super(strict);
        }

        public Object arrayGet(final List<?> list, final Collection<Integer> range) {
            final List<Object> rl = new ArrayList<>(range.size());
            for(final int i : range) {
                rl.add(list.get(i));
            }
            return rl;
        }

        public Object arraySet(final List<Object> list, final Collection<Integer> range, final Object value) {
            for(final int i : range) {
                list.set(i, value);
            }
            return list;
        }
    }

    @Test
    public void test248() throws Exception {
        final MapContext ctx = new MapContext();
        final List<Object> foo = new ArrayList<>(Arrays.asList(10, 20, 30, 40));
        ctx.set("foo", foo);

        final JexlEngine engine = new JexlBuilder().arithmetic(new Arithmetic248(true)).create();
        final JexlScript foo12 = engine.createScript("foo[1..2]");
        try {
            final Object r = foo12.execute(ctx);
            Assert.assertEquals(Arrays.asList(20, 30), r);
        } catch (final JexlException xp) {
            Assert.assertTrue(xp instanceof JexlException.Property);
        }

        final JexlScript foo12assign = engine.createScript("foo[1..2] = x", "x");
        try {
            final Object r = foo12assign.execute(ctx, 25);
            Assert.assertEquals(25, r);
            Assert.assertEquals(Arrays.asList(10, 25, 25, 40), foo);
        } catch (final JexlException xp) {
            Assert.assertTrue(xp instanceof JexlException.Property);
        }
    }

}
