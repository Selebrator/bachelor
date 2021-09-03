package de.lukaspanneke.bachelor.reachability.solver.sparse;

import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseIntVector;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseNetUtil;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;
import de.lukaspanneke.bachelor.reachability.AugmentedQueryResult;
import de.lukaspanneke.bachelor.reachability.QueryResult;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import de.lukaspanneke.bachelor.reachability.solver.MeasurementUtil;
import de.lukaspanneke.bachelor.reachability.solver.ParallelReachabilitySolver;
import de.lukaspanneke.bachelor.reachability.solver.ReachabilitySolver;
import de.lukaspanneke.bachelor.reachability.solver.result.SparseWitnessMarkingAndNumberOfVisitedMarkings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.IntStream;

import static de.lukaspanneke.bachelor.reachability.AugmentedQueryResult.dnf;
import static de.lukaspanneke.bachelor.reachability.AugmentedQueryResult.satisfied;
import static de.lukaspanneke.bachelor.reachability.AugmentedQueryResult.unsatisfied;

public class DynamicBreadthFirstReachabilitySetSolver implements ParallelReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, SparseWitnessMarkingAndNumberOfVisitedMarkings> {

	private static final Logger LOGGER = LogManager.getLogger(DynamicBreadthFirstReachabilitySetSolver.class);

	@Override
	public AugmentedQueryResult<SparseWitnessMarkingAndNumberOfVisitedMarkings> isReachable(SparsePetriNet net, StateFormula<SparsePetriNet, SparseIntVector, String, Integer> goal) {
		ReachabilitySet reachabilitySet = new ReachabilitySet(net);
		return reachabilitySet.contains(goal);
	}

//	@Override
//	public List<AugmentedBoolean<Integer>> isReachableSequential(SparsePetriNet net, List<? extends StateFormula<SparsePetriNet, SparseIntVector, String, Integer>> goals) {
//		ReachabilitySet reachabilitySet = new ReachabilitySet(net);
//		return goals.stream()
//				.map(reachabilitySet::contains)
//				.map(witnessMarking -> AugmentedBoolean.of(witnessMarking.isPresent(), reachabilitySet.currentDiscovered.size()))
//				.toList();
//	}

	@Override
	public List<AugmentedQueryResult<SparseWitnessMarkingAndNumberOfVisitedMarkings>> isReachableParallel(SparsePetriNet net, List<? extends StateFormula<SparsePetriNet, SparseIntVector, String, Integer>> goals) {
		SparseGoalSet goalSet = new SparseGoalSet(net, goals);
		LOGGER.trace("Start state space generation");
		MeasurementUtil speed = new MeasurementUtil();
		speed.start();
		goalSet.update(net.getInitialMarking(), 1);
		if (goalSet.isDecided()) {
			return goalSet.getResults();
		}

		Set<SparseIntVector> visited = new HashSet<>();
		LinkedList<SparseIntVector> unexplored = new LinkedList<>();
		visited.add(net.getInitialMarking());
		unexplored.add(net.getInitialMarking());
		int transitions = net.getTransitionCount();

		while (!unexplored.isEmpty()) {
			SparseIntVector currentMarking = unexplored.remove();
			for (int transition = 0; transition < transitions; transition++) {
				if (Thread.interrupted()) {
					LOGGER.error("Thread was interrupted. Aborting state space generation. Explored {} states", visited.size());
					goalSet.markRemainingAs(QueryResult.DID_NOT_FINISH, visited.size());
					return goalSet.getResults();
				}
				Optional<SparseIntVector> next_opt = net.fire(currentMarking, transition);
				if (next_opt.isEmpty()) {
					continue;
				}
				SparseIntVector nextMarking = next_opt.get();
				if (visited.add(nextMarking)) {
					goalSet.update(nextMarking, visited.size());
					if (goalSet.isDecided()) {
						return goalSet.getResults();
					}
					unexplored.add(nextMarking);
					speed.onNewMarking(visited.size());
				}
			}
		}
		LOGGER.info("Entire state space ({} states) was explored.", visited.size());
		goalSet.markRemainingAs(QueryResult.UNSATISFIED, visited.size());
		return goalSet.getResults();
	}

