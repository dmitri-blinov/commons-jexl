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

}
