package de.lukaspanneke.bachelor.reachability.reduction.stubborn;

import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;

import java.util.*;

@Deprecated
public class TrivialStubbornSetProvider implements StubbornSetProvider<PetriNet, Marking, Place, Transition> {
	@Override
	public Set<Transition> get(PetriNet net, Marking marking, StateFormula<PetriNet, Marking, Place, Transition> goal) {
		return net.getTransitions();
	}
}
