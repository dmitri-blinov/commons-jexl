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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the scoped variables.
 *
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ScopedVarTest extends JexlTestCase {

    JexlEngine jexl = new JexlBuilder().strict(false).arithmetic(new JexlArithmetic(false)).create();

    public ScopedVarTest() {
        super("ScopedVarTest");
    }

    @Test
    public void testSimple() throws Exception {
        JexlContext jc = new MapContext();

        JexlScript e = jexl.createScript("String x; x = 'abc'");
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not true", o instanceof String);
        Assert.assertEquals("Result is not as expected", "abc", o);
    }

    @Test
    public void testDeclaration() throws Exception {
        JexlContext jc = new MapContext();

        JexlScript e = jexl.createScript("String x = 'abc'");
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not true", o instanceof String);
        Assert.assertEquals("Result is not as expected", "abc", o);
    }

    @Test
    public void testNestedType() throws Exception {
        JexlContext jc = new MapContext();

        JexlScript e = jexl.createScript("Map.Entry x = [3:4]");
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not true", o instanceof Map.Entry);
        Assert.assertEquals("Result is not as expected", 3, ((Map.Entry) o).getKey());
        Assert.assertEquals("Result is not as expected", 4, ((Map.Entry) o).getValue());
    }

    @Test
    public void testArray() throws Exception {
        JexlContext jc = new MapContext();

        JexlScript e = jexl.createScript("String[] x = new String[] {'abc', 'cde'}; x");
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not true", o instanceof String[]);
    }

    @Test
    public void testVarClash() throws Exception {
        JexlContext jc = new MapContext();

        JexlScript e = jexl.createScript("String String = 'abc'; size(String)");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 3, o);
    }

    @Test
    public void testAssignCheck() throws Exception {
        JexlContext jc = new MapContext();

        JexlScript e = JEXL.createScript("String x; x = 123");
        try {
           Object o = e.execute(jc);
           Assert.fail("Should have failed");
        } catch (Exception ex) {
           //
        }
    }

    @Test
    public void testUndefinedType() throws Exception {
        JexlContext jc = new MapContext();

        try {
           JexlScript e = jexl.createScript("XXX x; x = 123");
           Assert.fail("Should have failed");
        } catch (Exception ex) {
           //
        }
    }

}
