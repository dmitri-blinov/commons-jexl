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

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import static org.apache.commons.jexl3.JexlFeatures.CONST_CAPTURE;

/**
 * Tests for features
 * @since 3.0
 */
@SuppressWarnings({"AssertEqualsBetweenInconvertibleTypes"})
public class FeaturesTest extends JexlTestCase {
    private final JexlEngine jexl = new JexlBuilder().create();
    /**
     * Create the test
     */
    public FeaturesTest() {
        super("FeaturesTest");
    }

    /**
     * Checks that the script is valid with all features on then verifies it
     * throws a feature exception with the given set (in features param).
     * @param features
     * @param scripts
     * @throws Exception
     */
    private void checkFeature(final JexlFeatures features, final String[] scripts) throws Exception {
        for (final String script : scripts) {
            final JexlScript ctl = JEXL.createScript(script);
            Assert.assertNotNull(ctl);
            try {
                final JexlScript e = jexl.createScript(features, null, script);
                Assert.fail("should fail parse: " + script);
            } catch (final JexlException.Parsing xfeature) {
                final String msg = xfeature.getMessage();
                Assert.assertNotNull(msg);
            }
        }
    }

    private void assertOk(final JexlFeatures features, final String[] scripts) {
        for(final String str : scripts) {
            try {
                final JexlScript e = jexl.createScript(str);
            } catch (final JexlException.Feature xfeature) {
                Assert.fail(str + " :: should not fail parse: " + xfeature.getMessage());
            }
        }
    }

    @Test
    public void testNoScript() throws Exception {
        final JexlFeatures f = new JexlFeatures().script(false);
        Assert.assertTrue(f.supportsExpression());
        final String[] scripts = new String[]{
            "if (false) { block(); }",
            "{ noway(); }",
            "while(true);",
            "for(var i : {0 .. 10}) { bar(i); }"
        };
        checkFeature(f, scripts);
    }

    @Test
    public void testNoLoop() throws Exception {
        final JexlFeatures f = new JexlFeatures().loops(false);
        final String[] scripts = new String[]{
            "while(true);",
            "for(var i : {0 .. 10}) { bar(i); }"
        };
        checkFeature(f, scripts);
    }

    @Test
    public void testNoLambda() throws Exception {
        final JexlFeatures f = new JexlFeatures().lambda(false);
        final String[] scripts = new String[]{
            "var x  = ()->{ return 0 };",
            "()->{ return 0 };",
            "(x, y)->{ return 0 };",
            "function() { return 0 };",
            "function(x, y) { return 0 };",
            "if (false) { (function(x, y) { return x + y })(3, 4) }"
        };
        checkFeature(f, scripts);
    }

    @Test
    public void testNoNew() throws Exception {
        final JexlFeatures f = new JexlFeatures().newInstance(false);
        final String[] scripts = new String[]{
            "return new(clazz);",
            "new('java.math.BigDecimal', 12) + 1"
        };
        checkFeature(f, scripts);
    }

    @Test
    public void testNoSideEffects() throws Exception {
        final JexlFeatures f = new JexlFeatures().sideEffect(false);
        final String[] scripts = new String[]{
            "x = 1",
            "x.y = 1",
            "x().y = 1",
            "x += 1",
            "x.y += 1",
            "x().y += 1",
            "x -= 1",
            "x *= 1",
            "x /= 1",
            "x %= 1",
            "x ^= 1",
            "x &= 1",
            "x |= 1",
            "x >>= 1",
            "x <<= 1",
            "x >>>= 1",
        };
        checkFeature(f, scripts);
    }

    @Test
    public void testNoSideEffectsGlobal() throws Exception {
        final JexlFeatures f = new JexlFeatures().sideEffectGlobal(false);
        final String[] scripts = new String[]{
            "x = 1",
            "x.y = 1",
            "x().y = 1",
            "x += 1",
            "x.y += 1",
            "x().y += 1",
            "x -= 1",
            "x *= 1",
            "x /= 1",
            "x %= 1",
            "x ^= 1",
            "x &= 1",
            "x |= 1",
            "x >>= 1",
            "x <<= 1",
            "x >>>= 1",
            "4 + (x.y = 1)",
            "if (true) x.y.z = 4"
        };
        // these should all fail with x undeclared as local, thus x as global
        checkFeature(f, scripts);
        // same ones with x as local should work
        for(final String str : scripts) {
            try {
                final JexlScript e = jexl.createScript("var x = foo(); " + str);
            } catch (final JexlException.Feature xfeature) {
                Assert.fail(str + " :: should not fail parse: " + xfeature.getMessage());
            }
        }
    }

