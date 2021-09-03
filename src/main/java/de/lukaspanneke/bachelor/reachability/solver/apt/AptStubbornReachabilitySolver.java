package de.lukaspanneke.bachelor.reachability.solver.apt;

import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import de.lukaspanneke.bachelor.reachability.reduction.stubborn.StubbornSetProvider;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;

import java.util.*;

@Deprecated
public class AptStubbornReachabilitySolver extends AptDynamicBreadthFirstReachabilityGraphSolver {
	private final StubbornSetProvider<PetriNet, Marking, Place, Transition> stubbornSetProvider;

	public AptStubbornReachabilitySolver(StubbornSetProvider<PetriNet, Marking, Place, Transition> stubbornSetProvider) {
		this.stubbornSetProvider = stubbornSetProvider;
	}

	@Override
	protected Set<Transition> consideredOutgoingTransitions(Marking marking, StateFormula<PetriNet, Marking, Place, Transition> formula) throws InterruptedException {
		return this.stubbornSetProvider.get(marking.getNet(), marking, formula);
	}

	@Override
	public String toString() {
		return super.toString() + " with " + this.stubbornSetProvider.toString();
	}
}
