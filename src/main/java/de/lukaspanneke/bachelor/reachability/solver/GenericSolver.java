package de.lukaspanneke.bachelor.reachability.solver;

import de.lukaspanneke.bachelor.reachability.AugmentedQueryResult;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import de.lukaspanneke.bachelor.reachability.reduction.stubborn.StubbornSetProvider;
import de.lukaspanneke.bachelor.reachability.solver.result.GenericWitnessMarkingAndNumberOfVisitedMarkings;

import java.util.*;

import static de.lukaspanneke.bachelor.reachability.AugmentedQueryResult.dnf;
import static de.lukaspanneke.bachelor.reachability.AugmentedQueryResult.satisfied;
import static de.lukaspanneke.bachelor.reachability.AugmentedQueryResult.unsatisfied;

@Deprecated
public class GenericSolver<N, M, P, T> implements ReachabilitySolver<N, M, P, T, GenericWitnessMarkingAndNumberOfVisitedMarkings<M>> {
	private final NetAccessor<N, M, P, T> netAccessor;
	private final StubbornSetProvider<N, M, P, T> stubborn;

	public GenericSolver(NetAccessor<N, M, P, T> netAccessor, StubbornSetProvider<N, M, P, T> stubborn) {
		this.netAccessor = netAccessor;
		this.stubborn = stubborn;
	}

	@Override
	public AugmentedQueryResult<GenericWitnessMarkingAndNumberOfVisitedMarkings<M>> isReachable(N net, StateFormula<N, M, P, T> goal) {
		MeasurementUtil speed = new MeasurementUtil();
		speed.start();
		M initialMarking = netAccessor.getInitialMarking(net);
		if (goal.test(net, initialMarking)) {
			return satisfied(new GenericWitnessMarkingAndNumberOfVisitedMarkings<>(Optional.of(initialMarking), 1, marking -> this.netAccessor.renderMarking(net, marking)));
		}
		Set<M> visited = new HashSet<>();
		Queue<M> q = new LinkedList<>();
		q.add(initialMarking);
		visited.add(initialMarking);
		while (!q.isEmpty()) {
			M currentMarking = q.remove();
			Set<T> transitions;
			try {
				transitions = stubborn.get(net, currentMarking, goal);
			} catch (InterruptedException e) {
				return dnf(new GenericWitnessMarkingAndNumberOfVisitedMarkings<>(Optional.empty(), visited.size(), marking -> this.netAccessor.renderMarking(net, marking)));
			}
			for (T transition : transitions) {
				if (Thread.interrupted()) {
					LOGGER.error("Thread was interrupted. Abort state space generation");
					return dnf(new GenericWitnessMarkingAndNumberOfVisitedMarkings<>(Optional.empty(), visited.size(), marking -> this.netAccessor.renderMarking(net, marking)));
				}
				Optional<M> next_opt = netAccessor.fire(net, currentMarking, transition);
				if (next_opt.isEmpty()) {
					continue;
				}
				M nextMarking = next_opt.get();
				if (goal.test(net, nextMarking)) {
					return satisfied(new GenericWitnessMarkingAndNumberOfVisitedMarkings<>(next_opt, visited.size(), marking -> this.netAccessor.renderMarking(net, marking)));
				} else if (!visited.contains(nextMarking)) {
					q.add(nextMarking);
					visited.add(nextMarking);
					speed.onNewMarking(visited.size());
				}
			}
		}
		return unsatisfied(new GenericWitnessMarkingAndNumberOfVisitedMarkings<>(Optional.empty(), visited.size(), marking -> this.netAccessor.renderMarking(net, marking)));
	}
}
