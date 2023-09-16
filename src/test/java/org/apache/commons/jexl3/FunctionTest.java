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

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests named function features.
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class FunctionTest extends JexlTestCase {

    public FunctionTest() {
        super("FunctionTest");
    }

    @Test
    public void testSyntax() throws Exception {
        JexlEngine jexl = createEngine();
        JexlScript s = jexl.createScript("function x() {return 42;}; x()");
        Object result = s.execute(null, s);
        Assert.assertEquals(42, result);
        s = jexl.createScript("function x(a,b) {return a+b;}; x(11,31)");
        result = s.execute(null, s);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testExprSyntax() throws Exception {
        JexlEngine jexl = createEngine();
        JexlScript s = jexl.createScript("function x() -> 42; x()");
        Object result = s.execute(null, s);
        Assert.assertEquals(42, result);
        s = jexl.createScript("function x(a,b) -> a+b; x(11,31)");
        result = s.execute(null, s);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testRecursion() throws Exception {
        JexlEngine jexl = createEngine();
        JexlScript s = jexl.createScript("function fib(n) {if (n <= 1) return n; return fib(n-1) + fib(n-2)}; fib(9)");
        Object result = s.execute(null, s);
        Assert.assertEquals(34, result);
    }

    @Test
    public void testStaticFunction() throws Exception {
        JexlEngine jexl = createEngine();
        JexlScript s = jexl.createScript("var a = 10; static function x() {return a}; x()");
        JexlContext jc = new MapContext();
        jc.set("a", 42);
        Object result = s.execute(jc);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testPrimitiveTypedFunction() throws Exception {
        JexlEngine jexl = createEngine();
        JexlScript s = jexl.createScript("long x() {return 42}; x()");
        JexlContext jc = new MapContext();
        Object result = s.execute(jc);
        Assert.assertEquals(42L, result);
    }

    @Test
    public void testNestedTypedFunction() throws Exception {
        JexlEngine jexl = createEngine();
        JexlScript s = jexl.createScript("#pragma jexl.import java.util\nMap.Entry x() {return [2:42]}; x()");
        JexlContext jc = new MapContext();
        Object result = s.execute(jc);
        Assert.assertTrue(result instanceof Map.Entry);
        Assert.assertEquals(42, ((Map.Entry) result).getValue());
        Assert.assertEquals(2, ((Map.Entry) result).getKey());
    }

    @Test
    public void testVoidTypedFunction() throws Exception {
        JexlEngine jexl = createEngine();
        JexlScript s = jexl.createScript("void x() {42; }; x()");
        JexlContext jc = new MapContext();
        Object result = s.execute(jc);
        Assert.assertNull(result);
    }

    @Test
    public void testVoidMustNotReturnValue() throws Exception {
        JexlEngine jexl = createEngine();
        try {
            JexlScript s = jexl.createScript("void x() {return 42; }; x()");
            Assert.fail("Should have failed");
        } catch (JexlException ex) {

        }
    }

}
