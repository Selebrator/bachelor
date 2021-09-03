package de.lukaspanneke.bachelor.reachability.reduction.structural;

import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseIntVector;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class TapaalG_EliminationOfSelfLoopTransition implements StructuralReduction {

	private static final Logger LOGGER = LogManager.getLogger(TapaalG_EliminationOfSelfLoopTransition.class);

	@Override
	public boolean reduce(SparsePetriNet net, Set<String> visible) {
		nextTransition:
		for (int t0 = 0; t0 < net.getTransitionCount(); t0++) {
			SparseIntVector t0Pre = net.presetT(t0);
			SparseIntVector t0Post = net.postsetT(t0);
			if (!t0Pre.containsKeys(t0Post)) {
				continue;
			}
			for (int i = 0; i < t0Pre.size(); i++) {
				int p = t0Pre.keyAt(i);
				int w_p_t0 = t0Pre.valueAt(i);
				int w_t0_p = t0Post.get(p);
				if (!(w_p_t0 == w_t0_p || (w_p_t0 > w_t0_p && !visible.contains(net.getPlaceName(p))))) {
					continue nextTransition;
				}
			}

			/* found structure */
			LOGGER.trace(this.getClass().getSimpleName()
					+ " t0 = " + net.getTransitionName(t0)
			);

			net.removeTransition(t0);
			return true;
		}
		return false;
	}
}
