package de.lukaspanneke.bachelor.reachability.reduction.structural;

import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;

import java.util.*;

public interface StructuralReduction {
	boolean reduce(SparsePetriNet net, Set<String> visible);
}
