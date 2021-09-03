package de.lukaspanneke.bachelor.reachability.logic.generic.formula;

import java.util.*;

public class NegatedFormula<N, M, P, T> extends StateFormula<N, M, P, T> {
	private final StateFormula<N, M, P, T> f;

	protected NegatedFormula(StateFormula<N, M, P, T> f) {
		this.f = f;
	}

	public static <N, M, P, T> StateFormula<N, M, P, T> of(StateFormula<N, M, P, T> f) {
		if (f instanceof NegatedFormula<N, M, P, T> n) {
			return n.f;
		} else {
			return new NegatedFormula<>(f);
		}
	}

	public StateFormula<N, M, P, T> formula() {
		return this.f;
	}

	@Override
	public boolean test(N net, M marking) {
		return !f.test(net, marking);
	}

	@Override
	public Set<T> interestingTransitions(N net, M marking) {
		if (this.test(net, marking)) {
			return Collections.emptySet();
		}
		return this.f.negatedInterestingTransitions(net, marking);
	}

	@Override
	protected Set<T> negatedInterestingTransitions(N net, M marking) {
		return this.f.interestingTransitions(net, marking);
	}

	@Override
	public Set<P> support() {
		return this.f.support();
	}

	@Override
	public String toString() {
		if (this.f instanceof ComparisonFormula) {
			return "¬(" + this.f + ")";
		} else {
			return "¬" + this.f;
		}
	}
}
