package de.lukaspanneke.bachelor.reachability.reachabilitygraph;

import de.lukaspanneke.bachelor.lts.GenericTransitionSystem;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Transition;

public interface ReachabilityGraphBuilder {
	GenericTransitionSystem<Marking, Transition> build(PetriNet net);
}
