/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.jexl3.junit.Asserter;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

/**
 * Tests for array access operator []
 *
 * @since 2.0
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ArrayAccessTest extends JexlTestCase {

    private Asserter asserter;

    private static final String GET_METHOD_STRING = "GetMethod string";

    // Needs to be accessible by Foo.class
    static final String[] GET_METHOD_ARRAY =
        new String[] { "One", "Two", "Three" };

    // Needs to be accessible by Foo.class
    static final String[][] GET_METHOD_ARRAY2 =
        new String[][] { {"One", "Two", "Three"},{"Four", "Five", "Six"} };

    public ArrayAccessTest() {
        super("ArrayAccessTest");
    }

    @Override
    @Before
    public void setUp() {
        asserter = new Asserter(JEXL);
    }

    /**
     * test simple array access
     */
    @Test
    public void testArrayAccess() throws Exception {

        /*
         * test List access
         */

        final List<Integer> l = new ArrayList<>();
        l.add(new Integer(1));
        l.add(new Integer(2));
        l.add(new Integer(3));

        asserter.setVariable("list", l);

        asserter.assertExpression("list[1]", new Integer(2));
        asserter.assertExpression("list[1+1]", new Integer(3));
        asserter.setVariable("loc", new Integer(1));
        asserter.assertExpression("list[loc+1]", new Integer(3));

        /*
         * test array access
         */

        final String[] args = { "hello", "there" };
        asserter.setVariable("array", args);
        asserter.assertExpression("array[0]", "hello");

        /*
         * to think that this was an intentional syntax...
         */
        asserter.assertExpression("array.0", "hello");

        /*
         * test map access
         */
        final Map<String, String> m = new HashMap<>();
        m.put("foo", "bar");

        asserter.setVariable("map", m);
        asserter.setVariable("key", "foo");

        asserter.assertExpression("map[\"foo\"]", "bar");
        asserter.assertExpression("map[key]", "bar");

        /*
         * test bean access
         */
        asserter.setVariable("foo", new Foo());
        asserter.assertExpression("foo[\"bar\"]", GET_METHOD_STRING);
        asserter.assertExpression("foo[\"bar\"] == foo.bar", Boolean.TRUE);
    }

    /**
     * test some simple double array lookups
     */
    @Test
    public void testDoubleArrays() throws Exception {
        final Object[][] foo = new Object[2][2];

        foo[0][0] = "one";
        foo[0][1] = "two";
        asserter.setVariable("foo", foo);
        asserter.assertExpression("foo[0][1]", "two");
        asserter.assertExpression("foo[0][1] = 'three'", "three");
        asserter.assertExpression("foo[0][1]", "three");

        foo[0][0] = "one";
        foo[0][1] = "two";
        asserter.assertExpression("foo.0[1]", "two");
        asserter.assertExpression("foo.0[1] = 'three'", "three");
        asserter.assertExpression("foo.0[1]", "three");

        foo[0][0] = "one";
        foo[0][1] = "two";
        asserter.assertExpression("foo.0.'1'", "two");
        asserter.assertExpression("foo.0.'1' = 'three'", "three");
        asserter.assertExpression("foo.0.'1'", "three");

        foo[0][0] = "one";
        foo[0][1] = "two";
        asserter.assertExpression("foo.'0'.'1'", "two");
        asserter.assertExpression("foo.'0'.'1' = 'three'", "three");
        asserter.assertExpression("foo.'0'.'1'", "three");


        foo[0][0] = "one";
        foo[0][1] = "two";
        asserter.assertExpression("foo.0.1", "two");
        asserter.assertExpression("foo.0.1 = 'three'", "three");
        asserter.assertExpression("foo.0.1", "three");
    }

    @Test
    public void testDoubleMaps() throws Exception {
        final Map<Object, Map<Object, Object>> foo = new HashMap<>();
        final Map<Object, Object> foo0 = new HashMap<>();
        foo.put(0, foo0);
        foo0.put(0, "one");
        foo0.put(1, "two");
        foo0.put("3.0", "three");
        asserter.setVariable("foo", foo);
        asserter.assertExpression("foo[0][1]", "two");
        asserter.assertExpression("foo[0][1] = 'three'", "three");
        asserter.assertExpression("foo[0][1]", "three");
        asserter.assertExpression("foo[0]['3.0']", "three");

        foo0.put(0, "one");
        foo0.put(1, "two");
        asserter.assertExpression("foo.0[1]", "two");
        asserter.assertExpression("foo.0[1] = 'three'", "three");
        asserter.assertExpression("foo.0[1]", "three");
        asserter.assertExpression("foo.0['3.0']", "three");

        foo0.put(0, "one");
        foo0.put(1, "two");
        asserter.assertExpression("foo.0.'1'", "two");
        asserter.assertExpression("foo.0.'1' = 'three'", "three");
        asserter.assertExpression("foo.0.'1'", "three");

        foo0.put(0, "one");
        foo0.put(1, "two");
        asserter.assertExpression("foo.'0'.'1'", "two");
        asserter.assertExpression("foo.'0'.'1' = 'three'", "three");
        asserter.assertExpression("foo.'0'.'1'", "three");

        foo0.put(0, "one");
        foo0.put(1, "two");
        asserter.assertExpression("foo.0.1", "two");
        asserter.assertExpression("foo.0.1 = 'three'", "three");
        asserter.assertExpression("foo.0.1", "three");
    }

    @Test
    public void testArrayProperty() throws Exception {
        final Foo foo = new Foo();

        asserter.setVariable("foo", foo);

        asserter.assertExpression("foo.array[1]", GET_METHOD_ARRAY[1]);
        asserter.assertExpression("foo.array.1", GET_METHOD_ARRAY[1]);
        asserter.assertExpression("foo.array2[1][1]", GET_METHOD_ARRAY2[1][1]);
        asserter.assertExpression("foo.array2[1].1", GET_METHOD_ARRAY2[1][1]);
    }

    // This is JEXL-26
    @Test
    public void testArrayAndDottedConflict() throws Exception {
        final Object[] objects = new Object[] {"an", "array", new Long(0)};
        asserter.setStrict(false);
        asserter.setSilent(true);
        asserter.setVariable("objects", objects);
        asserter.setVariable("status", "Enabled");
        asserter.assertExpression("objects[1].status", null);
        asserter.assertExpression("objects.1.status", null);

        asserter.setVariable("base.status", "Ok");
        asserter.assertExpression("base.objects[1].status", null);
        asserter.assertExpression("base.objects.1.status", null);
    }

    @Test
    public void testArrayIdentifierParsing() throws Exception {
        final Map<Object, Number> map = new HashMap<>();
        map.put("00200", -42.42d);
        map.put(200, 42.42d);
        asserter.setVariable("objects", map);
        asserter.assertExpression("objects.get('00200')", -42.42d);
        asserter.assertExpression("objects.'00200'", -42.42d);
        asserter.assertExpression("objects.get(200)", 42.42d);
        asserter.assertExpression("objects.'200'", 42.42d);
        asserter.assertExpression("objects.200", 42.42d);
    }

    @Test
    public void testArrayArray() throws Exception {
        final Integer i42 = Integer.valueOf(42);
        final Integer i43 = Integer.valueOf(43);
        final String s42 = "fourty-two";
        final String s43 = "fourty-three";
        final Object[] foo = new Object[3];
        foo[0] = foo;
        foo[1] = i42;
        foo[2] = s42;
        asserter.setVariable("foo", foo);
        asserter.setVariable("zero", Integer.valueOf(0));
        asserter.setVariable("one", Integer.valueOf(1));
        asserter.setVariable("two", Integer.valueOf(2));
        for(int l = 0; l < 2; ++l) {
            asserter.assertExpression("foo[0]", foo);
            asserter.assertExpression("foo[0][0]", foo);
            asserter.assertExpression("foo[1]", foo[1]);
            asserter.assertExpression("foo[0][1]", foo[1]);
            asserter.assertExpression("foo[0][1] = 43", i43);
            asserter.assertExpression("foo[0][1]", i43);
            asserter.assertExpression("foo[0][1] = 42", i42);
            asserter.assertExpression("foo[0][1]", i42);
            asserter.assertExpression("foo[0][0][1]", foo[1]);
            asserter.assertExpression("foo[0][0][1] = 43", i43);
            asserter.assertExpression("foo[0][0][1]", i43);
            asserter.assertExpression("foo[0][0][1] = 42", i42);
            asserter.assertExpression("foo[0][0][1]", i42);
            asserter.assertExpression("foo[2]", foo[2]);
            asserter.assertExpression("foo[0][2]", foo[2]);
            asserter.assertExpression("foo[0][2] = 'fourty-three'", s43);
            asserter.assertExpression("foo[0][2]", s43);
            asserter.assertExpression("foo[0][2] = 'fourty-two'", s42);
            asserter.assertExpression("foo[0][2]", s42);
            asserter.assertExpression("foo[0][0][2]", foo[2]);
            asserter.assertExpression("foo[0][0][2] = 'fourty-three'", s43);
            asserter.assertExpression("foo[0][0][2]", s43);
            asserter.assertExpression("foo[0][0][2] = 'fourty-two'", s42);
            asserter.assertExpression("foo[0][0][2]", s42);

            asserter.assertExpression("foo[zero]", foo);
            asserter.assertExpression("foo[zero][zero]", foo);
            asserter.assertExpression("foo[one]", foo[1]);
            asserter.assertExpression("foo[zero][one]", foo[1]);
            asserter.assertExpression("foo[zero][one] = 43", i43);
            asserter.assertExpression("foo[zero][one]", i43);
            asserter.assertExpression("foo[zero][one] = 42", i42);
            asserter.assertExpression("foo[zero][one]", i42);
            asserter.assertExpression("foo[zero][zero][one]", foo[1]);
            asserter.assertExpression("foo[zero][zero][one] = 43", i43);
            asserter.assertExpression("foo[zero][zero][one]", i43);
            asserter.assertExpression("foo[zero][zero][one] = 42", i42);
            asserter.assertExpression("foo[zero][zero][one]", i42);
            asserter.assertExpression("foo[two]", foo[2]);
            asserter.assertExpression("foo[zero][two]", foo[2]);
            asserter.assertExpression("foo[zero][two] = 'fourty-three'", s43);
            asserter.assertExpression("foo[zero][two]", s43);
            asserter.assertExpression("foo[zero][two] = 'fourty-two'", s42);
            asserter.assertExpression("foo[zero][two]", s42);
            asserter.assertExpression("foo[zero][zero][two]", foo[2]);
            asserter.assertExpression("foo[zero][zero][two] = 'fourty-three'", s43);
            asserter.assertExpression("foo[zero][zero][two]", s43);
            asserter.assertExpression("foo[zero][zero][two] = 'fourty-two'", s42);
            asserter.assertExpression("foo[zero][zero][two]", s42);
        }
    }

    public static class Sample {
        private int[] array;
        public void setFoo(final int[] a) {
            array = a;
        }
        public int[] getFoo() {
            return array;
        }
    }
    @Test
    public void testArrayGetSet() throws Exception {
        final Sample bar  = new Sample();
        bar.setFoo(new int[]{24});
        asserter.setVariable("bar", bar);
        asserter.assertExpression("bar.foo[0]", 24);
        asserter.assertExpression("bar.foo = []", new int[0]);
        //asserter.assertExpression("bar.foo[0]", 42);
    }

    @Test
    public void testSafeArrayAccess() throws Exception {

        /*
         * test List access
         */

        asserter.assertExpression("var list = null; list?[1]", null);
    }

    @Test
    public void testArrayLengthPropertyAccess() throws Exception {

        /*
         * test List access
         */

        asserter.assertExpression("var list = [1,2,3]; list.length", 3);
    }

}