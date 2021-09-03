package de.lukaspanneke.bachelor.reachability.symbolic;

import com.github.javabdd.BDD;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseIntVector;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Function;

public class BddGoalSet {

	private static final Logger LOGGER = LogManager.getLogger(BddGoalSet.class);

	private final BDD[] goals;
	private final Boolean[] results;
	private BDD undecidedGoals;
	private int undecided;

	public BddGoalSet(Boolean[] results, BDD[] goals) {
		this.results = results;
		this.goals = goals;
		for (BDD goal : this.goals) {
			if (goal != null) {
				this.undecided++;
				if (this.undecidedGoals == null) {
					this.undecidedGoals = goal.getFactory().zero();
				}
				this.undecidedGoals.orWith(goal.id());
			}
		}
	}

	/* the goal encoder may return null, when it is interrupted. */
	public static BddGoalSet
	fromGoalFormulas(
			List<? extends StateFormula<SparsePetriNet, SparseIntVector, String, Integer>> goals,
			SparsePetriNet net,
			Function<StateFormula<SparsePetriNet, SparseIntVector, String, Integer>, BDD> goalEncoder
	) throws InterruptedInitializationException {
		Boolean[] results = new Boolean[goals.size()];
		BDD[] bddGoals = new BDD[goals.size()];
		for (int i = 0; i < goals.size(); i++) {
			StateFormula<SparsePetriNet, SparseIntVector, String, Integer> goal = goals.get(i);
			if (goal.test(net, net.getInitialMarking())) {
				LOGGER.trace("Initial marking is a goal marking with formula #{}", i);
				results[i] = true;
			}
		}
		for (int i = 0; i < goals.size(); i++) {
			if (results[i] == null) {
				LOGGER.trace("Analysing formula #{}", i);
				StateFormula<SparsePetriNet, SparseIntVector, String, Integer> goal = goals.get(i);
				BDD bddGoal = goalEncoder.apply(goal);
				if (bddGoal == null) {
					throw new InterruptedInitializationException(Arrays.asList(results));
				}
				if (bddGoal.isZero()) {
					LOGGER.trace("Goal formula is contradiction");
					results[i] = false;
				} else if (bddGoal.isOne()) {
					LOGGER.trace("Goal formula is tautology");
					results[i] = true;
				} else {
					LOGGER.trace("No trivial case detected");
					bddGoals[i] = bddGoal;
				}
			}
		}
		return new BddGoalSet(results, bddGoals);
	}

	public static class InterruptedInitializationException extends Exception {
		private final List<Boolean> knownResults;

		public InterruptedInitializationException(List<Boolean> knownResults) {
			this.knownResults = knownResults;
		}

		public List<Boolean> getKnownResults() {
			return knownResults;
		}
	}

	public void update(BDD newStates) {
		if (this.undecidedGoals == null) {
			return;
		}
		if (BddUtil.contains(newStates, this.undecidedGoals)) {
			/* some formula matched. find which ones */
			for (int i = 0; i < this.goals.length; i++) {
				BDD goal = this.goals[i];
				if (goal != null && BddUtil.contains(newStates, goal)) {
					this.results[i] = true;
					this.undecided--;
					LOGGER.trace("Decided formula #{}. {} remaining", i, this.undecided);
					this.goals[i] = null;
					goal.free();
				}
			}
			if (this.undecided != 0) {
				/* some formulas are still undecided. */
				BDD stillUndecidedGoals = newStates.getFactory().zero();
				for (BDD goal : this.goals) {
					if (goal != null) {
						stillUndecidedGoals.orWith(goal.id());
					}
				}
			}
		}
	}

	public void markRemainingUnreachable() {
		LOGGER.trace("Decided remaining formulas");
		for (int i = 0; i < this.goals.length; i++) {
			BDD goal = this.goals[i];
			if (goal != null) {
				this.results[i] = false;
				this.undecided--;
				this.goals[i] = null;
				goal.free();
			}
		}
		assert this.isDecided();
	}

	public boolean isDecided() {
		return this.undecided == 0;
	}

	public int getNumUndecided() {
		return this.undecided;
	}

	public List<Boolean> getResults() {
		return Arrays.asList(this.results);
	}
}
