package de.lukaspanneke.bachelor.reachability.solver.result;

import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseIntVector;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseNetUtil;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;

import java.util.*;

public record SparseWitnessMarkingAndNumberOfVisitedMarkings(
		Optional<SparseIntVector> witnessMarking,
		long visitedMarkingsCount,
		SparsePetriNet net) {

	@Override
	public String toString() {
		return "visitedMarkingsCount=" + this.visitedMarkingsCount
				+ ", witnessMarking=" + this.witnessMarking.map(places -> SparseNetUtil.renderPlaces(places, this.net));
	}
}
