package de.lukaspanneke.bachelor.reachability.solver.apt;

import de.lukaspanneke.bachelor.reachability.AugmentedQueryResult;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import de.lukaspanneke.bachelor.reachability.solver.ReachabilitySolver;
import de.lukaspanneke.bachelor.reachability.solver.result.AptWitnessPathAndNumberOfVisitedMarkings;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;

import java.util.*;

import static de.lukaspanneke.bachelor.reachability.AptNetUtil.fire;
import static de.lukaspanneke.bachelor.reachability.AptNetUtil.fireable;
import static de.lukaspanneke.bachelor.reachability.AugmentedQueryResult.dnf;
import static de.lukaspanneke.bachelor.reachability.AugmentedQueryResult.satisfied;
import static de.lukaspanneke.bachelor.reachability.AugmentedQueryResult.unsatisfied;

public class AptDynamicBreadthFirstReachabilityGraphSolver implements ReachabilitySolver<PetriNet, Marking, Place, Transition, AptWitnessPathAndNumberOfVisitedMarkings> {

	@Override
	public AugmentedQueryResult<AptWitnessPathAndNumberOfVisitedMarkings> isReachable(PetriNet net, StateFormula<PetriNet, Marking, Place, Transition> goal) {
		Marking initialMarking = net.getInitialMarking();
		if (goal.test(net, initialMarking)) {
			return satisfied(new AptWitnessPathAndNumberOfVisitedMarkings(Optional.of(Collections.emptyList()), Optional.of(initialMarking), 1));
		}
		Set<Marking> visited = new HashSet<>();
		Queue<Marking> q = new LinkedList<>();
		record PredecessorTransitionOnShortestPathToMarking(Marking marking, Transition predecessorTransition) {
		}
		Map<Marking, PredecessorTransitionOnShortestPathToMarking> predecessorOnShortestPath = new HashMap<>();
		q.add(initialMarking);
		visited.add(initialMarking);
		while (!q.isEmpty()) {
			Marking current = q.remove();
			Set<Transition> transitions;
			try {
				transitions = this.consideredOutgoingTransitions(current, goal);
			} catch (InterruptedException e) {
				return dnf(new AptWitnessPathAndNumberOfVisitedMarkings(Optional.empty(), Optional.empty(), visited.size()));
			}
			for (Transition transition : transitions) {
				if (Thread.interrupted()) {
					LOGGER.error("Thread was interrupted. Abort state space generation");
					return dnf(new AptWitnessPathAndNumberOfVisitedMarkings(Optional.empty(), Optional.empty(), visited.size()));
				}
				if (!fireable(transition, current)) {
					continue;
				}
				Marking next = fire(current, transition);
				if (goal.test(net, next)) {
					LinkedList<Transition> path = new LinkedList<>();
					path.addFirst(transition);
					for (PredecessorTransitionOnShortestPathToMarking predecessor = predecessorOnShortestPath.get(current); predecessor != null; predecessor = predecessorOnShortestPath.get(predecessor.marking())) {
						path.addFirst(predecessor.predecessorTransition());
					}
					return satisfied(new AptWitnessPathAndNumberOfVisitedMarkings(Optional.of(Collections.unmodifiableList(path)), Optional.of(next), visited.size() + 1));
				} else if (!visited.contains(next)) {
					q.add(next);
					visited.add(next);
					predecessorOnShortestPath.put(next, new PredecessorTransitionOnShortestPathToMarking(current, transition));
				}
			}
		}
		return unsatisfied(new AptWitnessPathAndNumberOfVisitedMarkings(Optional.empty(), Optional.empty(), visited.size()));
	}

	protected Set<Transition> consideredOutgoingTransitions(Marking marking, StateFormula<PetriNet, Marking, Place, Transition> formula) throws InterruptedException {
		return marking.getNet().getTransitions();
	}
}
