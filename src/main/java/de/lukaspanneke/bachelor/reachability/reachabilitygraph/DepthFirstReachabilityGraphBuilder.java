package de.lukaspanneke.bachelor.reachability.reachabilitygraph;

import de.lukaspanneke.bachelor.lts.GenericTransitionSystem;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Transition;

import java.util.*;

public class DepthFirstReachabilityGraphBuilder implements ReachabilityGraphBuilder {

	@Override
	public GenericTransitionSystem<Marking, Transition> build(PetriNet net) {
		return new Builder(net).build();
	}

	private static class Builder {
		private final PetriNet net;
		private final GenericTransitionSystem<Marking, Transition> ts;
		private final Set<Transition> transitions;
		private final Set<Marking> visited = new HashSet<>();

		public Builder(PetriNet net) {
			this.net = net;
			this.ts = new GenericTransitionSystem<>(this.net.getInitialMarking());
			this.transitions = this.net.getTransitions();
		}

		public GenericTransitionSystem<Marking, Transition> build() {
			Marking initialMarking = this.net.getInitialMarking();
			rec(initialMarking);
			return this.ts;
		}

		private void rec(Marking marking) {
			if (!this.visited.add(marking)) {
				return;
			}
			for (Transition transition : this.transitions) {
				if (!transition.isFireable(marking)) {
					continue;
				}
				Marking nextMarking = transition.fire(marking);
				this.ts.createEdge(ts.createVertex(marking), this.ts.createVertex(nextMarking), transition);
				rec(nextMarking);
			}
		}
	}
}
