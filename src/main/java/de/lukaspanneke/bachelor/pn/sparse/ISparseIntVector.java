package de.lukaspanneke.bachelor.pn.sparse;

import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseIntVector;

import java.util.*;

public interface ISparseIntVector {

	int indexOfKey(int key);

	int size();

	int keyAt(int index);

	int valueAt(int index);

	OptionalInt maxKey();

	boolean containsKey(int key);

	default int get(int key) {
		return this.get(key, 0);
	}

	int get(int key, int valueIfNotFound);

	/* return true iff for all i: this[i] >= that[i] */
	boolean greaterEquals(SparseIntVector that);

	/* return true iff for all keys k in that: k is key in this */
	boolean containsKeys(SparseIntVector that);

	/* return k if (this == k * that), or empty otherwise. return 1 if both vectors are empty. */
	OptionalInt scalar(SparseIntVector that);

	default int remove(int key) {
		throw new UnsupportedOperationException();
	}

	default void removeAndDecrementFollowingKeys(int key) {
		throw new UnsupportedOperationException();
	}

	default void clear() {
		throw new UnsupportedOperationException();
	}

	default void removeAt(int index) {
		throw new UnsupportedOperationException();
	}

	default void set(int key, int value) {
		throw new UnsupportedOperationException();
	}

	default void plusAssign(int key, int toAdd) {
		throw new UnsupportedOperationException();
	}

	default void timesAssign(int factor) {
		throw new UnsupportedOperationException();
	}

}
