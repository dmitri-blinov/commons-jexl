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

/**
 * The JEXL operators.
 *
 * These are the operators that are executed by JexlArithmetic methods.
 *
 * <p>Each of them  associates a symbol to a method signature.
 * For instance, '+' is associated to 'T add(L x, R y)'.</p>
 *
 * <p>The default JexlArithmetic implements generic versions of these methods using Object as arguments.
 * You can use your own derived JexlArithmetic that override and/or overload those operator methods.
 * Note that these are overloads by convention, not actual Java overloads.
 * The following rules apply to operator methods:</p>
 * <ul>
 * <li>Operator methods should be public</li>
 * <li>Operators return type should be respected when primitive (int, boolean,...)</li>
 * <li>Operators may be overloaded multiple times with different signatures</li>
 * <li>Operators may return JexlEngine.TRY_AGAIN to fallback on default JEXL implementation</li>
 * </ul>
 *
 * @since 3.0
 */
public enum JexlOperator {
    /**
     * Add operator.
     * <br><strong>Syntax:</strong> <code>x + y</code>
     * <br><strong>Method:</strong> <code>T add(L x, R y);</code>.
     * @see JexlArithmetic#add
     */
    ADD("+", "add", 2),

    /**
     * Subtract operator.
     * <br><strong>Syntax:</strong> <code>x - y</code>
     * <br><strong>Method:</strong> <code>T subtract(L x, R y);</code>.
     * @see JexlArithmetic#subtract
     */
    SUBTRACT("-", "subtract", 2),

    /**
     * Multiply operator.
     * <br><strong>Syntax:</strong> <code>x * y</code>
     * <br><strong>Method:</strong> <code>T multiply(L x, R y);</code>.
     * @see JexlArithmetic#multiply
     */
    MULTIPLY("*", "multiply", 2),

    /**
     * Divide operator.
     * <br><strong>Syntax:</strong> <code>x / y</code>
     * <br><strong>Method:</strong> <code>T divide(L x, R y);</code>.
     * @see JexlArithmetic#divide
     */
    DIVIDE("/", "divide", 2),

    /**
     * Modulo operator.
     * <br><strong>Syntax:</strong> <code>x % y</code>
     * <br><strong>Method:</strong> <code>T mod(L x, R y);</code>.
     * @see JexlArithmetic#mod
     */
    MOD("%", "mod", 2),

    /**
     * Bitwise-and operator.
     * <br><strong>Syntax:</strong> <code>x &amp; y</code>
     * <br><strong>Method:</strong> <code>T and(L x, R y);</code>.
     * @see JexlArithmetic#and
     */
    AND("&", "and", 2),

    /**
     * Bitwise-or operator.
     * <br><strong>Syntax:</strong> <code>x | y</code>
     * <br><strong>Method:</strong> <code>T or(L x, R y);</code>.
     * @see JexlArithmetic#or
     */
    OR("|", "or", 2),

    /**
     * Bitwise-difference operator.
     * <br><strong>Syntax:</strong> <code>x \ y</code>
     * <br><strong>Method:</strong> <code>T diff(L x, R y);</code>.
     * @see JexlArithmetic#or
     */
    DIFF("\\", "diff", 2),

    /**
     * Bitwise-xor operator.
     * <br><strong>Syntax:</strong> <code>x ^ y</code>
     * <br><strong>Method:</strong> <code>T xor(L x, R y);</code>.
     * @see JexlArithmetic#xor
     */
    XOR("^", "xor", 2),

    /**
     * Bit-pattern right-shift operator.
     * <br><strong>Syntax:</strong> <code>x &gt;&gt; y</code>
     * <br><strong>Method:</strong> <code>T rightShift(L x, R y);</code>.
     * @see JexlArithmetic#shiftRight(Object, Object)
     */
    SHIFTRIGHT(">>", "shiftRight", 2),

    /**
     * Bit-pattern right-shift unsigned operator.
     * <br><strong>Syntax:</strong> <code>x &gt;&gt;&gt; y</code>
     * <br><strong>Method:</strong> <code>T rightShiftUnsigned(L x, R y);</code>.
     * @see JexlArithmetic#shiftRightUnsigned(Object, Object)
     */
    SHIFTRIGHTU(">>>", "shiftRightUnsigned", 2),

