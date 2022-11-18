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
 * Tests switch statement.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class SwitchTest extends JexlTestCase {

    public SwitchTest() {
        super("SwitchTest");
    }

    @Test
    public void testSyntax() throws Exception {
        JexlScript e = JEXL.createScript("switch (1) {case 1 : return 42; case 2: return 0}");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 42, o);

        e = JEXL.createScript("switch (1) {case 1 : return 42; case 2: return 0; default: return -1}");
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 42, o);

        e = JEXL.createScript("switch (1) {default: return 42}");
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 42, o);

        e = JEXL.createScript("switch (1) {case 2: return 0; default: return 42}");
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 42, o);

        e = JEXL.createScript("switch (1) {default: return -1; case 2: return 0; case 1 : return 42}");
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 42, o);

        e = JEXL.createScript("switch (1) {default: return -1; case 2: case 1 : return 42}");
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 42, o);

        e = JEXL.createScript("switch (1) {default: return -1; case 2, 1 : return 42}");
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 42, o);
    }

    @Test
    public void testBadSyntax() throws Exception {

        try {
           JEXL.createScript("switch (1) {}");
           Assert.fail("Should have at least one case");
        } catch (Exception ex) {
          //
        }

        try {
           JEXL.createScript("switch (1) {default: default: return}");
           Assert.fail("Should not have multiple default sections");
        } catch (Exception ex) {
          //
        }
    }

    @Test
    public void testFallThrough() throws Exception {
        JexlScript e = JEXL.createScript("x = 0; switch (1) {case 1 : x = x + 1; case 2: x = x + 2}; x");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 3, o);

        e = JEXL.createScript("x = 0; switch (1) {case 1 : x = x + 1; default: x = x + 2}; x");
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 3, o);

        e = JEXL.createScript("x = 0; switch (99) {default: x = x + 1; case 1 : x = x + 2}; x");
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 3, o);
    }

    @Test
    public void testBreak() throws Exception {
        JexlScript e = JEXL.createScript("x = 0; switch (1) {case 1 : x = x + 1; break; case 2: x = x + 2}; x");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 1, o);

        e = JEXL.createScript("x = 0; switch (99) {default: x = x + 1; break; case 1 : x = x + 2}; x");
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 1, o);

        e = JEXL.createScript("x = 0; while(x < 5) switch (99) {default: x = x + 1; break; case 1 : x = x + 2}; x");
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 5, o);
    }

    @Test
    public void testIdentifier() throws Exception {
        JexlScript e = JEXL.createScript("switch (x) {case MAX_VALUE : return 1; default: return 0}");
        JexlContext jc = new MapContext();
        jc.set("x", Integer.MAX_VALUE);
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 1, o);
    }

    @Test
    public void testNullCase() throws Exception {
        JexlScript e = JEXL.createScript("switch (x) {case null : return 1; default: return 0}");
        JexlContext jc = new MapContext();
        jc.set("x", null);
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 1, o);
    }

    @Test
    public void testNullDefaultCase() throws Exception {
        JexlScript e = JEXL.createScript("switch (x) {case null, default : return 1; case 1: return 0}");
        JexlContext jc = new MapContext();
        jc.set("x", 2);
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 1, o);
    }

    @Test
    public void testStringCase() throws Exception {
        JexlScript e = JEXL.createScript("switch (x) {case 'abs' : return 1; default: return 0}");
        JexlContext jc = new MapContext();
        jc.set("x", "abs");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 1, o);

        e = JEXL.createScript("switch (x) {case \"\"\"\nabs\"\"\" : return 1; default: return 0}");
    }

    @Test
    public void testTextBlockCase() throws Exception {
        JexlScript e = JEXL.createScript("switch (x) {case \"\"\"\nabs\"\"\" : return 1; default: return 0}");
        JexlContext jc = new MapContext();
        jc.set("x", "abs");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 1, o);
    }

    @Test
    public void testInterpolatedStringCase() throws Exception {
        JexlScript e = JEXL.createScript("switch (x) {case `a${y}s` : return 1; default: return 0}");
        JexlContext jc = new MapContext();
        jc.set("x", "abs");
        jc.set("y", "b");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 1, o);
    }

    @Test
    public void testTypePattern() throws Exception {
        JexlScript e = JEXL.createScript("switch (x) {case Long l : return -1; case Integer i : return i; default : return 0}");
        JexlContext jc = new MapContext();
        jc.set("x", 42);
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 42, o);
    }

    @Test
    public void testTypePatternWhen() throws Exception {
        JexlScript e = JEXL.createScript("switch (x) {case Long l when false : return -1; case Integer i : return i; default : return 0}");
        JexlContext jc = new MapContext();
        jc.set("x", 42);
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 42, o);
    }

}
