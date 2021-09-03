package de.lukaspanneke.bachelor.reachability.reduction.structural;

import de.lukaspanneke.bachelor.pn.sparse.mutable.IntMatrixSparseColumns;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseIntVector;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class TapaalH_SimplifyCycle implements StructuralReduction {

	private static final Logger LOGGER = LogManager.getLogger(TapaalH_SimplifyCycle.class);

	@Override
	public boolean reduce(SparsePetriNet net, Set<String> visible) {
		for (int t0 = 0; t0 < net.getTransitionCount(); t0++) {
			SparseIntVector t0Pre = net.presetT(t0);
			if (t0Pre.size() != 1) {
				continue;
			}
			int p0 = t0Pre.keyAt(0);
			int w_p0_t0 = t0Pre.valueAt(0);
			if (w_p0_t0 != 1) {
				continue;
			}

			SparseIntVector t0Post = net.postsetT(t0);
			if (t0Post.size() != 1) {
				continue;
			}
			int p1 = t0Post.keyAt(0);
			if (p0 == p1) {
				continue;
			}
			int w_t0_p1 = t0Post.valueAt(0);
			if (w_t0_p1 != 1) {
				continue;
			}

			if (visible.contains(net.getPlaceName(p0))) {
				continue;
			}
			if (visible.contains(net.getPlaceName(p1))) {
				continue;
			}

			for (int t1 = 0; t1 < net.getTransitionCount(); t1++) {
				if (t0 == t1) {
					continue;
				}
				SparseIntVector t1Pre = net.presetT(t1);
				if (!t1Pre.equals(t0Post)) {
					continue;
				}
				SparseIntVector t1Post = net.postsetT(t1);
				if (!t0Pre.equals(t1Post)) {
					continue;
				}

				int w_p1_t1 = t1Pre.valueAt(0);
				if (w_p1_t1 != 1) {
					continue;
				}
				int w_t1_p0 = t1Post.valueAt(0);
				if (w_t1_p0 != 1) {
					continue;
				}

				/* found structure */
				LOGGER.trace(this.getClass().getSimpleName()
						+ " p0 = " + net.getPlaceName(p0)
						+ ", p1 = " + net.getPlaceName(p1)
						+ ", t0 = " + net.getTransitionName(t0)
						+ ", t1 = " + net.getTransitionName(t1)
				);

				IntMatrixSparseColumns flowMatrixTP = net.getFlowMatrixTP();
				IntMatrixSparseColumns flowMatrixPT = net.getFlowMatrixPT();
				for (int t = 0; t < net.getTransitionCount(); t++) {
					if (t == t0 || t == t1) {
						continue;
					}
					flowMatrixTP.plusAssign(p1, t, flowMatrixTP.get(p0, t));
					flowMatrixPT.plusAssign(p1, t, flowMatrixPT.get(p0, t));
				}
				t1Post.set(p1, 1);
				t1Pre.set(p1, 1);

				SparseIntVector m0 = net.getInitialMarking();
				m0.plusAssign(p1, m0.get(p0));

				net.removeTransition(t0);
				net.removePlace(p0);
				return true;
			}
		}
		return false;
	}
}