    /**
     * Bit-pattern left-shift operator.
     * <br><strong>Syntax:</strong> <code>x &lt;&lt; y</code>
     * <br><strong>Method:</strong> <code>T leftShift(L x, R y);</code>.
     * @see JexlArithmetic#shiftLeft(Object, Object)
     */
    SHIFTLEFT("<<", "shiftLeft", 2),

    /**
     * Equals operator.
     * <br><strong>Syntax:</strong> <code>x == y</code>
     * <br><strong>Method:</strong> <code>boolean equals(L x, R y);</code>.
     * @see JexlArithmetic#equals
     */
    EQ("==", "equals", 2),

    /**
     * Not equals operator.
     * <br><strong>Syntax:</strong> <code>x != y</code>
     * <br><strong>Method:</strong> <code>calls !(equals(L x, R y));</code>.
     * @see JexlArithmetic#equals
     */
    NE("!=", null, EQ),

    /**
     * Less-than operator.
     * <br><strong>Syntax:</strong> <code>x &lt; y</code>
     * <br><strong>Method:</strong> <code>boolean lessThan(L x, R y);</code>.
     * @see JexlArithmetic#lessThan
     */
    LT("<", "lessThan", 2),

    /**
     * Less-than-or-equal operator.
     * <br><strong>Syntax:</strong> <code>x &lt;= y</code>
     * <br><strong>Method:</strong> <code>boolean lessThanOrEqual(L x, R y);</code>.
     * @see JexlArithmetic#lessThanOrEqual
     */
    LTE("<=", "lessThanOrEqual", 2),

    /**
     * Greater-than operator.
     * <br><strong>Syntax:</strong> <code>x &gt; y</code>
     * <br><strong>Method:</strong> <code>boolean greaterThan(L x, R y);</code>.
     * @see JexlArithmetic#greaterThan
     */
    GT(">", "greaterThan", 2),

    /**
     * Greater-than-or-equal operator.
     * <br><strong>Syntax:</strong> <code>x &gt;= y</code>
     * <br><strong>Method:</strong> <code>boolean greaterThanOrEqual(L x, R y);</code>.
     * @see JexlArithmetic#greaterThanOrEqual
     */
    GTE(">=", "greaterThanOrEqual", 2),

    /**
     * Contains operator.
     * <br><strong>Syntax:</strong> <code>x =~ y</code>
     * <br><strong>Method:</strong> <code>boolean contains(L x, R y);</code>.
     * @see JexlArithmetic#contains
     */
    CONTAINS("=~", "contains", 2),

    /**
     * Starts-with operator.
     * <br><strong>Syntax:</strong> <code>x =^ y</code>
     * <br><strong>Method:</strong> <code>boolean startsWith(L x, R y);</code>.
     * @see JexlArithmetic#startsWith
     */
    STARTSWITH("=^", "startsWith", 2),

    /**
     * Ends-with operator.
     * <br><strong>Syntax:</strong> <code>x =$ y</code>
     * <br><strong>Method:</strong> <code>boolean endsWith(L x, R y);</code>.
     * @see JexlArithmetic#endsWith
     */
    ENDSWITH("=$", "endsWith", 2),

    /**
     * Not operator.
     * <br><strong>Syntax:</strong> <code>!x</code>
     * <br><strong>Method:</strong> <code>T not(L x);</code>.
     * @see JexlArithmetic#not
     */
    NOT("!", "not", 1),

    /**
     * Complement operator.
     * <br><strong>Syntax:</strong> <code>~x</code>
     * <br><strong>Method:</strong> <code>T complement(L x);</code>.
     * @see JexlArithmetic#complement
     */
    COMPLEMENT("~", "complement", 1),

    /**
     * Negate operator.
     * <br><strong>Syntax:</strong> <code>-x</code>
     * <br><strong>Method:</strong> <code>T negate(L x);</code>.
     * @see JexlArithmetic#negate
     */
    NEGATE("-", "negate", 1),

    /**
     * Positivize operator.
     * <br><strong>Syntax:</strong> <code>+x</code>
     * <br><strong>Method:</strong> <code>T positivize(L x);</code>.
     * @see JexlArithmetic#positivize
     */
    POSITIVIZE("+", "positivize", 1),

