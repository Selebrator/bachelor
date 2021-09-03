package de.lukaspanneke.bachelor.reachability.solver.apt;

import com.google.common.collect.Lists;
import de.lukaspanneke.bachelor.Enumerate;
import de.lukaspanneke.bachelor.IndexedItem;
import de.lukaspanneke.bachelor.reachability.AugmentedQueryResult;
import de.lukaspanneke.bachelor.reachability.QueryResult;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import de.lukaspanneke.bachelor.reachability.solver.ParallelReachabilitySolver;
import de.lukaspanneke.bachelor.reachability.solver.result.AptWitnessMarkingAndNumberOfVisitedMarkings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;

import java.util.*;
import java.util.stream.Collectors;

import static de.lukaspanneke.bachelor.reachability.AptNetUtil.fire;
import static de.lukaspanneke.bachelor.reachability.AptNetUtil.fireable;

public class AptDynamicBreadthFirstParallelBulkReachabilitySolver implements ParallelReachabilitySolver<PetriNet, Marking, Place, Transition, AptWitnessMarkingAndNumberOfVisitedMarkings> {

	private static final Logger LOGGER = LogManager.getLogger(AptDynamicBreadthFirstParallelBulkReachabilitySolver.class);

	@Override
	public AugmentedQueryResult<AptWitnessMarkingAndNumberOfVisitedMarkings> isReachable(PetriNet net, StateFormula<PetriNet, Marking, Place, Transition> goal) {
		return this.isReachableParallel(net, List.of(goal)).get(0);
	}

	@Override
	public List<AugmentedQueryResult<AptWitnessMarkingAndNumberOfVisitedMarkings>> isReachableParallel(PetriNet net, List<? extends StateFormula<PetriNet, Marking, Place, Transition>> goals) {
		AugmentedQueryResult<AptWitnessMarkingAndNumberOfVisitedMarkings> initialQueryResults
				= new AugmentedQueryResult<>(QueryResult.DID_NOT_FINISH, new AptWitnessMarkingAndNumberOfVisitedMarkings(Optional.empty(), 0));
		List<AugmentedQueryResult<AptWitnessMarkingAndNumberOfVisitedMarkings>> results = goals.stream()
				.map(goal -> initialQueryResults)
				.collect(Collectors.toList());
		List<IndexedItem<? extends StateFormula<PetriNet, Marking, Place, Transition>>> remaining = Lists.newArrayList(Enumerate.enumerate(goals));

		Marking initialMarking = net.getInitialMarking();
		onNewMarking(initialMarking, results, remaining, 1);
		Set<Transition> transitions = net.getTransitions();

		Set<Marking> visited = new HashSet<>();
		Queue<Marking> q = new LinkedList<>();
		visited.add(initialMarking);
		q.add(initialMarking);
		while (!q.isEmpty()) {
			Marking current = q.remove();
			for (Transition transition : transitions) {
				if (Thread.interrupted()) {
					LOGGER.error("Thread was interrupted. Abort state space generation");
					AugmentedQueryResult<AptWitnessMarkingAndNumberOfVisitedMarkings> interruptedQueryResult
							= new AugmentedQueryResult<>(QueryResult.DID_NOT_FINISH, new AptWitnessMarkingAndNumberOfVisitedMarkings(Optional.empty(), visited.size()));
					for (IndexedItem<? extends StateFormula<PetriNet, Marking, Place, Transition>> elem : remaining) {
						results.set(elem.index(), interruptedQueryResult);
					}
					return results;
				}
				if (fireable(transition, current)) {
					Marking next = fire(current, transition);
					if (visited.add(next)) {
						onNewMarking(next, results, remaining, visited.size());
						if (remaining.isEmpty()) {
							return results;
						}
						q.add(next);
					}
				}
			}
		}

		AugmentedQueryResult<AptWitnessMarkingAndNumberOfVisitedMarkings> remainingResult
				= new AugmentedQueryResult<>(QueryResult.UNSATISFIED, new AptWitnessMarkingAndNumberOfVisitedMarkings(Optional.empty(), visited.size()));
		for (IndexedItem<? extends StateFormula<PetriNet, Marking, Place, Transition>> elem : remaining) {
			results.set(elem.index(), remainingResult);
		}
		return results;
	}

	private static void onNewMarking(Marking newMarking, List<AugmentedQueryResult<AptWitnessMarkingAndNumberOfVisitedMarkings>> results, List<IndexedItem<? extends StateFormula<PetriNet, Marking, Place, Transition>>> remaining, int size) {
		ListIterator<IndexedItem<? extends StateFormula<PetriNet, Marking, Place, Transition>>> itr = remaining.listIterator();
		while (itr.hasNext()) {
			IndexedItem<? extends StateFormula<PetriNet, Marking, Place, Transition>> elem = itr.next();
			if (elem.item().test(newMarking.getNet(), newMarking)) {
				AptWitnessMarkingAndNumberOfVisitedMarkings witness = new AptWitnessMarkingAndNumberOfVisitedMarkings(Optional.of(newMarking), size);
				results.set(elem.index(), new AugmentedQueryResult<>(QueryResult.SATISFIED, witness));
				LOGGER.trace("Found witness of " + elem.index() + ": " + witness);
				itr.remove();
			}
		}
	}
}