    @Test
    public void testNoLocals() throws Exception {
        final JexlFeatures f = new JexlFeatures().localVar(false);
        final String[] scripts = new String[]{
            "var x = 0;",
            "(x)->{ x }"
        };
        checkFeature(f, scripts);
    }

    @Test
    public void testReservedVars() throws Exception {
        final JexlFeatures f = new JexlFeatures().reservedNames(Arrays.asList("foo", "bar"));
        final String[] scripts = new String[]{
            "var foo = 0;",
            "(bar)->{ bar }",
            "var f = function(bar) { bar; }"
        };
        checkFeature(f, scripts);
        final String[] scriptsOk = new String[]{
            "var foo0 = 0;",
            "(bar1)->{ bar }",
            "var f = function(bar2) { bar2; }"
        };
        assertOk(f, scriptsOk);
    }

    @Test
    public void testArrayRefs() throws Exception {
        final JexlFeatures f = new JexlFeatures().arrayReferenceExpr(false);

        final String[] scripts = new String[]{
            "x[y]",
            "x['a'][b]",
            "x()['a'][b]",
            "x.y['a'][b]"
        };
        checkFeature(f, scripts);
        assertOk(f, scripts);
        // same ones with constant array refs should work
        final String[] scriptsOk = new String[]{
            "x['y']",
            "x['a'][1]",
            "x()['a']['b']",
            "x.y['a']['b']"
        };
        assertOk(f, scriptsOk);
    }
    @Test
    public void testMethodCalls() throws Exception {
        final JexlFeatures f = new JexlFeatures().methodCall(false);
        final String[] scripts = new String[]{
            "x.y(z)",
            "x['a'].m(b)",
            "x()['a'](b)",
            "x.y['a'](b)"
        };
        checkFeature(f, scripts);
        // same ones with constant array refs should work
        final String[] scriptsOk = new String[]{
            "x('y')",
            "x('a')[1]",
            "x()['a']['b']",
        };
        assertOk(f, scriptsOk);
    }

    @Test
    public void testStructuredLiterals() throws Exception {
        final JexlFeatures f = new JexlFeatures().structuredLiteral(false);
        final String[] scripts = new String[]{
            "{1, 2, 3}",
            "[1, 2, 3]",
            "{ 1 :'one', 2 : 'two', 3 : 'three' }",
            "(1 .. 5)"
        };
        checkFeature(f, scripts);
        assertOk(f, scripts);
    }

    @Test
    public void testAnnotations() throws Exception {
        final JexlFeatures f = new JexlFeatures().annotation(false);
        final String[] scripts = new String[]{
            "@synchronized(2) { return 42; }",
            "@two var x = 3;"
        };
        checkFeature(f, scripts);
    }


    @Test
    public void testPragma() throws Exception {
        final JexlFeatures f = new JexlFeatures().pragma(false);
        final String[] scripts = new String[]{
            "#pragma foo 42",
            "#pragma foo 'bar' @two var x = 3; "
        };
        checkFeature(f, scripts);
    }

    @Test
    public void testMixedFeatures() throws Exception {
        // no new, no local, no lambda, no loops, no-side effects
        final JexlFeatures f = new JexlFeatures()
                .newInstance(false)
                .localVar(false)
                .lambda(false)
                .loops(false)
                .sideEffectGlobal(false);
        final String[] scripts = new String[]{
            "return new(clazz);",
            "()->{ return 0 };",
            "var x = 0;",
            "(x, y)->{ return 0 };",
            "for(var i : {0 .. 10}) { bar(i); }",
            "x += 1",
            "x.y += 1"
        };
        checkFeature(f, scripts);
    }

    @Test
    public void testNoComparatorNames() throws Exception {
        final JexlFeatures f = new JexlFeatures().comparatorNames(false);
        final String[] scripts = new String[]{
            "1 eq 1",
            "2 ne 3",
            "1 lt 2",
            "3 le 3",
            "4 gt 2",
            "3 ge 2"
        };
        checkFeature(f, scripts);
    }

