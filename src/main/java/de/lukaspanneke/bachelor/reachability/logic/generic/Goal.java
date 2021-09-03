package de.lukaspanneke.bachelor.reachability.logic.generic;

import de.lukaspanneke.bachelor.reachability.AugmentedQueryResult;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;

public class Goal<N, M, P, T> extends Formula<N, M, P, T> {
	public Goal(StateFormula<N, M, P, T> formula) {
		super(formula);
	}

	@Override
	public StateFormula<N, M, P, T> asGoal() {
		return this.formula();
	}

	@Override
	public <R> AugmentedQueryResult<R> interpretGoalResult(AugmentedQueryResult<R> original) {
		return original;
	}

	@Override
	public String toString() {
		return "EF " + this.formula();
	}

}
