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

import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.JexlOperator;
import org.apache.commons.jexl3.parser.JexlNode;

import java.util.function.Predicate;


/**
 * A relational operator predicate.
 */
public class RelationalPredicate implements Predicate<Object> {

    /** The arithmetic. */
    protected final JexlArithmetic arithmetic;
    /** The operators implementation. */
    protected final Operators operators;
    /** The Jexl Node. */
    protected final JexlNode node;
    /** The relational operator. */
    protected final JexlOperator operator;
    /** The relational operator negate flag. */
    protected final boolean negated;
    /** The operand. */
    protected final Object operand;

    protected RelationalPredicate(JexlArithmetic arithmetic, Operators operators, JexlOperator operator, JexlNode node, boolean negate, Object operand) {
        this.arithmetic = arithmetic;
        this.operators = operators;
        this.node = node;
        this.operator = operator;
        this.negated = negate;
        this.operand = operand;
    }

    public JexlOperator getOperator() {
        return operator;
    }

    public Object getOperand() {
        return operand;
    }

    public boolean isNegated() {
        return negated;
    }

    @Override
    public boolean test(Object t) {

        switch(operator) {
            case EQ:
                return negated ? !arithmetic.equals(t, operand) : arithmetic.equals(t, operand);
            case CONTAINS:
                return negated ? !operators.contains(node, "!~", operand, t) : operators.contains(node, "=~", operand, t);
            case LT:
                return negated ? !arithmetic.lessThan(t, operand) : arithmetic.lessThan(t, operand);
            case LTE:
                return negated ? !arithmetic.lessThanOrEqual(t, operand) : arithmetic.lessThanOrEqual(t, operand);
            case GT:
                return negated ? !arithmetic.greaterThan(t, operand) : arithmetic.greaterThan(t, operand);
            case GTE:
                return negated ? !arithmetic.greaterThanOrEqual(t, operand) : arithmetic.greaterThanOrEqual(t, operand);
            case STARTSWITH:
                return negated ? !operators.startsWith(node, "^!", t, operand) : operators.startsWith(node, "^=", t, operand);
            case ENDSWITH:
                return negated ? !operators.endsWith(node, "$!", t, operand) : operators.endsWith(node, "$=", t, operand);
            default:
        }

        return false;
    }

    @Override
    public Predicate<Object> negate() {
        return create(arithmetic, operators, operator, node, !negated, operand);
    }

    @Override
    public Predicate<Object> and(Predicate<Object> t) {
        return new AndPredicate(this, t);
    }

    @Override
    public Predicate<Object> or(Predicate<Object> t) {
        return new OrPredicate(this, t);
    }

    static Predicate<Object> create(JexlArithmetic arithmetic, Operators operators, JexlOperator operator, JexlNode node, boolean negate, Object operand) {
        return new RelationalPredicate(arithmetic, operators, operator, node, negate, operand);
    }

    public static class NotPredicate implements Predicate<Object> {
        /** The negated predicate. */
        protected final Predicate<Object> predicate;

        public NotPredicate(Predicate<Object> predicate) {
            this.predicate = predicate;
        }

        public Predicate<Object> getPredicate() {
            return predicate;
        }

        @Override
        public boolean test(Object t) {
            return !predicate.test(t);
        }

        @Override
        public Predicate<Object> negate() {
            return new NotPredicate(this);
        }

        @Override
        public Predicate<Object> and(Predicate<Object> t) {
            return new AndPredicate(this, t);
        }

        @Override
        public Predicate<Object> or(Predicate<Object> t) {
            return new OrPredicate(this, t);
        }
    }

    public static class AndPredicate implements Predicate<Object> {
        /** The left predicate. */
        protected final Predicate<Object> left;
        /** The right predicate. */
        protected final Predicate<Object> right;

        public AndPredicate(Predicate<Object> left, Predicate<Object> right) {
            this.left = left;
            this.right = right;
        }

        public Predicate<Object> getLeft() {
            return left;
        }

        public Predicate<Object> getRight() {
            return right;
        }

        @Override
        public boolean test(Object t) {
            return left.test(t) && right.test(t);
        }

        @Override
        public Predicate<Object> negate() {
            return new NotPredicate(this);
        }

        @Override
        public Predicate<Object> and(Predicate<Object> t) {
            return new AndPredicate(this, t);
        }

        @Override
        public Predicate<Object> or(Predicate<Object> t) {
            return new OrPredicate(this, t);
        }
    }

    public static class OrPredicate implements Predicate<Object> {
        /** The left predicate. */
        protected final Predicate<Object> left;
        /** The right predicate. */
        protected final Predicate<Object> right;

        public OrPredicate(Predicate<Object> left, Predicate<Object> right) {
            this.left = left;
            this.right = right;
        }

        public Predicate<Object> getLeft() {
            return left;
        }

        public Predicate<Object> getRight() {
            return right;
        }

        @Override
        public boolean test(Object t) {
            return left.test(t) || right.test(t);
        }

        @Override
        public Predicate<Object> negate() {
            return new NotPredicate(this);
        }

        @Override
        public Predicate<Object> and(Predicate<Object> t) {
            return new AndPredicate(this, t);
        }

        @Override
        public Predicate<Object> or(Predicate<Object> t) {
            return new OrPredicate(this, t);
        }
    }

}