    @Test
    public void testNoMatchingNames() throws Exception {
        final JexlFeatures f = new JexlFeatures().matchingNames(false);
        final String[] scripts = new String[]{
            "1 in [1,2]"
        };
        checkFeature(f, scripts);

    }

    @Test
    public void testNotMatchingNames() throws Exception {
        final JexlFeatures f = new JexlFeatures().matchingNames(false);
        final String[] scripts = new String[]{
            "1 !in [1,2]"
        };
        checkFeature(f, scripts);

    }

    @Test
    public void testIssue409() {
        final JexlFeatures baseFeatures =  JexlFeatures.createDefault();
        Assert.assertFalse(baseFeatures.isLexical());
        Assert.assertFalse(baseFeatures.isLexicalShade());
        Assert.assertFalse(baseFeatures.supportsConstCapture());

        final JexlFeatures scriptFeatures = JexlFeatures.createScript();
        Assert.assertTrue(scriptFeatures.isLexical());
        Assert.assertTrue(scriptFeatures.isLexicalShade());
        scriptFeatures.lexical(false);
        Assert.assertFalse(scriptFeatures.isLexical());
        Assert.assertFalse(scriptFeatures.isLexicalShade());
    }

    @Test
    public void testCreate() {
        final JexlFeatures f = JexlFeatures.createNone();
        Assert.assertTrue(f.supportsExpression());

        Assert.assertFalse(f.supportsAnnotation());
        Assert.assertFalse(f.supportsArrayReferenceExpr());
        Assert.assertFalse(f.supportsComparatorNames());
        Assert.assertFalse(f.supportsFatArrow());
        Assert.assertFalse(f.supportsImportPragma());
        Assert.assertFalse(f.supportsLambda());
        Assert.assertFalse(f.supportsLocalVar());
        Assert.assertFalse(f.supportsLoops());
        Assert.assertFalse(f.supportsMethodCall());
        Assert.assertFalse(f.supportsNamespacePragma());
        Assert.assertFalse(f.supportsNewInstance());
        Assert.assertFalse(f.supportsPragma());
        Assert.assertFalse(f.supportsPragmaAnywhere());
        Assert.assertFalse(f.supportsScript());
        Assert.assertFalse(f.supportsStructuredLiteral());

        Assert.assertFalse(f.isLexical());
        Assert.assertFalse(f.isLexicalShade());
        Assert.assertFalse(f.supportsConstCapture());

        JexlEngine jnof = new JexlBuilder().features(f).create();
        Assert.assertThrows(JexlException.Feature.class, ()->jnof.createScript("{ 3 + 4 }"));
        Assert.assertNotNull(jnof.createExpression("3 + 4"));
    }

    @Test
    public void test410a() {
        long x = JexlFeatures.createAll().getFlags();
        Assert.assertEquals(CONST_CAPTURE + 1, Long.bitCount(x));
        Assert.assertTrue((x & (1L << CONST_CAPTURE)) != 0);

        JexlFeatures all = JexlFeatures.createAll();
        final JexlEngine jexl = new JexlBuilder().features(all).create();
        JexlScript script = jexl.createScript("#0 * #1", "#0", "#1");
        Object r = script.execute(null, 6, 7);
        Assert.assertEquals(42, r);
    }

    @Test
    public void test410b() {
        JexlFeatures features = JexlFeatures.createScript();
        Assert.assertTrue(features.isLexical());
        Assert.assertTrue(features.isLexicalShade());
        Assert.assertTrue(features.supportsConstCapture());
        //features.pragmaAnywhere(false);
        Assert.assertFalse(features.supportsPragmaAnywhere());
        //features.comparatorNames(false);
        Assert.assertFalse(features.supportsComparatorNames());

        final JexlEngine jexl = new JexlBuilder().features(features).create();
        Collection<String> reserved = features.getReservedNames();
        for(String varName : reserved) {
            String src = "var " + varName;
            //JexlScript script = jexl.createScript(src);
            Assert.assertThrows(JexlException.class, () -> jexl.createScript(src));
        }
        final String[] cmpNameScripts = {
            "1 eq 1",
            "2 ne 3",
            "1 lt 2",
            "3 le 3",
            "4 gt 2",
            "3 ge 2"
        };
        for(String src : cmpNameScripts) {
            Assert.assertThrows(JexlException.Ambiguous.class, () -> jexl.createScript(src));
        }
    }
}
