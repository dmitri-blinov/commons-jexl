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

import java.util.Collection;
import java.util.Iterator;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for text block literal.
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class TextBlockTest extends JexlTestCase {

    public TextBlockTest() {
        super("TextBlockTest");
    }

    @Test
    public void testTextBlock() throws Exception {
        JexlExpression e = JEXL.createExpression("\"\"\"\nabc\"\"\"");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Assert.assertTrue(o instanceof String);
        Assert.assertEquals("Result is not as expected", "abc", o);
    }

    @Test
    public void testTextBlockMultiline() throws Exception {
        JexlExpression e = JEXL.createExpression("\"\"\"\nabc\n\"\"\"");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Assert.assertEquals("Result is not as expected", "abc\n", o);
    }

    @Test
    public void testTextBlockEmptyline() throws Exception {
        JexlExpression e = JEXL.createExpression("\"\"\"\nabc\n   \n\"\"\"");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Assert.assertEquals("Result is not as expected", "abc\n\n", o);
    }

    @Test
    public void testTextBlockIndent() throws Exception {
        JexlExpression e = JEXL.createExpression("\"\"\"\n  abc\n  \"\"\"");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Assert.assertEquals("Result is not as expected", "abc\n", o);
    }

    @Test
    public void testTextBlockTrailingSpaces() throws Exception {
        JexlExpression e = JEXL.createExpression("\"\"\"\nabc  \n\"\"\"");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Assert.assertEquals("Result is not as expected", "abc\n", o);
    }

    @Test
    public void testTextBlockTrailingSpaces2() throws Exception {
        JexlExpression e = JEXL.createExpression("\"\"\"\nabc  \"\"\"");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Assert.assertEquals("Result is not as expected", "abc", o);
    }

    @Test
    public void testTextBlockQuotes() throws Exception {
        JexlExpression e = JEXL.createExpression("\"\"\"\none\" \"\"\"");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Assert.assertEquals("Result is not as expected", "one\"", o);
    }

    @Test
    public void testTextBlockDoubleQuotes() throws Exception {
        JexlExpression e = JEXL.createExpression("\"\"\"\ntwo\"\" \"\"\"");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Assert.assertEquals("Result is not as expected", "two\"\"", o);
    }

    @Test
    public void testTextBlockTripleQuotes() throws Exception {
        JexlExpression e = JEXL.createExpression("\"\"\"\nthree\\\"\"\" \"\"\"");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Assert.assertEquals("Result is not as expected", "three\"\"\"", o);
    }

    @Test
    public void testTextBlockLines() throws Exception {
        JexlExpression e = JEXL.createExpression("\"\"\"\nabc\\\ndef\"\"\"");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Assert.assertEquals("Result is not as expected", "abcdef", o);
    }

}
