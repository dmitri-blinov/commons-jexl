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

import org.apache.commons.jexl3.internal.Closure;
import org.apache.commons.jexl3.internal.Script;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Tests function/lambda/closure features.
 */
@SuppressWarnings({"AssertEqualsBetweenInconvertibleTypes"})
public class LambdaTest extends JexlTestCase {

    public LambdaTest() {
        super("LambdaTest");
    }

    @Test
    public void testScriptArguments() {
        final JexlEngine jexl = createEngine();
        final JexlScript s = jexl.createScript(" x + x ", "x");
        final JexlScript s42 = jexl.createScript("s(21)", "s");
        final Object result = s42.execute(null, s);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testScriptContext() {
        final JexlEngine jexl = createEngine();
        final JexlScript s = jexl.createScript("function(x) { x + x }");
        final String fsstr = s.getParsedText(0);
        Assert.assertEquals("x->{ x + x; }", fsstr);
        Assert.assertEquals(42, s.execute(null, 21));
        JexlScript s42 = jexl.createScript("s(21)");
        final JexlContext ctxt = new JexlEvalContext();
        ctxt.set("s", s);
        Object result = s42.execute(ctxt);
        Assert.assertEquals(42, result);
        result = s42.execute(ctxt);
        Assert.assertEquals(42, result);
        s42 = jexl.createScript("x-> { x + x }");
        result = s42.execute(ctxt, 21);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testParameterlessFunction() throws Exception {
        JexlEngine jexl = createEngine();
        String strs = "var s = function { 21 + 21 }; s()";
        JexlScript s42 = jexl.createScript(strs);
        Object result = s42.execute(null);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testLambda() {
        final JexlEngine jexl = createEngine();
        String strs = "var s = function(x) { x + x }; s(21)";
        JexlScript s42 = jexl.createScript(strs);
        Object result = s42.execute(null);
        Assert.assertEquals(42, result);
        strs = "var s = function(x, y) { x + y }; s(15, 27)";
        s42 = jexl.createScript(strs);
        result = s42.execute(null);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testStaticFunction() throws Exception {
        JexlEngine jexl = createEngine();
        String strs = "var a = 10; var s = static function { return a }; s()";
        JexlScript s42 = jexl.createScript(strs);
        JexlContext jc = new MapContext();
        jc.set("a", 42);
        Object result = s42.execute(jc);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testLambdaClosure()  {
        final JexlEngine jexl = createEngine();
        String strs = "var t = 20; var s = function(x, y) { x + y + t}; s(15, 7)";
        JexlScript s42 = jexl.createScript(strs);
        Object result = s42.execute(null);
        Assert.assertEquals(42, result);
        strs = "var t = 19; var s = function(x, y) { var t = 20; x + y + t}; s(15, 7)";
        s42 = jexl.createScript(strs);
        result = s42.execute(null);
        Assert.assertEquals(42, result);
        strs = "var t = 20; var s = function(x, y) {x + y + t}; t = 54; s(15, 7)";
        s42 = jexl.createScript(strs);
        result = s42.execute(null);
        Assert.assertEquals(42, result);
        strs = "var t = 19; var s = function(x, y) { var t = 20; x + y + t}; t = 54; s(15, 7)";
        s42 = jexl.createScript(strs);
        result = s42.execute(null);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testLambdaLambda() {
        final JexlEngine jexl = createEngine();
        String strs = "var t = 19; ( (x, y)->{ var t = 20; x + y + t} )(15, 7);";
        JexlScript s42 = jexl.createScript(strs);
        Object result = s42.execute(null);
        Assert.assertEquals(42, result);

        strs = "( (x, y)->{ ( (xx, yy)->{xx + yy } )(x, y) } )(15, 27)";
        s42 = jexl.createScript(strs);
        result = s42.execute(null);
        Assert.assertEquals(42, result);

        strs = "var t = 19; var s = (x, y)->{ var t = 20; x + y + t}; t = 54; s(15, 7)";
        s42 = jexl.createScript(strs);
        result = s42.execute(null);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testNestLambda() {
        final JexlEngine jexl = createEngine();
        final String strs = "( (x)->{ (y)->{ x + y } })(15)(27)";
        final JexlScript s42 = jexl.createScript(strs);
        final Object result = s42.execute(null);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testNestLambada() throws Exception {
        final JexlEngine jexl = createEngine();
        final String strs = "(x)->{ (y)->{ x + y } }";
        final JexlScript s42 = jexl.createScript(strs);
        final JexlScript s42b = jexl.createScript(s42.toString());
        Assert.assertEquals(s42.hashCode(), s42b.hashCode());
        Assert.assertEquals(s42, s42b);
        Object result = s42.execute(null, 15);
        Assert.assertTrue(result instanceof JexlScript);
        final Object resultb = s42.execute(null, 15);
        Assert.assertEquals(result.hashCode(), resultb.hashCode());
        Assert.assertEquals(result, resultb);
        Assert.assertEquals(result, jexl.createScript(resultb.toString(), "x").execute(null, 15));
        final JexlScript s15 = (JexlScript) result;
        final Callable<Object> s15b = s15.callable(null, 27);
        result = s15.execute(null, 27);
        Assert.assertEquals(42, result);
        result = s15b.call();
        Assert.assertEquals(42, result);
    }

    @Test
    public void testCompareLambdaRecurse() throws Exception {
        final JexlEngine jexl = createEngine();
        final String factSrc = "function fact(x) { x < 2? 1 : x * fact(x - 1) }";
        final JexlScript fact0 = jexl.createScript(factSrc);
        final JexlScript fact1 = jexl.createScript(fact0.toString());
        Assert.assertEquals(fact0, fact1);
        Closure r0 = (Closure) fact0.execute(null);
        Closure r1 = (Closure) fact1.execute(null);
        Assert.assertEquals(720, r0.execute(null, 6));
        Assert.assertEquals(720, r1.execute(null, 6));
        Assert.assertEquals(r0, r1);
        Assert.assertEquals(r1, r0);
        // ensure we did not break anything through equals
        Assert.assertEquals(720, r0.execute(null, 6));
        Assert.assertEquals(720, r1.execute(null, 6));
    }

    @Test
    public void testCapturedLambda() {
        final JexlEngine jexl = createEngine();
        final JexlEvalContext ctx = new JexlEvalContext();
        ctx.getEngineOptions().setLexical(false);
        JexlScript s42;
        Object result;
        JexlScript s15;
        String[] localv;
        Set<List<String>> hvars;
        String strs;

        // captured variables are NOT local variables
        strs = "(x)->{ (y)->{ x + y } }";
        s42 = jexl.createScript(strs);
        result = s42.execute(ctx, 15);
        Assert.assertTrue(result instanceof JexlScript);
        s15 = (JexlScript) result;
        localv = s15.getLocalVariables();
        Assert.assertEquals(0, localv.length);
        hvars = s15.getVariables();
        Assert.assertEquals(1, hvars.size());

        // declaring a local that overrides captured
        // in 3.1, such a local was considered local
        // per 3.2, this local is considered captured
        strs = "(x)->{ (y)->{ var z = 169; var x; x + y } }";
        s42 = jexl.createScript(strs);
        result = s42.execute(ctx, 15);
        Assert.assertTrue(result instanceof JexlScript);
        s15 = (JexlScript) result;
        localv = s15.getLocalVariables();
        Assert.assertNotNull(localv);
        Assert.assertEquals(1, localv.length);
        hvars = s15.getVariables();
        Assert.assertEquals(1, hvars.size());
        // evidence this is not (strictly) a local since it inherited a captured value
        result = s15.execute(ctx, 27);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testRecurse() {
        final JexlEngine jexl = createEngine();
        final JexlContext jc = new MapContext();
        final JexlScript script = jexl.createScript("var fact = (x)->{ if (x <= 1) 1; else x * fact(x - 1) }; fact(5)");
        final int result = (Integer) script.execute(jc);
        Assert.assertEquals(120, result);
    }

    @Test
    public void testRecurse1() {
        final JexlEngine jexl = createEngine();
        final JexlContext jc = new MapContext();
        String src = "var fact = x->x <= 1? 1 : x * fact(x - 1);\nfact(5);\n";
        final JexlScript script = jexl.createScript(src);
        final int result = (Integer) script.execute(jc);
        Assert.assertEquals(120, result);
        String parsed = script.getParsedText();
        Assert.assertEquals(src, parsed);
    }

    @Test
    public void testRecurse2() {
        final JexlEngine jexl = createEngine();
        final JexlContext jc = new MapContext();
        // adding some captured vars to get it confused
        final JexlScript script = jexl.createScript(
                "var y = 1; var z = 1; "
                +"var fact = (x)->{ if (x <= y) z; else x * fact(x - 1) }; fact(6)");
        final int result = (Integer) script.execute(jc);
        Assert.assertEquals(720, result);
    }

    @Test
    public void testRecurse2b() {
        final JexlEngine jexl = createEngine();
        final JexlContext jc = new MapContext();
        // adding some captured vars to get it confused
        final JexlScript fact = jexl.createScript(
                "var y = 1; var z = 1; "
                        +"var fact = (x)->{ if (x <= y) z; else x * fact(x - 1) };" +
                        "fact");
        Script func = (Script) fact.execute(jc);
        String[] captured = func.getCapturedVariables();
        Assert.assertEquals(3, captured.length);
        Assert.assertTrue(Arrays.asList(captured).containsAll(Arrays.asList("z", "y", "fact")));
        final int result = (Integer) func.execute(jc, 6);
        Assert.assertEquals(720, result);
    }

    @Test
    public void testRecurse3() {
        final JexlEngine jexl = createEngine();
        final JexlContext jc = new MapContext();
        // adding some captured vars to get it confused
        final JexlScript script = jexl.createScript(
                "var y = 1; var z = 1;var foo = (x)->{y + z}; "
                +"var fact = (x)->{ if (x <= y) z; else x * fact(x - 1) }; fact(6)");
        final int result = (Integer) script.execute(jc);
        Assert.assertEquals(720, result);
    }

    @Test
    public void testIdentity() {
        final JexlEngine jexl = createEngine();
        JexlScript script;
        Object result;

        script = jexl.createScript("(x)->{ x }");
        Assert.assertArrayEquals(new String[]{"x"}, script.getParameters());
        result = script.execute(null, 42);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testCurry1() {
        final JexlEngine jexl = createEngine();
        JexlScript script;
        Object result;
        String[] parms;

        final JexlScript base = jexl.createScript("(x, y, z)->{ x + y + z }");
        parms = base.getUnboundParameters();
        Assert.assertEquals(3, parms.length);
        script = base.curry(5);
        parms = script.getUnboundParameters();
        Assert.assertEquals(2, parms.length);
        script = script.curry(15);
        parms = script.getUnboundParameters();
        Assert.assertEquals(1, parms.length);
        script = script.curry(22);
        parms = script.getUnboundParameters();
        Assert.assertEquals(0, parms.length);
        result = script.execute(null);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testCurry2() {
        final JexlEngine jexl = createEngine();
        JexlScript script;
        Object result;
        String[] parms;

        final JexlScript base = jexl.createScript("(x, y, z)->{ x + y + z }");
        script = base.curry(5, 15);
        parms = script.getUnboundParameters();
        Assert.assertEquals(1, parms.length);
        script = script.curry(22);
        result = script.execute(null);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testCurry3() {
        final JexlEngine jexl = createEngine();
        JexlScript script;
        Object result;

        final JexlScript base = jexl.createScript("(x, y, z)->{ x + y + z }");
        script = base.curry(5, 15);
        result = script.execute(null, 22);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testCurry4() {
        final JexlEngine jexl = createEngine();
        JexlScript script;
        Object result;

        final JexlScript base = jexl.createScript("(x, y, z)->{ x + y + z }");
        script = base.curry(5);
        result = script.execute(null, 15, 22);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testCurry5() {
        final JexlEngine jexl = createEngine();
        JexlScript script;
        Object result;

        final JexlScript base = jexl.createScript("var t = x + y + z; return t", "x", "y", "z");
        script = base.curry(5);
        result = script.execute(null, 15, 22);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testReturnValue() throws Exception {
        JexlEngine jexl = createEngine();
        String strs = "var s = function { return 21 + 21 }; s()";
        JexlScript s42 = jexl.createScript(strs);
        Object result = s42.execute(null);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testEmptyReturn() throws Exception {
        JexlEngine jexl = createEngine();
        String strs = "var s = function { return }; s()";
        JexlScript s42 = jexl.createScript(strs);
        Object result = s42.execute(null);
        Assert.assertNull(result);
    }

    @Test
    public void test270() {
        final JexlEngine jexl = createEngine();
        final JexlScript base = jexl.createScript("(x, y, z)->{ x + y + z }");
        final String text = base.toString();
        JexlScript script = base.curry(5, 15);
        Assert.assertEquals(text, script.toString());

        final JexlContext ctxt = new JexlEvalContext();
        ctxt.set("s", base);
        script = jexl.createScript("return s");
        Object result = script.execute(ctxt);
        Assert.assertEquals(text, result.toString());

        script = jexl.createScript("return s.curry(1)");
        result = script.execute(ctxt);
        Assert.assertEquals(text, result.toString());
    }

    @Test
    public void test271a() {
        final JexlEngine jexl = createEngine();
        final JexlScript base = jexl.createScript("var base = 1; var x = (a)->{ var y = (b) -> {base + b}; return base + y(a)}; x(40)");
        final Object result = base.execute(null);
        Assert.assertEquals(42, result);
    }

    @Test
    public void test271b() {
        final JexlEngine jexl = createEngine();
        final JexlScript base = jexl.createScript("var base = 2; var sum = (x, y, z)->{ base + x + y + z }; var y = sum.curry(1); y(2,3)");
        final Object result = base.execute(null);
        Assert.assertEquals(8, result);
    }

    @Test
    public void test271c() {
        final JexlEngine jexl = createEngine();
        final JexlScript base = jexl.createScript("(x, y, z)->{ 2 + x + y + z };");
        final JexlScript y = base.curry(1);
        final Object result = y.execute(null, 2, 3);
        Assert.assertEquals(8, result);
    }

    @Test
    public void test271d() {
        final JexlEngine jexl = createEngine();
        final JexlScript base = jexl.createScript("var base = 2; (x, y, z)->base + x + y + z;");
        final JexlScript y = ((JexlScript) base.execute(null)).curry(1);
        final Object result = y.execute(null, 2, 3);
        Assert.assertEquals(8, result);
    }

    // Redefining a captured var is not resolved correctly in left-hand side;
    // declare the var in local frame, resolved in local frame instead of parent.
    @Test
    public void test271e() {
        JexlEngine jexl = createEngine();
        JexlScript base = jexl.createScript("var base = 1000; var f = (x, y)->{ var base = x + y + (base?:-1000); base; }; f(100, 20)");
        Object result = base.execute(null);
        Assert.assertEquals(1120, result);
    }

    @Test
    public void testFatFact0() {
        JexlFeatures features = new JexlFeatures();
        features.fatArrow(true);
        String src = "function (a) { const fact = x =>{ x <= 1? 1 : x * fact(x - 1) }; fact(a) }";
        JexlEngine jexl = createEngine(features);
        JexlScript script = jexl.createScript(src);
        Object result = script.execute(null, 6);
        Assert.assertEquals(720, result);
    }

    @Test
    public void testFatFact1() {
        String src = "function (a) { const fact = (x)=> x <= 1? 1 : x * fact(x - 1) ; fact(a) }";
        JexlFeatures features = new JexlFeatures();
        features.fatArrow(true);
        JexlEngine jexl = createEngine(features);
        JexlScript script = jexl.createScript(src);
        Object result = script.execute(null, 6);
        Assert.assertEquals(720, result);
        features.fatArrow(false);
        jexl = createEngine(features);
        try {
            script = jexl.createScript(src);
        } catch(JexlException.Feature xfeature) {
            Assert.assertTrue(xfeature.getMessage().contains("fat-arrow"));
        }
    }

    @Test
    public void testNamedFunc() {
        String src = "(let a)->{ function fact(const x) { x <= 1? 1 : x * fact(x - 1); } fact(a); }";
        JexlEngine jexl = createEngine();
        JexlScript script = jexl.createScript(src);
        Object result = script.execute(null, 6);
        Assert.assertEquals(720, result);
        String parsed = simpleWhitespace(script.getParsedText());
        Assert.assertEquals(simpleWhitespace(src), parsed);
    }

    @Test
    public void testNamedFuncIsConst() {
        JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        // Only valid if lexical mode is enabled
        String src = "function foo(x) { x + x }; var foo ='nonononon'";
        JexlEngine jexl = createEngine(f);
        try {
            JexlScript script = jexl.createScript(src);
            Assert.fail("should fail, foo is already defined");
        } catch(JexlException.Parsing xparse) {
            Assert.assertTrue(xparse.getMessage().contains("foo"));
        }
    }

    @Test
    public void testFailParseFunc0() {
        String src = "if (false) function foo(x) { x + x }; var foo = 1";
        JexlEngine jexl = createEngine();
        try {
            JexlScript script = jexl.createScript(src);
        } catch(JexlException.Parsing xparse) {
            Assert.assertTrue(xparse.getMessage().contains("function"));
        }
    }

    @Test
    public void testFailParseFunc1() {
        String src = "if (false) let foo = (x) { x + x }; var foo = 1";
        JexlEngine jexl = createEngine();
        try {
            JexlScript script = jexl.createScript(src);
        } catch(JexlException.Parsing xparse) {
            // Assert.assertTrue(xparse.getMessage().contains("let"));
        }
    }

    @Test public void testLambdaExpr0() {
        String src = "(x, y) -> x + y";
        JexlEngine jexl = createEngine();
        JexlScript script = jexl.createScript(src);
        Object result = script.execute(null, 11, 31);
        Assert.assertEquals(42, result);
    }

    @Test public void testLambdaExpr1() {
        String src = "x -> x + x";
        JexlEngine jexl = createEngine();
        JexlScript script = jexl.createScript(src);
        Object result = script.execute(null, 21);
        Assert.assertEquals(42, result);
    }

    @Test public void testLambdaExpr10() {
        String src = "(a)->{ var x = x -> x + x; x(a) }";
        JexlEngine jexl = createEngine();
        JexlScript script = jexl.createScript(src);
        Object result = script.execute(null, 21);
        Assert.assertEquals(42, result);
    }

    @Test public void testLambdaExpr2() {
        String src = "x -> { { x + x } }";
        JexlEngine jexl = createEngine();
        JexlScript script = jexl.createScript(src);
        Object result = script.execute(null, 21);
        Assert.assertTrue(result instanceof Set);
        Set<?> set = (Set<?>) result;
        Assert.assertEquals(1, set.size());
        Assert.assertTrue(set.contains(42));
    }

    @Test public void testLambdaExpr3() {
        String src = "x -> ( { x + x } )";
        JexlEngine jexl = createEngine();
        JexlScript script = jexl.createScript(src);
        Object result = script.execute(null, 21);
        Assert.assertTrue(result instanceof Set);
        Set<?> set = (Set<?>) result;
        Assert.assertEquals(1, set.size());
        Assert.assertTrue(set.contains(42));
    }


    @Test
    public void test405a() {
        final JexlEngine jexl = new JexlBuilder()
            .cache(4).strict(true).safe(false)
            .create();
        String libSrc = "var theFunction = argFn -> { var fn = argFn; fn() }; { 'theFunction' : theFunction }";
        String src1 = "var v0 = 42; var v1 = -42; lib.theFunction(()->{ v1 + v0 }) ";
        JexlScript libMap = jexl.createScript(libSrc);
        Object theLib = libMap.execute(null);
        JexlScript f1 = jexl.createScript(src1, "lib");
        Object result = f1.execute(null, theLib);
        Assert.assertEquals(0, result);
    }

    @Test
    public void test405b() {
        final JexlEngine jexl = new JexlBuilder()
            .cache(4).strict(true).safe(false)
            .create();
        String libSrc = "function theFunction(argFn) { var fn = argFn; fn() }; { 'theFunction' : theFunction }";
        String src1 = "var v0 = 42; var v1 = -42; lib.theFunction(()->{ v1 + v0 }) ";
        JexlScript libMap = jexl.createScript(libSrc);
        Object theLib = libMap.execute(null);
        JexlScript f1 = jexl.createScript(src1, "lib");
        Object result = f1.execute(null, theLib);
        Assert.assertEquals(0, result);
    }
}
