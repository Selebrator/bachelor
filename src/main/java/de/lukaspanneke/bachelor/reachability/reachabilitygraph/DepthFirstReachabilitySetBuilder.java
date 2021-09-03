package de.lukaspanneke.bachelor.reachability.reachabilitygraph;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Transition;

import java.util.*;

import static de.lukaspanneke.bachelor.reachability.AptNetUtil.fire;
import static de.lukaspanneke.bachelor.reachability.AptNetUtil.fireable;

public class DepthFirstReachabilitySetBuilder implements ReachabilitySetBuilder {
	@Override
	public Set<Marking> build(PetriNet net) {
		Marking initialMarking = net.getInitialMarking();
		Set<Marking> visited = new HashSet<>();
		build(initialMarking, visited, net.getTransitions());
		return Collections.unmodifiableSet(visited);
	}

	private static void build(Marking marking, Set<Marking> visited, Set<Transition> transitions) {
		if (visited.add(marking)) {
			for (Transition transition : transitions) {
				if (fireable(transition, marking)) {
					build(fire(marking, transition), visited, transitions);
				}
			}
		}
	}
}
