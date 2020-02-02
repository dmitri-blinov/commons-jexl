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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the class literal.
 *
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ClassLiteralTest extends JexlTestCase {

    public ClassLiteralTest() {
        super("ClassLiteralTest");
    }

    @Test
    public void testPrimitive() throws Exception {
        JexlContext jc = new MapContext();
        JexlScript e = JEXL.createScript("byte.class");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", Byte.TYPE, o);
    }

    @Test
    public void testClass() throws Exception {
        JexlContext jc = new MapContext();
        JexlScript e = JEXL.createScript("Integer.class");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", Integer.class, o);
    }
}
