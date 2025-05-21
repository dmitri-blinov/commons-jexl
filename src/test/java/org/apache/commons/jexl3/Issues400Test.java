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

import static org.apache.commons.jexl3.introspection.JexlPermissions.RESTRICTED;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Test;

import org.apache.commons.jexl3.introspection.JexlPermissions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

/**
 * Test cases for reported issue between JEXL-400 and JEXL-499.
 */
public class Issues400Test {

    @Test
    public void test402() {
        final JexlContext jc = new MapContext();
      // @formatter:off
      final String[] sources = {
        "if (true) { return }",
        "if (true) { 3; return }",
        "(x->{ 3; return })()"
      };
      // @formatter:on
        final JexlEngine jexl = new JexlBuilder().create();
        for (final String source : sources) {
            final JexlScript e = jexl.createScript(source);
            final Object o = e.execute(jc);
            assertNull(o);
        }
    }

    @Test
    public void test403() {
        // @formatter:off
        final String[] strings = {
            "  map1.`${item.a}` = 1;\n",
            "  map1[`${item.a}`] = 1;\n",
            "  map1[item.a] = 1;\n"
         };
        // @formatter:on
        for (final String setmap : strings) {
            // @formatter:off
            final String src = "var a = {'a': 1};\n" +
                "var list = [a, a];\n" +
                "let map1 = {:};\n" +
                "for (var item : list) {\n" +
                setmap +
                "}\n " +
                "map1";
            // @formatter:on
            final JexlEngine jexl = new JexlBuilder().cache(64).create();
            final JexlScript script = jexl.createScript(src);
            for (int i = 0; i < 2; ++i) {
                final Object result = script.execute(null);
                assertTrue(result instanceof Map);
                final Map<?, ?> map = (Map<?, ?>) result;
                assertEquals(1, map.size());
                assertTrue(map.containsKey(1));
                assertTrue(map.containsValue(1));
            }
        }
    }

    @Test
    public void test404a() {
      final JexlEngine jexl = new JexlBuilder()
          .cache(64)
          .strict(true)
          .safe(false)
          .create();
      Map<String,Object> a = Collections.singletonMap("b", 42);
      // access is constant
      for(String src : new String[]{ "a.b", "a?.b", "a['b']", "a?['b']", "a?.`b`"}) {
        run404(jexl, src, a);
        run404(jexl, src + ";", a);
      }
      // access is variable
      for(String src : new String[]{ "a[b]", "a?[b]", "a?.`${b}`"}) {
        run404(jexl, src, a, "b");
        run404(jexl, src + ";", a, "b");
      }
      // add a 3rd access
      Map<String,Object> b = Collections.singletonMap("c", 42);
      a = Collections.singletonMap("b", b);
      for(String src : new String[]{ "a[b].c", "a?[b]?['c']", "a?.`${b}`.c"}) {
        run404(jexl, src, a, "b");
      }
    }

    private static void run404(JexlEngine jexl, String src, Object...a) {
      try {
        JexlScript script = jexl.createScript(src, "a", "b");
        if (!src.endsWith(";")) {
          Assert.assertEquals(script.getSourceText(), script.getParsedText());
        }
        Object result = script.execute(null, a);
        Assert.assertEquals(42, result);
      } catch(JexlException.Parsing xparse) {
        Assert.fail(src);
      }
    }

    @Test
    public void test404b() {
      final JexlEngine jexl = new JexlBuilder()
          .cache(64)
          .strict(true)
          .safe(false)
          .create();
      Map<String, Object> b = Collections.singletonMap("c", 42);
      Map<String, Object> a = Collections.singletonMap("b", b);
      JexlScript script;
      Object result = -42;
      script = jexl.createScript("a?['B']?['C']", "a");
      result = script.execute(null, a);
      Assert.assertEquals(script.getSourceText(), script.getParsedText());
      Assert.assertNull(result);
      script = jexl.createScript("a?['b']?['C']", "a");
      Assert.assertEquals(script.getSourceText(), script.getParsedText());
      result = script.execute(null, a);
      Assert.assertNull(result);
      script = jexl.createScript("a?['b']?['c']", "a");
      Assert.assertEquals(script.getSourceText(), script.getParsedText());
      result = script.execute(null, a);
      Assert.assertEquals(42, result);
      script = jexl.createScript("a?['B']?['C']?: 1042", "a");
      Assert.assertEquals(script.getSourceText(), script.getParsedText());
      result = script.execute(null, a);
      Assert.assertEquals(1042, result);
      // can still do ternary, note the space between ? and [
      script = jexl.createScript("a? ['B']:['C']", "a");
      result = script.execute(null, a);
      Assert.assertArrayEquals(new String[]{"B"}, (String[]) result);
      script = jexl.createScript("a?['b'] ?: ['C']", "a");
      result = script.execute(null, a);
      Assert.assertEquals(b, result);
      script = jexl.createScript("a?['B'] ?: ['C']", "a");
      result = script.execute(null, a);
      Assert.assertArrayEquals(new String[]{"C"}, (String[]) result);
    }

