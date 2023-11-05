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

import org.apache.commons.jexl3.junit.Asserter;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests shift operators.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class DeleteOperatorTest extends JexlTestCase {

    private Asserter asserter;

    public DeleteOperatorTest() {
        super("DeleteOperatorTest", new JexlBuilder().cache(512).arithmetic(new DeleteArithmetic(false)).create());
    }

    public static class DeleteArithmetic extends JexlArithmetic {
        DeleteArithmetic(boolean flag) {
            super(flag);
        }

        public Object propertyDelete(StringBuilder s, Integer index) {
            s.deleteCharAt(index);
            return JexlOperator.ASSIGN;
        }

        public Object arrayDelete(StringBuilder s, Integer index) {
            s.deleteCharAt(index);
            return JexlOperator.ASSIGN;
        }
    }

    @Test
    public void testMapDelete() throws Exception {

        Map<String, Object> map = new HashMap<String, Object> ();
        map.put("key", 42);


        JexlContext jc = new MapContext();
        jc.set("map", map);

        final JexlScript script = JEXL.createScript("delete map.key");
        final Object result = script.execute(jc);
        Assert.assertTrue(map.isEmpty());

    }


    @Test
    public void testListDelete() throws Exception {

        List<String> list = new ArrayList<String> ();
        list.add("key");


        JexlContext jc = new MapContext();
        jc.set("list", list);

        final JexlScript script = JEXL.createScript("delete list[0]");
        final Object result = script.execute(jc);

        Assert.assertEquals(0, list.size());

    }


    public class DuckTyped {

        protected List<String> list = new ArrayList<String> ();

        public DuckTyped() {
           list.add("key");
        }

        public int size() {
           return list.size();
        }

        public void remove(Integer value) {
            list.remove((int) value);
        }
    }

    @Test
    public void testDuckTypedDelete() throws Exception {
        JexlScript e = JEXL.createScript("delete x[0]");
        JexlContext jc = new MapContext();
        DuckTyped x = new DuckTyped();
        jc.set("x", x);
        e.execute(jc);
        Assert.assertEquals("Result is not as expected", 0, x.size());
    }

    @Test
    public void testArrayDeleteOverride() throws Exception {
        JexlScript s = JEXL.createScript("delete x[0]");
        JexlContext jc = new MapContext();
        StringBuilder value = new StringBuilder("foo");
        jc.set("x", value);
        s.execute(jc);

        Assert.assertEquals("Result is not as expected", 2, value.length());
    }

    @Test
    public void testPropertyDeleteOverride() throws Exception {
        JexlScript s = JEXL.createScript("delete x.0");
        JexlContext jc = new MapContext();
        StringBuilder value = new StringBuilder("bar");
        jc.set("x", value);
        s.execute(jc);

        Assert.assertEquals("Result is not as expected", 2, value.length());
    }

    @Test
    public void testBadSyntax() throws Exception {

        try {
           JEXL.createScript("delete x");
           Assert.fail("Should have property or array accessor");
        } catch (Exception ex) {
          //
        }

        try {
           JEXL.createScript("delete x.foo()");
           Assert.fail("Should use assignable syntax");
        } catch (Exception ex) {
          //
        }
    }

}
