package de.lukaspanneke.bachelor.reachability.reduction.structural;

import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseIntVector;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class TapaalB_FusionOfSequentialTransitions implements StructuralReduction {

	private static final Logger LOGGER = LogManager.getLogger(TapaalB_FusionOfSequentialTransitions.class);

	@Override
	public boolean reduce(SparsePetriNet net, Set<String> visible) {
		return reduceFirstTry(net, visible);
	}

	private static boolean reduceFirstTry(SparsePetriNet net, Set<String> visible) {
		nextPlace:
		for (int p0 = 0; p0 < net.getPlaceCount(); p0++) {
			if (visible.contains(net.getPlaceName(p0))) {
				continue;
			}

			SparseIntVector p0Pre_copy = net.presetP_copy(p0);
			if (p0Pre_copy.size() != 1) {
				continue;
			}
			int t0 = p0Pre_copy.keyAt(0);

			SparseIntVector p0Post_copy = net.postsetP_copy(p0);
			if (p0Post_copy.size() != 1) {
				continue;
			}
			int t1 = p0Post_copy.keyAt(0);

			SparseIntVector t1Pre = net.presetT(t1);
			if (t1Pre.size() != 1) {
				continue;
			}
			if (p0 != t1Pre.keyAt(0)) {
				continue;
			}

			int w = t1Pre.valueAt(0);
			int w_t0_p0 = net.weightTP(t0, p0);
			if (w_t0_p0 % w != 0) {
				continue;
			}
			int k = w_t0_p0 / w;

			SparseIntVector t1Post = net.postsetT(t1);
			for (int p = 0; p < t1Post.size(); p++) {
				if (visible.contains(net.getPlaceName(t1Post.keyAt(p)))) {
					continue nextPlace;
				}
			}

			/* found structure */
			update(net, p0, t0, t1, w, k);
			return true;
		}
		return false;
	}

	private static boolean reduceWithDelayedPresetOfPlace(SparsePetriNet net, Set<String> visible) {
		nextT1:
		for (int t1 = 0; t1 < net.getTransitionCount(); t1++) {
			SparseIntVector t1Pre = net.presetT(t1);
			if (t1Pre.size() != 1) {
				continue;
			}
			int p0 = t1Pre.keyAt(0);
			if (visible.contains(net.getPlaceName(p0))) {
				continue;
			}
			int w = t1Pre.valueAt(0);
			SparseIntVector t1Post = net.postsetT(t1);
			for (int i = 0; i < t1Post.size(); i++) {
				int p = t1Post.keyAt(i);
				if (visible.contains(net.getPlaceName(p))) {
					continue nextT1;
				}
			}

			Optional<SparseIntVector> p0Pre_copy = net.presetP_copy(p0, 1, 1);
			if (p0Pre_copy.isEmpty()) {
				continue;
			}
			int t0 = p0Pre_copy.get().keyAt(0);
			int w_t0_p0 = net.weightTP(t0, p0);
			if (w_t0_p0 % w != 0) {
				continue;
			}
			int k = w_t0_p0 / w;

			Optional<SparseIntVector> p0Post_copy = net.presetP_copy(p0, 1, 1);
			if (p0Post_copy.isEmpty()) {
				continue;
			}

			/* found structure */
			update(net, p0, t0, t1, w, k);
			return true;
		}
		return false;
	}

	private static void update(SparsePetriNet net, int p0, int t0, int t1, int w, int k) {
		LOGGER.trace(TapaalB_FusionOfSequentialTransitions.class.getSimpleName()
				+ " p0 = " + net.getPlaceName(p0)
				+ ", t0 = " + net.getTransitionName(t0)
				+ ", t1 = " + net.getTransitionName(t1)
		);
		SparseIntVector m0 = net.getInitialMarking();
		SparseIntVector t0Post = net.postsetT(t0);
		SparseIntVector t1Post = net.postsetT(t1);
		int m0_p0 = m0.get(p0);
		int factor = m0_p0 / w;
		m0.replaceWith(SparseIntVector.weightedSum(1, m0, factor, t1Post));
		t0Post.replaceWith(SparseIntVector.weightedSum(1, t0Post, k, t1Post));

		net.removePlace(p0);
		net.removeTransition(t1);
	}
}
