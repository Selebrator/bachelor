package de.lukaspanneke.bachelor.reachability.reduction.structural;

import de.lukaspanneke.bachelor.pn.sparse.mutable.IntMatrixSparseColumns;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseIntVector;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class TapaalA_FusionOfSequentialPlaces implements StructuralReduction {

	private static final Logger LOGGER = LogManager.getLogger(TapaalA_FusionOfSequentialPlaces.class);

	@Override
	public boolean reduce(SparsePetriNet net, Set<String> visible) {
		nextT0:
		for (int t0 = 0; t0 < net.getTransitionCount(); t0++) {
			SparseIntVector t0Pre = net.presetT(t0);
			if (t0Pre.size() != 1) {
				continue;
			}
			int w_p0_t0 = t0Pre.valueAt(0);
			if (w_p0_t0 != 1) {
				continue;
			}
			int p0 = t0Pre.keyAt(0);
			if (visible.contains(net.getPlaceName(p0))) {
				continue;
			}
			SparseIntVector t0Post = net.postsetT(t0);
			if (t0Post.containsKey(p0)) {
				continue;
			}
			for (int i = 0; i < t0Post.size(); i++) {
				int p = t0Post.keyAt(i);
				if (visible.contains(net.getPlaceName(p))) {
					continue nextT0;
				}
			}
			Optional<SparseIntVector> p0Post_copy = net.postsetP_copy(p0, 1, 1);
			if (p0Post_copy.isEmpty()) {
				continue;
			}

			/* found structure */
			LOGGER.trace(this.getClass().getSimpleName() +
					" p0 = " + net.getPlaceName(p0) +
					", t0 = " + net.getTransitionName(t0)
			);

			int m0_p0 = net.getInitialMarking().get(p0);
			SparseIntVector p0Pre_copy = net.presetP_copy(p0);
			IntMatrixSparseColumns flowMatrixTP = net.getFlowMatrixTP();
			for (int i = 0; i < t0Post.size(); i++) {
				int p = t0Post.keyAt(i);
				int w_t0_p = t0Post.valueAt(i);
				net.getInitialMarking().plusAssign(p, m0_p0 * w_t0_p);

				for (int j = 0; j < p0Pre_copy.size(); j++) {
					int t = p0Pre_copy.keyAt(j);
					int w_t_p0 = p0Pre_copy.valueAt(j);
					flowMatrixTP.plusAssign(p, t, w_t_p0 * w_t0_p);
				}
			}

			net.removeTransition(t0);
			net.removePlace(p0);
			return true;
		}
		return false;
	}
}
