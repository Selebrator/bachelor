package de.lukaspanneke.bachelor.reachability.reduction.structural;

import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;

import java.util.*;

public class Its9_DiscardConstantPlace implements StructuralReduction {
	@Override
	public boolean reduce(SparsePetriNet net, Set<String> visible) {
		for (int p = 0; p < net.getPlaceCount(); p++) {
			if (visible.contains(net.getPlaceName(p))) {
				continue;
			}
			if (net.presetP_copy(p).equals(net.postsetP_copy(p))) {
				int m0_p = net.getInitialMarking().get(p);
				for (int t = 0; t < net.getTransitionCount(); t++) {
					if (net.weightPT(p, t) > m0_p) {
						net.removeTransition(t);
					}
				}
				net.removePlace(p);
				return true;
			}
		}
		return false;
	}
}
