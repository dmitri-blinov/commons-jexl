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
 * Tests for pipe operator.
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class PipeTest extends JexlTestCase {

    public PipeTest() {
        super("PipeTest");
    }

    @Test
    public void testSimpleValue() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = 1; x.(@+41)").execute(jc);
        Assert.assertEquals(42, o);
    }

    @Test
    public void testNullValue() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = null; x.(@+41)").execute(jc);
        Assert.assertNull(o);
    }

    @Test
    public void testIteratorValue() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = [1,2,3]; var sum = 0; (...x).(sum += @)").execute(jc);
        Assert.assertEquals(6, o);
    }

}
