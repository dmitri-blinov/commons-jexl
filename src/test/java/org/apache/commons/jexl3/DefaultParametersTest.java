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

import org.apache.commons.jexl3.internal.Closure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.math.BigInteger;
import java.math.BigDecimal;
import org.junit.Assert;
import org.junit.Test;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Tests default parameters.
 *
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class DefaultParametersTest extends JexlTestCase {

    public DefaultParametersTest() {
        super("DefaultParametersTest");
    }

    public static class CurryArithmetic extends JexlArithmetic {
        CurryArithmetic(boolean flag) {
            super(flag);
        }

        @Override
        public Object arrayGet(Object object, Object key) throws Exception {

            if (object instanceof Closure)
                return ((Closure) object).curry(key);

            return super.arrayGet(object, key);
        }

    }

    @Test
    public void testBasic() throws Exception {
        JexlScript e = JEXL.createScript("var x = function(var a = 42) {a}; x()");
        Object o = e.execute(null);
        Assert.assertEquals("Result is not 42", 42, o);
        e = JEXL.createScript("var x = function(var a = 0) {a}; x(42)");
        o = e.execute(null);
        Assert.assertEquals("Result is not 42", 42, o);
        e = JEXL.createScript("var x = function(var a = 42) {a}; x(null)");
        o = e.execute(null);
        Assert.assertNull("Result is not null", o);
    }

    @Test
    public void testMultiple() throws Exception {
        JexlScript e = JEXL.createScript("var x = function(var a = 41, var b = 1) {a + b}; x()");
        Object o = e.execute(null);
        Assert.assertEquals("Result is not 42", 42, o);
        e = JEXL.createScript("var x = function(var a = 2, var b = 22) {a + b}; x(20)");
        o = e.execute(null);
        Assert.assertEquals("Result is not 42", 42, o);
        e = JEXL.createScript("var x = function(var a = 42, var b = 21) {a + b}; x(40, 2)");
        o = e.execute(null);
        Assert.assertEquals("Result is not 42", 42, o);
    }

    @Test
    public void testNonDefault() throws Exception {
        JexlScript e = JEXL.createScript("var x = function(var a, var b = 1) {a}; x()");
        Object o = e.execute(null);
        Assert.assertNull("Result is not null", o);
        e = JEXL.createScript("var x = function(var a, var b = 22) {a + b}; x(20)");
        o = e.execute(null);
        Assert.assertEquals("Result is not 42", 42, o);
        e = JEXL.createScript("var x = function(var a = 42, var b, var c = 21) {b}; x()");
        o = e.execute(null);
        Assert.assertNull("Result is not null", o);
        e = JEXL.createScript("var x = function(var a = 42, var b, var c = 21) {c}; x()");
        o = e.execute(null);
        Assert.assertEquals("Result is not 21", 21, o);
    }

    @Test
    public void testStringType() throws Exception {
        JexlScript e = JEXL.createScript("var x = function(var a = 'World') {a}; x()");
        Object o = e.execute(null);
        Assert.assertEquals("Result is not as expected", "World", o);
    }

    @Test
    public void testRegexType() throws Exception {
        JexlScript e = JEXL.createScript("var x = function(var a = ~/ABC.*/) {a}; x()");
        Object o = e.execute(null);
        Assert.assertEquals("Result is not as expected", "ABC.*", String.valueOf(o));
    }

    @Test
    public void testNumericType() throws Exception {
        JexlScript e = JEXL.createScript("var x = function(var a = 42.0) {a}; x()");
        Object o = e.execute(null);
        Assert.assertEquals("Result is not as expected", 42.0, o);
    }

    @Test
    public void testBigIntegerType() throws Exception {
        JexlScript e = JEXL.createScript("var x = function(var a = 42H) {a}; x()");
        Object o = e.execute(null);
        Assert.assertEquals("Result is not as expected", BigInteger.valueOf(42L), o);
    }

    @Test
    public void testBigDecimalType() throws Exception {
        JexlScript e = JEXL.createScript("var x = function(var a = 42.0B) {a}; x()");
        Object o = e.execute(null);
        Assert.assertEquals("Result is not as expected", BigDecimal.valueOf(42.0), o);
    }

    @Test
    public void testBooleanType() throws Exception {
        JexlScript e = JEXL.createScript("var x = function(var a = true) {a}; x()");
        Object o = e.execute(null);
        Assert.assertEquals("Result is not as expected", Boolean.TRUE, o);
    }

    @Test
    public void testNull() throws Exception {
        JexlScript e = JEXL.createScript("var x = function(var a = null) {a}; x()");
        Object o = e.execute(null);
        Assert.assertNull("Result is not as expected", o);
    }

    @Test
    public void testConstant() throws Exception {
        JexlScript e = JEXL.createScript("var x = function(int a = MAX_VALUE) {a}; x()");
        Object o = e.execute(null);
        Assert.assertEquals("Result is not as expected", Integer.MAX_VALUE, o);
    }

    @Test
    public void testUndefinedConstant() throws Exception {
        try {
            JexlScript e = JEXL.createScript("var x = function(int a = MAX_VALUE1) {a}; x()");
            Assert.fail("Should have failed");
        } catch (Exception ex) {
            // OK
        }
    }

    @Test
    public void testCurried() throws Exception {
        JexlEngine jexl = new JexlBuilder().arithmetic(new CurryArithmetic(false)).create();
        JexlScript e = jexl.createScript("var x = function(var a, var b = 42) {a + b}; var y = x[2]; y(40)");
        Object o = e.execute(null);
        Assert.assertEquals("Result is not as expected", 42, o);
    }

    @Test
    public void testCurriedDefault() throws Exception {
        JexlEngine jexl = new JexlBuilder().arithmetic(new CurryArithmetic(false)).create();
        JexlScript e = jexl.createScript("var x = function(var a, var b = 42) {a + b}; var y = x[2]; y()");
        Object o = e.execute(null);
        Assert.assertEquals("Result is not as expected", 44, o);
    }

    @Test
    public void testBug() throws Exception {
        JexlScript e = JEXL.createScript("var x = function(int a = 12,int b = 20,int c = 2, int d = 5, int e = 10) {c}; x()");
        Object o = e.execute(null);
        Assert.assertEquals("Result is not as expected", 2, o);
    }


}
