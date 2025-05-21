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
package org.apache.commons.jexl3.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlFeatures;
import org.apache.commons.jexl3.JexlInfo;
import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.internal.Scope;
import org.apache.commons.jexl3.internal.LexicalScope;
import org.apache.commons.jexl3.introspection.JexlUberspect;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Constructor;

import java.math.BigInteger;
import java.math.BigDecimal;

import static org.apache.commons.jexl3.parser.ParserConstants.EOF;
import static org.apache.commons.jexl3.parser.ParserConstants.SEMICOL;

/**
 * The base class for parsing, manages the parameter/local variable frame.
 */
public abstract class JexlParser extends StringParser implements JexlScriptParser {
    /**
     * The associated controller.
     */
    protected final FeatureController featureController = new FeatureController(JexlEngine.DEFAULT_FEATURES);
    /**
     * The associated JexlUberspect.
     */
    protected JexlUberspect uberspect;
    /**
     * The basic source info.
     */
    protected JexlInfo info;
    /**
     * The source being processed.
     */
    protected String source;
    /**
     * The map of named registers aka script parameters.
     * <p>Each parameter is associated to a register and is materialized
     * as an offset in the registers array used during evaluation.</p>
     */
    protected Scope scope;
    /**
     * When parsing inner functions/lambda, need to stack the scope (sic).
     */
    protected final Deque<Scope> scopes = new ArrayDeque<>();
    /**
     * The list of pragma declarations.
     */
    protected Map<String, Object> pragmas;
    /**
     * The list of imported classes.
     */
    protected Map<String, Class> classes;
    /**
     * The known namespaces.
     */
    protected Set<String> namespaces;
    /**
     * Implicitly imported java packages for resolving simple class names from
     */
    protected Collection<String> implicitPackages;
    /**
     * The current lexical block.
     */
    protected LexicalUnit block;
    /**
     * Stack of lexical blocks.
     */
    protected final Deque<LexicalUnit> blocks = new ArrayDeque<>();

    /**
     * A lexical unit is the container defining local symbols and their
     * visibility boundaries.
     */
    public interface LexicalUnit {
        /**
         * Declares a local symbol.
         * @param symbol the symbol index in the scope
         * @return true if declaration was successful, false if symbol was already declared
         */
        boolean declareSymbol(int symbol);

        /**
         * Declares a local symbol.
         * @param symbol the symbol index in the scope
         * @param c the variable type
         * @param lex whether the variable is lexical
         * @param fin whether the variable is final
         * @param req whether the variable is required
         * @return true if declaration was successful, false if symbol was already declared
         */
        boolean declareSymbol(int symbol, Class c, boolean lex, boolean fin, boolean req);

        /**
         * Checks whether a symbol is declared in this lexical unit.
         * @param symbol the symbol
         * @return true if declared, false otherwise
         */
        boolean hasSymbol(int symbol);

        /**
         * Checks whether a symbol is declared lexically scoped in this lexical unit.
         * @param symbol the symbol
         * @return true if declared lexical, false otherwise
         */
        boolean isSymbolLexical(int symbol);

        /**
         * Checks whether a symbol is declared final in this lexical unit.
         * @param symbol the symbol
         * @return true if declared final, false otherwise
         */
        boolean isSymbolFinal(int symbol);

        /**
         * @return the number of local variables declared in this unit
         */
        int getSymbolCount();

        /**
         * @return the set of symbols identifiers declared in this unit
         */
        LexicalScope getLexicalScope();
    }

    /**
     * Cleanup.
     * @param features the feature set to restore if any
     */
    protected void cleanup(final JexlFeatures features) {
        uberspect = null;
        info = null;
        source = null;
        scope = null;
        scopes.clear();
        pragmas = null;
        branchScope = null;
        branchScopes.clear();
        namespaces = null;
        blocks.clear();
        block = null;
        this.setFeatures(features);
    }

    /**
     * Utility function to create '.' separated string from a list of string.
     * @param lstr the list of strings
     * @return the dotted version
     */
    protected static String stringify(final Iterable<String> lstr) {
        final StringBuilder strb = new StringBuilder();
        boolean dot = false;
        for(final String str : lstr) {
            if (!dot) {
               dot = true;
            } else {
               strb.append('.');
            }
            strb.append(str);
        }
        return strb.toString();
    }

