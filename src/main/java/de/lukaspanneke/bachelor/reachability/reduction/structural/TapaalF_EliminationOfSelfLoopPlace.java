package de.lukaspanneke.bachelor.reachability.reduction.structural;

import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseIntVector;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class TapaalF_EliminationOfSelfLoopPlace implements StructuralReduction {

	private static final Logger LOGGER = LogManager.getLogger(TapaalF_EliminationOfSelfLoopPlace.class);

	@Override
	public boolean reduce(SparsePetriNet net, Set<String> visible) {
		nextPlace:
		for (int p0 = 0; p0 < net.getPlaceCount(); p0++) {
			if (visible.contains(net.getPlaceName(p0))) {
				continue;
			}
			SparseIntVector p0Post_copy = net.postsetP_copy(p0);
			int m0_p0 = net.getInitialMarking().get(p0);
			for (int i = 0; i < p0Post_copy.size(); i++) {
				if (m0_p0 < p0Post_copy.valueAt(i)) {
					continue nextPlace;
				}
			}
			SparseIntVector p0Pre_copy = net.presetP_copy(p0);
			if (!p0Pre_copy.greaterEquals(p0Post_copy)) {
				continue;
			}

			/* found structure */
			LOGGER.trace(this.getClass().getSimpleName()
					+ " p0 = " + net.getPlaceName(p0)
			);

			net.removePlace(p0);
			return true;
		}
		return false;
	}
}
