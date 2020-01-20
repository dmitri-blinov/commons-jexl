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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Iterator;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for generator operator.
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class GeneratorTest extends JexlTestCase {

    public GeneratorTest() {
        super("GeneratorTest");
    }

    @Test
    public void testSyntax() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = ...{yield 42;}").execute(jc);
        Assert.assertTrue(o instanceof Iterator);
    }

    @Test
    public void testSingleValue() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = [...{yield 42;}]").execute(jc);
        int[] check = {42};
        Assert.assertTrue(Arrays.equals(check, (int[]) o));
    }

    @Test
    public void testNoValue() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = [...{42;}]").execute(jc);
        Object[] check = {};
        Assert.assertTrue(Arrays.equals(check, (Object[]) o));
    }

    @Test
    public void testSimple() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = [...{yield 1; yield 2; yield 3;}]").execute(jc);
        int[] check = {1, 2, 3};
        Assert.assertTrue(Arrays.equals(check, (int[]) o));
    }

    @Test
    public void testNestedBlock() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = [...{yield 1; yield 2; {yield 3; yield 4;}; yield 5;}]").execute(jc);
        int[] check = {1, 2, 3, 4, 5};
        Assert.assertTrue(Arrays.equals(check, (int[]) o));

    }

    @Test
    public void testBlockBreak() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = [...{yield 1; yield 2; x: {yield 3; break x; yield 4;}; yield 5;}]").execute(jc);
        int[] check = {1, 2, 3, 5};
        Assert.assertTrue(Arrays.equals(check, (int[]) o));

    }

    @Test
    public void testVariable() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = [...{var i = 1; yield i; var j = i + 1; yield j; yield i+j;}]").execute(jc);
        int[] check = {1, 2, 3};
        Assert.assertTrue(Arrays.equals(check, (int[]) o));
    }

    @Test
    public void testIfBlock() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = [...{if (true) {yield 1; yield 2; yield 3;}; if (false) {} else {yield 4; yield 5; yield 6;}}]").execute(jc);
        int[] check = {1, 2, 3, 4, 5, 6};
        Assert.assertTrue(Arrays.equals(check, (int[]) o));
    }

    @Test
    public void testIfYield() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = [...{if (true) yield 1; if (false) {} else yield 2; }]").execute(jc);
        int[] check = {1, 2};
        Assert.assertTrue(Arrays.equals(check, (int[]) o));
    }

    @Test
    public void testWhile() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = [...{var i = 0; while (i < 4) {i = i + 1; yield i;}}]").execute(jc);
        int[] check = {1, 2, 3, 4};
        Assert.assertTrue(Arrays.equals(check, (int[]) o));
    }

    @Test
    public void testWhileContinue() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = [...{var i = 0; while (i < 4) {i = i + 1; yield i; continue; }}]").execute(jc);
        int[] check = {1, 2, 3, 4};
        Assert.assertTrue(Arrays.equals(check, (int[]) o));
    }

    @Test
    public void testDoWhile() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = [...{var i = 0; do {i = i + 1; yield i; } while (i < 4) }]").execute(jc);
        int[] check = {1, 2, 3, 4};
        Assert.assertTrue(Arrays.equals(check, (int[]) o));
    }

    @Test
    public void testForBlock() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = [...{for(var i = 0; i < 4; i++) { yield i+1; }}]").execute(jc);
        int[] check = {1, 2, 3, 4};
        Assert.assertTrue(Arrays.equals(check, (int[]) o));
    }

    @Test
    public void testForYield() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = [...{for(var i = 0; i < 4; i++) yield i+1; }]").execute(jc);
        int[] check = {1, 2, 3, 4};
        Assert.assertTrue(Arrays.equals(check, (int[]) o));
    }

    @Test
    public void testForeachBlock() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = [...{for(var i : 0 .. 3) { yield i+1; }}]").execute(jc);
        int[] check = {1, 2, 3, 4};
        Assert.assertTrue(Arrays.equals(check, (int[]) o));
    }

    @Test
    public void testForeachYield() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = [...{for(var i : 0 .. 3) yield i+1; }]").execute(jc);
        int[] check = {1, 2, 3, 4};
        Assert.assertTrue(Arrays.equals(check, (int[]) o));
    }

    @Test
    public void testForeachIndexed() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = [...{for(var i, a : [40,41,42,43]) { yield i+1; }}]").execute(jc);
        int[] check = {1, 2, 3, 4};
        Assert.assertTrue(Arrays.equals(check, (int[]) o));
    }

    @Test
    public void testSwitchBlock() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = [...{switch(0) {case 0: {yield 1; yield 2;}; case 1 : {yield 3; yield 4;}}}]").execute(jc);
        int[] check = {1, 2, 3, 4};
        Assert.assertTrue(Arrays.equals(check, (int[]) o));
    }

    @Test
    public void testSwitchYield() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = [...{switch(0) {case 0: yield 1; yield 2; case 1 : yield 3; yield 4;}}]").execute(jc);
        int[] check = {1, 2, 3, 4};
        Assert.assertTrue(Arrays.equals(check, (int[]) o));
    }

    @Test
    public void testYieldExpr() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = [...{yield switch(0) {case 0 -> {1;} case 1 -> {3;}}}]").execute(jc);
        int[] check = {1};
        Assert.assertTrue(Arrays.equals(check, (int[]) o));
    }

    @Test
    public void testYieldSwitch() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = [...{yield switch(0) {case 0 -> {yield 1;} case 1 -> {yield 3;}}}]").execute(jc);
        int[] check = {1};
        Assert.assertTrue(Arrays.equals(check, (int[]) o));
    }

    @Test
    public void testYieldBlock() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = [...{yield ({yield 3;})}]").execute(jc);
        int[] check = {3};
        Assert.assertTrue(Arrays.equals(check, (int[]) o));
    }

    public static class TestContext extends MapContext implements JexlContext.NamespaceResolver {
        @Override
        public Object resolveNamespace(String name) {
            return name == null ? this : null;
        }

        public int interrupt() throws InterruptedException {
            Thread.currentThread().interrupt();
            return 42;
        }
    }

    @Test
    public void testInterrupt() throws Exception {
        JexlContext jc = new TestContext();
        try {
            Object o = JEXL.createScript("var x = [...{interrupt();}]").execute(jc);
            Assert.fail("Should have been cancelled");
        } catch (JexlException.Cancel c) {
            // OK
        }
    }

    @Test
    public void testError() throws Exception {
        JexlContext jc = new MapContext();
        try {
            Object o = JEXL.createScript("var x = [...{a1;}]").execute(jc);
            Assert.fail("Should have failed");
        } catch (JexlException c) {
            // OK
        }
    }

    @Test
    public void testPowerOf2() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("function Power(int number, int exponent) {return ...{int result = 1; for (int i = 0; i < exponent; i++) {result = result * number; yield result;}}}; var x = [...Power(2,8)]").execute(jc);
        int[] check = {2,4,8,16,32,64,128,256};
        Assert.assertTrue(Arrays.equals(check, (int[]) o));
    }

}
