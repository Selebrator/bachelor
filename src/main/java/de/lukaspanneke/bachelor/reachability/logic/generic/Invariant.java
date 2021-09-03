package de.lukaspanneke.bachelor.reachability.logic.generic;

import de.lukaspanneke.bachelor.reachability.AugmentedQueryResult;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.NegatedFormula;

public class Invariant<N, M, P, T> extends Formula<N, M, P, T> {
	public Invariant(StateFormula<N, M, P, T> formula) {
		super(formula);
	}


	@Override
	public StateFormula<N, M, P, T> asGoal() {
		return NegatedFormula.of(this.formula());
	}

	@Override
	public <R> AugmentedQueryResult<R> interpretGoalResult(AugmentedQueryResult<R> original) {
		return new AugmentedQueryResult<>(original.result().negate(), original.detail());
	}

	@Override
	public String toString() {
		return "AG " + this.formula();
	}
}
