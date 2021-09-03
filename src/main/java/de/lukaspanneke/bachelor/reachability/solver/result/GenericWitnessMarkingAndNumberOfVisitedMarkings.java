package de.lukaspanneke.bachelor.reachability.solver.result;

import java.util.*;
import java.util.function.Function;

@Deprecated
public record GenericWitnessMarkingAndNumberOfVisitedMarkings<M>(
		Optional<M> witnessMarking,
		long visitedMarkingsCount,
		Function<M, String> witnessPathRenderer) {

	@Override
	public String toString() {
		return "visitedMarkingsCount=" + this.visitedMarkingsCount
				+ ", witnessMarking=" + this.witnessMarking.map(this.witnessPathRenderer);
	}
}
