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

/**
 * Test cases for equality assignment operator.
 *
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class EqualityAssignTest extends JexlTestCase {

    public static class Froboz {
        String value;

        public Froboz() {
        }

        public Froboz(String v) {
            value = v;
        }

        public void setValue(String v) {
            value = v;
        }
        public String getValue() {
            return value;
        }
    }

    public EqualityAssignTest() {
        super("EqualityAssignTest", new JexlBuilder().cache(512).strict(false).silent(false).create());
    }

    @Test
    public void testNotEquals() throws Exception {
        JexlScript assign = JEXL.createScript("x := 'cde'; x");
        JexlContext jc = new MapContext();
        jc.set("x", "abc");
        Object o = assign.execute(jc);
        Assert.assertEquals("Result is not as expected", "cde", o);
    }

    @Test
    public void testEquals() throws Exception {
        JexlScript assign = JEXL.createScript("x := 'abc'; x");
        JexlContext jc = new MapContext();
        String value = "abc";
        jc.set("x", value);
        Object o = assign.execute(jc);
        Assert.assertTrue("Result is not as expected", value == o);
    }

    @Test
    public void testAntishNotEquals() throws Exception {
        JexlScript assign = JEXL.createScript("froboz.value := 10");
        JexlContext jc = new MapContext();
        Object o = assign.execute(jc);
        Assert.assertEquals("Result is not 10", 10, jc.get("froboz.value"));
    }

    @Test
    public void testAntishEquals() throws Exception {
        JexlScript assign = JEXL.createScript("froboz.value := 'abc'");
        JexlContext jc = new MapContext();
        String value = "abc";
        jc.set("froboz.value", value);
        Object o = assign.execute(jc);
        Assert.assertTrue("Result is not as expected", value == jc.get("froboz.value"));
    }

    @Test
    public void testStrict() throws Exception {
        JexlEngine jexl = new JexlBuilder().cache(512).strict(true).silent(false).create();
        JexlScript assign = jexl.createScript("froboz.value := 10");
        JexlContext jc = new MapContext();
        Object o = assign.execute(jc);
        Assert.assertEquals("Result is not 10", 10, jc.get("froboz.value"));
    }

    @Test
    public void testBean() throws Exception {
        JexlScript assign = JEXL.createScript("froboz.value := 'abc'");
        JexlContext jc = new MapContext();
        String value = "abc";
        Froboz fb = new Froboz(value);
        jc.set("froboz", fb);
        Object o = assign.execute(jc);
        Assert.assertTrue("Result is not as expected", value == fb.getValue());
        assign = JEXL.createScript("froboz.value := 'cde'");
        o = assign.execute(jc);
        Assert.assertEquals("Result is not 10", "cde", fb.getValue());
    }

}