package de.lukaspanneke.bachelor.reachability.logic.apt;

import de.lukaspanneke.bachelor.reachability.logic.generic.expression.PlaceExpression;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;

import java.util.*;

import static de.lukaspanneke.bachelor.reachability.AptNetUtil.decreasingPostset;
import static de.lukaspanneke.bachelor.reachability.AptNetUtil.increasingPreset;
import static de.lukaspanneke.bachelor.reachability.AptNetUtil.tokens;

public class AptPlaceExpression implements PlaceExpression<PetriNet, Marking, Place, Transition> {
	private final String placeName;
	private Place lastPlace;

	private AptPlaceExpression(Place place) {
		this.placeName = place.getId();
		this.lastPlace = place;
	}

	public static AptPlaceExpression of(Place place) {
		return new AptPlaceExpression(place);
	}

	@Override
	public Place place() {
		return this.lastPlace;
	}

	@Override
	public Set<Transition> increasingTransitions(PetriNet net) {
		this.updatePlace(net);
		return increasingPreset(this.lastPlace);
	}

	@Override
	public Set<Transition> decreasingTransitions(PetriNet net) {
		this.updatePlace(net);
		return decreasingPostset(this.lastPlace);
	}

	@Override
	public Set<Place> support() {
		return Collections.singleton(this.lastPlace);
	}

	@Override
	public long evaluate(PetriNet net, Marking marking) {
		this.updatePlace(net);
		return tokens(this.lastPlace, marking);
	}

	private void updatePlace(PetriNet net) {
		if (this.lastPlace.getGraph() != net) {
			this.lastPlace = net.getPlace(this.placeName);
		}
	}

	@Override
	public String toString() {
		return this.placeName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AptPlaceExpression that = (AptPlaceExpression) o;
		return Objects.equals(this.placeName, that.placeName) && Objects.equals(this.lastPlace, that.lastPlace);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.placeName, this.lastPlace);
	}
}
