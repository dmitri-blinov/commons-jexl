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
 * Test cases for multiple var declaration.
 *
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class MultiDeclareVarTest extends JexlTestCase {

    public MultiDeclareVarTest() {
        super("MultiDeclareVarTest", new JexlBuilder().cache(512).strict(true).silent(false).create());
    }

    @Test
    public void testBasic() throws Exception {
        JexlScript assign = JEXL.createScript("var x,y,z; z");
        Object o = assign.execute(null);
        Assert.assertEquals("Result is not as expected", null, o);
    }

    @Test
    public void testBasicWithInit() throws Exception {
        JexlScript assign = JEXL.createScript("var x,y=2,z=40; y+z");
        Object o = assign.execute(null);
        Assert.assertEquals("Result is not as expected", 42, o);
    }

    @Test
    public void testTyped() throws Exception {
        JexlScript assign = JEXL.createScript("long x,y=42; y");
        Object o = assign.execute(null);
        Assert.assertEquals("Result is not as expected", 42L, o);
    }

    @Test
    public void testVisibilityScope() throws Exception {
        JexlScript assign = JEXL.createScript("long x = 42, y = x; y");
        Object o = assign.execute(null);
        Assert.assertEquals("Result is not as expected", 42L, o);
    }

    @Test
    public void testFinalRequired() throws Exception {
        try {
            JexlScript assign = JEXL.createScript("final var x=40,y");
            Assert.fail("Initialization of a final variable is required");
        } catch (JexlException ex) {
            // OK
        }
    }

    @Test
    public void testRequired() throws Exception {
        try {
            JexlScript assign = JEXL.createScript("var x=40,&y");
            Assert.fail("Initialization of a non null variable is required");
        } catch (JexlException ex) {
            // OK
        }
    }

    @Test
    public void testFinalReassignment() throws Exception {
        try {
            JexlScript assign = JEXL.createScript("final var x=40,y=2; y = 42");
            Object o = assign.execute(null);
            Assert.fail("Initialization of a final variable is required");
        } catch (JexlException ex) {
            // OK
        }
    }

}