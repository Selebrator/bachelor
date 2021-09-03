package de.lukaspanneke.bachelor.reachability.logic.generic.expression;

import com.google.common.collect.Sets;
import de.lukaspanneke.bachelor.reachability.logic.CalculationOperator;

import java.util.*;

public class CalculationExpression<N, M, P, T> implements ArithmeticExpression<N, M, P, T> {
	private final ArithmeticExpression<N, M, P, T> e1;
	private final ArithmeticExpression<N, M, P, T> e2;
	private final CalculationOperator operator;

	protected CalculationExpression(ArithmeticExpression<N, M, P, T> e1, CalculationOperator operator, ArithmeticExpression<N, M, P, T> e2) {
		this.e1 = e1;
		this.e2 = e2;
		this.operator = operator;
	}

	public static <N, M, P, T> CalculationExpression<N, M, P, T> of(ArithmeticExpression<N, M, P, T> e1, CalculationOperator operator, ArithmeticExpression<N, M, P, T> e2) {
		return new CalculationExpression<>(e1, operator, e2);
	}

	public ArithmeticExpression<N, M, P, T> left() {
		return this.e1;
	}

	public ArithmeticExpression<N, M, P, T> right() {
		return this.e2;
	}

	public CalculationOperator operator() {
		return this.operator;
	}

	@Override
	public Set<T> increasingTransitions(N net) {
		return switch (this.operator) {
			case PLUS -> Sets.union(this.e1.increasingTransitions(net), this.e2.increasingTransitions(net));
			case MINUS -> Sets.union(this.e1.increasingTransitions(net), this.e2.decreasingTransitions(net));
			case TIMES -> Sets.union(
					Sets.union(this.e1.increasingTransitions(net), this.e1.decreasingTransitions(net)),
					Sets.union(this.e2.increasingTransitions(net), this.e2.decreasingTransitions(net))
			);
		};
	}

	@Override
	public Set<T> decreasingTransitions(N net) {
		return switch (this.operator) {
			case PLUS -> Sets.union(this.e1.decreasingTransitions(net), this.e2.decreasingTransitions(net));
			case MINUS -> Sets.union(this.e1.decreasingTransitions(net), this.e2.increasingTransitions(net));
			case TIMES -> Sets.union(
					Sets.union(this.e1.increasingTransitions(net), this.e1.decreasingTransitions(net)),
					Sets.union(this.e2.increasingTransitions(net), this.e2.decreasingTransitions(net))
			);
		};
	}

	@Override
	public Set<P> support() {
		return Sets.union(this.e1.support(), this.e2.support());
	}

	@Override
	public long evaluate(N net, M marking) {
		return this.operator.apply(this.e1.evaluate(net, marking), this.e2.evaluate(net, marking));
	}

	@Override
	public String toString() {
		return "(" + this.e1.toString() + " " + this.operator.symbol() + " " + this.e2.toString() + ")";
	}
}
