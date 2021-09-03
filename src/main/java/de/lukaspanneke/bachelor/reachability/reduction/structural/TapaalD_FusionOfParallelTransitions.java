package de.lukaspanneke.bachelor.reachability.reduction.structural;

import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class TapaalD_FusionOfParallelTransitions implements StructuralReduction {

	private static final Logger LOGGER = LogManager.getLogger(TapaalD_FusionOfParallelTransitions.class);

	@Override
	public boolean reduce(SparsePetriNet net, Set<String> visible) {
		for (int t0 = 0; t0 < net.getTransitionCount(); t0++) {
			for (int t1 = 0; t1 < net.getTransitionCount(); t1++) {
				if (t0 == t1) {
					continue;
				}

				OptionalInt kPre = net.presetT(t0).scalar(net.presetT(t1));
				OptionalInt kPost = net.postsetT(t0).scalar(net.postsetT(t1));
				if (kPre.isEmpty() || kPost.isEmpty() || kPre.getAsInt() != kPost.getAsInt()) {
					continue;
				}

				/* found structure */
				LOGGER.trace(this.getClass().getSimpleName()
						+ " t0 = " + net.getTransitionName(t0)
						+ ", t1 = " + net.getTransitionName(t1)
				);

				net.removeTransition(t0);
				return true;
			}
		}
		return false;
	}
}
