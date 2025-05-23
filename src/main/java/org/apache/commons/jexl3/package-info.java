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
/**
 * Provides a framework for evaluating JEXL expressions.
 * <ul>
 * <li><a href="#intro">Introduction</a></li>
 * <li><a href="#example">Brief Example</a></li>
 * <li><a href="#usage">Using JEXL</a></li>
 * <li><a href="#configuration">Configuring JEXL</a></li>
 * <li><a href="#customization">Customizing JEXL</a></li>
 * <li><a href="#extension">Extending JEXL</a></li>
 * </ul>
 * <h2><a id="intro">Introduction</a></h2>
 * <p>
 * JEXL is a library intended to facilitate the implementation of dynamic and scripting features in applications
 * and frameworks.
 * </p>
 * <h2><a id="example">A Brief Example</a></h2>
 * <p>
 * In its simplest form, JEXL merges an
 * {@link org.apache.commons.jexl3.JexlExpression}
 * with a
 * {@link org.apache.commons.jexl3.JexlContext} when evaluating expressions.
 * An Expression is created using
 * {@link org.apache.commons.jexl3.JexlEngine#createExpression(String)},
 * passing a String containing valid JEXL syntax.  A simple JexlContext can be created using
 * a {@link org.apache.commons.jexl3.MapContext} instance;
 * a map of variables that will be internally wrapped can be optionally provided through its constructor.
 * The following example, takes a variable named 'car', and
 * invokes the checkStatus() method on the property 'engine'
 * </p>
 * <pre>
 * // Create a JexlEngine (could reuse one instead)
 * JexlEngine jexl = new JexlBuilder().create();
 * // Create an expression object equivalent to 'car.getEngine().checkStatus()':
 * String jexlExp = "car.engine.checkStatus()";
 * Expression e = jexl.createExpression( jexlExp );
 * // The car we have to handle coming as an argument...
 * Car car = theCarThatWeHandle;
 * // Create a context and add data
 * JexlContext jc = new MapContext();
 * jc.set("car", car );
 * // Now evaluate the expression, getting the result
 * Object o = e.evaluate(jc);
 * </pre>
 * <h2><a id="usage">Using JEXL</a></h2>
 * The API is composed of three levels addressing different functional needs:
 * <ul>
 * <li>Dynamic invocation of setters, getters, methods and constructors</li>
 * <li>Script expressions known as JEXL expressions</li>
 * <li>JSP/JSF like expression known as JXLT expressions</li>
 * </ul>
 * <h3><a id="usage_note">Important note</a></h3>
 * The public API classes reside in the 2 packages:
 * <ul>
 * <li>org.apache.commons.jexl3</li>
 * <li>org.apache.commons.jexl3.introspection</li>
 * </ul>
 * <p>
 * The following packages follow a "use at your own maintenance cost" policy; these are only intended to be used
 * for extending JEXL.
 * Their classes and methods are not guaranteed to remain compatible in subsequent versions.
 * If you think you need to use  directly some of their features or methods, it might be a good idea to check with
 * the community through the mailing list first.
 * </p>
 * <ul>
 * <li>org.apache.commons.jexl3.parser</li>
 * <li>org.apache.commons.jexl3.scripting</li>
 * <li>org.apache.commons.jexl3.internal</li>
 * <li>org.apache.commons.jexl3.internal.introspection</li>
 * </ul>
 * <h3><a id="usage_api">Dynamic Invocation</a></h3>
 * <p>
 * These functionalities are close to the core level utilities found in
 * <a href="https://commons.apache.org/beanutils/">BeanUtils</a>.
 * For basic dynamic property manipulations and method invocation, you can use the following
 * set of methods:
 * </p>
 * <ul>
 * <li>{@link org.apache.commons.jexl3.JexlEngine#newInstance}</li>
 * <li>{@link org.apache.commons.jexl3.JexlEngine#setProperty}</li>
 * <li>{@link org.apache.commons.jexl3.JexlEngine#getProperty}</li>
 * <li>{@link org.apache.commons.jexl3.JexlEngine#invokeMethod}</li>
 * </ul>
 * The following example illustrate their usage:
 * <pre>
 * // test outer class
 * public static class Froboz {
 * int value;
 * public Froboz(int v) { value = v; }
 * public void setValue(int v) { value = v; }
 * public int getValue() { return value; }
 * }
 * // test inner class
 * public static class Quux {
 * String str;
 * Froboz froboz;
 * public Quux(String str, int fro) {
 * this.str = str;
 * froboz = new Froboz(fro);
 * }
 * public Froboz getFroboz() { return froboz; }
 * public void setFroboz(Froboz froboz) { this.froboz = froboz; }
 * public String getStr() { return str; }
 * public void setStr(String str) { this.str = str; }
 * }
 * // test API
 * JexlEngine jexl = new JexlBuilder().create();
 * Quux quux = jexl.newInstance(Quux.class, "xuuq", 100);
 * jexl.setProperty(quux, "froboz.value", Integer.valueOf(100));
 * Object o = jexl.getProperty(quux, "froboz.value");
 * assertEquals("Result is not 100", new Integer(100), o);
 * jexl.setProperty(quux, "['froboz'].value", Integer.valueOf(1000));
 * o = jexl.getProperty(quux, "['froboz']['value']");
 * assertEquals("Result is not 1000", new Integer(1000), o);
 * </pre>
 * <h3><a id="usage_jexl">Expressions and Scripts</a></h3>
 * <p>
 * If your needs require simple expression evaluation capabilities, the core JEXL features
 * will most likely fit.
 * The main methods are:
 * </p>
 * <ul>
 * <li>{@link org.apache.commons.jexl3.JexlEngine#createScript}</li>
 * <li>{@link org.apache.commons.jexl3.JexlScript#execute}</li>
 * <li>{@link org.apache.commons.jexl3.JexlEngine#createExpression}</li>
 * <li>{@link org.apache.commons.jexl3.JexlExpression#evaluate}</li>
 * </ul>
 * The following example illustrates their usage:
 * <pre>
 * JexlEngine jexl = new JexlBuilder().create();
 * JexlContext jc = new MapContext();
 * jc.set("quuxClass", quux.class);
 * JexlExpression create = jexl.createExpression("quux = new(quuxClass, 'xuuq', 100)");
 * JelxExpression assign = jexl.createExpression("quux.froboz.value = 10");
 * JexlExpression check = jexl.createExpression("quux[\"froboz\"].value");
 * Quux quux = (Quux) create.evaluate(jc);
 * Object o = assign.evaluate(jc);
 * assertEquals("Result is not 10", new Integer(10), o);
 * o = check.evaluate(jc);
 * assertEquals("Result is not 10", new Integer(10), o);
 * </pre>
 * <h3><a id="usage_ujexl">Unified Expressions and Templates</a></h3>
 * <p>
 * If you are looking for JSP-EL like and basic templating features, you can
 * use Expression from a JxltEngine.
 * </p>
 * The main methods are:
 * <ul>
 * <li>{@link org.apache.commons.jexl3.JxltEngine#createExpression}</li>
 * <li>{@link org.apache.commons.jexl3.JxltEngine.Expression#prepare}</li>
 * <li>{@link org.apache.commons.jexl3.JxltEngine.Expression#evaluate}</li>
 * <li>{@link org.apache.commons.jexl3.JxltEngine#createTemplate}</li>
 * <li>{@link org.apache.commons.jexl3.JxltEngine.Template#prepare}</li>
 * <li>{@link org.apache.commons.jexl3.JxltEngine.Template#evaluate}</li>
 * </ul>
 * The following example illustrates their usage:
 * <pre>
 * JexlEngine jexl = new JexlBuilder().create();
 * JxltEngine jxlt = jexl.createJxltEngine();
 * JxltEngine.Expression expr = jxlt.createExpression("Hello ${user}");
 * String hello = expr.evaluate(context).toString();
 * </pre>
 * <h3>JexlExpression, JexlScript, Expression and Template: summary</h3>
 * <h4>JexlExpression </h4>
 * <p>
 * These are the most basic form of JexlEngine expressions and only allow for a single command
 * to be executed and its result returned. If you try to use multiple commands, it ignores
 * everything after the first semi-colon and just returns the result from
 * the first command.
 * </p>
 * <p>
 * Also note that expressions are not statements (which is what scripts are made of) and do not allow
 * using the flow control (if, while, for), variables or lambdas syntactic elements.
 * </p>
 * <h4>JexlScript</h4>
 * <p>
 * These allow you to use multiple statements and you can
 * use variable assignments, loops, calculations, etc. More or less what can be achieved in Shell or
 * JavaScript at its basic level. The result from the last command is returned from the script.
 * </p>
 * <h4>JxltEngine.Expression</h4>
 * <p>
 * These are ideal to produce "one-liner" text, like a 'toString()' on steroids.
 * To get a calculation you use the EL-like syntax
 * as in ${someVariable}. The expression that goes between the brackets
 * behaves like a JexlScript, not an expression. You can use semi-colons to
 * execute multiple commands and the result from the last command is
 * returned from the script. You also have the ability to use a 2-pass evaluation using
 * the #{someScript} syntax.
 * </p>
 * <h4>JxltEngine.Template</h4>
 * <p>
 * These produce text documents. Each line beginning with '$$' (as a default) is
 * considered JEXL code and all others considered as JxltEngine.Expression.
 * Think of those as simple Velocity templates. A rewritten MudStore initial Velocity sample looks like this:
 * </p>
 * <pre><code>
 * &lt;html&gt;
 * &lt;body&gt;
 * Hello ${customer.name}!
 * &lt;table&gt;
 * $$      for(var mud : mudsOnSpecial ) {
 * $$          if (customer.hasPurchased(mud) ) {
 * &lt;tr&gt;
 * &lt;td&gt;
 * ${flogger.getPromo( mud )}
 * &lt;/td&gt;
 * &lt;/tr&gt;
 * $$          }
 * $$      }
 * &lt;/table&gt;
 * &lt;/body&gt;
 * &lt;/html&gt;
 * </code></pre>
 * <h2><a id="configuration">JEXL Configuration</a></h2>
 * <p>
 * The JexlEngine can be configured through a few parameters that will drive how it reacts
 * in case of errors.
 * These configuration methods are embedded through a {@link org.apache.commons.jexl3.JexlBuilder}.
 * </p>
 * <h3><a id="static_configuration">Static &amp; Shared Configuration</a></h3>
 * <p>
 * Both JexlEngine and JxltEngine are thread-safe, most of their inner fields are final; the same instance can
 * be shared between different  threads and proper synchronization is enforced in critical areas (introspection caches).
 * </p>
 * <p>
 * Of particular importance is {@link org.apache.commons.jexl3.JexlBuilder#loader(java.lang.ClassLoader)} which indicates
 * to the JexlEngine being built which class loader to use to solve a class name;
 * this directly affects how JexlEngine.newInstance and the 'new' script method operates.
 * </p>
 * <p>
 * This can also be very useful in cases where you rely on JEXL to dynamically load and call plugins for your application.
 * To avoid having to restart the server in case of a plugin implementation change, you can call
 * {@link org.apache.commons.jexl3.JexlEngine#setClassLoader} and all the scripts created through this engine instance
 * will automatically point to the newly loaded classes.
 * </p>
 * <p>
 * You can state what can be manipulated through scripting by the {@link org.apache.commons.jexl3.annotations.NoJexl}
 * annotation that completely shield classes and methods from JEXL introspection.
 * The other configurable way to restrict JEXL is by using a
 * {@link org.apache.commons.jexl3.introspection.JexlSandbox} which allows finer control over what is exposed; the sandbox
 * can be set through {@link org.apache.commons.jexl3.JexlBuilder#sandbox(org.apache.commons.jexl3.introspection.JexlSandbox)}.
 * </p>
 * <p>
 * {@link org.apache.commons.jexl3.JexlBuilder#namespaces} extends JEXL scripting by registering your own classes as
 * namespaces allowing your own functions to be exposed at will.
 * </p>
 * This can be used as in:
 * <pre><code>
 * public static MyMath {
 * public double cos(double x) {
 * return Math.cos(x);
 * }
 * }
 * Map&lt;String, Object&gt; funcs = new HashMap&lt;String, Object&gt;();
 * funcs.put("math", new MyMath());
 * JexlEngine jexl = new JexlBuilder().namespaces(funcs).create();
 * JexlContext jc = new MapContext();
 * jc.set("pi", Math.PI);
 * JexlExpression e = JEXL.createExpression("math:cos(pi)");
 * o = e.evaluate(jc);
 * assertEquals(Double.valueOf(-1),o);
 * </code></pre>
 * <p>
 * If the <i>namespace</i> is a Class and that class declares a constructor that takes a JexlContext (or
 * a class extending JexlContext), one <i>namespace</i> instance is created on first usage in an
 * expression; this instance lifetime is limited to the expression evaluation.
 * </p>
 * <p>
 * JexlEngine and JxltEngine expression caches can be configured as well. If you intend to use JEXL
 * repeatedly in your application, these are worth configuring since expression parsing is quite heavy.
 * Note that all caches created by JEXL are held through SoftReference; under high memory pressure, the GC will be able
 * to reclaim those caches and JEXL will rebuild them if needed. By default, a JexlEngine does create a cache for
 * "small" expressions and a JxltEngine does create one for Expression .
 * </p>
 * <p>{@link org.apache.commons.jexl3.JexlBuilder#cache(int)} will set how many expressions can be simultaneously cached by the
 * JEXL engine. JxltEngine allows to define the cache size through its constructor.
 * </p>
 * <p>
 * {@link org.apache.commons.jexl3.JexlBuilder#debug(boolean)}
 * makes stack traces carried by JExlException more meaningful; in particular, these
 * traces will carry the exact caller location the Expression was created from.
 * </p>
 * <h3><a id="dynamic_configuration">Dynamic Configuration</a></h3>
 * <p>
 * Those configuration options can be overridden during evaluation by implementing a
 * {@link org.apache.commons.jexl3.JexlContext}
 * that also implements {@link org.apache.commons.jexl3.JexlEngine.Options} to carry evaluation options.
 * An example of such a class exists in the test package.
 * </p>
 * <p>
 * {@link org.apache.commons.jexl3.JexlBuilder#strict} or {@link org.apache.commons.jexl3.JexlEngine.Options#isStrict}
 * configures when JEXL considers 'null' as an error or not in various situations;
 * when facing an unreferenceable variable, using null as an argument to an arithmetic operator or failing to call
 * a method or constructor. The lenient mode is close to JEXL-1.1 behavior.
 * </p>
 * <p>
 * {@link org.apache.commons.jexl3.JexlBuilder#silent} or {@link org.apache.commons.jexl3.JexlEngine.Options#isSilent}
 * configures how JEXL reacts to errors; if silent, the engine will not throw exceptions
 * but will warn through loggers and return null in case of errors. Note that when non-silent, JEXL throws
 * JexlException which are unchecked exception.
 * </p>
 * <p>
 * Implementing a {@link org.apache.commons.jexl3.JexlContext.NamespaceResolver} through a JexlContext - look at
 * JexlEvalContext in the test directory
 * as an example - allows to override the namespace resolution and the default namespace map defined
 * through {@link org.apache.commons.jexl3.JexlBuilder#namespaces}.
 * </p>
 * <h2><a id="customization">JEXL Customization</a></h2>
 * <p>
 * The {@link org.apache.commons.jexl3.JexlContext}, {@link org.apache.commons.jexl3.JexlBuilder} and
 * {@link org.apache.commons.jexl3.JexlEngine.Options} are
 * the most likely interfaces you'll want to implement for customization. Since they expose variables and options,
 * they are the primary targets. Before you do so, have a look at JexlEvalContext in the test directory
 * and {@link org.apache.commons.jexl3.ObjectContext} which may already cover some of your needs.
 * </p>
 * <p>
 * {@link org.apache.commons.jexl3.JexlArithmetic}
 * is the class to derive if you need to change how operators behave or add types upon which they
 * operate.
 * There are 3 entry points that allow customizing the type of objects created:
 * </p>
 * <ul>
 * <li>array literals: {@link org.apache.commons.jexl3.JexlArithmetic#arrayBuilder}</li>
 * <li>map literals: {@link org.apache.commons.jexl3.JexlArithmetic#mapBuilder}</li>
 * <li>set literals: {@link org.apache.commons.jexl3.JexlArithmetic#setBuilder}</li>
 * <li>range objects: {@link org.apache.commons.jexl3.JexlArithmetic#createRange}</li>
 * </ul>
 * <p>
 * You can also overload operator methods; by convention, each operator has a method name associated to it.
 * If you overload some in your JexlArithmetic derived implementation, these methods will be called when the
 * arguments match your method signature.
 * For example, this would be the case if you wanted '+' to operate on arrays; you'd need to derive
 * JexlArithmetic and implement 'public Object add(Set&lt;?;&gt; x, Set&lt;?;&gt; y)' method.
 * Note however that you can <em>not</em> change the operator precedence.
 * The list of operator / method matches is described in {@link org.apache.commons.jexl3.JexlOperator}:
 * </p>
 * <p>
 * You can also add methods to overload property getters and setters operators behaviors.
 * Public methods of the JexlArithmetic instance named propertyGet/propertySet/arrayGet/arraySet are potential
 * overrides that will be called when appropriate.
 * The following table is an overview of the relation between a syntactic form and the method to call
 * where V is the property value class, O the object class and  P the property identifier class (usually String or Integer).
 * </p>
 * <table><caption>Property Accessors</caption>
 * <tr>
 * <th>Expression</th>
 * <th>Method Template</th>
 * </tr>
 * <tr>
 * <td>foo.property</td>
 * <td>public V propertyGet(O obj, P property);</td>
 * </tr>
 * <tr>
 * <td>foo.property = value</td>
 * <td>public V propertySet(O obj, P property, V value);</td>
 * </tr>
 * <tr>
 * <td>foo[property]</td>
 * <td>public V arrayGet(O obj, P property, V value);</td>
 * </tr>
 * <tr>
 * <td>foo[property] = value</td>
 * <td>public V arraySet(O obj, P property, V value);</td>
 * </tr>
 * </table>
 * <p>
 * You can also override the base operator methods, those whose arguments are Object which gives you total
 * control.
 * </p>
 * <h2><a id="extension">Extending JEXL</a></h2>
 * If you need to make JEXL treat some objects in a specialized manner or tweak how it
 * reacts to some settings, you can derive most of its inner-workings. The classes and methods are rarely private or
 * final - only when the inner contract really requires it. However, using the protected methods
 * and internal package classes imply you might have to re-adapt your code when new JEXL versions are released.
 * <p>
 * {@link org.apache.commons.jexl3.internal.Engine} can be
 * extended to let you capture your own configuration defaults wrt cache sizes and various flags.
 * Implementing your own cache - instead of the basic LinkedHashMap based one - would be
 * another possible extension.
 * </p>
 * <p>
 * {@link org.apache.commons.jexl3.internal.Interpreter}
 * is the class to derive if you need to add more features to the evaluation
 * itself; for instance, you want pre- and post- resolvers for variables or nested scopes for
 * for variable contexts.
 * </p>
 * <p>
 * {@link org.apache.commons.jexl3.internal.introspection.Uberspect}
 * is the class to derive if you need to add introspection or reflection capabilities for some objects, for
 * instance adding factory based support to the 'new' operator.
 * The code already reflects public fields as properties on top of Java-beans conventions.
 * </p>
 */
package org.apache.commons.jexl3;
