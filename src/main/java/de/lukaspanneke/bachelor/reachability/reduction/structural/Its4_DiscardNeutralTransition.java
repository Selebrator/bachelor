package de.lukaspanneke.bachelor.reachability.reduction.structural;

import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;

import java.util.*;

public class Its4_DiscardNeutralTransition implements StructuralReduction {
	@Override
	public boolean reduce(SparsePetriNet net, Set<String> visible) {
		for (int transition = 0; transition < net.getTransitionCount(); transition++) {
			if (net.presetT(transition).equals(net.postsetT(transition))) {
				net.removeTransition(transition);
				return true;
			}
		}
		return false;
	}
}
