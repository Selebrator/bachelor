package de.lukaspanneke.bachelor.reachability.reachabilitygraph;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;

import java.util.*;

public interface ReachabilitySetBuilder {
	Set<Marking> build(PetriNet net);
}
