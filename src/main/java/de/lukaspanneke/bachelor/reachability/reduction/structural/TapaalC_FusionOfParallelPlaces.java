package de.lukaspanneke.bachelor.reachability.reduction.structural;

import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseIntVector;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class TapaalC_FusionOfParallelPlaces implements StructuralReduction {

	private static final Logger LOGGER = LogManager.getLogger(TapaalC_FusionOfParallelPlaces.class);

	@Override
	public boolean reduce(SparsePetriNet net, Set<String> visible) {
		for (int p0 = 0; p0 < net.getPlaceCount(); p0++) {
			if (visible.contains(net.getPlaceName(p0))) {
				continue;
			}
			int k = 1; // TODO
			nextP1:
			for (int p1 = 0; p1 < net.getPlaceCount(); p1++) {
				if (p0 == p1) {
					continue;
				}
				if (net.getInitialMarking().get(p0) < net.getInitialMarking().get(p1)) {
					continue;
				}
				SparseIntVector p0Pre_copy = net.presetP_copy(p0);
				for (int i = 0; i < p0Pre_copy.size(); i++) {
					int t = p0Pre_copy.keyAt(i);
					int w_t_p0 = p0Pre_copy.valueAt(i);
					int w_t_p1 = net.weightTP(t, p1);
					if (w_t_p0 != w_t_p1 * k) { // TODO
						continue nextP1;
					}
				}
				SparseIntVector p0Post_copy = net.postsetP_copy(p0);
				for (int i = 0; i < p0Post_copy.size(); i++) {
					int t = p0Post_copy.keyAt(i);
					int w_p0_t = p0Post_copy.valueAt(i);
					int w_p1_t = net.weightPT(p1, t);
					if (w_p0_t != w_p1_t * k) { // TODO
						continue nextP1;
					}
				}

				/* found structure */
				LOGGER.trace(this.getClass().getSimpleName() +
						" p0 = " + net.getPlaceName(p0) +
						", p1 = " + net.getPlaceName(p1)
				);

				net.removePlace(p0);
				return true;
			}
		}
		return false;
	}
}
