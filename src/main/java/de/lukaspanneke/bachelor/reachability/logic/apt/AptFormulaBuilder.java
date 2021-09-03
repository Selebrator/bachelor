package de.lukaspanneke.bachelor.reachability.logic.apt;

import de.lukaspanneke.bachelor.reachability.logic.generic.FormulaBuilder;
import de.lukaspanneke.bachelor.reachability.logic.generic.expression.PlaceExpression;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;

public class AptFormulaBuilder implements FormulaBuilder<PetriNet, Marking, Place, Transition> {
	private final PetriNet net;

	public AptFormulaBuilder(PetriNet net) {
		this.net = net;
	}

	@Override
	public PlaceExpression<PetriNet, Marking, Place, Transition> place(String placeName) {
		return AptPlaceExpression.of(this.net.getPlace(placeName));
	}
}
