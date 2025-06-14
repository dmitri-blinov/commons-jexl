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

import org.apache.commons.jexl3.parser.JexlNode;

/**
 * Script debug interface.
 *
 */
public interface JexlProbe {

    /**
     * Gets the debug session status.
     * @return true if the probe session is enabled
     */
    default boolean isEnabled() {
        return false;
    }

    /**
     * Creates script source
     * @param script the script info
     * @param source the script code source
     *
     * @return the loaded source identifier
     */
    long loadSource(JexlInfo script, String source);

    /**
     * Creates unique execution identifier for script evaluation
     * @param script the script info
     *
     * @return the stack frame identifier unique among all threads
     */
    long startScript(JexlInfo script);

    /**
     * Finishes script evaluation
     * @param frame the stack frame reference
     * @param result the script evaluation result
     * @param any the script evaluation error if any
     *
     * @return whether the script execution should be continued
     */
    boolean endScript(Frame frame, Object result, Throwable any);

    /**
     * Starts statement evaluation
     * @param source the statement source info
     * @param frame the stack frame reference
     *
     * @return whether the script execution should be continued
     */
    boolean startStatement(JexlInfo source, JexlNode node, Frame frame);

    /**
     * Finishes statement evaluation
     * @param source the statement source info
     * @param frame the stack frame reference
     * @param result the statement evaluation result
     * @param any the statement evaluation error if any
     *
     * @return whether the script execution should be continued
     */
    boolean endStatement(JexlInfo source, JexlNode node, Frame frame, Object result, Throwable any);

    /**
     * A script evaluation stack frame
     */
    interface Frame {

        /**
         * Gets the the script execution identifier.
         * @return the frame id
         */
        long getFrameId();

        /**
         * Gets the frame live attribute.
         * @return true if the frame is still on the stack; false if it has completed execution or been popped in some other way
         */
        boolean isLive();

        /**
         * Gets the frame script info.
         * @return The script info being executed in this frame
         */
        JexlInfo getScript();

        /**
         * Gets the frame generator attribute.
         * @return true if the frame is a generator frame, false otherwise
         */
        boolean isGenerator();

        /**
         * Gets the stack frame parameters.
         * @return the parameters scope
         */
        Scope getParameters();

        /**
         * Gets the stack frame local variables.
         * @return the local variables scope
         */
        Scope getLocals();

        /**
         * Gets the stack frame captured variables.
         * @return the captured variables scope
         */
        Scope getCapturedVariables();
    }

    /**
     * A stack frame scope
     */
    interface Scope {

        /**
         * Gets the stack frame variable names.
         * @return the variable names
         */
        String[] getNames();

        /**
         * Gets the scope variable info.
         * @param name the variable name

         * @return the variable info
         */
        Variable getVariableInfo(String name);

        /**
         * Gets the scope variable value.
         * @param name the variable name

         * @return the variable value
         */
        Object getVariable(String name);

        /**
         * Sets the scope variable value.
         * @param name the variable name
         * @param value the variable value

         */
        void setVariable(String name, Object value);
    }

    /**
     * A scoped variable
     */
    interface Variable {

        /**
         * Gets the variable symbol.
         * @return the variable symbol
         */
        int getSymbol();

        /**
         * Gets the variable name.
         * @return the variable name
         */
        String getName();

        /**
         * Gets the variable type.
         * @return the variable type if any
         */
        Class getType();

        /**
         * Gets the variable final modifier.
         * @return true if the variable is final
         */
        boolean isFinal();

        /**
         * Gets the variable required modifier.
         * @return true if the variable is required
         */
        boolean isRequired();
    }

}
