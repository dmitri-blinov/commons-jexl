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
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the qualified new syntax.
 *
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ConstructorTest extends JexlTestCase {

    public ConstructorTest() {
        super("ConstructorTest");
    }

    @Test
    public void testSimpleName() throws Exception {
        JexlScript e = JEXL.createScript("new String()");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not true", o instanceof String);
    }

    @Test
    public void testQualifiedName() throws Exception {
        JexlScript e = JEXL.createScript("new java.util.concurrent.atomic.AtomicLong()");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not true", o instanceof AtomicLong);
    }

    @Test
    public void testSimpleNameParams() throws Exception {
        JexlScript e = JEXL.createScript("new String('abc'); ");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", "abc", o);
    }

    @Test
    public void testQualifiedNameParams() throws Exception {
        JexlScript e = JEXL.createScript("var x = new java.util.concurrent.atomic.AtomicLong(42); x.get()");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 42L, o);
    }

    @Test
    public void testArray() throws Exception {
        JexlScript e = JEXL.createScript("new String[5]");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not true", o instanceof String[]);
        Assert.assertEquals("Result is not as expected", 5, ((String[])o).length);
    }

    @Test
    public void testPrimitiveArray() throws Exception {
        JexlScript e = JEXL.createScript("new int[5]");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not true", o instanceof int[]);
        Assert.assertEquals("Result is not as expected", 5, ((int[])o).length);
    }

    @Test
    public void testQualifiedArray() throws Exception {
        JexlScript e = JEXL.createScript("new java.util.concurrent.atomic.AtomicLong[5]");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not true", o instanceof AtomicLong[]);
        Assert.assertEquals("Result is not as expected", 5, ((AtomicLong[])o).length);
    }

    @Test
    public void testMultidimensionalArray() throws Exception {
        JexlScript e = JEXL.createScript("new String[6][5]");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not true", o instanceof Object[]);
        Assert.assertEquals("Result is not as expected", 6, ((Object[])o).length);
    }

    @Test
    public void testOpenArray() throws Exception {
        JexlScript e = JEXL.createScript("new String[6][]");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not true", o instanceof Object[]);
        Assert.assertEquals("Result is not as expected", 6, ((Object[])o).length);
    }

    @Test
    public void testMultidimensionalPrimitiveArray() throws Exception {
        JexlScript e = JEXL.createScript("new int[6][5]");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not true", o instanceof Object[]);
        Assert.assertEquals("Result is not as expected", 6, ((Object[])o).length);
    }

    @Test
    public void testInitializedArray() throws Exception {
        JexlScript e = JEXL.createScript("new String[] {'abc','def'}");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not true", o instanceof String[]);
        Assert.assertEquals("Result is not as expected", 2, ((String[])o).length);
    }

    @Test
    public void testInitializedArrayComprehension() throws Exception {
        JexlScript e = JEXL.createScript("new String[] {'abc','def',...a}");
        JexlContext jc = new MapContext();
        jc.set("a", new String[] {"opq", "xyz"});
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not true", o instanceof String[]);
        Assert.assertEquals("Result is not as expected", 4, ((String[])o).length);
    }

    @Test
    public void testInitializedCollection() throws Exception {
        JexlScript e = JEXL.createScript("new LinkedHashSet {'abc','def'}");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not true", o instanceof LinkedHashSet);
        Assert.assertEquals("Result is not as expected", 2, ((Set)o).size());
    }

    @Test
    public void testInitializedCollectionComprehension() throws Exception {
        JexlScript e = JEXL.createScript("new LinkedHashSet {'abc','def', ...a}");
        JexlContext jc = new MapContext();
        jc.set("a", new String[] {"opq", "xyz"});
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not true", o instanceof LinkedHashSet);
        Assert.assertEquals("Result is not as expected", 4, ((Set)o).size());
    }

    @Test
    public void testInitializedMap() throws Exception {
        JexlScript e = JEXL.createScript("new LinkedHashMap {'abc':1,'def':2}");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not true", o instanceof LinkedHashMap);
        Assert.assertEquals("Result is not as expected", 2, ((Map)o).size());
    }

    @Test
    public void testInitializedMapComprehension() throws Exception {
        JexlScript e = JEXL.createScript("new LinkedHashMap {'abc':1,'def':2, *:...x}");
        JexlContext jc = new MapContext();
        HashMap map = new HashMap();
        map.put("opq",3);
        map.put("xyz",4);
        jc.set("x", map);
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not true", o instanceof LinkedHashMap);
        Assert.assertEquals("Result is not as expected", 4, ((Map)o).size());
    }

    @Test
    public void testInnerClass() throws Exception {
        JexlScript e = JEXL.createScript("x.new Cheezy()");
        JexlContext jc = new MapContext();
        jc.set("x", new Foo());
        Object o = e.execute(jc);
        Assert.assertTrue("Result is not true", o instanceof Foo.Cheezy);
    }
    
}
