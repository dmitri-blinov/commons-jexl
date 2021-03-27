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
import java.util.Map;
import java.util.StringTokenizer;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the for field access operator
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class FieldAccessTest extends JexlTestCase {

    public class FieldObject {
        public String z;
        public String y;
        public String x;

        public String getX() {
           return z;
        }

        public void setX(String val) {
           z = val;
        }
    }

    public class DummyObject {
        protected Object v;
    }


    /** create a named test */
    public FieldAccessTest() {
        super("FieldAccessTest", new JexlBuilder().cache(512).arithmetic(new FieldArithmetic(false)).create());
    }

    public static class FieldArithmetic extends JexlArithmetic {
        FieldArithmetic(boolean flag) {
            super(flag);
        }

        public Object fieldGet(DummyObject o, Object key) {
            return key;
        }

        public Object fieldSet(DummyObject o, Object key, Object value) {
            o.v = value;
            return o;
        }
    }


    @Test
    public void testSimpleAccess() throws Exception {
        JexlScript e = JEXL.createScript("x.@y");
        FieldObject x = new FieldObject();
        x.y = "hello";
        JexlContext jc = new MapContext();
        jc.set("x", x);
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", "hello", o);
    }

    @Test
    public void testSimpleAssignAccess() throws Exception {
        JexlScript e = JEXL.createScript("x.@y = 'hello'");
        FieldObject x = new FieldObject();
        JexlContext jc = new MapContext();
        jc.set("x", x);
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", "hello", x.y);
    }

    @Test
    public void testPropertyShadowedAccess() throws Exception {
        JexlScript e = JEXL.createScript("x.@x");
        FieldObject x = new FieldObject();
        x.z = "world";
        x.x = "hello";
        JexlContext jc = new MapContext();
        jc.set("x", x);
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", "hello", o);
    }

    @Test
    public void testPropertyShadowedAssignAccess() throws Exception {
        JexlScript e = JEXL.createScript("x.@x = 'hello'");
        FieldObject x = new FieldObject();
        x.z = "world";
        JexlContext jc = new MapContext();
        jc.set("x", x);
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", "hello", x.x);
    }

    @Test
    public void testFieldOverloadedAccess() throws Exception {
        JexlScript e = JEXL.createScript("x.@x");
        DummyObject x = new DummyObject();
        JexlContext jc = new MapContext();
        jc.set("x", x);
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", "x", o);
    }

    @Test
    public void testFieldOverloadedAssignAccess() throws Exception {
        JexlScript e = JEXL.createScript("x.@x = 'hello'");
        DummyObject x = new DummyObject();
        JexlContext jc = new MapContext();
        jc.set("x", x);
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", "hello", x.v);
    }

}
