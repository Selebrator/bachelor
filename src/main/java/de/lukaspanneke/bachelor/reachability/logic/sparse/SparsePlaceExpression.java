package de.lukaspanneke.bachelor.reachability.logic.sparse;

import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseIntVector;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;
import de.lukaspanneke.bachelor.reachability.logic.generic.expression.PlaceExpression;

import java.util.*;

import static de.lukaspanneke.bachelor.pn.sparse.mutable.SparseNetUtil.filterKeysIntoSet;

public class SparsePlaceExpression implements PlaceExpression<SparsePetriNet, SparseIntVector, String, Integer> {
	private final String placeName;
	private int lastPlaceId = 0;

	private SparsePlaceExpression(String placeName) {
		this.placeName = placeName;
	}

	public static SparsePlaceExpression of(String placeName) {
		return new SparsePlaceExpression(placeName);
	}

	private int placeId(SparsePetriNet net) {
		if (!net.getPlaceName(this.lastPlaceId).equals(this.placeName)) {
			this.lastPlaceId = net.getPlaceId(this.placeName);
			if (this.lastPlaceId == -1) {
				throw new NoSuchElementException("net does not contain place " + this.placeName);
			}
		}
		return this.lastPlaceId;
	}

	@Override
	public String place() {
		return this.placeName;
	}

	@Override
	public Set<Integer> increasingTransitions(SparsePetriNet net) {
		int p = this.placeId(net);
		return filterKeysIntoSet(net.presetP_copy_cached(p), (t, gain) -> gain > net.weightPT(p, t));
	}

	@Override
	public Set<Integer> decreasingTransitions(SparsePetriNet net) {
		int p = this.placeId(net);
		return filterKeysIntoSet(net.postsetP_copy_cached(p), (t, loss) -> loss > net.weightTP(t, p));
	}

	@Override
	public Set<String> support() {
		return Set.of(this.placeName);
	}

	@Override
	public long evaluate(SparsePetriNet net, SparseIntVector marking) {
		return marking.get(this.placeId(net));
	}

	@Override
	public String toString() {
		return this.placeName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SparsePlaceExpression that = (SparsePlaceExpression) o;
		return placeName.equals(that.placeName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(placeName);
	}
}
