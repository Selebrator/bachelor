package de.lukaspanneke.bachelor.reachability.solver.apt;

import de.lukaspanneke.bachelor.reachability.AugmentedQueryResult;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import de.lukaspanneke.bachelor.reachability.solver.MeasurementUtil;
import de.lukaspanneke.bachelor.reachability.solver.ReachabilitySolver;
import de.lukaspanneke.bachelor.reachability.solver.result.AptWitnessMarkingAndNumberOfVisitedMarkings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

@Deprecated
public class AptDynamicBreadthFirstReachabilitySetSolver implements ReachabilitySolver<PetriNet, Marking, Place, Transition, AptWitnessMarkingAndNumberOfVisitedMarkings> {
	@Override
	public AugmentedQueryResult<AptWitnessMarkingAndNumberOfVisitedMarkings> isReachable(PetriNet net, StateFormula<PetriNet, Marking, Place, Transition> goal) {
		ReachabilitySet reachabilitySet = new ReachabilitySet(net);
		return reachabilitySet.contains(goal);
	}

//	@Override
//	public List<AugmentedBoolean<AptWitnessMarkingAndNumberOfVisitedMarkings>> isReachableSequential(PetriNet net, List<? extends StateFormula<PetriNet, Marking, Place, Transition>> goals) {
//		ReachabilitySet reachabilitySet = new ReachabilitySet(net);
//		return goals.stream()
//				.map(reachabilitySet::contains)
//				.map(witnessMarking -> AugmentedBoolean.of(witnessMarking.isPresent(), new AptWitnessMarkingAndNumberOfVisitedMarkings(witnessMarking, reachabilitySet.currentDiscovered.size())))
//				.toList();
//	}

	private static class ReachabilitySet {
		private static final Logger LOGGER = LogManager.getLogger(ReachabilitySet.class);
		private final MeasurementUtil speed = new MeasurementUtil();

		private final PetriNet net;
		private final Set<Transition> transitions;
		private final Set<Marking> currentDiscovered;
		private final LinkedList<Marking> currentDiscoveredUnexplored;
		private Iterator<Transition> currentTransitions = Collections.emptyIterator();
		private boolean isComplete = false;


		public ReachabilitySet(PetriNet net) {
			this.net = net;
			this.transitions = net.getTransitions();
			this.currentDiscovered = new HashSet<>();
			this.currentDiscovered.add(net.getInitialMarking());
			this.currentDiscoveredUnexplored = new LinkedList<>();
			this.currentDiscoveredUnexplored.add(net.getInitialMarking());
		}

		public AugmentedQueryResult<AptWitnessMarkingAndNumberOfVisitedMarkings> contains(StateFormula<PetriNet, Marking, Place, Transition> formula) {
			this.speed.start();
			Optional<Marking> matchInAlreadyDiscoveredStates = this.currentDiscovered.stream().filter(marking -> formula.test(this.net, marking)).findAny();
			if (matchInAlreadyDiscoveredStates.isPresent()) {
				return satisfied(new AptWitnessMarkingAndNumberOfVisitedMarkings(matchInAlreadyDiscoveredStates, this.currentDiscovered.size()));
			} else if (this.isComplete) {
				return unsatisfied(new AptWitnessMarkingAndNumberOfVisitedMarkings(Optional.empty(), this.currentDiscovered.size()));
			}
			this.speed.onNewMarking(this.currentDiscovered.size());

			if (this.currentDiscovered.size() == 1) {
				LOGGER.info("Start state space construction ...");
			} else {
				LOGGER.info("Continue state space construction ...");
			}
			while (!this.currentDiscoveredUnexplored.isEmpty()) {
				if (Thread.interrupted()) {
					LOGGER.error("... aborting state space construction, because thread was interrupted.");
					LOGGER.info("... is still in a good state; calculation can be resumed.");
					return dnf(new AptWitnessMarkingAndNumberOfVisitedMarkings(Optional.empty(), this.currentDiscovered.size()));
				}
				if (!this.currentTransitions.hasNext()) {
					this.currentTransitions = this.transitions.iterator();
				} else {
					LOGGER.debug("... resuming exploration of last marking.");
				}
				Marking currentMarking = this.currentDiscoveredUnexplored.remove();
				while (this.currentTransitions.hasNext()) {
					Transition transition = this.currentTransitions.next();
					if (fireable(transition, currentMarking)) {
						Marking nextMarking = fire(currentMarking, transition);
						if (this.currentDiscovered.add(nextMarking)) {
							this.speed.onNewMarking(this.currentDiscovered.size());
							this.currentDiscoveredUnexplored.add(nextMarking);
						}
						if (formula.test(this.net, nextMarking)) {
							LOGGER.info("... pausing state space construction, because goal state was reached.");
							if (this.currentTransitions.hasNext()) /* currentMarking is not yet fully explored. */ {
								this.currentDiscoveredUnexplored.addLast(currentMarking);
							}
							return satisfied(new AptWitnessMarkingAndNumberOfVisitedMarkings(Optional.of(nextMarking), this.currentDiscovered.size()));
						}
					}
				}
			}

			this.isComplete = true;
			LOGGER.info("... full state space constructed.");
			return unsatisfied(new AptWitnessMarkingAndNumberOfVisitedMarkings(Optional.empty(), this.currentDiscovered.size()));
		}
	}
}