    /**
     * Empty operator.
     * <br><strong>Syntax:</strong> <code>empty x</code> or <code>empty(x)</code>
     * <br><strong>Method:</strong> <code>boolean empty(L x);</code>.
     * @see JexlArithmetic#empty
     */
    EMPTY("empty", "empty", 1),

    /**
     * Size operator.
     * <br><strong>Syntax:</strong> <code>size x</code> or <code>size(x)</code>
     * <br><strong>Method:</strong> <code>int size(L x);</code>.
     * @see JexlArithmetic#size
     */
    SIZE("size", "size", 1),

    /**
     * Self-add operator.
     * <br><strong>Syntax:</strong> <code>x += y</code>
     * <br><strong>Method:</strong> <code>T selfAdd(L x, R y);</code>.
     */
    SELF_ADD("+=", "selfAdd", ADD),

    /**
     * Self-subtract operator.
     * <br><strong>Syntax:</strong> <code>x -= y</code>
     * <br><strong>Method:</strong> <code>T selfSubtract(L x, R y);</code>.
     */
    SELF_SUBTRACT("-=", "selfSubtract", SUBTRACT),

    /**
     * Self-multiply operator.
     * <br><strong>Syntax:</strong> <code>x *= y</code>
     * <br><strong>Method:</strong> <code>T selfMultiply(L x, R y);</code>.
     */
    SELF_MULTIPLY("*=", "selfMultiply", MULTIPLY),

    /**
     * Self-divide operator.
     * <br><strong>Syntax:</strong> <code>x /= y</code>
     * <br><strong>Method:</strong> <code>T selfDivide(L x, R y);</code>.
     */
    SELF_DIVIDE("/=", "selfDivide", DIVIDE),

    /**
     * Self-modulo operator.
     * <br><strong>Syntax:</strong> <code>x %= y</code>
     * <br><strong>Method:</strong> <code>T selfMod(L x, R y);</code>.
     */
    SELF_MOD("%=", "selfMod", MOD),

    /**
     * Self-and operator.
     * <br><strong>Syntax:</strong> <code>x &amp;= y</code>
     * <br><strong>Method:</strong> <code>T selfAnd(L x, R y);</code>.
     */
    SELF_AND("&=", "selfAnd", AND),

    /**
     * Self-or operator.
     * <br><strong>Syntax:</strong> <code>x |= y</code>
     * <br><strong>Method:</strong> <code>T selfOr(L x, R y);</code>.
     */
    SELF_OR("|=", "selfOr", OR),

    /**
     * Self-diff operator.
     * <br><strong>Syntax:</strong> <code>x \= y</code>
     * <br><strong>Method:</strong> <code>T selfDiff(L x, R y);</code>.
     */
    SELF_DIFF("\\=", "selfDiff", DIFF),

    /**
     * Self-xor operator.
     * <br><strong>Syntax:</strong> <code>x ^= y</code>
     * <br><strong>Method:</strong> <code>T selfXor(L x, R y);</code>.
     */
    SELF_XOR("^=", "selfXor", XOR),

    /**
     * Increment pseudo-operator.
     * <br>No syntax, used as helper for the prefix and postfix versions of {@code ++}.
     * @see JexlArithmetic#increment(Object)
     */
    INCREMENT("+1", "increment", 1),

    /**
     * Decrement pseudo-operator.
     * <br>No syntax, used as helper for the prefix and postfix versions of {@code --}.
     * @see JexlArithmetic#decrement(Object)
     */
    DECREMENT("-1", "decrement", 1),

    /**
     * Prefix ++ operator, increments and returns the value after incrementing.
     * <br><strong>Syntax:</strong> {@code ++x}
     * <br><strong>Method:</strong> {@code T incrementAndGet(L x);}.
     */
    INCREMENT_AND_GET("++.", "incrementAndGet", INCREMENT, 1),

    /**
     * Postfix ++, increments and returns the value before incrementing.
     * <br><strong>Syntax:</strong> {@code x++}
     * <br><strong>Method:</strong> {@code T getAndIncrement(L x);}.
     */
    GET_AND_INCREMENT(".++", "getAndIncrement", INCREMENT, 1),

    /**
     * Prefix --, decrements and returns the value after decrementing.
     * <br><strong>Syntax:</strong> {@code --x}
     * <br><strong>Method:</strong> {@code T decrementAndGet(L x);}.
     */
    DECREMENT_AND_GET("--.", "decrementAndGet", DECREMENT, 1),

