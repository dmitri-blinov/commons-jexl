<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
Apache Commons JEXL Pro
=======================

The Apache Commons JEXL Pro library is an experimental fork of the The Apache Commons JEXL library.

Idea of the fork
----------------
While JEXL in its latest version (3.3-SNAPSHOT) is already a mature Java object manipulation language which supports many great features,
I feel that something more can be done to fill the gap between what functionality Java objects expose to the environment and
how it can be tackled in JEXL by its current means. The fork is intended to be source compatible with the JEXL, but provide some
enhancements and changes to the capabilities of the scripting language. In result, compatibility with Java language is greatly improved,
as the majority of modern Java syntax is now supported. Such compatibility gives the script writers the opportunity to start using
the language faster by using well-known patterns, for example, the following Java code snippet is perfectly valid
construct in JEXL Pro and gives the same results as in Java:

```
String bytesToHex(byte[] bytes, int offset, int count) {
    final char[] hexArray = "0123456789ABCDEF".toCharArray();
    char[] hexChars = new char[count * 2];
    for (int j = 0; j < count; j++) {
        int v = bytes[j+offset] & 0xFF;
        hexChars[j * 2] = hexArray[v >>> 4];
        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
}
```

On the other hand, some new language features and more syntactic sugar are aimed at productivity and will allow for more compact and
less error-prone code to be written, for example, instead of writing

```
var x = [...]; for (var i : items) if (i.color == 'red') x += i.price;
```
one can write

```
var x = [...{items.[@.color == 'red'].{@.price}}]
```

There are also some under-the-hood performance and memory usage improvements.

Development Roadmap
----------------------
The fork is feature complete and stable in both design and implementation, though minor enhancements are likely to come.
The library source code is aligned with upstream as much as possible, passing practically all original test cases, except a coulple
of those that were intentionally disabled as obsolete due to incompatible changes, see below.
Periodic alignment with upstream is being done, with fork being already more than three years old.

Distribution Status
----------------------
I have no intention of promoting this fork as an alternative to the main library, and I would be happy to have all
the changes to be backported to the base JEXL library one day, but the decision whether these changes are the ones
the JEXL community would benefit from remains at the descretion of the Apache JEXL team. In a mean time everyone is welcomed
to use it for testing or work

Language Compatibility
----------------------
The library tends to maintain as much syntax compatibility with the original syntax as possible, but there are
some changes that may break your existing scripts. The main reason for this comes from the introduction of the new
reserved words to support new syntax constructs, so your variables may no longer be named by one of those keywords
that are introduced. There are also some minor tweaks to the original syntax in order to streamline language structure and
align some language constructs with other popular scripting languages, to minimize the learning curve and syntactic diversity.
These changes are all reflected in the documentation, but the breef summary is given below.

Incompatible changes
--------------------
+ New reserved words are introduced. Those are:
  `switch` `case` `default` `try` `catch` `finally` `throw` `synchronized` `this` `instanceof` `in` `remove` `delete` `static`
  `assert` `final` `boolean` `char` `byte` `short` `int` `long` `float` `double` `void` `class` `yield` `_`.
  You may not longer use them as the names of the variables.

+ Pragmas can only be defined at the beginning of the script. The reason for this is that the pragmas are not executable constructs,
  so it is pointless and misleading to have them being incorporated in flow-control structures somewhere in the middle.

+ Literal `null` can not have any properies, so it is forbidden to use it in expressions like `null.prop`.
  If, for some reason, you still want to do this, use parentheses like `(null).prop`.

+ Precedence of the `range` operator (`..`) is changed to be higher than that of relational operators,
  but lower than that of arithmetic operators.

+ Precedence of the `match` operator (`=~`) and `not-match` operator (`!~`) is changed to be that of other equality operators.

+ Passing to a function more arguments than is specified in the function declaration now results in error

+ Captured variables are final by default. The reason for this is that captured variables are in fact copies of the original variables,
  so assigning a new value to a captured variable does not affect the original variable in outer scope.
  Such behaviour is misleading and thus restricted.

+ Left-hand assignment expression can not use safe access operator `?.`

+ Arrays do not expose wrapper methods `.contains()`, `.get()`, `.set()` etc

+ Class literals `Integer.class` may cover variable access if the variable name can be resolved as a class name

New features
------------
+ Java-like `switch` statement is introduced

+ Java-like `synchronized` statement is introduced

+ Java-like `try-with-resources` statement is introduced

+ Java-like `try-catch-finally` statement is introduced

+ Java-like `throw` statement is introduced

+ Java-like `assert` statement is introduced

+ Java-like `this` literal is introduced to allow easier access to the current evaluation context

+ Java-like `instanceof` operator is introduced

+ Java-like method reference `::` operator is introduced

+ Java-like switch expression `switch` operator is introduced

+ Java-like `yield` statement is introduced

+ Javascript-like `delete` operator is introduced

+ Java-like type-cast `()` operator is introduced

+ Java-like class `Integer.class` literals are introduced

