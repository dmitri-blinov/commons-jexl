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
package org.apache.commons.jexl3.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * A script scope, stores the declaration of parameters and local variables as symbols.
 * <p>This also acts as the functional scope and variable definition store.
 * @since 3.0
 */
public final class Scope {
    /**
     * The value of an as-yet  undeclared but variable, for instance: x; before var x;.
     */
    static final Object UNDECLARED = new Object() {
        @Override public String toString() {
            return "??";
        }
    };
    /**
     * The value of a declared but undefined variable, for instance: var x;.
     */
    static final Object UNDEFINED = new Object() {
        @Override public String toString() {
            return "?";
        }
    };
    /**
     * The parent scope.
     */
    private final Scope parent;
    /**
     * The number of parameters.
     */
    private int parms;
    /**
     * If params have variable number.
     */
    private boolean varArgs;
    /**
     * If scope is static.
     */
    private boolean isStatic;
    /**
     * Return type
     */
    private Class returnType;
    /**
     * The number of local variables.
     */
    private int vars;
    /**
     * The map of named variables aka script parameters and local variables.
     * Each parameter is associated to a symbol and is materialized as an offset in the stacked array used
     * during evaluation.
     */
    private Map<String, Integer> namedVariables = null;
    /**
     * The map of local captured variables to parent scope variables, ie closure.
     */
    private Map<Integer, Integer> capturedVariables = null;
    /**
     * The map of variable types.
     * Each variable may be associated with specific type
     */
    private Map<Integer, Class> variableTypes = null;
    /**
     * The set of final variables.
     */
    private Set<Integer> finalVariables = null;
    /**
     * The set of non-null variables.
     */
    private Set<Integer> requiredVariables = null;
    /**
     * The map of default values for local variables, ie parameters.
     */
    private Map<Integer, Object> defaultValues = null;
    /**
     * The empty string array.
     */
    private static final String[] EMPTY_STRS = new String[0];

    /**
     * Creates a new scope.
     */
    public Scope() {
       this(null);
    }

    /**
     * Creates a new scope with a list of parameters.
     * @param scope the parent scope if any
     * @param parameters the list of parameters
     */
    public Scope(final Scope scope, final String... parameters) {
        if (parameters != null) {
            parms = parameters.length;
            for (int p = 0; p < parms; ++p) {
                addSymbol(parameters[p]);
            }
        } else {
            parms = 0;
        }
        vars = 0;
        parent = scope;
    }

