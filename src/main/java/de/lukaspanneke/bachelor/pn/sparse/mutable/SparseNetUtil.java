package de.lukaspanneke.bachelor.pn.sparse.mutable;

import de.lukaspanneke.bachelor.help.BiIntPredicate;

import java.util.*;
import java.util.function.IntFunction;

public class SparseNetUtil {

	private static final char LEFT = '[';
	private static final char RIGHT = ']';
	private static final String EMPTY = "[]";


	public static String renderPlaces(SparseIntVector places, SparsePetriNet net) {
		return renderNodes(places, net::getPlaceName);
	}

	public static String renderTransitions(SparseIntVector transitions, SparsePetriNet net) {
		return renderNodes(transitions, net::getTransitionName);
	}

	private static String renderNodes(SparseIntVector nodes, IntFunction<String> name) {
		if (nodes == null) {
			return null;
		}
		int size = nodes.size();
		if (size <= 0) {
			return EMPTY;
		}
		StringBuilder buffer = new StringBuilder(size * 32);
		buffer.append(LEFT);
		for (int i = 0; i < size; i++) {
			if (i > 0) {
				buffer.append(',').append(' ');
			}
			int multi = nodes.valueAt(i);
			if (multi != 1) {
				buffer.append(multi).append(' ');
			}
			buffer.append(name.apply(nodes.keyAt(i)));
		}
		buffer.append(RIGHT);
		return buffer.toString();
	}

	public static int[] filterKeys(SparseIntVector vector, BiIntPredicate filter) {
		int[] keep = new int[vector.size()];
		int size = 0;
		for (int i = 0; i < vector.size(); i++) {
			int key = vector.keyAt(i);
			if (filter.test(key, vector.valueAt(i))) {
				keep[size++] = key;
			}
		}
		return Arrays.copyOfRange(keep, 0, size);
	}

	public static Set<Integer> filterKeysIntoSet(SparseIntVector vector, BiIntPredicate filter) {
		Set<Integer> keep = new HashSet<>();
		for (int i = 0; i < vector.size(); i++) {
			int key = vector.keyAt(i);
			if (filter.test(key, vector.valueAt(i))) {
				keep.add(key);
			}
		}
		return keep;
	}

	public static Set<Integer> increasingPreset_copy_cached(SparsePetriNet net, int placeId) {
		return filterKeysIntoSet(net.presetP_copy(placeId), (t, gain) -> gain > net.weightPT(placeId, t));
	}

	public static Set<Integer> decreasingPostset_copy_cached(SparsePetriNet net, int placeId) {
		return filterKeysIntoSet(net.postsetP_copy(placeId), (t, loss) -> loss > net.weightTP(t, placeId));
	}

	public static int[] inflate(SparseIntVector sparse, int length) {
		int[] inflated = new int[length];
		for (int i = 0; i < sparse.size(); i++) {
			inflated[sparse.keyAt(i)] = sparse.valueAt(i);
		}
		return inflated;
	}
}
