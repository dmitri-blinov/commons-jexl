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

import java.util.Collection;
import java.util.Iterator;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for static type fields access.
 * @since 3.3
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class StaticTypeAccessTest extends JexlTestCase {

    public StaticTypeAccessTest() {
        super("StaticTypeAccessTest");
    }

    @Test
    public void testStringBuilder() throws Exception {
        JexlExpression e = JEXL.createExpression("Integer.MAX_VALUE");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Assert.assertEquals("Result is not as expected", Integer.MAX_VALUE, o);
    }
}
