package de.lukaspanneke.bachelor.reachability.solver.sparse;

import com.google.common.hash.BloomFilter;
import de.lukaspanneke.bachelor.help.ResizingIntArray;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseIntVector;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseNetUtil;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;
import de.lukaspanneke.bachelor.reachability.AugmentedQueryResult;
import de.lukaspanneke.bachelor.reachability.QueryResult;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import de.lukaspanneke.bachelor.reachability.solver.MeasurementUtil;
import de.lukaspanneke.bachelor.reachability.solver.ParallelReachabilitySolver;
import de.lukaspanneke.bachelor.reachability.solver.result.SparseWitnessMarkingAndNumberOfVisitedMarkings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static de.lukaspanneke.bachelor.reachability.AugmentedQueryResult.dnf;
import static de.lukaspanneke.bachelor.reachability.AugmentedQueryResult.satisfied;
import static de.lukaspanneke.bachelor.reachability.AugmentedQueryResult.unsatisfied;

public class ProbabilisticDepthFirstReachabilitySolver implements ParallelReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, SparseWitnessMarkingAndNumberOfVisitedMarkings> {

	private static final Logger LOGGER = LogManager.getLogger(ProbabilisticDepthFirstReachabilitySolver.class);

	public AugmentedQueryResult<SparseWitnessMarkingAndNumberOfVisitedMarkings> isReachable(SparsePetriNet net, StateFormula<SparsePetriNet, SparseIntVector, String, Integer> goal) {
		ProbabilisticReachabilitySet stateSpace = new ProbabilisticReachabilitySet(net);
		return stateSpace.contains(goal);
	}

	@Override
	public List<AugmentedQueryResult<SparseWitnessMarkingAndNumberOfVisitedMarkings>> isReachableParallel(SparsePetriNet net, List<? extends StateFormula<SparsePetriNet, SparseIntVector, String, Integer>> goals) {
		ProbabilisticReachabilitySet stateSpace = new ProbabilisticReachabilitySet(net);
		return stateSpace.contains(goals);
	}

	private static long maxNumInsert(long sizeBytes, double fpp) {
		return (long) (-sizeBytes * Math.log(2) * Math.log(2) / Math.log(fpp));
	}

	private static double minFpp(long sizeBytes, long insertions) {
		return Math.exp(-sizeBytes * Math.log(2) * Math.log(2) / insertions);
	}

	@SuppressWarnings("UnstableApiUsage")
	public static class ProbabilisticReachabilitySet {

		public final MeasurementUtil speed = new MeasurementUtil();

		private final SparsePetriNet net;
		private final BloomFilter<SparseIntVector> probablyVisited;
		private long numberVisited = 0;
		private final int numberOfTransitions;

		private SparseIntVector marking;
		private final ResizingIntArray transitions = new ResizingIntArray(1024);

		public ProbabilisticReachabilitySet(SparsePetriNet net) {
			this.net = net;
			this.numberOfTransitions = this.net.getTransitionCount();

			/* use 75% of all jvm memory, but at most 12 gb */
			long memSize = Math.min(Runtime.getRuntime().maxMemory(), 16L * 1024 * 1024 * 1024) * 75 / 100;
			double fpp = 0.000001;
			//long insertions = 30_000_000L;
			//double minFpp = minFpp(memSize, insertions);
			long maxInsertions = maxNumInsert(memSize, fpp);
			this.probablyVisited = BloomFilter.create(SparseIntVector::funnel, maxInsertions, fpp);

			this.marking = this.net.getInitialMarking();

			this.transitions.addLast2(-1, 0);
		}