  /**
   * Any function in a context can be used as a method of its first parameter.
   * Overloads are respected.
   */
  public static class XuContext extends MapContext {

    public String join(Iterator<?> iterator, String str) {
      if (!iterator.hasNext()) {
        return "";
      }
      StringBuilder strb = new StringBuilder(256);
      strb.append(iterator.next().toString());
      while(iterator.hasNext()) {
        strb.append(str);
        strb.append(Objects.toString(iterator.next(), "?"));
      }
      return strb.toString();
    }

    public String join(Iterable<?> list, String str) {
      return join(list.iterator(), str);
    }

    public String join(int[] list, String str) {
      return join(Arrays.stream(list).iterator(), str);
    }
  }

  @Test
  public void test406a() {
    final JexlEngine jexl = new JexlBuilder()
        .cache(64)
        .strict(true)
        .safe(false)
        .create();

    JexlContext context = new XuContext();
    for(String src : Arrays.asList(
        "[1, 2, 3, 4, ...].join('-')", // List<Integer>
        "[1, 2, 3, 4,].join('-')", // int[]
        "(1 .. 4).join('-')", // iterable<Integer>
        "join([1, 2, 3, 4, ...], '-')",
        "join([1, 2, 3, 4], '-')",
        "join((1 .. 4), '-')")) {
      JexlScript script = jexl.createScript(src);
      Object result = script.execute(context);
      Assert.assertEquals(src,"1-2-3-4", result);
    }

    String src0 = "x.join('*')";
    JexlScript script0 = jexl.createScript(src0, "x");
    String src1 = "join(x, '*')";
    JexlScript script1 = jexl.createScript(src1, "x");
    for(Object x : Arrays.asList(
        Arrays.asList(1, 2, 3, 4),
        new int[]{1, 2, 3, 4})) {
      Object result = script0.execute(context, x);
      Assert.assertEquals(src0, "1*2*3*4", result);
      result = script1.execute(context, x);
      Assert.assertEquals(src1, "1*2*3*4", result);
    }
  }

  @Test
  public void test407() {
    // Java version
    double r = 99.0d + 7.82d -99.0d -7.82d;
    Assert.assertEquals(0d, r, 8.e-15); // Not zero, IEEE 754
    // jexl
    final JexlEngine jexl = new JexlBuilder().create();
    JexlScript script = jexl.createScript("a + b - a - b", "a", "b");
    // using doubles, same as Java
    Number result = (Number) script.execute(null, 99.0d, 7.82d);
    Assert.assertEquals(0d, result.doubleValue(), 8.e-15);
    // using BigdDecimal, more precise, still not zero
    result = (Number) script.execute(null, new BigDecimal(99.0d), new BigDecimal(7.82d));
    Assert.assertEquals(0d, result.doubleValue(), 3.e-32);
  }


  @Test
  public void test412() {
    Map<Object,Object> ctl = new HashMap<>();
    ctl.put("one", 1);
    ctl.put("two", 2);
    final String fnsrc0 = "function f(x) { x }\n" +
        "let one = 'one', two = 'two';\n";
    for(String map0 : Arrays.asList(
        "{ one : f(1), two:f(2) }",
        "{ one: f(1), two: f(2) }",
        "{ one: f(1), two:f(2) }",
        "{ one :f(1), two:f(2) }")) {
      final String fnsrc = fnsrc0 + map0;
      final JexlContext jc = new MapContext();
      final JexlEngine jexl = new JexlBuilder().create();
      try {
        final JexlScript e = jexl.createScript(fnsrc);
        final Object o = e.execute(jc);
        Assert.assertTrue(o instanceof Map);
        Map<?, ?> map = (Map<?, ?>) o;
        Assert.assertEquals(map, ctl);
      } catch (JexlException.Parsing xparse) {
        Assert.fail(fnsrc + " : " + xparse.getMessage());
      } catch (JexlException xother) {
        Assert.fail(fnsrc + " : " + xother.getMessage());
      }
    }
  }
}
