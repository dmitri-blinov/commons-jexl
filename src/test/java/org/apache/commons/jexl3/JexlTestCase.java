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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;


import org.apache.commons.jexl3.internal.Interpreter;
import org.apache.commons.jexl3.internal.OptionsContext;
import org.apache.commons.jexl3.internal.Util;
import org.apache.commons.jexl3.internal.introspection.Permissions;
import org.apache.commons.jexl3.internal.introspection.Uberspect;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.junit.After;
import org.junit.Assert;

/**
 * Implements runTest methods to dynamically instantiate and invoke a test,
 * wrapping the call with setUp(), tearDown() calls.
 * Eases the implementation of main methods to debug.
 */
public class JexlTestCase {
    // The default options: all tests where engine lexicality is
    // important can be identified by the builder  calling lexical(...).
    static {
        JexlOptions.setDefaultFlags("-safe", "+lexical");
    }
    /** No parameters signature for test run. */
    private static final Class<?>[] NO_PARMS = {};
    /** String parameter signature for test run. */
    private static final Class<?>[] STRING_PARM = {String.class};

    /** A default JEXL engine instance. */
    protected final JexlEngine JEXL;

    public JexlTestCase(final String name) {
        this(name, new JexlBuilder().imports(Arrays.asList("java.lang","java.math","java.util","java.io","java.net")).permissions(null).cache(128).create());
    }

    protected JexlTestCase(final String name, final JexlEngine jexl) {
        //super(name);
        JEXL = jexl;
    }

    public void setUp() throws Exception {
        // nothing to do
    }

    @After
    public void tearDown() throws Exception {
        debuggerCheck(JEXL);
    }

    static JexlEngine createEngine() {
        return new JexlBuilder().create();
    }

    static JexlEngine createEngine(JexlFeatures features) {
        return new JexlBuilder().features(features).create();
    }

    // define mode pro50
    static final JexlOptions MODE_PRO50 = new JexlOptions();
    static {
        MODE_PRO50.setFlags( "+strict +cancellable +lexical +lexicalShade -safe".split(" "));
    }

    public static class PragmaticContext extends OptionsContext implements JexlContext.PragmaProcessor, JexlContext.OptionsHandle {
        private final JexlOptions options;

        public PragmaticContext() {
            this(new JexlOptions());
        }

        public PragmaticContext(final JexlOptions o) {
            super();
            this.options = o;
        }

        @Override
        public void processPragma(String key, Object value) {
            processPragma(null, key, value);
        }

        @Override
        public void processPragma(JexlOptions opts, final String key, final Object value) {
            if ("script.mode".equals(key) && "pro50".equals(value)) {
                opts.set(MODE_PRO50);
            }
        }

        @Override
        public JexlOptions getEngineOptions() {
            return options;
        }
    }

    /**
     * Compare strings ignoring white space differences.
     * <p>This replaces any sequence of whitespaces (ie \\s) by one space (ie ASCII 32) in both
     * arguments then compares them.</p>
     * @param lhs left hand side
     * @param rhs right hand side
     * @return true if strings are equal besides whitespace
     */
    public static boolean equalsIgnoreWhiteSpace(String lhs, String rhs) {
        String lhsw = lhs.trim().replaceAll("\\s+", "");
        String rhsw = rhs.trim().replaceAll("\\s+", "");
        return lhsw.equals(rhsw);
    }

    public String simpleWhitespace(String arg) {
        return arg.trim().replaceAll("\\s+", " ");
    }

    /**
     * A very secure singleton.
     */
    public static final JexlPermissions SECURE = JexlPermissions.RESTRICTED;

    public static JexlEngine createEngine(final boolean lenient) {
        return createEngine(lenient, SECURE);
    }

    public static JexlEngine createEngine(final boolean lenient, JexlPermissions permissions) {
        return new JexlBuilder()
                .uberspect(new Uberspect(null, null, permissions))
                .arithmetic(new JexlArithmetic(!lenient)).cache(128).create();
    }

    /**
     * Will force testing the debugger for each derived test class by
     * recreating each expression from the JexlNode in the JexlEngine cache &
     * testing them for equality with the origin.
     * @throws Exception
     */
    public static void debuggerCheck(final JexlEngine ijexl) throws Exception {
         Util.debuggerCheck(ijexl);
    }

    /**
     * Dynamically runs a test method.
     * @param name the test method to run
     * @throws Exception if anything goes wrong
     */
    public void runTest(final String name) throws Exception {
        if ("runTest".equals(name)) {
            return;
        }
        Method method = null;
        try {
            method = this.getClass().getDeclaredMethod(name, NO_PARMS);
        }
        catch(final Exception xany) {
            Assert.fail("no such test: " + name);
            return;
        }
        try {
            this.setUp();
            method.invoke(this);
        } finally {
            this.tearDown();
        }
    }

    /**
     * Instantiate and runs a test method; useful for debugging purpose.
     * For instance:
     * <code>
     * public static void main(String[] args) throws Exception {
     *   runTest("BitwiseOperatorTest","testAndVariableNumberCoercion");
     * }
     * </code>
     * @param tname the test class name
     * @param mname the test class method
     * @throws Exception
     */
    public static void runTest(final String tname, final String mname) throws Exception {
        final String testClassName = "org.apache.commons.jexl3."+tname;
        Class<JexlTestCase> clazz = null;
        JexlTestCase test = null;
        // find the class
        try {
            clazz = (Class<JexlTestCase>) Class.forName(testClassName);
        }
        catch(final ClassNotFoundException xclass) {
            Assert.fail("no such class: " + testClassName);
            return;
        }
        // find ctor & instantiate
        Constructor<JexlTestCase> ctor = null;
        try {
            ctor = clazz.getConstructor(STRING_PARM);
            test = ctor.newInstance("debug");
        }
        catch(final NoSuchMethodException xctor) {
            // instantiate default class ctor
            try {
                test = clazz.getConstructor().newInstance();
            }
            catch(final Exception xany) {
                Assert.fail("cant instantiate test: " + xany);
                return;
            }
        }
        catch(final Exception xany) {
            Assert.fail("cant instantiate test: " + xany);
            return;
        }
        // Run the test
        test.runTest(mname);
    }

    /**
     * Runs a test.
     * @param args where args[0] is the test class name and args[1] the test class method
     * @throws Exception
     */
    public static void main(final String[] args) throws Exception {
        runTest(args[0], args[1]);
    }
}