	private static class ReachabilitySet {
		private static final Logger LOGGER = LogManager.getLogger(ReachabilitySet.class);
		private final MeasurementUtil speed = new MeasurementUtil();

		private final SparsePetriNet net;
		private final Set<SparseIntVector> currentDiscovered;
		private final LinkedList<SparseIntVector> currentDiscoveredUnexplored;
		private Iterator<Integer> currentTransitions = Collections.emptyIterator();
		private boolean isComplete = false;

		public ReachabilitySet(SparsePetriNet net) {
			this.net = net;
			this.currentDiscovered = new HashSet<>();
			this.currentDiscovered.add(net.getInitialMarking());
			this.currentDiscoveredUnexplored = new LinkedList<>();
			this.currentDiscoveredUnexplored.add(net.getInitialMarking());
		}

		public AugmentedQueryResult<SparseWitnessMarkingAndNumberOfVisitedMarkings> contains(StateFormula<SparsePetriNet, SparseIntVector, String, Integer> formula) {
			this.speed.start();
			Optional<SparseIntVector> matchInAlreadyDiscoveredStates = this.currentDiscovered.stream().filter(marking -> formula.test(this.net, marking)).findAny();
			if (matchInAlreadyDiscoveredStates.isPresent()) {
				return satisfied(new SparseWitnessMarkingAndNumberOfVisitedMarkings(matchInAlreadyDiscoveredStates, this.currentDiscovered.size(), this.net));
			} else if (this.isComplete) {
				return unsatisfied(new SparseWitnessMarkingAndNumberOfVisitedMarkings(Optional.empty(), this.currentDiscovered.size(), this.net));
			}
			this.speed.onNewMarking(this.currentDiscovered.size());

			if (this.currentDiscovered.size() == 1) {
				LOGGER.trace("Start state space generation");
			} else {
				LOGGER.trace("Continue state space generation");
			}
			while (!this.currentDiscoveredUnexplored.isEmpty()) {
				if (Thread.interrupted()) {
					LOGGER.error("Thread was interrupted. Aborting state space generation. Explored {} states", this.currentDiscovered.size());
					//LOGGER.info("... is still in a good state; calculation can be resumed.");
					return dnf(new SparseWitnessMarkingAndNumberOfVisitedMarkings(Optional.empty(), this.currentDiscovered.size(), this.net));
				}
				if (!this.currentTransitions.hasNext()) {
					this.currentTransitions = IntStream.range(0, net.getTransitionCount()).iterator();
				} else {
					LOGGER.debug("Resuming exploration of last marking.");
				}
				SparseIntVector currentMarking = this.currentDiscoveredUnexplored.remove();
				while (this.currentTransitions.hasNext()) {
					int transition = this.currentTransitions.next();
					Optional<SparseIntVector> opt_nextMarking = this.net.fire(currentMarking, transition);
					if (opt_nextMarking.isPresent()) {
						SparseIntVector nextMarking = opt_nextMarking.get();
						if (this.currentDiscovered.add(nextMarking)) {
							this.speed.onNewMarking(this.currentDiscovered.size());
							this.currentDiscoveredUnexplored.add(nextMarking);
						}
						if (formula.test(this.net, nextMarking)) {
							LOGGER.trace("Found goal state {}", SparseNetUtil.renderPlaces(nextMarking, net));
							if (this.currentTransitions.hasNext()) /* currentMarking is not yet fully explored. */ {
								this.currentDiscoveredUnexplored.addLast(currentMarking);
							}
							return satisfied(new SparseWitnessMarkingAndNumberOfVisitedMarkings(Optional.of(nextMarking), this.currentDiscovered.size(), this.net));
						}
					}
				}
			}

			this.isComplete = true;
			LOGGER.info("Entire state space ({} states) was explored. No witness for formula exists.", this.currentDiscovered.size());
			return unsatisfied(new SparseWitnessMarkingAndNumberOfVisitedMarkings(Optional.empty(), this.currentDiscovered.size(), this.net));
		}
	}
}
