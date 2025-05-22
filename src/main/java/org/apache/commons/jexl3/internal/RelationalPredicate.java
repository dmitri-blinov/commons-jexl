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
    /** The compare with any of operands succeeds flag. */
    protected final boolean any;
    /** The operands. */
    protected final Object[] operands;

    protected RelationalPredicate(JexlArithmetic arithmetic, Operators operators, JexlOperator operator, JexlNode node, boolean negate, boolean any, Object... operands) {
        this.arithmetic = arithmetic;
        this.operators = operators;
        this.node = node;
        this.operator = operator;
        this.negated = negate;
        this.any = any;
        this.operands = operands;
    }

    public JexlOperator getOperator() {
        return operator;
    }

    public Object[] getOperands() {
        return operands;
    }

    public Object getOperand() {
        return operands != null && operands.length > 0 ? operands[0] : null;
    }

    public boolean isNegated() {
        return negated;
    }

    public boolean isAny() {
        return any;
    }

    @Override
    public boolean test(Object t) {

        Boolean ok = false;

        for (Object operand : operands) {

           switch(operator) {
               case EQ:
                   ok = negated ? !operators.equals(node, JexlOperator.NE, t, operand) : operators.equals(node, JexlOperator.EQ, t, operand);
                   break;
               case CONTAINS:
                   ok = negated ? operators.contains(node, JexlOperator.NOT_CONTAINS, operand, t) : operators.contains(node, JexlOperator.CONTAINS, operand, t);
                   break;
               case LT:
                   ok = negated ? !operators.lessThan(node, t, operand) : operators.lessThan(node, t, operand);
                   break;
               case LTE:
                   ok = negated ? !operators.lessThanOrEqual(node, t, operand) : operators.lessThanOrEqual(node, t, operand);
                   break;
               case GT:
                   ok = negated ? !operators.greaterThan(node, t, operand) : operators.greaterThan(node, t, operand);
                   break;
               case GTE:
                   ok = negated ? !operators.greaterThanOrEqual(node, t, operand) : operators.greaterThanOrEqual(node, t, operand);
                   break;
               case STARTSWITH:
                   ok = negated ? operators.startsWith(node, JexlOperator.NOT_STARTSWITH, t, operand) : operators.startsWith(node, JexlOperator.STARTSWITH, t, operand);
                   break;
               case ENDSWITH:
                   ok = negated ? operators.endsWith(node, JexlOperator.NOT_ENDSWITH, t, operand) : operators.endsWith(node, JexlOperator.ENDSWITH, t, operand);
                   break;
               default:
           }

           if (ok && any || !ok && !any) {
               return ok;
           }
        }

        return ok;
    }


    @Override
    public Predicate<Object> negate() {
        return create(arithmetic, operators, operator, node, !negated, !any, operands);
    }

    @Override
    public Predicate<Object> and(Predicate<Object> t) {
        return new AndPredicate(this, t);
    }

    @Override
    public Predicate<Object> or(Predicate<Object> t) {
        return new OrPredicate(this, t);
    }

    static Predicate<Object> create(JexlArithmetic arithmetic, Operators operators, JexlOperator operator, JexlNode node, boolean negate, boolean any, Object... operands) {
        return new RelationalPredicate(arithmetic, operators, operator, node, negate, any, operands);
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
