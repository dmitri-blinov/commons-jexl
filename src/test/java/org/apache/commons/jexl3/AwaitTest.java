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
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;

/**
 * Tests for await function.
 * @since 4.0
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class AwaitTest extends JexlTestCase {

    public AwaitTest() {
        super("AwaitTest");
    }

    @Test
    public void testAwaitSimpleValue() throws Exception {
        final JexlContext jc = new MapContext();
        final Object o = JEXL.createExpression("await 10").evaluate(jc);
        Assert.assertEquals(10, o);
    }

    @Test
    public void testAwaitFuture() throws Exception {
        final JexlContext jc = new MapContext();
        jc.set("x", CompletableFuture.completedFuture(10));
        final Object o = JEXL.createExpression("await x").evaluate(jc);
        Assert.assertEquals(10, o);
    }

}
