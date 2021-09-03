package de.lukaspanneke.bachelor.reachability.logic.generic.formula;

import com.google.common.collect.Sets;
import de.lukaspanneke.bachelor.reachability.logic.ComparisonOperator;
import de.lukaspanneke.bachelor.reachability.logic.generic.expression.ArithmeticExpression;

import java.util.*;

public class ComparisonFormula<N, M, P, T> extends StateFormula<N, M, P, T> {

	private final ArithmeticExpression<N, M, P, T> e1;
	private final ArithmeticExpression<N, M, P, T> e2;
	private final ComparisonOperator operator;

	protected ComparisonFormula(ArithmeticExpression<N, M, P, T> e1, ComparisonOperator operator, ArithmeticExpression<N, M, P, T> e2) {
		this.e1 = e1;
		this.e2 = e2;
		this.operator = operator;
	}

	public static <N, M, P, T> ComparisonFormula<N, M, P, T> of(ArithmeticExpression<N, M, P, T> e1, ComparisonOperator operator, ArithmeticExpression<N, M, P, T> e2) {
		return new ComparisonFormula<>(e1, operator, e2);
	}

	public ArithmeticExpression<N, M, P, T> left() {
		return this.e1;
	}

	public ArithmeticExpression<N, M, P, T> right() {
		return this.e2;
	}

	public ComparisonOperator operator() {
		return this.operator;
	}

	@Override
	public boolean test(N net, M marking) {
		return this.operator.test(this.e1.evaluate(net, marking), this.e2.evaluate(net, marking));
	}

	@Override
	public Set<T> interestingTransitions(N net, M marking) {
		if (this.test(net, marking)) {
			return Collections.emptySet();
		}
		return switch (this.operator) {
			case LESS_THEN, LESS_EQUALS -> InterestingTransitions.less(net, marking, this.e1, this.e2);
			case GREATER_EQUALS, GREATER_THEN -> InterestingTransitions.greater(net, marking, this.e1, this.e2);
			case EQUALS -> InterestingTransitions.equal(net, marking, this.e1, this.e2);
			case NOT_EQUALS -> InterestingTransitions.notEqual(net, marking, this.e1, this.e2);
		};
	}

	@Override
	protected Set<T> negatedInterestingTransitions(N net, M marking) {
		return switch (this.operator) {
			case LESS_THEN, LESS_EQUALS -> InterestingTransitions.greater(net, marking, this.e1, this.e2);
			case GREATER_EQUALS, GREATER_THEN -> InterestingTransitions.less(net, marking, this.e1, this.e2);
			case EQUALS -> InterestingTransitions.notEqual(net, marking, this.e1, this.e2);
			case NOT_EQUALS -> InterestingTransitions.equal(net, marking, this.e1, this.e2);
		};
	}

	@Override
	public Set<P> support() {
		return Sets.union(this.e1.support(), this.e2.support());
	}

	@Override
	public String toString() {
		return this.e1.toString() + " " + this.operator.symbol() + " " + this.e2.toString();
	}
}
