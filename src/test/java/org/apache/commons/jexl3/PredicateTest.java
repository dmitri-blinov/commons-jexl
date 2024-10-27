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

import java.util.function.Predicate;

/**
 * Tests for relational predicates
 * @since 4.0
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class PredicateTest extends JexlTestCase {

    public PredicateTest() {
        super("PredicateTest");
    }

    @Test
    public void testEqualsPredicate() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = (==42)").execute(jc);
        Assert.assertTrue(o instanceof Predicate<?>);
        Assert.assertTrue(((Predicate<Object>) o).test(42));
        Assert.assertFalse(((Predicate<Object>) o).test(41));
    }

    @Test
    public void testEqualsSetAnyPredicate() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = (==?(42,43))").execute(jc);
        Assert.assertTrue(o instanceof Predicate<?>);
        Assert.assertTrue(((Predicate<Object>) o).test(42));
        Assert.assertFalse(((Predicate<Object>) o).test(41));
    }

    @Test
    public void testNotEqualsPredicate() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = (!=42)").execute(jc);
        Assert.assertTrue(o instanceof Predicate<?>);
        Assert.assertFalse(((Predicate<Object>) o).test(42));
        Assert.assertTrue(((Predicate<Object>) o).test(41));
    }

    @Test
    public void testNotEqualsSetAllPredicate() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = (!=??(42,43))").execute(jc);
        Assert.assertTrue(o instanceof Predicate<?>);
        Assert.assertFalse(((Predicate<Object>) o).test(42));
        Assert.assertTrue(((Predicate<Object>) o).test(41));
    }

    @Test
    public void testLessPredicate() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = (<42)").execute(jc);
        Assert.assertTrue(o instanceof Predicate<?>);
        Assert.assertFalse(((Predicate<Object>) o).test(42));
        Assert.assertTrue(((Predicate<Object>) o).test(41));
    }

    @Test
    public void testLessOrEqualsPredicate() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = (<=42)").execute(jc);
        Assert.assertTrue(o instanceof Predicate<?>);
        Assert.assertTrue(((Predicate<Object>) o).test(42));
        Assert.assertFalse(((Predicate<Object>) o).test(43));
    }

    @Test
    public void testGreaterPredicate() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = (>42)").execute(jc);
        Assert.assertTrue(o instanceof Predicate<?>);
        Assert.assertFalse(((Predicate<Object>) o).test(42));
        Assert.assertTrue(((Predicate<Object>) o).test(43));
    }

    @Test
    public void testGreaterOrEqualsPredicate() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = (>=42)").execute(jc);
        Assert.assertTrue(o instanceof Predicate<?>);
        Assert.assertTrue(((Predicate<Object>) o).test(42));
        Assert.assertFalse(((Predicate<Object>) o).test(41));
    }

    @Test
    public void testStartsWithPredicate() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = (=^ 'abc')").execute(jc);
        Assert.assertTrue(o instanceof Predicate<?>);
        Assert.assertTrue(((Predicate<Object>) o).test("abcent"));
        Assert.assertFalse(((Predicate<Object>) o).test("present"));
    }

    @Test
    public void testNotStartsWithPredicate() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = (!^ 'abc')").execute(jc);
        Assert.assertTrue(o instanceof Predicate<?>);
        Assert.assertFalse(((Predicate<Object>) o).test("abcent"));
        Assert.assertTrue(((Predicate<Object>) o).test("present"));
    }

    @Test
    public void testEndsWithPredicate() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = (=$ 'cent')").execute(jc);
        Assert.assertTrue(o instanceof Predicate<?>);
        Assert.assertTrue(((Predicate<Object>) o).test("abcent"));
        Assert.assertFalse(((Predicate<Object>) o).test("present"));
    }

    @Test
    public void testNotEndsWithPredicate() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = (!$ 'cent')").execute(jc);
        Assert.assertTrue(o instanceof Predicate<?>);
        Assert.assertFalse(((Predicate<Object>) o).test("abcent"));
        Assert.assertTrue(((Predicate<Object>) o).test("present"));
    }

    @Test
    public void testInPredicate() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = (=~ [1,2,42])").execute(jc);
        Assert.assertTrue(o instanceof Predicate<?>);
        Assert.assertTrue(((Predicate<Object>) o).test(42));
        Assert.assertFalse(((Predicate<Object>) o).test(41));
    }

    @Test
    public void testNotInPredicate() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = (!~ [1,2,42])").execute(jc);
        Assert.assertTrue(o instanceof Predicate<?>);
        Assert.assertFalse(((Predicate<Object>) o).test(42));
        Assert.assertTrue(((Predicate<Object>) o).test(41));
    }
}