    /**
     * Read a given source line.
     * @param src the source
     * @param lineno the line number
     * @return the line
     */
    protected static String readSourceLine(final String src, final int lineno) {
        String msg = "";
        if (src != null && lineno >= 0) {
            try {
                final BufferedReader reader = new BufferedReader(new StringReader(src));
                for (int l = 0; l < lineno; ++l) {
                    msg = reader.readLine();
                }
            } catch (final IOException xio) {
                // ignore, very unlikely but then again...
            }
        }
        return msg;
    }

    /**
     * Internal, for debug purpose only.
     * @param registers whether register syntax is recognized by this parser
     */
    public void allowRegisters(final boolean registers) {
        featureController.setFeatures(new JexlFeatures(featureController.getFeatures()).register(registers));
    }

    /**
     * Sets a new set of options.
     * @param features the parser features
     */
    protected void setFeatures(final JexlFeatures features) {
        this.featureController.setFeatures(features);
    }

    /**
     * @return the current set of features active during parsing
     */
    protected JexlFeatures getFeatures() {
        return featureController.getFeatures();
    }

    /**
     * Disables pragma feature if pragma-anywhere feature is disabled.
     */
    protected void controlPragmaAnywhere() {
        final JexlFeatures features = getFeatures();
        if (features.supportsPragma() && !features.supportsPragmaAnywhere()) {
            featureController.setFeatures(new JexlFeatures(featureController.getFeatures()).pragma(false));
        }
    }

    /**
     * Gets the frame used by this parser.
     * <p>
     * Since local variables create new symbols, it is important to
     * regain access after parsing to known which / how-many registers are needed. 
     * </p>
     * @return the named register map
     */
    protected Scope getScope() {
        return scope;
    }

    /**
     * Create a new local variable scope and push it as current.
     */
    protected void pushScope() {
        if (scope != null) {
            scopes.push(scope);
        }
        scope = new Scope(scope);
        if (branchScope != null) {
            branchScopes.push(branchScope);
        }
        branchScope = new BranchScope();
    }

    /**
     * Pops back to previous local variable scope.
     */
    protected void popScope() {
        if (!branchScopes.isEmpty()) {
            branchScope = branchScopes.pop();
        } else {
            branchScope = null;
        }
        if (!scopes.isEmpty()) {
            scope = scopes.pop();
        } else {
            scope = null;
        }
    }

    /**
     * Gets the lexical unit currently used by this parser.
     * @return the named register map
     */
    protected LexicalUnit getUnit() {
        return block;
    }

    /**
     * Pushes a new lexical unit.
     * @param unit the new lexical unit
     */
    protected void pushUnit(final LexicalUnit unit) {
        if (block != null) {
            blocks.push(block);
        }
        block = unit;
    }

    /**
     * Restores the previous lexical unit.
     * @param unit restores the previous lexical scope
     */
    protected void popUnit(final LexicalUnit unit) {
        if (block == unit){
            if (!blocks.isEmpty()) {
                block = blocks.pop();
            } else {
                block = null;
            }
        }
    }

    /**
     * Checks if a symbol is defined in lexical scopes.
     * <p>This works with parsed scripts in template resolution only.
     * @param info an info linked to a node
     * @param symbol the symbol number
     * @return true if symbol accessible in lexical scope
     */
    private boolean isSymbolDeclared(final JexlNode.Info info, final int symbol) {
        JexlNode walk = info.getNode();
        while (walk != null) {
            if (walk instanceof JexlParser.LexicalUnit) {
                final LexicalScope scope = ((JexlParser.LexicalUnit) walk).getLexicalScope();
                if (scope != null && scope.hasSymbol(symbol)) {
                    return true;
                }
                // stop at first new scope reset, aka lambda
                if (walk instanceof ASTJexlLambda) {
                    break;
                }
            }
            walk = walk.jjtGetParent();
        }
        return false;
    }

    /**
     * Checks whether an identifier is a local variable or argument.
     * @param name the variable name
     * @return true if a variable with that name was declared
     */
    protected boolean isVariable(String name) {
        return scope != null && scope.getSymbol(name) != null;
    }

