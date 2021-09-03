package de.lukaspanneke.bachelor.reachability.solver.sparse;

import com.google.common.collect.Lists;
import de.lukaspanneke.bachelor.Enumerate;
import de.lukaspanneke.bachelor.IndexedItem;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseIntVector;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;
import de.lukaspanneke.bachelor.reachability.AugmentedQueryResult;
import de.lukaspanneke.bachelor.reachability.QueryResult;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import de.lukaspanneke.bachelor.reachability.solver.result.SparseWitnessMarkingAndNumberOfVisitedMarkings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static de.lukaspanneke.bachelor.reachability.QueryResult.DID_NOT_FINISH;
import static de.lukaspanneke.bachelor.reachability.QueryResult.SATISFIED;
import static de.lukaspanneke.bachelor.reachability.QueryResult.UNSATISFIED;

public class SparseGoalSet {

	private static final Logger LOGGER = LogManager.getLogger(SparseGoalSet.class);

	private final SparsePetriNet net;
	private final List<IndexedItem<? extends StateFormula<SparsePetriNet, SparseIntVector, String, Integer>>> undecidedGoals;
	private final List<AugmentedQueryResult<SparseWitnessMarkingAndNumberOfVisitedMarkings>> results;

	public SparseGoalSet(SparsePetriNet net, List<? extends StateFormula<SparsePetriNet, SparseIntVector, String, Integer>> goals) {
		this.net = net;
		this.undecidedGoals = Lists.newLinkedList(Enumerate.enumerate(goals));
		this.results = goals.stream()
				.map(goal -> result(DID_NOT_FINISH, null, 0))
				.collect(Collectors.toList());
	}

	private AugmentedQueryResult<SparseWitnessMarkingAndNumberOfVisitedMarkings> result(QueryResult isReachable, SparseIntVector witness, long visited) {
		return new AugmentedQueryResult<>(isReachable, new SparseWitnessMarkingAndNumberOfVisitedMarkings(Optional.ofNullable(witness), visited, this.net));
	}

	public void update(SparseIntVector newMarking, long visited) {
		for (var itr = this.undecidedGoals.iterator(); itr.hasNext(); ) {
			IndexedItem<? extends StateFormula<SparsePetriNet, SparseIntVector, String, Integer>> elem = itr.next();
			if (elem.item().test(this.net, newMarking)) {
				this.results.set(elem.index(), result(SATISFIED, newMarking, visited));
				itr.remove();
				LOGGER.trace("Decided formula #{}. {} remaining", elem.index(), this.undecidedGoals.size() - 1);
			}
		}
	}

	public void markRemainingAs(QueryResult result, long visited) {
		if (result == SATISFIED || result == UNSATISFIED) {
			LOGGER.trace("Decided remaining formulas");
		}
		for (var itr = this.undecidedGoals.iterator(); itr.hasNext(); ) {
			IndexedItem<? extends StateFormula<SparsePetriNet, SparseIntVector, String, Integer>> elem = itr.next();
			this.results.set(elem.index(), result(result, null, visited));
			itr.remove();
		}
	}

	public boolean isDecided() {
		return this.undecidedGoals.isEmpty();
	}

	public int getNumUndecided() {
		return this.undecidedGoals.size();
	}

	public List<AugmentedQueryResult<SparseWitnessMarkingAndNumberOfVisitedMarkings>> getResults() {
		return this.results;
	}
}