		/* callable only once per instance */
		public AugmentedQueryResult<SparseWitnessMarkingAndNumberOfVisitedMarkings> contains(StateFormula<SparsePetriNet, SparseIntVector, String, Integer> goal) {
			if (++this.numberVisited != 1) {
				throw new IllegalStateException();
			}
			LOGGER.trace("Start probabilistic state space generation");
			this.speed.start();
			SparseIntVector currentMarking = this.marking;
			while (currentMarking != null) {
				if (Thread.interrupted()) {
					LOGGER.error("Thread was interrupted. Aborting state space generation. Explored {} states", this.numberVisited);
					return dnf(new SparseWitnessMarkingAndNumberOfVisitedMarkings(Optional.empty(), this.numberVisited, this.net));
				}
				if (goal.test(this.net, currentMarking)) {
					LOGGER.trace("Found goal state {}", SparseNetUtil.renderPlaces(currentMarking, this.net));
					return satisfied(new SparseWitnessMarkingAndNumberOfVisitedMarkings(Optional.of(currentMarking), this.numberVisited, this.net));
				} else {
					try {
						currentMarking = this.nextState();
					} catch (InterruptedException e) {
						return dnf(new SparseWitnessMarkingAndNumberOfVisitedMarkings(Optional.empty(), this.numberVisited, this.net));
					}
				}
			}
			LOGGER.info("Entire probabilistic state space ({} states) was explored. No witness for formula exists. This approach has a low chance to miss important states!", this.numberVisited);
			return unsatisfied(new SparseWitnessMarkingAndNumberOfVisitedMarkings(Optional.empty(), this.numberVisited, this.net));
		}

		/* callable only once per instance */
		public List<AugmentedQueryResult<SparseWitnessMarkingAndNumberOfVisitedMarkings>> contains(List<? extends StateFormula<SparsePetriNet, SparseIntVector, String, Integer>> goals) {
			if (++this.numberVisited != 1) {
				throw new IllegalStateException();
			}
			SparseGoalSet goalSet = new SparseGoalSet(this.net, goals);
			LOGGER.trace("Start probabilistic state space generation");
			this.speed.start();

			SparseIntVector currentMarking = this.marking;
			while (currentMarking != null) {
				if (Thread.interrupted()) {
					LOGGER.error("Thread was interrupted. Aborting state space generation. Explored {} states", this.numberVisited);
					goalSet.markRemainingAs(QueryResult.DID_NOT_FINISH, this.numberVisited);
					return goalSet.getResults();
				}
				goalSet.update(currentMarking, this.numberVisited);

				if (goalSet.isDecided()) {
					LOGGER.info("Decided set");
					return goalSet.getResults();
				} else {
					try {
						currentMarking = this.nextState();
					} catch (InterruptedException e) {
						goalSet.markRemainingAs(QueryResult.DID_NOT_FINISH, this.numberVisited);
						return goalSet.getResults();
					}
				}
			}
			LOGGER.info("Entire probabilistic state space ({} states) was explored. This approach has a low chance to miss important states!", this.numberVisited);
			goalSet.markRemainingAs(QueryResult.UNSATISFIED, this.numberVisited);
			return goalSet.getResults();
		}

		private SparseIntVector nextState() throws InterruptedException {
			while (!this.transitions.isEmpty()) {
				int nextUnexploredTransition;
				while ((nextUnexploredTransition = this.transitions.getAndIncrementLast()) < this.numberOfTransitions) {
					if (Thread.interrupted()) {
						LOGGER.error("Thread was interrupted. Aborting state space generation. Explored {} states", this.numberVisited);
						throw new InterruptedException();
					}
					Optional<SparseIntVector> nextMarking_opt = this.net.fire(this.marking, nextUnexploredTransition);
					if (nextMarking_opt.isPresent()) {
						SparseIntVector nextMarking = nextMarking_opt.get();
						if (!this.probablyVisited.mightContain(nextMarking)) {
							this.probablyVisited.put(nextMarking);
							this.numberVisited++;
							this.speed.onNewMarking(this.numberVisited);
							this.transitions.addLast2(nextUnexploredTransition, 0);
							this.marking = nextMarking;
							return nextMarking;
						}
					}
				}

				/* the current marking is fully explored.
				 * go back one marking and continue exploring it.
				 */
				if (!this.undoLastTransition()) {
					return null;
				}
			}
			return null;
		}

		private boolean undoLastTransition() {
			if (this.transitions.isEmpty()) {
				return false;
			}
			this.transitions.removeLast();
			int lastTransition = this.transitions.removeLast();
			if (lastTransition == -1) {
				return false;
			}
			this.marking = SparseIntVector.weightedSum(1, SparseIntVector.weightedSum(1, this.marking, -1, net.postsetT(lastTransition)), 1, net.presetT(lastTransition));
			return true;
		}
	}
}
