package de.lukaspanneke.bachelor.reachability.solver.apt;

import de.lukaspanneke.bachelor.reachability.AugmentedQueryResult;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import de.lukaspanneke.bachelor.reachability.reachabilitygraph.ReachabilitySetBuilder;
import de.lukaspanneke.bachelor.reachability.solver.ReachabilitySolver;
import de.lukaspanneke.bachelor.reachability.solver.result.AptWitnessMarkingAndNumberOfVisitedMarkings;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;

import java.util.*;

import static de.lukaspanneke.bachelor.reachability.AugmentedQueryResult.satisfied;
import static de.lukaspanneke.bachelor.reachability.AugmentedQueryResult.unsatisfied;

public class AptFullReachabilitySetSolver implements ReachabilitySolver<PetriNet, Marking, Place, Transition, AptWitnessMarkingAndNumberOfVisitedMarkings> {
	private final ReachabilitySetBuilder setBuilder;

	public AptFullReachabilitySetSolver(ReachabilitySetBuilder setBuilder) {
		this.setBuilder = setBuilder;
	}

	@Override
	public AugmentedQueryResult<AptWitnessMarkingAndNumberOfVisitedMarkings> isReachable(PetriNet net, StateFormula<PetriNet, Marking, Place, Transition> goal) {
		Set<Marking> reachableMarkings = this.setBuilder.build(net);
		return this.isReachable(net, goal, reachableMarkings);
	}

//	@Override
//	public List<AugmentedBoolean<AptWitnessMarkingAndNumberOfVisitedMarkings>> isReachableSequential(PetriNet net, List<? extends StateFormula<PetriNet, Marking, Place, Transition>> goals) {
//		Set<Marking> reachableMarkings = this.setBuilder.build(net);
//		return goals.stream()
//				.map(goal -> isReachable(net, goal, reachableMarkings))
//				.toList();
//	}

	public AugmentedQueryResult<AptWitnessMarkingAndNumberOfVisitedMarkings> isReachable(PetriNet net, StateFormula<PetriNet, Marking, Place, Transition> goal, Set<Marking> reachableMarkings) {
		for (Marking marking : reachableMarkings) {
			if (goal.test(net, marking)) {
				return satisfied(new AptWitnessMarkingAndNumberOfVisitedMarkings(Optional.of(marking), reachableMarkings.size()));
			}
		}
		return unsatisfied(new AptWitnessMarkingAndNumberOfVisitedMarkings(Optional.empty(), reachableMarkings.size()));
	}
}
