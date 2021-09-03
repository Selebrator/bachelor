package de.lukaspanneke.bachelor.reachability.reachabilitygraph;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Transition;

import java.util.*;

import static de.lukaspanneke.bachelor.reachability.AptNetUtil.fire;
import static de.lukaspanneke.bachelor.reachability.AptNetUtil.fireable;

public class BreadthFirstReachabilitySetBuilder implements ReachabilitySetBuilder {
	@Override
	public Set<Marking> build(PetriNet net) {
		Marking initialMarking = net.getInitialMarking();
		Set<Transition> transitions = net.getTransitions();

		Set<Marking> visited = new HashSet<>();
		Queue<Marking> q = new LinkedList<>();
		visited.add(initialMarking);
		q.add(initialMarking);
		while (!q.isEmpty()) {
			Marking current = q.remove();
			for (Transition transition : transitions) {
				if (fireable(transition, current)) {
					Marking next = fire(current, transition);
					if (visited.add(next)) {
						q.add(next);
					}
				}
			}
		}
		return Collections.unmodifiableSet(visited);
	}
}
