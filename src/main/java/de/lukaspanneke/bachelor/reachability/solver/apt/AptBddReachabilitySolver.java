package de.lukaspanneke.bachelor.reachability.solver.apt;

import de.lukaspanneke.bachelor.reachability.AugmentedQueryResult;
import de.lukaspanneke.bachelor.reachability.QueryResult;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import de.lukaspanneke.bachelor.reachability.solver.ReachabilitySolver;
import de.lukaspanneke.bachelor.reachability.symbolic.BddAptSolver;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;

@Deprecated
public class AptBddReachabilitySolver implements ReachabilitySolver<PetriNet, Marking, Place, Transition, Void> {

	@Override
	public AugmentedQueryResult<Void> isReachable(PetriNet net, StateFormula<PetriNet, Marking, Place, Transition> goal) {
		try (BddAptSolver bdd = new BddAptSolver(net)) {
			Boolean reachable = bdd.isReachable(goal);
			return new AugmentedQueryResult<>(reachable == null ? QueryResult.DID_NOT_FINISH : reachable ? QueryResult.SATISFIED : QueryResult.UNSATISFIED, null);
		}
	}

//	@Override
//	public List<AugmentedBoolean<Void>> isReachableSequential(PetriNet net, List<? extends StateFormula<PetriNet, Marking, Place, Transition>> goals) {
//		int[] i = { 0 };
//		try (BddAptSolver bdd = new BddAptSolver(net)) {
//			try {
//				return goals.stream()
//						.peek(x -> LOGGER.trace("# Formula #{}", i[0]++))
//						.map(bdd::isReachable)
//						.map(AugmentedBoolean::of)
//						.toList();
//			} finally {
//				LOGGER.trace("Spent {} calculating the state space, {} encoding formulas, {} encoding edges",
//						bdd.timer.formatTotal(BDD_STATESPACE_FIXPOINT),
//						bdd.timer.formatTotal(BDD_ENCODE_FORUMLA),
//						bdd.timer.formatTotal(BDD_ENCODE_EDGES)
//				);
//			}
//		}
//	}
}
