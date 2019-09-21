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
 * Tests switch expression.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class RelationalSetOperandTest extends JexlTestCase {

    public RelationalSetOperandTest() {
        super("RelationalSetOperandTest");
    }

    @Test
    public void testEqualsAll() throws Exception {
        JexlScript e = JEXL.createScript("1 == ??(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", 1);
        jc.set("b", 1);
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testEqualsAny() throws Exception {
        JexlScript e = JEXL.createScript("1 == ?(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", 1);
        jc.set("b", 2);
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testEqualsAllFalse() throws Exception {
        JexlScript e = JEXL.createScript("1 == ??(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", 1);
        jc.set("b", 2);
        Object o = e.execute(jc);
        Assert.assertFalse("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testEqualsAnyFalse() throws Exception {
        JexlScript e = JEXL.createScript("1 == ?(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", 2);
        jc.set("b", 3);
        Object o = e.execute(jc);
        Assert.assertFalse("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testNotEqualsAll() throws Exception {
        JexlScript e = JEXL.createScript("1 != ??(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", 2);
        jc.set("b", 3);
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testNotEqualsAny() throws Exception {
        JexlScript e = JEXL.createScript("1 != ?(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", 2);
        jc.set("b", 1);
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testNotEqualsAllFalse() throws Exception {
        JexlScript e = JEXL.createScript("1 != ??(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", 1);
        jc.set("b", 1);
        Object o = e.execute(jc);
        Assert.assertFalse("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testNotEqualsAnyFalse() throws Exception {
        JexlScript e = JEXL.createScript("1 != ?(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", 1);
        jc.set("b", 1);
        Object o = e.execute(jc);
        Assert.assertFalse("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testGreaterAll() throws Exception {
        JexlScript e = JEXL.createScript("3 > ??(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", 1);
        jc.set("b", 2);
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testGreaterAny() throws Exception {
        JexlScript e = JEXL.createScript("2 > ?(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", 1);
        jc.set("b", 2);
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testGreaterAllFalse() throws Exception {
        JexlScript e = JEXL.createScript("3 > ??(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", 2);
        jc.set("b", 3);
        Object o = e.execute(jc);
        Assert.assertFalse("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testGreaterAnyFalse() throws Exception {
        JexlScript e = JEXL.createScript("2 > ?(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", 3);
        jc.set("b", 2);
        Object o = e.execute(jc);
        Assert.assertFalse("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testGreaterEqualsAll() throws Exception {
        JexlScript e = JEXL.createScript("3 >= ??(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", 2);
        jc.set("b", 3);
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testGreaterEqualsAny() throws Exception {
        JexlScript e = JEXL.createScript("2 >= ?(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", 2);
        jc.set("b", 3);
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testGreaterEqualsAllFalse() throws Exception {
        JexlScript e = JEXL.createScript("3 >= ??(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", 4);
        jc.set("b", 3);
        Object o = e.execute(jc);
        Assert.assertFalse("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testGreaterEqualsAnyFalse() throws Exception {
        JexlScript e = JEXL.createScript("2 >= ?(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", 3);
        jc.set("b", 4);
        Object o = e.execute(jc);
        Assert.assertFalse("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testLessAll() throws Exception {
        JexlScript e = JEXL.createScript("3 < ??(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", 4);
        jc.set("b", 5);
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testLessAny() throws Exception {
        JexlScript e = JEXL.createScript("2 < ?(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", 3);
        jc.set("b", 2);
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testLessAllFalse() throws Exception {
        JexlScript e = JEXL.createScript("3 < ??(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", 4);
        jc.set("b", 3);
        Object o = e.execute(jc);
        Assert.assertFalse("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testLessAnyFalse() throws Exception {
        JexlScript e = JEXL.createScript("2 < ?(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", 1);
        jc.set("b", 2);
        Object o = e.execute(jc);
        Assert.assertFalse("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testLessEqualsAll() throws Exception {
        JexlScript e = JEXL.createScript("3 <= ??(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", 4);
        jc.set("b", 3);
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testLessEqualsAny() throws Exception {
        JexlScript e = JEXL.createScript("2 <= ?(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", 3);
        jc.set("b", 2);
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testLessEqualsAllFalse() throws Exception {
        JexlScript e = JEXL.createScript("3 <= ??(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", 2);
        jc.set("b", 3);
        Object o = e.execute(jc);
        Assert.assertFalse("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testLessEqualsAnyFalse() throws Exception {
        JexlScript e = JEXL.createScript("2 <= ?(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", 1);
        jc.set("b", 0);
        Object o = e.execute(jc);
        Assert.assertFalse("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testContainsAll() throws Exception {
        JexlScript e = JEXL.createScript("1 =~ ??(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", new int[] {1});
        jc.set("b", new int[] {1,2});
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testContainsAny() throws Exception {
        JexlScript e = JEXL.createScript("1 =~ ?(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", new int[] {1});
        jc.set("b", new int[] {2});
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testContainsAllFalse() throws Exception {
        JexlScript e = JEXL.createScript("1 =~ ??(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", new int[] {2});
        jc.set("b", new int[] {1});
        Object o = e.execute(jc);
        Assert.assertFalse("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testContainsAnyFalse() throws Exception {
        JexlScript e = JEXL.createScript("1 =~ ?(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", new int[] {2});
        jc.set("b", new int[] {3});
        Object o = e.execute(jc);
        Assert.assertFalse("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testNotContainsAll() throws Exception {
        JexlScript e = JEXL.createScript("1 !~ ??(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", new int[] {2});
        jc.set("b", new int[] {3,2});
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testNotContainsAny() throws Exception {
        JexlScript e = JEXL.createScript("1 !~ ?(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", new int[] {1});
        jc.set("b", new int[] {2});
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testNotContainsAllFalse() throws Exception {
        JexlScript e = JEXL.createScript("1 !~ ??(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", new int[] {1});
        jc.set("b", new int[] {2});
        Object o = e.execute(jc);
        Assert.assertFalse("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testNotContainsAnyFalse() throws Exception {
        JexlScript e = JEXL.createScript("1 !~ ?(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", new int[] {1});
        jc.set("b", new int[] {1});
        Object o = e.execute(jc);
        Assert.assertFalse("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testStartsWithAll() throws Exception {
        JexlScript e = JEXL.createScript("'abc' =^ ??(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", "ab");
        jc.set("b", "a");
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testStartsWithAny() throws Exception {
        JexlScript e = JEXL.createScript("'abc' =^ ?(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", "cd");
        jc.set("b", "ab");
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testStartsWithAllFalse() throws Exception {
        JexlScript e = JEXL.createScript("'abc' =^ ??(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", "ab");
        jc.set("b", "cd");
        Object o = e.execute(jc);
        Assert.assertFalse("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testStartsWithAnyFalse() throws Exception {
        JexlScript e = JEXL.createScript("'abc' =^ ?(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", "cd");
        jc.set("b", "de");
        Object o = e.execute(jc);
        Assert.assertFalse("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testNotStartsWithAll() throws Exception {
        JexlScript e = JEXL.createScript("'abc' !^ ??(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", "cd");
        jc.set("b", "ef");
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testNotStartsWithAny() throws Exception {
        JexlScript e = JEXL.createScript("'abc' !^ ?(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", "cd");
        jc.set("b", "ab");
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testNotStartsWithAllFalse() throws Exception {
        JexlScript e = JEXL.createScript("'abc' !^ ??(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", "ab");
        jc.set("b", "cd");
        Object o = e.execute(jc);
        Assert.assertFalse("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testNotStartsWithAnyFalse() throws Exception {
        JexlScript e = JEXL.createScript("'abc' !^ ?(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", "ab");
        jc.set("b", "a");
        Object o = e.execute(jc);
        Assert.assertFalse("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testEndsWithAll() throws Exception {
        JexlScript e = JEXL.createScript("'abc' =$ ??(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", "bc");
        jc.set("b", "c");
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testEndsWithAny() throws Exception {
        JexlScript e = JEXL.createScript("'abc' =$ ?(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", "bc");
        jc.set("b", "ab");
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testEndsWithAllFalse() throws Exception {
        JexlScript e = JEXL.createScript("'abc' =$ ??(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", "bc");
        jc.set("b", "cd");
        Object o = e.execute(jc);
        Assert.assertFalse("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testEndsWithAnyFalse() throws Exception {
        JexlScript e = JEXL.createScript("'abc' =$ ?(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", "cd");
        jc.set("b", "de");
        Object o = e.execute(jc);
        Assert.assertFalse("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testNotEndsWithAll() throws Exception {
        JexlScript e = JEXL.createScript("'abc' !$ ??(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", "cd");
        jc.set("b", "de");
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testNotEndsWithAny() throws Exception {
        JexlScript e = JEXL.createScript("'abc' !$ ?(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", "bc");
        jc.set("b", "ab");
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testNotEndsWithAllFalse() throws Exception {
        JexlScript e = JEXL.createScript("'abc' !$ ??(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", "bc");
        jc.set("b", "cd");
        Object o = e.execute(jc);
        Assert.assertFalse("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testNotEndsWithAnyFalse() throws Exception {
        JexlScript e = JEXL.createScript("'abc' !$ ?(a,b)");
        JexlContext jc = new MapContext();
        jc.set("a", "bc");
        jc.set("b", "c");
        Object o = e.execute(jc);
        Assert.assertFalse("Result is not as expected", (Boolean) o);
    }

    @Test
    public void testArgumentExpansion() throws Exception {
        JexlScript e = JEXL.createScript("'abc' eq ?(...['sdf','abc'])");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not as expected", (Boolean) o);
    }

}
