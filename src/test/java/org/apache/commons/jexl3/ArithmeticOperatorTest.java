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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.jexl3.junit.Asserter;

import java.io.StringWriter;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * Tests for the startsWith, endsWith, match and range operators.
 * @since 3.0
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ArithmeticOperatorTest extends JexlTestCase {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private Asserter asserter;

    @Before
    @Override
    public void setUp() {
        asserter = new Asserter(JEXL);
        asserter.setStrict(false);
    }

    /**
     * Create the named test.
     */
    public ArithmeticOperatorTest() {
        super("ArithmeticOperatorTest");
    }

    @Test
    public void testRegexp() throws Exception {
        asserter.setVariable("str", "abc456");
        asserter.assertExpression("str =~ '.*456'", Boolean.TRUE);
        asserter.assertExpression("str !~ 'ABC.*'", Boolean.TRUE);
        asserter.setVariable("match", "abc.*");
        asserter.setVariable("nomatch", ".*123");
        asserter.assertExpression("str =~ match", Boolean.TRUE);
        asserter.assertExpression("str !~ match", Boolean.FALSE);
        asserter.assertExpression("str !~ nomatch", Boolean.TRUE);
        asserter.assertExpression("str =~ nomatch", Boolean.FALSE);
        asserter.setVariable("match", new StringBuilder("abc.*"));
        asserter.setVariable("nomatch", new StringBuilder(".*123"));
        asserter.assertExpression("str =~ match", Boolean.TRUE);
        asserter.assertExpression("str !~ match", Boolean.FALSE);
        asserter.assertExpression("str !~ nomatch", Boolean.TRUE);
        asserter.assertExpression("str =~ nomatch", Boolean.FALSE);
        asserter.setVariable("match", java.util.regex.Pattern.compile("abc.*"));
        asserter.setVariable("nomatch", java.util.regex.Pattern.compile(".*123"));
        asserter.assertExpression("str =~ match", Boolean.TRUE);
        asserter.assertExpression("str !~ match", Boolean.FALSE);
        asserter.assertExpression("str !~ nomatch", Boolean.TRUE);
        asserter.assertExpression("str =~ nomatch", Boolean.FALSE);
        // check the in/not-in variant
        asserter.assertExpression("'a' =~ ['a','b','c','d','e','f']", Boolean.TRUE);
        asserter.assertExpression("'a' !~ ['a','b','c','d','e','f']", Boolean.FALSE);
        asserter.assertExpression("'z' =~ ['a','b','c','d','e','f']", Boolean.FALSE);
        asserter.assertExpression("'z' !~ ['a','b','c','d','e','f']", Boolean.TRUE);
    }

    @Test
    public void test391() throws Exception {
        // with literals
        for(String src : Arrays.asList(
                "2 =~ [1, 2, 3, 4]",
                "[2, 3] =~ [1, 2, 3, 4]",
                "[2, 3,...] =~ [1, 2, 3, 4]",
                "3 =~ [1, 2, 3, 4,...]",
                "[2, 3] =~ [1, 2, 3, 4,...]",
                "[2, 3,...] =~ [1, 2, 3, 4,...]")) {
            asserter.assertExpression(src, Boolean.TRUE);
        }
        // with variables
        int[] ic = new int[]{1, 2,  3, 4};
        List<Integer> iic = new ArrayList<>();
        for(int v : ic) { iic.add(v); }
        int[] iv = new int[]{2, 3};
        List<Integer> iiv = new ArrayList<>();
        for(int v : iv) { iiv.add(v); }
        String src = "(x,y) -> x =~ y ";
        for(Object v : Arrays.asList(iv, iiv, 2)) {
            for(Object c : Arrays.asList(ic, iic)) {
                asserter.assertExpression(src, Boolean.TRUE, v, c);
            }
        }
    }

    @Test
    public void testRegexp2() throws Exception {
        asserter.setVariable("str", "abc456");
        asserter.assertExpression("str =~ ~/.*456/", Boolean.TRUE);
        asserter.assertExpression("str !~ ~/ABC.*/", Boolean.TRUE);
        asserter.assertExpression("str =~ ~/abc\\d{3}/", Boolean.TRUE);
        // legacy, deprecated
        asserter.assertExpression("matches(str, ~/.*456/)", Boolean.TRUE);
        asserter.setVariable("str", "4/6");
        asserter.assertExpression("str =~ ~/\\d\\/\\d/", Boolean.TRUE);
    }

    @Test
    public void testStartsEndsWithString() throws Exception {
        asserter.setVariable("x", "foobar");
        asserter.assertExpression("x =^ 'foo'", Boolean.TRUE);
        asserter.assertExpression("x =$ 'foo'", Boolean.FALSE);
        asserter.setVariable("x", "barfoo");
        asserter.assertExpression("x =^ 'foo'", Boolean.FALSE);
        asserter.assertExpression("x =$ 'foo'", Boolean.TRUE);
    }

    @Test
    public void testStartsEndsWithStringDot() throws Exception {
        asserter.setVariable("x.y", "foobar");
        asserter.assertExpression("x.y =^ 'foo'", Boolean.TRUE);
        asserter.assertExpression("x.y =$ 'foo'", Boolean.FALSE);
        asserter.setVariable("x.y", "barfoo");
        asserter.assertExpression("x.y =^ 'foo'", Boolean.FALSE);
        asserter.assertExpression("x.y =$ 'foo'", Boolean.TRUE);
    }

    @Test
    public void testNotStartsEndsWithString() throws Exception {
        asserter.setVariable("x", "foobar");
        asserter.assertExpression("x !^ 'foo'", Boolean.FALSE);
        asserter.assertExpression("x !$ 'foo'", Boolean.TRUE);
        asserter.setVariable("x", "barfoo");
        asserter.assertExpression("x !^ 'foo'", Boolean.TRUE);
        asserter.assertExpression("x !$ 'foo'", Boolean.FALSE);
    }

    @Test
    public void testNotStartsEndsWithStringDot() throws Exception {
        asserter.setVariable("x.y", "foobar");
        asserter.assertExpression("x.y !^ 'foo'", Boolean.FALSE);
        asserter.assertExpression("x.y !$ 'foo'", Boolean.TRUE);
        asserter.setVariable("x.y", "barfoo");
        asserter.assertExpression("x.y !^ 'foo'", Boolean.TRUE);
        asserter.assertExpression("x.y !$ 'foo'", Boolean.FALSE);
    }

    @Test
    public void testStartsEndsWithStringBuilder() throws Exception {
        asserter.setVariable("x", new StringBuilder("foobar"));
        asserter.assertExpression("x =^ 'foo'", Boolean.TRUE);
        asserter.assertExpression("x =$ 'foo'", Boolean.FALSE);
        asserter.setVariable("x", new StringBuilder("barfoo"));
        asserter.assertExpression("x =^ 'foo'", Boolean.FALSE);
        asserter.assertExpression("x =$ 'foo'", Boolean.TRUE);
    }

    @Test
    public void testNotStartsEndsWithStringBuilder() throws Exception {
        asserter.setVariable("x", new StringBuilder("foobar"));
        asserter.assertExpression("x !^ 'foo'", Boolean.FALSE);
        asserter.assertExpression("x !$ 'foo'", Boolean.TRUE);
        asserter.setVariable("x", new StringBuilder("barfoo"));
        asserter.assertExpression("x !^ 'foo'", Boolean.TRUE);
        asserter.assertExpression("x !$ 'foo'", Boolean.FALSE);
    }

    public static class MatchingContainer {
        private final Set<Integer> values;

        public MatchingContainer(final int[] is) {
            values = new HashSet<>();
            for (final int value : is) {
                values.add(value);
            }
        }

        public boolean contains(final int value) {
            return values.contains(value);
        }
    }

    public static class IterableContainer implements Iterable<Integer> {
        private final SortedSet<Integer> values;

        public IterableContainer(final int[] is) {
            values = new TreeSet<>();
            for (final int value : is) {
                values.add(value);
            }
        }

        @Override
        public Iterator<Integer> iterator() {
            return values.iterator();
        }

        public boolean contains(final int i) {
            return values.contains(i);
        }

        public boolean contains(final int[] i) {
            for(int ii : i) if (!values.contains(ii)) return false;
            return true;
        }

        public boolean startsWith(final int i) {
            return values.first().equals(i);
        }

        public boolean endsWith(final int i) {
            return values.last().equals(i);
        }

        public boolean startsWith(final int[] i) {
            final SortedSet<Integer> sw = values.headSet(i.length);
            int n = 0;
            for (final Integer value : sw) {
                if (!value.equals(i[n++])) {
                    return false;
                }
            }
            return true;
        }
        public boolean endsWith(final int[] i) {
            final SortedSet<Integer> sw =  values.tailSet(values.size() - i.length);
            int n = 0;
            for (final Integer value : sw) {
                if (!value.equals(i[n++])) {
                    return false;
                }
            }
            return true;
        }
    }

    @Test
    public void testMatch() throws Exception {
        // check in/not-in on array, list, map, set and duck-type collection
        final int[] ai = {2, 4, 42, 54};
        final List<Integer> al = new ArrayList<>();
        for (final int i : ai) {
            al.add(i);
        }
        final Map<Integer, String> am = new HashMap<>();
        am.put(2, "two");
        am.put(4, "four");
        am.put(42, "forty-two");
        am.put(54, "fifty-four");
        final MatchingContainer ad = new MatchingContainer(ai);
        final IterableContainer ic = new IterableContainer(ai);
        final Set<Integer> as = ad.values;
        final Object[] vars = {ai, al, am, ad, as, ic};

        for (final Object variable : vars) {
            asserter.setVariable("container", variable);
            for (final int x : ai) {
                asserter.setVariable("x", x);
                asserter.assertExpression("x =~ container", Boolean.TRUE);
            }
            asserter.setVariable("x", 169);
            asserter.assertExpression("x !~ container", Boolean.TRUE);
        }
    }

    @Test
    public void testStartsEndsWith() throws Exception {
        asserter.setVariable("x", "foobar");
        asserter.assertExpression("x =^ 'foo'", Boolean.TRUE);
        asserter.assertExpression("x =$ 'foo'", Boolean.FALSE);
        asserter.setVariable("x", "barfoo");
        asserter.assertExpression("x =^ 'foo'", Boolean.FALSE);
        asserter.assertExpression("x =$ 'foo'", Boolean.TRUE);

        final int[] ai = {2, 4, 42, 54};
        final IterableContainer ic = new IterableContainer(ai);
        asserter.setVariable("x", ic);
        asserter.assertExpression("x =^ 2", Boolean.TRUE);
        asserter.assertExpression("x =$ 54", Boolean.TRUE);
        asserter.assertExpression("x =^ 4", Boolean.FALSE);
        asserter.assertExpression("x =$ 42", Boolean.FALSE);
        asserter.assertExpression("x =^ [2, 4]", Boolean.TRUE);
        asserter.assertExpression("x =^ [42, 54]", Boolean.TRUE);
    }

    @Test
    public void testNotStartsEndsWith() throws Exception {
        asserter.setVariable("x", "foobar");
        asserter.assertExpression("x !^ 'foo'", Boolean.FALSE);
        asserter.assertExpression("x !$ 'foo'", Boolean.TRUE);
        asserter.setVariable("x", "barfoo");
        asserter.assertExpression("x !^ 'foo'", Boolean.TRUE);
        asserter.assertExpression("x !$ 'foo'", Boolean.FALSE);

        final int[] ai = {2, 4, 42, 54};
        final IterableContainer ic = new IterableContainer(ai);
        asserter.setVariable("x", ic);
        asserter.assertExpression("x !^ 2", Boolean.FALSE);
        asserter.assertExpression("x !$ 54", Boolean.FALSE);
        asserter.assertExpression("x !^ 4", Boolean.TRUE);
        asserter.assertExpression("x !$ 42", Boolean.TRUE);
        asserter.assertExpression("x !^ [2, 4]", Boolean.FALSE);
        asserter.assertExpression("x !^ [42, 54]", Boolean.FALSE);
    }

    public static class Aggregate {
        private Aggregate() {}
        public static int sum(final Iterable<Integer> ii) {
            int sum = 0;
            for(final Integer i : ii) {
                sum += i;
            }
            return sum;
        }
    }

    @Test
    public void testIncrementOperatorOnNull() {
        final JexlEngine jexl = new JexlBuilder().strict(false).create();
        JexlScript script;
        Object result;
        script = jexl.createScript("var i = null; ++i");
        result = script.execute(null);
        assertEquals(1, result);
        script = jexl.createScript("var i = null; --i");
        result = script.execute(null);
        assertEquals(-1, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInterval() {
        final Map<String, Object> ns = new HashMap<>();
        ns.put("calc", Aggregate.class);
        final JexlEngine jexl = new JexlBuilder().namespaces(ns).create();
        JexlScript script;
        Object result;

        script = jexl.createScript("1 .. 3");
        result = script.execute(null);
        Assert.assertTrue(result instanceof Iterable<?>);
        Iterator<Integer> ii = ((Iterable<Integer>) result).iterator();
        Assert.assertEquals(Integer.valueOf(1), ii.next());
        Assert.assertEquals(Integer.valueOf(2), ii.next());
        Assert.assertEquals(Integer.valueOf(3), ii.next());

        script = jexl.createScript("(4 - 3) .. (9 / 3)");
        result = script.execute(null);
        Assert.assertTrue(result instanceof Iterable<?>);
        ii = ((Iterable<Integer>) result).iterator();
        Assert.assertEquals(Integer.valueOf(1), ii.next());
        Assert.assertEquals(Integer.valueOf(2), ii.next());
        Assert.assertEquals(Integer.valueOf(3), ii.next());

        // sum of 1, 2, 3
        script = jexl.createScript("var x = 0; for(var y : ((5 - 4) .. (12 / 4))) { x = x + y }; x");
        result = script.execute(null);
        Assert.assertEquals(Integer.valueOf(6), result);

        script = jexl.createScript("calc:sum(1 .. 3)");
        result = script.execute(null);
        Assert.assertEquals(Integer.valueOf(6), result);

        script = jexl.createScript("calc:sum(-3 .. 3)");
        result = script.execute(null);
        Assert.assertEquals(Integer.valueOf(0), result);
    }

    public static class DateArithmetic extends JexlArithmetic {
        DateArithmetic(final boolean flag) {
            super(flag);
        }

        protected Object getDateValue(final Date date, final String key) {
            try {
                final Calendar cal = Calendar.getInstance(UTC);
                cal.setTime(date);
                if ("yyyy".equals(key)) {
                    return cal.get(Calendar.YEAR);
                }
                if ("MM".equals(key)) {
                    return cal.get(Calendar.MONTH) + 1;
                }
                if ("dd".equals(key)) {
                    return cal.get(Calendar.DAY_OF_MONTH);
                }
                // Otherwise treat as format mask
                final SimpleDateFormat df = new SimpleDateFormat(key);//, dfs);
                return df.format(date);

            } catch (final Exception ex) {
                return null;
            }
        }

        protected Object setDateValue(final Date date, final String key, final Object value) {
            final Calendar cal = Calendar.getInstance(UTC);
            cal.setTime(date);
            if ("yyyy".equals(key)) {
                cal.set(Calendar.YEAR, toInteger(value));
            } else if ("MM".equals(key)) {
                cal.set(Calendar.MONTH, toInteger(value) - 1);
            } else if ("dd".equals(key)) {
                cal.set(Calendar.DAY_OF_MONTH, toInteger(value));
            }
            date.setTime(cal.getTimeInMillis());
            return date;
        }

        public Object propertyGet(final Date date, final String identifier) {
            return getDateValue(date, identifier);
        }

        public Object propertySet(final Date date, final String identifier, final Object value) throws Exception {
            return setDateValue(date, identifier, value);
        }

        public Object arrayGet(final Date date, final String identifier) {
            return getDateValue(date, identifier);
        }

        public Object arraySet(final Date date, final String identifier, final Object value) throws Exception {
            return setDateValue(date, identifier, value);
        }

        public Date now() {
            return new Date(System.currentTimeMillis());
        }

        public Date multiply(final Date d0, final Date d1) {
            throw new ArithmeticException("unsupported");
        }
    }

    public static class DateContext extends MapContext {
        private Locale locale = Locale.US;

        void setLocale(final Locale l10n) {
            this.locale = l10n;
        }

        public String format(final Date date, final String fmt) {
            final SimpleDateFormat sdf = new SimpleDateFormat(fmt, locale);
            sdf.setTimeZone(UTC);
            return sdf.format(date);
        }

        public String format(final Number number, final String fmt) {
            return new DecimalFormat(fmt).format(number);
        }
    }

    @Test
    public void testOperatorError() throws Exception {
        runOperatorError(true);
        runOperatorError(false);
    }

    private void runOperatorError(final boolean silent) {
        final CaptureLog log = new CaptureLog();
        final DateContext jc = new DateContext();
        final Date d = new Date();
        final JexlEngine jexl = new JexlBuilder().logger(log).strict(true).silent(silent).cache(32)
                                           .arithmetic(new DateArithmetic(true)).create();
        final JexlScript expr0 = jexl.createScript("date * date", "date");
        try {
            final Object value0 = expr0.execute(jc, d);
            if (!silent) {
                Assert.fail("should have failed");
            } else {
                Assert.assertEquals(1, log.count("warn"));
            }
        } catch(final JexlException.Operator xop) {
            Assert.assertEquals("*", xop.getSymbol());
        }
        if (!silent) {
            Assert.assertEquals(0, log.count("warn"));
        }
    }

    @Test
    public void testDateArithmetic() {
        final Date d = new Date();
        final JexlContext jc = new MapContext();
        final JexlEngine jexl = new JexlBuilder().cache(32).arithmetic(new DateArithmetic(true)).create();
        final JexlScript expr0 = jexl.createScript("date.yyyy = 1969; date.MM=7; date.dd=20; ", "date");
        Object value0 = expr0.execute(jc, d);
        Assert.assertNotNull(value0);
        value0 = d;
        //d = new Date();
        Assert.assertEquals(1969, jexl.createScript("date.yyyy", "date").execute(jc, value0));
        Assert.assertEquals(7, jexl.createScript("date.MM", "date").execute(jc, value0));
        Assert.assertEquals(20, jexl.createScript("date.dd", "date").execute(jc, value0));
    }

    @Test
    public void testFormatArithmetic() {
        final Calendar cal = Calendar.getInstance(UTC);
        cal.set(1969, Calendar.AUGUST, 20);
        final Date x0 = cal.getTime();
        final String y0 =  "MM/yy/dd";
        final Number x1 = 42.12345;
        final String y1 = "##0.##";
        final DateContext jc = new DateContext();
        final JexlEngine jexl = new JexlBuilder().cache(32).arithmetic(new DateArithmetic(true)).create();
        final JexlScript expr0 = jexl.createScript("x.format(y)", "x", "y");
        Object value10 = expr0.execute(jc, x0, y0);
        final Object value20 = expr0.execute(jc, x0, y0);
        Assert.assertEquals(value10, value20);
        Object value11 = expr0.execute(jc, x1, y1);
        final Object value21 = expr0.execute(jc, x1, y1);
        Assert.assertEquals(value11, value21);
        value10 = expr0.execute(jc, x0, y0);
        Assert.assertEquals(value10, value20);
        value11 = expr0.execute(jc, x1, y1);
        Assert.assertEquals(value11, value21);
        value10 = expr0.execute(jc, x0, y0);
        Assert.assertEquals(value10, value20);
        value11 = expr0.execute(jc, x1, y1);
        Assert.assertEquals(value11, value21);

        JexlScript expr1 = jexl.createScript("format(x, y)", "x", "y");
        value10 = expr1.execute(jc, x0, y0);
        Assert.assertEquals(value10, value20);
        Object s0 = expr1.execute(jc, x0, "EEE dd MMM yyyy");
        Assert.assertEquals("Wed 20 Aug 1969", s0);
        jc.setLocale(Locale.FRANCE);
        s0 = expr1.execute(jc, x0, "EEE dd MMM yyyy");
        Assert.assertEquals("mer. 20 ao\u00fbt 1969", s0);

        expr1 = jexl.createScript("format(now(), y)", "y");
        final Object n0 = expr1.execute(jc, y0);
        Assert.assertNotNull(n0);
        expr1 = jexl.createScript("now().format(y)", "y");
        final Object n1 = expr1.execute(jc, y0);
        Assert.assertNotNull(n0);
        Assert.assertEquals(n0, n1);
    }

    @Test
    public void testFormatArithmeticJxlt() throws Exception {
        final Map<String, Object> ns = new HashMap<>();
        ns.put("calc", Aggregate.class);
        final Calendar cal = Calendar.getInstance(UTC);
        cal.set(1969, Calendar.AUGUST, 20);
        final Date x0 = cal.getTime();
        final String y0 =  "yyyy-MM-dd";
        final DateContext jc = new DateContext();
        final JexlEngine jexl = new JexlBuilder().cache(32).namespaces(ns).arithmetic(new DateArithmetic(true)).create();
        final JxltEngine jxlt = jexl.createJxltEngine();

        JxltEngine.Template expr0 = jxlt.createTemplate("${x.format(y)}", "x", "y");
        StringWriter strw = new StringWriter();
        expr0.evaluate(jc, strw, x0, y0);
        String strws = strw.toString();
        Assert.assertEquals("1969-08-20", strws);

        expr0 = jxlt.createTemplate("${calc:sum(x .. y)}", "x", "y");
        strw = new StringWriter();
        expr0.evaluate(jc, strw, 1, 3);
        strws = strw.toString();
        Assert.assertEquals("6", strws);

        final JxltEngine.Template expr1 = jxlt.createTemplate("${jexl:include(s, x, y)}", "s", "x", "y");
        strw = new StringWriter();
        expr1.evaluate(jc, strw, expr0, 1, 3);
        strws = strw.toString();
        Assert.assertEquals("6", strws);

        expr0 = jxlt.createTemplate("${now().format(y)}", "y");
        strw = new StringWriter();
        expr0.evaluate(jc, strw, y0);
        strws = strw.toString();
        Assert.assertNotNull(strws);
    }


    /**
     * A comparator using an evaluated expression on objects as comparison arguments.
     */
    private static class PropertyComparator implements JexlCache.Reference, Comparator<Object> {
        private final JexlContext context = JexlEngine.getThreadContext();
        private final JexlArithmetic arithmetic;
        private final JexlOperator.Uberspect operator;
        private final JexlScript expr;
        private Object cache;

        PropertyComparator(JexlArithmetic jexla, JexlScript expr) {
            this.arithmetic = jexla;
            this.operator = JexlEngine.getThreadEngine().getUberspect().getOperator(arithmetic);
            this.expr = expr;
        }
        @Override
        public int compare(Object o1, Object o2) {
            final Object left = expr.execute(context, o1);
            final Object right = expr.execute(context, o2);
            Object result = operator.tryOverload(this, JexlOperator.COMPARE, left, right);
            if (result instanceof Integer) {
                return (int) result;
            }
            return arithmetic.compare(left, right, JexlOperator.COMPARE);
        }

        @Override
        public Object getCache() {
            return cache;
        }

        @Override
        public void setCache(Object cache) {
            this.cache = cache;
        }
    }

    public static class SortingArithmetic extends JexlArithmetic {
        public SortingArithmetic(boolean strict) {
            this( strict, null, Integer.MIN_VALUE);
        }

        private SortingArithmetic(boolean strict, MathContext context, int scale) {
            super(strict, context, scale);
        }

        public int compare(Integer left, Integer right) {
            return left.compareTo(right);
        }

        public int compare(String left, String right) {
            return left.compareTo(right);
        }

        /**
         * Sorts an array using a script to evaluate the property used to compare elements.
         * @param array the elements array
         * @param expr the property evaluation lambda
         */
        public void sort(final Object[] array, final JexlScript expr) {
            Arrays.sort(array, new PropertyComparator(this, expr));
        }
    }

    @Test
    public void testSortArray() {
        final JexlEngine jexl = new JexlBuilder()
                .cache(32)
                .arithmetic(new SortingArithmetic(true))
                .silent(false).create();
        // test data, json like
        final String src = "[{'id':1,'name':'John','type':9},{'id':2,'name':'Doe','type':7},{'id':3,'name':'Doe','type':10}]";
        final Object a =  jexl.createExpression(src).evaluate(null);
        assertNotNull(a);
        // row 0 and 1 are not ordered
        final Map[] m = (Map[]) a;
        assertEquals(9, m[0].get("type"));
        assertEquals(7, m[1].get("type"));
        // sort the elements on the type
        jexl.createScript("array.sort( e -> e.type )", "array").execute(null, a);
        // row 0 and 1 are now ordered
        assertEquals(7, m[0].get("type"));
        assertEquals(9, m[1].get("type"));
    }


    public static class MatchingArithmetic extends JexlArithmetic {
        public MatchingArithmetic(final boolean astrict) {
            super(astrict);
        }

        public boolean contains(final Pattern[] container, final String str) {
            for(final Pattern pattern : container) {
                if (pattern.matcher(str).matches()) {
                    return true;
                }
            }
            return false;
        }
    }

    @Test
    public void testPatterns() {
        final JexlEngine jexl = new JexlBuilder().arithmetic(new MatchingArithmetic(true)).create();
        final JexlScript script = jexl.createScript("str =~ [~/abc.*/, ~/def.*/]", "str");
        assertTrue((boolean) script.execute(null, "abcdef"));
        assertTrue((boolean) script.execute(null, "defghi"));
        assertFalse((boolean) script.execute(null, "ghijkl"));
    }


    public static class Arithmetic428 extends JexlArithmetic {
        public Arithmetic428(boolean strict) {
            this( strict, null, Integer.MIN_VALUE);
        }

        private Arithmetic428(boolean strict, MathContext context, int scale) {
            super(strict, context, scale);
        }

        public int compare(Instant lhs, String str) {
            Instant rhs = Instant.parse(str);
            return lhs.compareTo(rhs);
        }
    }

    static final List<Integer> LOOPS = new ArrayList<>(Arrays.asList(0, 1));

    @Test
    public void test428() {
        // see JEXL-428
        final JexlEngine jexl = new JexlBuilder().cache(32).arithmetic(new Arithmetic428(true)).create();
        final String rhsstr ="2024-09-09T10:42:42.00Z";
        final Instant rhs = Instant.parse(rhsstr);
        final String lhs = "2020-09-09T01:24:24.00Z";
        JexlScript script;
        script = jexl.createScript("x < y", "x", "y");
        final JexlScript s0 = script;
        assertThrows(JexlException.class, () -> s0.execute(null, 42, rhs));

        for(int i : LOOPS) { assertTrue((boolean) script.execute(null, lhs, rhs)); }
        for(int i : LOOPS) { assertTrue((boolean) script.execute(null, lhs, rhs)); }
        for(int i : LOOPS) { assertFalse((boolean) script.execute(null, rhs, lhs)); }
        for(int i : LOOPS) { assertFalse((boolean) script.execute(null, rhs, lhs)); }
        for(int i : LOOPS) { assertTrue((boolean) script.execute(null, lhs, rhs)); }
        for(int i : LOOPS) { assertFalse((boolean) script.execute(null, rhs, lhs)); }

        script = jexl.createScript("x <= y", "x", "y");
        final JexlScript s1 = script;
        assertThrows(JexlException.class, () -> s1.execute(null, 42, rhs));
        assertTrue((boolean) script.execute(null, lhs, rhs));
        assertFalse((boolean) script.execute(null, rhs, lhs));

        script = jexl.createScript("x >= y", "x", "y");
        final JexlScript s2 = script;
        assertThrows(JexlException.class, () -> s2.execute(null, 42, rhs));
        assertFalse((boolean) script.execute(null, lhs, rhs));
        assertFalse((boolean) script.execute(null, lhs, rhs));
        assertTrue((boolean) script.execute(null, rhs, lhs));
        assertTrue((boolean) script.execute(null, rhs, lhs));
        assertFalse((boolean) script.execute(null, lhs, rhs));
        assertTrue((boolean) script.execute(null, rhs, lhs));

        script = jexl.createScript("x > y", "x", "y");
        final JexlScript s3 = script;
        assertThrows(JexlException.class, () -> s3.execute(null, 42, rhs));
        assertFalse((boolean) script.execute(null, lhs, rhs));
        assertTrue((boolean) script.execute(null, rhs, lhs));

        script = jexl.createScript("x == y", "x", "y");
        assertFalse((boolean) script.execute(null, 42, rhs));
        assertFalse((boolean) script.execute(null, lhs, rhs));
        assertFalse((boolean) script.execute(null, lhs, rhs));
        assertTrue((boolean) script.execute(null, rhs, rhsstr));
        assertTrue((boolean) script.execute(null, rhsstr, rhs));
        assertFalse((boolean) script.execute(null, lhs, rhs));

        script = jexl.createScript("x != y", "x", "y");
        assertTrue((boolean) script.execute(null, 42, rhs));
        assertTrue((boolean) script.execute(null, lhs, rhs));
        assertFalse((boolean) script.execute(null, rhs, rhsstr));
    }

    public static class Arithmetic429 extends JexlArithmetic {
        public Arithmetic429(boolean astrict) {
            super(astrict);
        }

        public int compare(String lhs, Number rhs) {
            return lhs.compareTo(rhs.toString());
        }
    }

    @Test
    public void test429a() {
        final JexlEngine jexl = new JexlBuilder()
                .arithmetic(new Arithmetic429(true))
                .cache(32)
                .create();
        String src;
        JexlScript script;
        src = "'1.1' > 0";
        script = jexl.createScript(src);
        assertTrue((boolean) script.execute(null));
        src = "1.2 <= '1.20'";
        script = jexl.createScript(src);
        assertTrue((boolean) script.execute(null));
        src = "1.2 >= '1.2'";
        script = jexl.createScript(src);
        assertTrue((boolean) script.execute(null));
        src = "1.2 < '1.2'";
        script = jexl.createScript(src);
        assertFalse((boolean) script.execute(null));
        src = "1.2 > '1.2'";
        script = jexl.createScript(src);
        assertFalse((boolean) script.execute(null));
        src = "1.20 == 'a'";
        script = jexl.createScript(src);
        assertFalse((boolean) script.execute(null));
    }
}
