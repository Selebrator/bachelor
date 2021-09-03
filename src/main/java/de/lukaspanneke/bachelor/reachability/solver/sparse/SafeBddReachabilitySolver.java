package de.lukaspanneke.bachelor.reachability.solver.sparse;

import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseIntVector;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;
import de.lukaspanneke.bachelor.reachability.AugmentedQueryResult;
import de.lukaspanneke.bachelor.reachability.QueryResult;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import de.lukaspanneke.bachelor.reachability.solver.ParallelReachabilitySolver;
import de.lukaspanneke.bachelor.reachability.symbolic.BddSparseSolver;

import java.util.*;

public class SafeBddReachabilitySolver implements ParallelReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, Void> {
	@Override
	public AugmentedQueryResult<Void> isReachable(SparsePetriNet net, StateFormula<SparsePetriNet, SparseIntVector, String, Integer> goal) {
		try (BddSparseSolver solver = new BddSparseSolver(net)) {
			Boolean reachable = solver.isReachable(goal);
			return new AugmentedQueryResult<>(reachable == null ? QueryResult.DID_NOT_FINISH : reachable ? QueryResult.SATISFIED : QueryResult.UNSATISFIED, null);
		}
	}

	@Override
	public List<AugmentedQueryResult<Void>> isReachableParallel(SparsePetriNet net, List<? extends StateFormula<SparsePetriNet, SparseIntVector, String, Integer>> goals) {
		try (BddSparseSolver solver = new BddSparseSolver(net)) {
			List<Boolean> results = solver.isReachable(goals);
			@SuppressWarnings("unchecked") List<AugmentedQueryResult<Void>> ret = Arrays.asList(new AugmentedQueryResult[goals.size()]);
			for (int i = 0; i < results.size(); i++) {
				Boolean reachable = results.get(i);
				ret.set(i, new AugmentedQueryResult<>(reachable == null ? QueryResult.DID_NOT_FINISH : reachable ? QueryResult.SATISFIED : QueryResult.UNSATISFIED, null));
			}
			return ret;
		}
	}
}
