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
public class YieldTest extends JexlTestCase {

    public YieldTest() {
        super("YieldTest");
    }

    @Test
    public void testSwitchYield() throws Exception {
        JexlScript e = JEXL.createScript("var e = switch (1) {case 1 -> {if (true) yield 42} case 2 -> {0}}");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 42, o);
    }

    @Test
    public void testValueBlock() throws Exception {
        JexlScript e = JEXL.createScript("var a = ({ x = 'Hello'; yield 'World';}); a");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Block result is wrong", "World", o);
    }

    @Test
    public void testYieldBroken() throws Exception {
        try {
            JexlScript e = JEXL.createScript("if (true) { yield 42; }");
            Assert.fail("yield is out of expression!");
        } catch (JexlException.Parsing xparse) {
            String str = xparse.detailedMessage();
            Assert.assertTrue(str.contains("yield"));
        }
    }

}
