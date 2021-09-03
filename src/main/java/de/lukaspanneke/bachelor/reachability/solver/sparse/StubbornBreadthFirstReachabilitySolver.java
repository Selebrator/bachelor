package de.lukaspanneke.bachelor.reachability.solver.sparse;

import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseIntVector;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseNetUtil;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;
import de.lukaspanneke.bachelor.reachability.AugmentedQueryResult;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import de.lukaspanneke.bachelor.reachability.reduction.stubborn.StubbornSetProvider;
import de.lukaspanneke.bachelor.reachability.solver.MeasurementUtil;
import de.lukaspanneke.bachelor.reachability.solver.ReachabilitySolver;
import de.lukaspanneke.bachelor.reachability.solver.result.SparseWitnessMarkingAndNumberOfVisitedMarkings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static de.lukaspanneke.bachelor.reachability.AugmentedQueryResult.dnf;
import static de.lukaspanneke.bachelor.reachability.AugmentedQueryResult.satisfied;
import static de.lukaspanneke.bachelor.reachability.AugmentedQueryResult.unsatisfied;

public class StubbornBreadthFirstReachabilitySolver implements ReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, SparseWitnessMarkingAndNumberOfVisitedMarkings> {

	private static final Logger LOGGER = LogManager.getLogger(StubbornBreadthFirstReachabilitySolver.class);

	private final StubbornSetProvider<SparsePetriNet, SparseIntVector, String, Integer> stubborn;

	public StubbornBreadthFirstReachabilitySolver(StubbornSetProvider<SparsePetriNet, SparseIntVector, String, Integer> stubborn) {
		this.stubborn = stubborn;
	}

	public AugmentedQueryResult<SparseWitnessMarkingAndNumberOfVisitedMarkings> isReachable(SparsePetriNet net, StateFormula<SparsePetriNet, SparseIntVector, String, Integer> goal) {
		MeasurementUtil speed = new MeasurementUtil();
		speed.start();
		SparseIntVector initialMarking = net.getInitialMarking();
		if (goal.test(net, initialMarking)) {
			LOGGER.debug("Initial state is goal state");
			return satisfied(new SparseWitnessMarkingAndNumberOfVisitedMarkings(Optional.of(initialMarking), 1, net));
		}
		LOGGER.trace("Start stubborn state space generation");
		Set<SparseIntVector> visited = new HashSet<>();
		Queue<SparseIntVector> unexplored = new LinkedList<>();
		unexplored.add(initialMarking);
		visited.add(initialMarking);
		speed.onNewMarking(1);
		while (!unexplored.isEmpty()) {
			SparseIntVector currentMarking = unexplored.remove();
			Set<Integer> transitions;
			try {
				transitions = this.stubborn.get(net, currentMarking, goal);
			} catch (InterruptedException e) {
				return dnf(new SparseWitnessMarkingAndNumberOfVisitedMarkings(Optional.empty(), visited.size(), net));
			}
			for (int transition : transitions) {
				if (Thread.interrupted()) {
					LOGGER.error("Thread was interrupted. Abort state space generation. Explored {} markings", visited.size());
					return dnf(new SparseWitnessMarkingAndNumberOfVisitedMarkings(Optional.empty(), visited.size(), net));
				}
				Optional<SparseIntVector> next_opt = net.fire(currentMarking, transition);
				if (next_opt.isEmpty()) {
					continue;
				}
				SparseIntVector nextMarking = next_opt.get();
				if (visited.add(nextMarking)) {
					if (goal.test(net, nextMarking)) {
						LOGGER.trace("Found goal state {}", SparseNetUtil.renderPlaces(nextMarking, net));
						return satisfied(new SparseWitnessMarkingAndNumberOfVisitedMarkings(next_opt, visited.size(), net));
					}
					unexplored.add(nextMarking);
					speed.onNewMarking(visited.size());
				}
			}
		}
		LOGGER.info("Entire stubborn state space ({} states) was explored. No witness for formula exists.", visited.size());
		return unsatisfied(new SparseWitnessMarkingAndNumberOfVisitedMarkings(Optional.empty(), visited.size(), net));
	}
}
