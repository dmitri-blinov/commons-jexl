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
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for ordered set/map literals.
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class OrderedTest extends JexlTestCase {

    public OrderedTest() {
        super("OrderedTest");
    }

    @Test
    public void testOrderedSetLiteral() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var o = ''; var x = {'10', '20', '30', ...}; for (var i : x) o = o + i; o").execute(jc);
        Assert.assertEquals("102030", o);
        o = JEXL.createScript("var o = ''; var x = {'10', '20', '30'}; for (var i : x) o = o + i; o").execute(jc);
        Assert.assertNotEquals("102030", o);
    }

    @Test
    public void testEmptyOrderedSetLiteral() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var o = ''; var x = {...}; x.add('10'); x.add('20'); x.add('30'); for (var i : x) o = o + i; o").execute(jc);
        Assert.assertEquals("102030", o);
    }

    @Test
    public void testOrderedImmutableSetLiteral() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var o = ''; var x = #{'10', '20', '30', ...}; for (var i : x) o = o + i; o").execute(jc);
        Assert.assertEquals("102030", o);
        o = JEXL.createScript("var o = ''; var x = #{'10', '20', '30'}; for (var i : x) o = o + i; o").execute(jc);
        Assert.assertNotEquals("102030", o);
    }

    @Test
    public void testOrderedMapLiteral() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var o = ''; var x = {'10':'10', '20':'20', '30':'30', ...}; for (var i : x) o = o + i; o").execute(jc);
        Assert.assertEquals("102030", o);
        o = JEXL.createScript("var o = ''; var x = {'10':'10', '20':'20', '30':'30'}; for (var i : x) o = o + i; o").execute(jc);
        Assert.assertNotEquals("102030", o);
    }

    @Test
    public void testEmptyOrderedMapLiteral() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var o = ''; var x = {:...}; x.put('10','10'); x.put('20','20'); x.put('30','30'); for (var i : x) o = o + i; o").execute(jc);
        Assert.assertEquals("102030", o);
    }

    @Test
    public void testOrderedImmutableMapLiteral() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var o = ''; var x = #{'10':'10', '20':'20', '30':'30', ...}; for (var i : x) o = o + i; o").execute(jc);
        Assert.assertEquals("102030", o);
        o = JEXL.createScript("var o = ''; var x = #{'10':'10', '20':'20', '30':'30'}; for (var i : x) o = o + i; o").execute(jc);
        Assert.assertNotEquals("102030", o);
    }

}