    /**
     * Postfix --, decrements and returns the value before decrementing.
     * <br><strong>Syntax:</strong> {@code x--}
     * <br><strong>Method:</strong> {@code T getAndDecrement(L x);}.
     */
    GET_AND_DECREMENT(".--", "getAndDecrement", DECREMENT, 1),

    /**
     * Indirect operator.
     * <br><strong>Syntax:</strong> <code>*x</code>
     * <br><strong>Method:</strong> <code>T indirect(L x);</code>.
     */
    INDIRECT("*", "indirect", 1),

    /**
     * Indirect assign operator.
     * <br><strong>Syntax:</strong> <code>*x = y</code>
     * <br><strong>Method:</strong> <code>T indirectAssign(L x, R y);</code>.
     */
    INDIRECT_ASSIGN("*=", "indirectAssign", 2),

    /**
     * Self-right-shift operator.
     * <br><strong>Syntax:</strong> <code>x &gt;&gt;= y</code>
     * <br><strong>Method:</strong> <code>T selfShiftRight(L x, R y);</code>.
     */
    SELF_SHIFTRIGHT(">>=", "selfShiftRight", SHIFTRIGHT),

    /**
     * Self-right-shift unsigned operator.
     * <br><strong>Syntax:</strong> <code>x &gt;&gt;&gt; y</code>
     * <br><strong>Method:</strong> <code>T selfShiftRightUnsigned(L x, R y);</code>.
     */
    SELF_SHIFTRIGHTU(">>>=", "selfShiftRightUnsigned", SHIFTRIGHTU),

    /**
     * Self-left-shift operator.
     * <br><strong>Syntax:</strong> <code>x &lt;&lt; y</code>
     * <br><strong>Method:</strong> <code>T selfShiftLeft(L x, R y);</code>.
     */
    SELF_SHIFTLEFT("<<=", "selfShiftLeft", SHIFTLEFT),

    /**
     * Marker for side effect.
     * <br>Returns this from 'self*' overload method to let the engine know the side effect has been performed and
     * there is no need to assign the result.
     */
    ASSIGN("=", null, null),

    /**
     * Property get operator as in: x.y.
     * <br><strong>Syntax:</strong> <code>x.y</code>
     * <br><strong>Method:</strong> <code>Object propertyGet(L x, R y);</code>.
     */
    PROPERTY_GET(".", "propertyGet", 2),

    /**
     * Property set operator as in: x.y = z.
     * <br><strong>Syntax:</strong> <code>x.y = z</code>
     * <br><strong>Method:</strong> <code>void propertySet(L x, R y, V z);</code>.
     */
    PROPERTY_SET(".=", "propertySet", 3),

    /**
     * Property delete operator as in: delete x.y
     * <br><strong>Syntax:</strong> <code>delete x.y</code>
     * <br><strong>Method:</strong> <code>void propertyDelete(L x, R y);</code>.
     */
    PROPERTY_DELETE(".^", "propertyDelete", 2),

    /**
     * Field get operator as in: x.@y.
     * <br><strong>Syntax:</strong> <code>x.@y</code>
     * <br><strong>Method:</strong> <code>Object fieldGet(L x, R y);</code>.
     */
    FIELD_GET(".@", "fieldGet", 2),

    /**
     * Field set operator as in: x.@y = z.
     * <br><strong>Syntax:</strong> <code>x.@y = z</code>
     * <br><strong>Method:</strong> <code>void fieldSet(L x, R y, V z);</code>.
     */
    FIELD_SET(".@=", "fieldSet", 3),

    /**
     * Array get operator as in: x[y].
     * <br><strong>Syntax:</strong> <code>x.y</code>
     * <br><strong>Method:</strong> <code>Object arrayGet(L x, R y);</code>.
     */
    ARRAY_GET("[]", "arrayGet", 2),

    /**
     * Array set operator as in: x[y] = z.
     * <br><strong>Syntax:</strong> <code>x[y] = z</code>
     * <br><strong>Method:</strong> <code>void arraySet(L x, R y, V z);</code>.
     */
    ARRAY_SET("[]=", "arraySet", 3),