+ Java-like static class field/method access can be resolved via direct type specification `Integer.MAX_VALUE` if not shaded by local or context variables

+ Java-like text block `"""\nabcd"""` literal is introduced

+ Java-like `.length` property is exposed for arrays

+ Groovy-like `.@` field access operator is introduced

+ Groovy-like `!instanceof` operator is introduced

+ Groovy-like `?=` conditional assignment operator is introduced

+ Javascript-like `===` and `!==` identity operators are introduced

+ C-like pointer `&` and pointer dereference `*` operators are introduced

+ C#-like safe array access `?[]` operator is introduced

+ New equality conditional assignment `:=` operator is introduced

+ New flow-control `remove` statement is introduced

+ New Map.Entry `[a : b]` literal is introduced

+ New StringBuilder `"abcd"...` literal is introduced

+ New block evaluation `({})` operator is introduced

+ New iterator `...arr` operator is introduced

+ New iterator generator `...{for (var i : 1 .. 10) yield i;}` operator is introduced

+ New iterator selection `a.[@.color == 'red']` operator is introduced

+ New iterator projection `a.{@.qty,@.price}` operator is introduced

+ New pipe `foo.(x -> {x + 1})` operator is introduced

+ New multiple assignment statement `(x,_,y) = [2,1,3]` is introduced

+ New inline property assignment `a{b:3,c:4}` construct is introduced

+ New set operators *any* `?()` and *all* `??()` that can be used in conjunction with relational operators are introduced

+ New predicates operators like (>42) or (<0) are introduced

+ New bitwise difference operator ` \ ` is introduced

Enhancements
------------
+ Java-like labelled blocks and statements like `switch`, `for`, `while`, `do`, `if`, `try`, `synchronized` can be used.
  The defined labels can be further specified for inner `break`, `continue` and `remove` flow-control statements

+ Multidimensional arrays can be accessed by using new syntax `arr[x,y]` as well as by using older syntax `arr[x][y]`

+ Array element assignment operator `arr[x] = value` now tries to perform implicit type cast of the assigned value

+ Variadic functions can be defined by using `(a...)` syntax after the last function argument

+ Function closures implement all basic Java 8 `@FunctionalInterface` interfaces,
  so that it is possible to pass a function closure as an argument to a java method that accepts such interfaces

+ Java-like `for` classical loop statement is supported with full java syntax

+ Java-like increment/decrement `++` and `--`  operators use self-asignment operators for evaluation

+ Function can be declared as `static` to prevent variable capturing

+ Function parameters can be declared strongly typed by using java class or primitive types `function(int a, int b) {a+b}`

+ Function parameters can be declared as final `function(final var x) {}`

+ Function parameters can be declared as non-null `function(var &x) {}`

+ Function parameters can have default values `function(var x = 0) {}`

+ Functions can be declared strongly typed or void by using java class or primitive types `int a(int a, int b) {a+b}`

+ Return statement expression can be omitted, implying `null` as a result

+ Return statement should not use value expression for void functions

+ Local variables can be declared strongly typed by using java class or primitive types `int i = 0`

+ Local variables can be declared as final `final var i = 0`

+ Multiple local variables of the same type can be declared `int i = 0, j = 1`

+ Local variables can be declared as non-null `var &i = 0`

+ Last part of the ternary expression `x?y:z` (along with the separating `:`) can be omitted, implying `null` as a result

+ Pattern matching operators `=~` and `!~` can use new `in` and `!in` aliases, may be disabled

+ Operator `new` supports Java-like inner object creation syntax `outerObject.new InnerClass()`

+ Operator `new` supports Java-like initialized array creation syntax `new String[] {'abc','def'}`

+ Operator `new` supports initialized collection creation syntax `new LinkedHashSet {'abc','def'}`

+ Operator `new` supports initialized map creation syntax `new LinkedHashMap {'abc' : 1, 'def' : 2}`

+ Foreach statement may also define additional `counter` variable `for (var counter,x : list) {}` along with the current loop value variable

+ Foreach statement support strongly types variable `for (String x : list) {}`

+ Immutable list `#[1,2,3]` literal constructs can be used

+ Immutable set `#{1,2,3}` literal constructs can be used

+ Immutable map `#{1:2,3:4}` literal constructs can be used

+ Ordered set `{1,2,3,...}` literal constructs can be used

+ Ordered map `{1:2,3:4,...}` literal constructs can be used

+ Array comprehensions `[...a]` can be used in array literals

+ Set comprehensions `{...a}` can be used in set literals

+ Map comprehensions `{*:...a}` can be used in map literals

+ Function argument comprehensions `func(...a)` can be used

+ Java-like binary format `0b...` support for natural literals

+ Java-like support for underscores in numeric literals

+ Groovy-like lambda composition operators `<<`, `>>` may be used

+ Corresponding unicode characters may be used for the operators like `!=`, `>=` etc

License
-------
This code is under the [Apache Licence v2](https://www.apache.org/licenses/LICENSE-2.0).

See the `NOTICE.txt` file for required notices and attributions.