    /**
     * Checks whether an identifier is a local variable or argument, ie a symbol, stored in a register.
     * @param identifier the identifier
     * @param name the identifier name
     * @return the image
     */
    protected String checkVariable(final ASTIdentifier identifier, final String name) {
        if (scope != null) {
            final Integer symbol = scope.getSymbol(name);
            if (symbol != null) {
                identifier.setSymbol(symbol, name);
                boolean declared = true;
                if (scope.isCapturedSymbol(symbol)) {
                    // captured are declared in all cases
                    identifier.setCaptured(true);
                } else {
                    LexicalUnit unit = block;
                    declared = unit.hasSymbol(symbol);
                    // one of the lexical blocks above should declare it
                    if (!declared) {
                        for (final LexicalUnit u : blocks) {
                            if (u.hasSymbol(symbol)) {
                                unit = u;
                                declared = true;
                                break;
                            }
                            // stop at first new scope reset, aka lambda
                            if (u instanceof ASTJexlLambda) {
                                break;
                            }
                        }
                    }
                    if (declared) {
                        // track if const is defined or not
                        if (unit.isSymbolFinal(symbol)) {
                            identifier.setConstant(true);
                        }
                    } else if (info instanceof JexlNode.Info) {
                        declared = isSymbolDeclared((JexlNode.Info) info, symbol);
                    }
                }
                if (!declared) {
                    identifier.setShaded(true);
                    if (getFeatures().isLexicalShade()) {
                        // can not reuse a local as a global
                        throw new JexlException.Parsing(info, name + ": variable is not defined").clean();
                    }
                }
            }
        }
        return name;
    }

