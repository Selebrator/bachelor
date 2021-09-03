package de.lukaspanneke.bachelor.reachability.solver.apt;

import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;
import de.lukaspanneke.bachelor.reachability.AugmentedQueryResult;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import de.lukaspanneke.bachelor.reachability.reduction.structural.StructuralReduction;
import de.lukaspanneke.bachelor.reachability.solver.ReachabilitySolver;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;

import java.util.*;
import java.util.stream.Collectors;

@Deprecated
public class AptStructuralReductionPreprocessingSolver<R> implements ReachabilitySolver<PetriNet, Marking, Place, Transition, R> {

	private final List<StructuralReduction> reductions;
	private final ReachabilitySolver<PetriNet, Marking, Place, Transition, R> solver;

	public AptStructuralReductionPreprocessingSolver(List<StructuralReduction> reductions, ReachabilitySolver<PetriNet, Marking, Place, Transition, R> solver) {
		this.reductions = reductions;
		this.solver = solver;
	}

	@Override
	public AugmentedQueryResult<R> isReachable(PetriNet aptNet, StateFormula<PetriNet, Marking, Place, Transition> goal) {
		SparsePetriNet net = new SparsePetriNet(aptNet);
		Set<String> visible = goal.support().stream()
				.map(Place::getId)
				.collect(Collectors.toUnmodifiableSet());

		boolean change = true;
		while (change) {
			change = false;
			for (StructuralReduction reduction : this.reductions) {
				if (Thread.interrupted()) {
					return AugmentedQueryResult.dnf(null);
				}
				if (reduction.reduce(net, visible)) {
					change = true;
					break;
				}
			}
		}
		PetriNet reducedNet = net.toApt();
		goal.test(reducedNet, reducedNet.getInitialMarking()); // result does not matter. this is only to update the goals place instance.
		return this.solver.isReachable(reducedNet, goal);
	}
}
