package de.lukaspanneke.bachelor.reachability.logic.generic.expression;

import java.util.*;

public class ConstantExpression<N, M, P, T> implements ArithmeticExpression<N, M, P, T> {
	private final long constant;

	protected ConstantExpression(long constant) {
		this.constant = constant;
	}

	public static <N, M, P, T> ConstantExpression<N, M, P, T> of(long constant) {
		return new ConstantExpression<>(constant);
	}

	public long constant() {
		return this.constant;
	}

	@Override
	public Set<T> increasingTransitions(N net) {
		return Collections.emptySet();
	}

	@Override
	public Set<T> decreasingTransitions(N net) {
		return Collections.emptySet();
	}

	@Override
	public Set<P> support() {
		return Collections.emptySet();
	}

	@Override
	public long evaluate(N net, M marking) {
		return this.constant;
	}

	@Override
	public String toString() {
		return Long.toString(this.constant);
	}
}