    /**
     * Checks whether a local variable is lexically defined.
     * @param image the identifier image
     * @return true if final, false otherwise
     */
    protected boolean isLexicalVariable(String image) {
        if (scope != null) {
            Integer symbol = scope.getSymbol(image);
            if (symbol != null) {
                if (scope.isVariableLexical(symbol)) {
                    return true;
                } else {
                    if (block.hasSymbol(symbol))
                        return block.isSymbolLexical(symbol);
                    // one of the lexical blocks above should declare it
                    for (LexicalUnit u : blocks) {
                        if (u.hasSymbol(symbol)) {
                            return u.isSymbolLexical(symbol);
                        }
                        // stop at first new scope reset, aka lambda
                        if (u instanceof ASTJexlLambda) {
                            break;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks whether a local variable is final.
     * @param image the identifier image
     * @return true if final, false otherwise
     */
    protected boolean isFinalVariable(String image) {
        if (scope != null) {
            Integer symbol = scope.getSymbol(image);
            if (symbol != null) {
                if (scope.isVariableFinal(symbol)) {
                    return true;
                } else {
                    if (block.hasSymbol(symbol))
                        return block.isSymbolFinal(symbol);
                    // one of the lexical blocks above should declare it
                    for (LexicalUnit u : blocks) {
                        if (u.hasSymbol(symbol)) {
                            return u.isSymbolFinal(symbol);
                        }
                        // stop at first new scope reset, aka lambda
                        if (u instanceof ASTJexlLambda) {
                            break;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Whether a given variable name is allowed.
     * @param image the name
     * @return true if allowed, false if reserved
     */
    protected boolean allowVariable(final String image) {
        final JexlFeatures features = getFeatures();
        if (!features.supportsLocalVar()) {
            return false;
        }
        if (features.isReservedName(image)) {
            return false;
        }
        return true;
    }

    /**
     * Declares a symbol.
     * @param symbol the symbol index
     * @param c the variable type
     * @param lex whether the variable is lexical
     * @param fin whether the variable is final
     * @param req whether the variable is required
     * @return true if symbol can be declared in lexical scope, false (error)
     * if it is already declared
     */
    private boolean declareSymbol(int symbol, Class c, boolean lex, boolean fin, boolean req) {
        for (LexicalUnit lu : blocks) {
            if (lu.hasSymbol(symbol)) {
                return false;
            }
            // stop at first new scope reset, aka lambda
            if (lu instanceof ASTJexlLambda) {
                break;
            }
        }
        return block == null || block.declareSymbol(symbol, c, lex, fin, req);
    }

    /**
     * Declares a symbol.
     * @param symbol the symbol index
     * @return true if symbol can be declared in lexical scope, false (error)
     * if it is already declared
     */
    private boolean declareSymbol(final int symbol) {
        for (final LexicalUnit lu : blocks) {
            if (lu.hasSymbol(symbol)) {
                return false;
            }
            // stop at first new scope reset, aka lambda
            if (lu instanceof ASTJexlLambda) {
                break;
            }
        }
        return block == null || block.declareSymbol(symbol);
    }

    /**
     * Declares a local variable.
     * <p>
     * This method creates an new entry in the symbol map.
     * </p>
     *
     * @param variable the identifier used to declare
     * @param token the variable name token
     */
    protected void declareVariable(final ASTVar variable, final Token token) {
        final String name = token.image;
        if (!allowVariable(name)) {
            throwFeatureException(JexlFeatures.LOCAL_VAR, token);
        }
        if (scope == null) {
            scope = new Scope();
        }
        Integer symbol = scope.getSymbol(name, false);
        if (symbol != null && scope.isVariableFinal(symbol) && !scope.isCapturedSymbol(symbol)) {
            throwParsingException(variable);
        }
        symbol = scope.declareVariable(name);
        variable.setSymbol(symbol, name);
        if (scope.isCapturedSymbol(symbol)) {
            variable.setCaptured(true);
        }
        // if not the first time we declare this symbol...
        if (!declareSymbol(symbol, variable.getType(), variable.isLexical(), variable.isConstant(), variable.isRequired())) {
            if (isLexicalVariable(name) || variable.isLexical() || getFeatures().isLexical()) {
                throw new JexlException.Parsing(variable.jexlInfo(info != null ? info.getName() : null, info != null ? info.getPath() : null), name + ": variable is already declared").clean();
            }
            variable.setRedefined(true);
        }
    }

    /**
     * The name of the options pragma.
     */
    public static final String PRAGMA_OPTIONS = "jexl.options";
    /**
     * The prefix of a namespace pragma.
     */
    public static final String PRAGMA_JEXLNS = "jexl.namespace.";
    /**
     * The prefix of a module pragma.
     */
    public static final String PRAGMA_MODULE = "jexl.module.";
    /**
     * The import pragma.
     */
    public static final String PRAGMA_IMPORT = "jexl.import";

    /**
     * Adds a pragma declaration.
     * @param key the pragma key
     * @param value the pragma value
     */
    protected void declarePragma(final String key, final Object value) {
        if (!getFeatures().supportsPragma()) {
            throwFeatureException(JexlFeatures.PRAGMA, getToken(0));
        }
        if (PRAGMA_IMPORT.equals(key) && !getFeatures().supportsImportPragma()) {
            throwFeatureException(JexlFeatures.IMPORT_PRAGMA, getToken(0));
        }
        if (pragmas == null) {
            pragmas = new TreeMap<>();
        }
        // declaring a namespace or module
        final String[] nsprefixes = { PRAGMA_JEXLNS, PRAGMA_MODULE };
        for(String nsprefix : nsprefixes) {
            if (key.startsWith(nsprefix)) {
                if (!getFeatures().supportsNamespacePragma()) {
                    throwFeatureException(JexlFeatures.NS_PRAGMA, getToken(0));
                }
                final String nsname = key.substring(nsprefix.length());
                if (!nsname.isEmpty()) {
                    if (namespaces == null) {
                        namespaces = new HashSet<>();
                    }
                    namespaces.add(nsname);
                }
                break;
            }
        }

        if (PRAGMA_IMPORT.equals(key)) {
            // jexl.import, may use a set
            Set<?> values = value instanceof Set<?>
                    ? (Set<?>) value
                    : Collections.singleton(value);
            for (Object o : values) {
                if (o instanceof String) {
                    if (implicitPackages == null) {
                        implicitPackages = new LinkedHashSet<>();
                    } else {
                        implicitPackages = new LinkedHashSet<>(implicitPackages);
                    }
                    implicitPackages.add(o.toString());
                }
            }
            classes = null;
        }

        // merge new value into a set created on the fly if key is already mapped
        pragmas.merge(key, value, (previous, newValue)->{
            if (previous instanceof Set<?>) {
                ((Set<Object>) previous).add(newValue);
                return previous;
            }
            Set<Object> values = new LinkedHashSet<>();
            values.add(previous);
            values.add(newValue);
            return values;
        });
    }

    /**
     * Semantic check identifying whether a list of 4 tokens forms a namespace function call.
     * <p>This is needed to disambiguate ternary operator, map entries and actual calls.</p>
     * <p>Note that this check is performed before syntactic check so the expected parameters need to be
     * verified.</p>
     * @param ns the namespace token
     * @param colon expected to be &quot;:&quot;
     * @param fun the function name
     * @param paren expected to be &quot;(&quot;
     * @return true if the name qualifies a namespace function call
     */
    protected boolean isNamespaceFuncall(final Token ns, final Token colon, final Token fun, final Token paren) {
        // let's make sure this is a namespace function call
        if (!":".equals(colon.image)) {
            return false;
        }
        if (!"(".equals(paren.image)) {
            return false;
        }
        // if namespace name is shared with a variable name, use syntactic hint
        final String name = ns.image;
        if (isVariable(name)) {
            // the namespace sticks to the colon as in 'ns:fun()' (vs 'ns : fun()')
            return colon.beginColumn - 1 == ns.endColumn && ((colon.endColumn == fun.beginColumn - 1) || isNamespace(name));
        }
        return true;
    }

    /**
     * Checks whether a name is a declared namespace.
     * @param name the namespace name
     * @return true if declared, false otherwise
     */
    private boolean isNamespace(String name) {
        // templates
        if ("jexl".equals(name) || "$jexl".equals(name)) {
            return true;
        }
        final Set<String> ns = namespaces;
        // declared through local pragma ?
        if (ns != null && ns.contains(name)) {
            return true;
        }
        // declared through engine features ?
        if (getFeatures().namespaceTest().test(name)) {
            return true;
        }
        return false;
    }

    /**
     * Declares a local parameter.
     * <p>
     * This method creates an new entry in the symbol map.
     * </p>
     *
     * @param token the parameter name token
     */ 
    protected void declareParameter(final Token token) {
        declareParameter(token, null, false, false, false, null);
    }

    /**
     * Declares a local parameter.
     * <p>
     * This method creates an new entry in the symbol map.
     * </p>
     * @param token the parameter name token
     * @param type the parameter class if any
     * @param lexical whether the declared parameter is lexical
     * @param constant whether the declared parameter is final
     * @param required whether the declared parameter is required
     * @param value the parameter default value
     */
    protected void declareParameter(final Token token, final Class type, final boolean lexical, final boolean constant, final boolean required, final Object value) {
        final String identifier = token.image;
        if (!allowVariable(identifier)) {
            throwFeatureException(JexlFeatures.LOCAL_VAR, token);
        }
        if (scope == null) {
            scope = new Scope();
        }
        final int symbol = scope.declareParameter(identifier, type, lexical, constant, required, value);
        // not sure how declaring a parameter could fail...
        // lexical feature error
        if (!block.declareSymbol(symbol) && getFeatures().isLexical()) {
            final JexlInfo xinfo = info.at(token.beginLine, token.beginColumn);
            throw new JexlException.Parsing(xinfo, identifier + ": variable is already declared").clean();
        }
    }

    /**
     * Declares a static scope.
     */
    protected void declareStaticSupport() {
        if (scope == null) {
            scope = new Scope();
        }
        scope.declareStatic();
    }

    /**
     * Declares a local vararg parameter.
     */
    protected void declareVarArgSupport() {
        if (scope == null) {
            scope = new Scope();
        }
        scope.declareVarArgs();
    }

    /**
     * If this script expects a variable number of arguments.
     * @return true or false
     */
    protected boolean isVarArgs() {
        return scope != null && scope.isVarArgs();
    }

    /**
     * Declares a return type.
     * @param type the return type
     */
    protected void declareReturnType(Class type) {
        if (type == null)
            return;
        if (scope == null) {
            scope = new Scope();
        }
        scope.declareReturnType(type);
    }

    /**
     * If this script expects a variable number of arguments.
     * @return true or false
     */
    protected boolean isDeclaredVoid() {
        return scope != null && scope.getReturnType() == Void.TYPE;
    }

    /**
     * Default implementation does nothing but is overridden by generated code.
     * @param top whether the identifier is beginning an l/r value
     * @throws ParseException subclasses may throw this
     */
    protected void Identifier(final boolean top) throws ParseException {
        // Overridden by generated code
    }

    final protected void Identifier() throws ParseException {
        Identifier(false);
        // Overridden by generated code
    }

    /**
     * Overridden in actual parser to access tokens stack.
     * @param index 0 to get current token
     * @return the token on the stack
     */
    protected abstract Token getToken(int index);

    /**
     * Overridden in actual parser to access tokens stack.
     * @return the next token on the stack
     */
    protected abstract Token getNextToken();

    /**
     * The set of assignment operators as classes.
     */
    private static final Set<Class<? extends JexlNode>> ASSIGN_NODES = new HashSet<>(
        Arrays.asList(
            ASTAssignment.class,
            ASTMultipleAssignment.class,
            ASTNullAssignment.class,
            ASTNEAssignment.class,
            ASTSetAddNode.class,
            ASTSetSubNode.class,
            ASTSetMultNode.class,
            ASTSetDivNode.class,
            ASTSetModNode.class,
            ASTSetAndNode.class,
            ASTSetOrNode.class,
            ASTSetDiffNode.class,
            ASTSetXorNode.class,
            ASTSetShiftLeftNode.class,
            ASTSetShiftRightNode.class,
            ASTSetShiftRightUnsignedNode.class,
            ASTIncrementGetNode.class,
            ASTDecrementGetNode.class,
            ASTGetIncrementNode.class,
            ASTGetDecrementNode.class,
            ASTDelete.class
        )
    );

    /**
     * Called by parser at beginning of node construction.
     * @param node the node
     */
    protected void jjtreeOpenNodeScope(final JexlNode node) {
        // nothing
    }

    /**
     * Called by parser at end of node construction.
     * <p>
     * Detects "Ambiguous statement" and 'non-left value assignment'.</p>
     * @param node the node
     * @throws JexlException.Parsing when parsing fails
     */
    protected void jjtreeCloseNodeScope(final JexlNode node) {
        if (node instanceof ASTAmbiguous) {
            throwAmbiguousException(node);
        }
        if (node instanceof ASTJexlScript) {
            if (node instanceof ASTJexlLambda && !getFeatures().supportsLambda()) {
                throwFeatureException(JexlFeatures.LAMBDA, node.jexlInfo());
            }
        } else if (ASSIGN_NODES.contains(node.getClass())) {
            final JexlNode lv = node.jjtGetChild(0);
            if (!lv.isLeftValue()) {
                JexlInfo xinfo = lv.jexlInfo();
                xinfo = info.at(xinfo.getLine(), xinfo.getColumn());
                final String msg = lv instanceof ASTIdentifier ? ((ASTIdentifier) lv).getName() :
                    readSourceLine(source, xinfo.getLine());
                throw new JexlException.Assignment(xinfo, msg).clean();
            }
        } else if (node instanceof ASTPointerNode) {
            JexlNode lv = node.jjtGetChild(0);
            if (!lv.isLeftValue()) {
                throw new JexlException.Assignment(lv.jexlInfo(), null).clean();
            }
        } else if (node instanceof ASTMultipleAssignment) {
            JexlNode lv = node.jjtGetChild(0);
            if (!lv.isLeftValue()) {
                JexlInfo xinfo = lv.jexlInfo();
                xinfo = info.at(xinfo.getLine(), xinfo.getColumn());
                final String msg = readSourceLine(source, xinfo.getLine());
                throw new JexlException.Assignment(xinfo, msg).clean();
            }
        }

        // heavy check
        featureController.controlNode(node);
    }

    /**
     * Check fat vs thin arrow syntax feature.
     * @param token the arrow token
     */
    protected void checkLambda(Token token) {
        final String arrow = token.image;
        if ("->".equals(arrow)) {
            if (!getFeatures().supportsThinArrow()) {
                throwFeatureException(JexlFeatures.THIN_ARROW, token);
            }
            return;
        }
        if ("=>".equals(arrow) && !getFeatures().supportsFatArrow()) {
            throwFeatureException(JexlFeatures.FAT_ARROW, token);
        }
    }

    /**
     * Throws Ambiguous exception.
     * <p>Seeks the end of the ambiguous statement to recover.
     * @param node the first token in ambiguous expression
     * @throws JexlException.Ambiguous in all cases
     */
    protected void throwAmbiguousException(final JexlNode node) {
        final JexlInfo begin = node.jexlInfo(info != null ? info.getName() : null, info != null ? info.getPath() : null);
        final Token t = getToken(0);
        final JexlInfo end = info.at(t.beginLine, t.endColumn);
        final String msg = readSourceLine(source, end.getLine());
        throw new JexlException.Ambiguous(begin, end, msg).clean();
    }

    /**
     * Throws a feature exception.
     * @param feature the feature code
     * @param info the exception surroundings
     * @throws JexlException.Feature in all cases
     */
    protected void throwFeatureException(final int feature, final JexlInfo info) {
        final String msg = info != null? readSourceLine(source, info.getLine()) : null;
        throw new JexlException.Feature(info, feature, msg).clean();
    }

    /**
     * Throws a feature exception.
     * @param feature the feature code
     * @param trigger the token that triggered it
     * @throws JexlException.Parsing if actual error token can not be found
     * @throws JexlException.Feature in all other cases
     */
    protected void throwFeatureException(final int feature, Token trigger) {
        Token token = trigger;
        if (token == null) {
            token = this.getToken(0);
            if (token == null) {
                throw new JexlException.Parsing(null, JexlFeatures.stringify(feature)).clean();
            }
        }
        final JexlInfo xinfo = info.at(token.beginLine, token.beginColumn);
        throwFeatureException(feature, xinfo);
    }

    /**
     * Throws a parsing exception.
     * @param node the node that caused it
     */
    protected void throwParsingException(final JexlNode node) {
        throwParsingException((Token) null);
    }

    /**
     * Creates a parsing exception.
     * @param xclazz the class of exception
     * @param parsed the token to report
     * @param <T> the parsing exception subclass
     * @throws JexlException.Parsing in all cases
     */
    protected void throwParsingException(final Token parsed) {
        JexlInfo xinfo  = null;
        String msg = "unrecoverable state";
        JexlException.Parsing xparse = null;
        Token token = parsed;
        if (token == null) {
            token = this.getToken(0);
        }
        if (token != null) {
            xinfo = info.at(token.beginLine, token.beginColumn);
            msg = token.image;
        }
        throw new JexlException.Parsing(xinfo, msg).clean();
    }

    /**
     * Checks whether the class has simple name 
     * @param c the type
     * @return true if class has simple name
     */
    public boolean isSimpleName(Class c) {
        String qn = c.getName();
        Package pack = c.getPackage();
        String p = pack != null ? pack.getName() : null;
        if (p == null)
            return true;
        for (String packageName : implicitPackages) {
            if (p.equals(packageName))
                return true;
        }
        return false;
    }

    /**
     * Constructs an array type
     * @param c the component type
     * @return the Class
     */
    protected static Class arrayType(Class c) {
        return c != null ? Array.newInstance(c, 0).getClass() : null;
    }

    private static Class NOT_A_CLASS = (new Object() {}).getClass();

    /**
     * Resolves a type by its name.
     * @param name the name of the type
     * @return the Class
     */
    public Class resolveType(String name) {
        if (name == null || "".equals(name))
            return null;
        switch (name) {
            case "char" : return Character.TYPE;
            case "boolean" : return Boolean.TYPE;
            case "byte" : return Byte.TYPE;
            case "short" : return Short.TYPE;
            case "int" : return Integer.TYPE;
            case "long" : return Long.TYPE;
            case "float" : return Float.TYPE;
            case "double" : return Double.TYPE;
            case "void" : return Void.TYPE;
            case "Character" : return Character.class;
            case "Boolean" : return Boolean.class;
            case "Byte" : return Byte.class;
            case "Short" : return Short.class;
            case "Integer" : return Integer.class;
            case "Long" : return Long.class;
            case "Float" : return Float.class;
            case "Double" : return Double.class;
            case "BigInteger" : return BigInteger.class;
            case "BigDecimal" : return BigDecimal.class;
            case "Object" : return Object.class;
            case "String" : return String.class;
            case "Exception" : return Exception.class;
            case "Throwable" : return Throwable.class;
        }

        if (Character.isLowerCase(name.charAt(0)) && name.indexOf(".") == -1)
            return null;

        if (classes == null)
            classes = new ConcurrentHashMap<String, Class> ();
        Class result = classes.computeIfAbsent(name, x -> forName(x));
        return result != NOT_A_CLASS ? result : null;
    }

    /**
     * Resolves a type that is subtype by its name.
     * @param type the supertype
     * @param name the name of the type
     * @return the Class that is subtype of the specified type, or null otherwise
     */
    public Class resolveType(Class type, String name) {
        Class result = resolveType(name);
        if (result != null && type.isAssignableFrom(type)) {
            return result;
        } else {
            return null;
        }
    }

    /**
     * Resolves a nested type by its name.
     * @param type the type
     * @param name the name of the nested type
     * @return the Class or null otherwise
     */
    public static Class resolveNestedType(Class type, String name) {
        if (name == null)
            return null;
        for (Class c : type.getClasses()) {
            if (name.equals(c.getSimpleName()))
                return c;
        }
        return null;
    }

    /**
     * Gets a class by its name.
     * @param name the name of the class
     * @return the Class
     */
    protected Class forName(String name) {
        if (name.indexOf(".") == -1) {
            for (String prefix : implicitPackages) {
                String className = prefix + "." + name;
                Class result = uberspect.getClassByName(className);
                if (result != null)
                    return result;
            }
            return NOT_A_CLASS;
        }
        Class result = uberspect.getClassByName(name);
        return result != null ? result : NOT_A_CLASS;
    }

    /**
     * Resolves an instantiable type by its name.
     * @param name the name of the type
     * @return the Class
     */
    protected Class resolveInstantiableType(String name) {
        Class result = resolveType(name);
        return (result == null || result.isPrimitive() || result.isInterface() || result.isMemberClass() ||
                result.isAnnotation() || result.isEnum() || result.isArray() ||
                Modifier.isAbstract(result.getModifiers())) ? null : result;
    }

    /**
     * Resolves a static field of the type by its name.
     * @param c the the type
     * @param name the name of the field
     * @return the value or null if not resolved
     */
    protected Object resolveStaticField(Token t, Class<?> c, String name) {
        if (name == null)
            return null;
        if (c == null) {
            c = Object.class;
        } else {
            c = JexlArithmetic.getWrapperClass(c);
        }
        try {
            Field field = c.getDeclaredField(name);
            if (Modifier.isStatic(field.getModifiers())) {
                return field.get(null);
            }
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            //
        }
        throwParsingException(t);
        return null;
    }

    /**
     * The target scope class for break/continue/remove/yield statements.
     */
    protected class BranchScope {

        protected int loopCount = 0;
        protected int foreachLoopCount = 0;
        protected int switchCount = 0;
        protected int yieldCount = 0;
        protected int yieldReturnCount = 0;

        protected Deque<String> blockLabels = new ArrayDeque<String> ();
        protected Deque<String> loopLabels = new ArrayDeque<String> ();
        protected Deque<String> foreachLabels = new ArrayDeque<String> ();

        protected boolean breakSupported() {
            return loopCount > 0 || foreachLoopCount > 0 || switchCount > 0;
        }

        protected boolean breakSupported(String label) {
            return blockLabels.contains(label);
        }

        protected boolean yieldSupported() {
            return yieldCount > 0 || yieldReturnCount > 0;
        }

        protected boolean continueSupported() {
            return loopCount > 0 || foreachLoopCount > 0;
        }

        protected boolean continueSupported(String label) {
            return loopLabels.contains(label);
        }

        protected boolean removeSupported() {
            return foreachLoopCount != 0;
        }

        protected boolean removeSupported(String label) {
            return foreachLabels.contains(label);
        }

        protected void pushBlockLabel(String label) {
            blockLabels.push(label);
        }

        protected void popBlockLabel() {
            blockLabels.pop();
        }

        protected void pushLoopLabel(String label) {
            blockLabels.push(label);
            loopLabels.push(label);
        }

        protected void popLoopLabel() {
            blockLabels.pop();
            loopLabels.pop();
        }

        protected void pushForeachLabel(String label) {
            blockLabels.push(label);
            loopLabels.push(label);
            foreachLabels.push(label);
        }

        protected void popForeachLabel() {
            blockLabels.pop();
            loopLabels.pop();
            foreachLabels.pop();
        }
    }

    /**
     * The current scope for break/continue/remove statements.
     */
    protected BranchScope branchScope = null;

    /**
     * When parsing inner functions/lambda, need to stack the target scope
     */
    protected Deque<BranchScope> branchScopes = new ArrayDeque<BranchScope> ();

    /**
     * Pick the most significant token for error reporting.
     * @param tokens the tokens to choose from
     * @return the token
     */
    protected static Token errorToken(final Token... tokens) {
        for (final Token token : tokens) {
            if (token != null && token.image != null && !token.image.isEmpty()) {
                return token;
            }
        }
        return null;
    }
}
