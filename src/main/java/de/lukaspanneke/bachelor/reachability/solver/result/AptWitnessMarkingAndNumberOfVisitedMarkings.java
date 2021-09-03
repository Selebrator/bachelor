package de.lukaspanneke.bachelor.reachability.solver.result;

import de.lukaspanneke.bachelor.PrintUtil;
import uniol.apt.adt.pn.Marking;

import java.util.*;

public record AptWitnessMarkingAndNumberOfVisitedMarkings(
		Optional<Marking> witnessMarking,
		long visitedMarkingsCount) {

	@Override
	public String toString() {
		return "visitedMarkingsCount=" + this.visitedMarkingsCount
				+ ", witnessMarking=" + this.witnessMarking.map(PrintUtil::renderMarking);
	}
}