    /**
     * Array delete operator as in: delete x[y].
     * <br><strong>Syntax:</strong> <code>delete x[y]</code>
     * <br><strong>Method:</strong> <code>void arrayDelete(L x, R y);</code>.
     */
    ARRAY_DELETE("[]^", "arrayDelete", 2),

    /**
     * Iterator generator as in for(var x : y).
     * If the returned Iterator is AutoCloseable, close will be called after the last execution of the loop block.
     * <br><strong>Syntax:</strong> <code>for(var x : y){...} </code>
     * <br><strong>Method:</strong> <code>Iterator&lt;Object&gt; forEach(R y);</code>.
     * @since 3.1
     */
    FOR_EACH("for(x:y)", "forEach", 1),

    /**
     * Iterator generator as in for(var i,j : y).
     * If the returned Iterator is AutoCloseable, close will be called after the last execution of the loop block.
     * <br><strong>Syntax:</strong> <code>for(var i,j : y){...} </code>
     * <br><strong>Method:</strong> <code>Iterator&lt;Object&gt; forEachIndexed(R y);</code>.
     * @since 3.2
     */
    FOR_EACH_INDEXED("for(i,j:y)", "forEachIndexed", 1),

    /**
     * Test condition in if, for, while.
     * <br><strong>Method:</strong> <code>boolean testCondition(R y);</code>.
     * @since 3.3
     */
    CONDITION("?", "testCondition", 1),

    /**
     * Compare overload as in compare(x, y).
     * <br><strong>Method:</strong> {@code boolean compare(L x, R y);}.
     * @since 3.4.1
     */
    COMPARE("<>", "compare", 2),

    /**
     * Not-Contains operator.
     * <p>Not overridable, calls !(contain(...))</p>
     */
    NOT_CONTAINS("!~", null, CONTAINS),

    /**
     * Not-Starts-With operator.
     * <p>Not overridable, calls !(startsWith(...))</p>
     */
    NOT_STARTSWITH("!^", null, STARTSWITH),

    /**
     * Not-Ends-With operator.
     * <p>Not overridable, calls !(endsWith(...))</p>
     */
    NOT_ENDSWITH("!$", null, ENDSWITH),

    /**
     * Resource generator as in try(var x : y).
     * If the returned Resource is AutoCloseable, close will be called after the execution of the statement.
     * <br><strong>Syntax:</strong> <code>try(var x : y){...} </code>
     * <br><strong>Method:</strong> <code>Object tryWith(R y);</code>.
     * @since 3.2
     */
    TRY_WITH("try(x:y)", "tryWith", 1);

    /**
     * The operator symbol.
     */
    private final String operator;

    /**
     * The associated operator method name.
     */
    private final String methodName;

    /**
     * The method arity (ie number of arguments).
     */
    private final int arity;

    /**
     * The base operator.
     */
    private final JexlOperator base;

    /**
     * Creates a base operator.
     *
     * @param o    the operator name
     * @param m    the method name associated to this operator in a JexlArithmetic
     * @param argc the number of parameters for the method
     */
    JexlOperator(final String o, final String m, final int argc) {
        this(o, m, null, argc);
    }

    /**
     * Creates a side effect operator with arity == 2.
     *
     * @param o the operator name
     * @param m the method name associated to this operator in a JexlArithmetic
     * @param b the base operator, ie + for +=
     */
    JexlOperator(final String o, final String m, final JexlOperator b) {
        this(o, m, b, 2);
    }

    /**
     * Creates a side effect operator.
     *
     * @param o the operator name
     * @param m the method name associated to this operator in a JexlArithmetic
     * @param b the base operator, ie + for +=
     * @param a the operator arity
     */
    JexlOperator(final String o, final String m, final JexlOperator b, final int a) {
        this.operator = o;
        this.methodName = m;
        this.arity = a;
        this.base = b;
    }

    /**
     * Gets this operator symbol.
     *
     * @return the symbol
     */
    public final String getOperatorSymbol() {
        return operator;
    }

    /**
     * Gets this operator method name in a JexlArithmetic.
     *
     * @return the method name
     */
    public final String getMethodName() {
        return methodName;
    }

    /**
     * Gets this operator number of parameters.
     *
     * @return the method arity
     */
    public int getArity() {
        return arity;
    }

    /**
     * Gets the base operator.
     *
     * @return the base operator
     */
    public final JexlOperator getBaseOperator() {
        return base;
    }

}
