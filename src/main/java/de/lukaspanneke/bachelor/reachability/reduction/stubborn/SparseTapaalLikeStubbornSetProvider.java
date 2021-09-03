package de.lukaspanneke.bachelor.reachability.reduction.stubborn;

import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseIntVector;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static de.lukaspanneke.bachelor.pn.sparse.mutable.SparseNetUtil.filterKeysIntoSet;

public class SparseTapaalLikeStubbornSetProvider implements StubbornSetProvider<SparsePetriNet, SparseIntVector, String, Integer> {

	private static final Logger LOGGER = LogManager.getLogger(SparseTapaalLikeStubbornSetProvider.class);

	public static final SparseTapaalLikeStubbornSetProvider INSTANCE = new SparseTapaalLikeStubbornSetProvider();

	private SparseTapaalLikeStubbornSetProvider() {
	}

	public Set<Integer> get(SparsePetriNet net, SparseIntVector marking, StateFormula<SparsePetriNet, SparseIntVector, String, Integer> goal) throws InterruptedException {
		Set<Integer> interesting = goal.interestingTransitions(net, marking);
		Set<Integer> unprocessed = new LinkedHashSet<>(interesting);
		Set<Integer> stubborn = new HashSet<>();
		while (!unprocessed.isEmpty()) {
			if (Thread.interrupted()) {
				LOGGER.error("Thread was interrupted. Abort calculation of stubborn set");
				throw new InterruptedException();
			}
			Iterator<Integer> iterator = unprocessed.iterator();
			int t = iterator.next();

			if (net.firable(marking, t)) {
				SparseIntVector t_pre = net.presetT(t);
				for (int i = 0; i < t_pre.size(); i++) {
					int p = t_pre.keyAt(i);
					int w_t_p = net.weightTP(t, p);
					int w_p_t = t_pre.valueAt(i);
					if (w_t_p < w_p_t) { /* decreasingPostset(p).contains(t) */
						unprocessed.addAll(filterKeysIntoSet(net.postsetP_copy_cached(p), (key, value) -> !stubborn.contains(key)));
					}
				}
			} else {
				SparseIntVector t_pre = net.presetT(t);
				for (int i = 0; i < t_pre.size(); i++) {
					int p = t_pre.keyAt(i);
					int m_p = marking.get(p);
					int w_p_t = t_pre.valueAt(i);
					if (m_p < w_p_t) {
						unprocessed.addAll(filterKeysIntoSet(net.presetP_copy_cached(p), (t1, gain) -> !stubborn.contains(t1) && net.weightPT(p, t1) < gain));
						break;
					}
				}
			}
			unprocessed.remove(t);
			stubborn.add(t);
		}
		return stubborn;
	}
}
