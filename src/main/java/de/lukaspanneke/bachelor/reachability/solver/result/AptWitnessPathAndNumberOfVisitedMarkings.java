package de.lukaspanneke.bachelor.reachability.solver.result;

import de.lukaspanneke.bachelor.PrintUtil;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Transition;

import java.util.*;

public record AptWitnessPathAndNumberOfVisitedMarkings(
		Optional<List<Transition>> witnessPath,
		Optional<Marking> witnessMarking,
		long visitedMarkingsCount) {

	@Override
	public String toString() {
		return "visitedMarkingCount=" + visitedMarkingsCount
				+ ", witnessPath=" + this.witnessPath.map(PrintUtil::renderNodes)
				+ ", witnessMarking=" + this.witnessMarking.map(PrintUtil::renderMarking);
	}
}
