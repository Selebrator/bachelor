package de.lukaspanneke.bachelor.reachability.solver;

import de.lukaspanneke.bachelor.reachability.AugmentedQueryResult;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;

import java.util.*;

public interface ParallelReachabilitySolver<N, M, P, T, R> extends ReachabilitySolver<N, M, P, T, R> {
	List<AugmentedQueryResult<R>> isReachableParallel(N net, List<? extends StateFormula<N, M, P, T>> goals);
}
