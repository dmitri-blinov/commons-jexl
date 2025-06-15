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

import org.apache.commons.jexl3.internal.Script;

import java.util.Objects;

/**
 * Helper class to carry information such as a url/file name, line and column for
 * debugging information reporting.
 */
public class JexlInfo {

    /** line number. */
    private final int line;

    /** column number. */
    private final int column;

    /** name. */
    private final String name;

    /** path. */
    private final String path;

    /**
     * @return the detailed information in case of an error
     */
    public Detail getDetail() {
        return null;
    }

    /**
     * Describes errors more precisely.
     */
    public interface Detail {
        /**
         * @return the start column on the line that triggered the error
         */
        int start();

        /**
         * @return the end column on the line that triggered the error
         */
        int end();

        /**
         * @return the actual part of code that triggered the error
         */

        @Override
        String toString();
    }

    /**
     * Create info.
     *
     * @param source source name
     * @param l line number
     * @param c column number
     */
    public JexlInfo() {
        this(null, null, 1, 1);
    }

    /**
     * Create info.
     *
     * @param source source name
     * @param l line number
     * @param c column number
     */
    public JexlInfo(final String source, final int l, final int c) {
        this(source, null, l, c);
    }

    /**
     * Create info.
     *
     * @param source source name
     * @param p source path
     * @param l line number
     * @param c column number
     */
    public JexlInfo(final String source, final String p, final int l, final int c) {
        name = source;
        path = p;
        line = l <= 0? 1: l;
        column = c <= 0? 1 : c;
    }

    /**
     * Creates info reusing the name.
     *
     * @param l the line
     * @param c the column
     * @return a new info instance
     */
    public JexlInfo at(final int l, final int c) {
        return new JexlInfo(name, path, l, c);
    }

    /**
     * The copy constructor.
     *
     * @param copy the instance to copy
     */
    protected JexlInfo(final JexlInfo copy) {
        this(copy.getName(), copy.getPath(), copy.getLine(), copy.getColumn());
    }

    /**
     * Formats this info in the form 'name&#064;line:column'.
     *
     * @return the formatted info
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (path != null) {
            sb.append(path);
        } else if (name != null) {
            sb.append(name);
        }
        sb.append("@");
        sb.append(line);
        sb.append(":");
        sb.append(column);
        final JexlInfo.Detail dbg = getDetail();
        if (dbg!= null) {
            sb.append("![");
            sb.append(dbg.start());
            sb.append("..");
            sb.append(dbg.end());
            sb.append("]: '");
            sb.append(dbg.toString());
            sb.append("'");
        }
        return sb.toString();
    }

    /**
     * Gets the file/script/url name.
     *
     * @return template name
     */
    public final String getName() {
        return name;
    }

    /**
     * Gets the file/script/url path.
     *
     * @return template path
     */
    public final String getPath() {
        return path;
    }

    /**
     * Gets the line number.
     *
     * @return line number.
     */
    public final int getLine() {
        return line;
    }

    /**
     * Gets the column number.
     *
     * @return the column.
     */
    public final int getColumn() {
        return column;
    }

    /**
     * @return this instance or a copy without any decorations
     */
    public JexlInfo detach() {
        return this;
    }

    /**
     * Gets the info from a script.
     * @param script the script
     * @return the info
     */
    public static JexlInfo from(final JexlScript script) {
        return script instanceof Script? ((Script) script).getInfo() :  null;
    }

    @Override
    public int hashCode() { //CSOFF: MagicNumber
        int hash = 3;
        hash = 53 * hash + (int) (this.line ^ (this.line >>> 32));
        hash = 53 * hash + (int) (this.column ^ (this.column >>> 32));
        hash = 53 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 53 * hash + (this.path != null ? this.path.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JexlInfo other = (JexlInfo) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.path, other.path)) {
            return false;
        }
        if (this.line != other.line) {
            return false;
        }
        if (this.column != other.column) {
            return false;
        }
        return true;
    }
}

