package de.lukaspanneke.bachelor.reachability.logic.generic.formula;

import java.util.*;

public abstract class StateFormula<N, M, P, T> {

	public abstract boolean test(N net, M marking);

	public abstract Set<T> interestingTransitions(N net, M marking);

	protected abstract Set<T> negatedInterestingTransitions(N net, M marking);

	public abstract Set<P> support();

	public static <N, M, P, T> StateFormula<N, M, P, T> top() {
		return new StateFormula<>() {
			@Override
			public boolean test(N net, M marking) {
				return true;
			}

			@Override
			public Set<T> interestingTransitions(N net, M marking) {
				return Collections.emptySet();
			}

			@Override
			protected Set<T> negatedInterestingTransitions(N net, M marking) {
				return Collections.emptySet();
			}

			@Override
			public Set<P> support() {
				return Collections.emptySet();
			}

			@Override
			public String toString() {
				return "⊤";
			}
		};
	}

	public static <N, M, P, T> StateFormula<N, M, P, T> bottom() {
		return new StateFormula<>() {
			@Override
			public boolean test(N net, M marking) {
				return false;
			}

			@Override
			public Set<T> interestingTransitions(N net, M marking) {
				return Collections.emptySet();
			}

			@Override
			protected Set<T> negatedInterestingTransitions(N net, M marking) {
				return Collections.emptySet();
			}

			@Override
			public Set<P> support() {
				return Collections.emptySet();
			}

			@Override
			public String toString() {
				return "⊥";
			}
		};
	}
}