    @Override
    public int hashCode() {
        return namedVariables == null ? 0 : parms ^ namedVariables.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Scope)) {
            return false;
        }
        final Scope scope = (Scope) o;
        if (parms != scope.parms) {
            return false;
        }
        if (namedVariables == null) {
            return scope.namedVariables == null;
        } else {
            return namedVariables.equals(scope.namedVariables);
        }
    }

    /**
     * Adds identifier as a local symbol.
     * @param name the symbol name
     * @return the symbol index
     */
    public Integer addSymbol(String name) {
        if (namedVariables == null) {
            namedVariables = new LinkedHashMap<String, Integer>();
        }
        Integer register = namedVariables.size();
        namedVariables.put(name, register);
        return register;
    }

    /**
     * Checks whether an identifier is a local variable or argument, ie a symbol.
     * If this fails, look in parents for symbol that can be captured.
     * @param name the symbol name
     * @return the symbol index
     */
    public Integer getSymbol(final String name) {
        return getSymbol(name, true);
    }

    /**
     * Checks whether an identifier is a local variable or argument, ie a symbol.
     * @param name the symbol name
     * @param capture whether solving by capturing a parent symbol is allowed
     * @return the symbol index
     */
    public Integer getSymbol(final String name, final boolean capture) {
        Integer register = namedVariables != null ? namedVariables.get(name) : null;
        if (register == null && capture && parent != null && !isStatic) {
            Integer pr = parent.getSymbol(name, true);
            if (pr != null) {
                register = addSymbol(name);
                setVariableCaptured(register, pr);
                // Preserve captured variable type
                setVariableType(register, parent.getVariableType(pr));
                // Make captured variables final by default
                setVariableFinal(register);
            }
        }
        return register;
    }

    /**
     * Checks whether a given symbol is captured.
     * @param symbol the symbol number
     * @return true if captured, false otherwise
     */
    public boolean isCapturedSymbol(final int symbol) {
        return capturedVariables != null && capturedVariables.containsKey(symbol);
    }

    /**
     * Sets the local variable as captured.
     * @param symbol the symbol index
     * @param register the parent frame register
     */
    protected void setVariableCaptured(final int symbol, final int register) {
        if (capturedVariables == null) {
            capturedVariables = new LinkedHashMap<Integer, Integer>();
        }
        capturedVariables.put(symbol, register);
    }

    /**
     * Returns the local variable type if any.
     * @param symbol the symbol index
     * @return the variable class
     */
    public Class getVariableType(int symbol) {
        return variableTypes == null ? null : variableTypes.get(symbol);
    }

    /**
     * Sets the local variable type.
     * @param symbol the symbol index
     * @param type the variable class
     */
    protected void setVariableType(int symbol, Class type) {
        if (type != null) {
            if (variableTypes == null) {
                variableTypes = new HashMap<Integer, Class>();
            }
            variableTypes.put(symbol, type);
        }
    }

    /**
     * Returns if the local variable is declared final.
     * @param symbol the symbol index
     * @return true if final, false otherwise
     */
    public boolean isVariableFinal(int symbol) {
        return finalVariables == null ? false : finalVariables.contains(symbol);
    }

    /**
     * Sets the local variable as declared final.
     * @param symbol the symbol index
     */
    protected void setVariableFinal(int symbol) {
        if (finalVariables == null) {
            finalVariables = new HashSet<Integer>();
        }
        finalVariables.add(symbol);
    }

    /**
     * Returns if the local variable is declared non-null.
     * @param symbol the symbol index
     * @return true if non-null, false otherwise
     */
    public boolean isVariableRequired(int symbol) {
        return requiredVariables == null ? false : requiredVariables.contains(symbol);
    }

    /**
     * Sets the local variable as declared required.
     * @param symbol the symbol index
     */
    protected void setVariableRequired(int symbol) {
        if (requiredVariables == null) {
            requiredVariables = new HashSet<Integer>();
        }
        requiredVariables.add(symbol);
    }

    /**
     * Sets the local variable default value.
     * @param symbol the symbol index
     * @param value the variable default value
     */
    protected void setVariableValue(int symbol, Object value) {
        if (value != null) {
            if (defaultValues == null) {
                defaultValues = new HashMap<Integer, Object>();
            }
            defaultValues.put(symbol, value);
        }
    }

    /**
     * Returns the local variable default value.
     * @param symbol the symbol index
     * @return the variable value
     */
    public Object getVariableValue(int symbol) {
        return defaultValues == null ? null : defaultValues.get(symbol);
    }

    /**
     * Declares a parameter.
     * <p>
     * This method creates an new entry in the symbol map.
     * </p>
     * @param name the parameter name
     * @return the register index storing this variable
     */
    public int declareParameter(final String name) {
        return declareParameter(name, null, false, false, null);
    }

    /**
     * Declares a parameter.
     * <p>
     * This method creates an new entry in the symbol map.
     * </p>
     * @param name the parameter name
     * @param type the parameter class
     * @param isFinal if the declared parameter is final
     * @param isRequired if the declared parameter is non-null
     * @param value the default parameter value
     * @return the register index storing this variable
     */
    public int declareParameter(final String name, final Class type, final boolean isFinal, final boolean isRequired, 
                                final Object value) {
        if (vars > 0) {
            throw new IllegalStateException("cant declare parameters after variables");
        }
        Integer register = getSymbol(name, false);
        if (register == null) {
            register = addSymbol(name);
            parms += 1;
            setVariableType(register, type);
            if (isFinal) {
                setVariableFinal(register);
            }
            if (isRequired) {
                setVariableRequired(register);
            }
            if (value != null) {
                setVariableValue(register, value);
            }
        }
        return register;
    }

    /**
     * Declares a scope to support variable parameters.
     */
    public void declareVarArgs() {
        varArgs = true;
    }

    /**
     * Declares a scope to be static.
     */
    public void declareStatic() {
        isStatic = true;
    }

    /**
     * Declares return type
     */
    public void declareReturnType(final Class type) {
        returnType = type;
    }

    /**
     * Declares a local variable.
     * <p>
     * This method creates an new entry in the symbol map.
     * </p>
     * @param name the variable name
     * @return the register index storing this variable
     */
    public int declareVariable(final String name) {
        return declareVariable(name, null, false, false);
    }

    /**
     * Declares a local variable.
     * <p>
     * This method creates an new entry in the symbol map.
     * </p>
     * @param name the variable name
     * @param type the variable class
     * @param isFinal if the declared variable is final
     * @param isRequired if the declared variable is non-null
     * @return the register index storing this variable
     */
    public int declareVariable(final String name, final Class type, final boolean isFinal, final boolean isRequired) {
        Integer register = getSymbol(name, false);
        if (register == null) {
            register = addSymbol(name);
            vars += 1;
            setVariableType(register, type);
            if (isFinal) {
                setVariableFinal(register);
            }
            if (isRequired) {
                setVariableRequired(register);
            }
            // check if local is redefining captured
            if (parent != null) {
                final Integer pr = parent.getSymbol(name, true);
                if (pr != null) {
                    setVariableCaptured(register, pr);
                }
            }
        } else {
            if (isVariableFinal(register)) {
                throw new IllegalStateException("final variable can not be redeclared");
            }
            setVariableType(register, type);
            if (isFinal) {
                setVariableFinal(register);
            }
            if (isRequired) {
                setVariableRequired(register);
            }
        }
        return register;
    }

    /**
     * Creates a frame by copying values up to the number of parameters.
     * <p>This captures the captured variables values.</p>
     * @param frame the caller frame
     * @param args the arguments
     * @return the arguments array
     */
    public Frame createFrame(final Frame frame, final Object...args) {
        if (namedVariables != null) {
            final Object[] arguments = new Object[namedVariables.size()];
            Arrays.fill(arguments, UNDECLARED);
            if (frame != null && capturedVariables != null && parent != null) {
                for (final Map.Entry<Integer, Integer> capture : capturedVariables.entrySet()) {
                    final Integer target = capture.getKey();
                    final Integer source = capture.getValue();
                    final Object arg = frame.get(source);
                    arguments[target] = arg;
                }
            }
            return new Frame(this, arguments, 0).assign(args);
        }
        if (returnType != null) {
            return new Frame(this);
        }
        return null;
    }

    /**
     * Gets the captured index of a given symbol, ie the target index of a symbol in a child frame.
     * @param symbol the symbol index
     * @return the target symbol index or null if the symbol is not captured
     */
    public Integer getCaptured(final int symbol) {
        if (capturedVariables != null) {
            for (final Map.Entry<Integer, Integer> capture : capturedVariables.entrySet()) {
                final Integer source = capture.getValue();
                if (source == symbol) {
                    return capture.getKey();
                }
            }
        }
        return null;
    }

    /**
     * Gets the (maximum) number of arguments this script expects.
     * @return the number of parameters
     */
    public int getArgCount() {
        return parms;
    }

    /**
     * If this script expects a variable number of arguments.
     * @return true or false
     */
    public boolean isVarArgs() {
        return varArgs;
    }

    /**
     * If this script has static scope.
     * @return true or false
     */
    public boolean isStatic() {
        return isStatic;
    }

    /**
     * Return type of the script.
     * @return class
     */
    public Class getReturnType() {
        return returnType;
    }

    /**
     * Gets this script symbols names, i.e. parameters and local variables.
     * @return the symbol names
     */
    public String[] getSymbols() {
        return namedVariables != null ? namedVariables.keySet().toArray(new String[0]) : EMPTY_STRS;
    }

    /**
     * Gets this script parameters, i.e. symbols assigned before creating local variables.
     * @return the parameter names
     */
    public String[] getParameters() {
        return getParameters(0);
    }

    /**
     * Gets this script parameters.
     * @param bound number of known bound parameters (curry)
     * @return the parameter names
     */
    protected String[] getParameters(final int bound) {
        final int unbound = parms - bound;
        if ((namedVariables == null) || (unbound <= 0)) {
            return EMPTY_STRS;
        }
        final String[] pa = new String[unbound];
        int p = 0;
        for (final Map.Entry<String, Integer> entry : namedVariables.entrySet()) {
            final int argn = entry.getValue();
            if (argn >= bound && argn < parms) {
                pa[p++] = entry.getKey();
            }
        }
        return pa;
    }

    /**
     * Gets this script local variable, i.e. symbols assigned to local variables excluding captured variables.
     * @return the local variable names
     */
    public String[] getLocalVariables() {
        if (namedVariables != null && vars > 0) {
            final List<String> locals = new ArrayList<String>(vars);
            for (Map.Entry<String, Integer> entry : namedVariables.entrySet()) {
                int symnum = entry.getValue();
                if (symnum >= parms && !isCapturedSymbol(symnum)) {
                    locals.add(entry.getKey());
                }
            }
            return locals.toArray(new String[locals.size()]);
        } 
        return EMPTY_STRS;
    }
}
