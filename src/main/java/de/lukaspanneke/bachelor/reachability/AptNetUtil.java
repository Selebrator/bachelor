package de.lukaspanneke.bachelor.reachability;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import uniol.apt.adt.extension.Extensible;
import uniol.apt.adt.pn.Flow;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Node;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Token;
import uniol.apt.adt.pn.Transition;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AptNetUtil {

	public static Set<Place> preset(Transition transition) {
		return cached(transition, "caching_preset", transition::getPreset);
	}

	public static Set<Transition> preset(Place place) {
		return cached(place, "caching_preset", place::getPreset);
	}

	public static Set<Transition> increasingPreset(Place place) {
		return cached(place, "caching_increasingPreset", () -> preset(place).stream()
				.filter(transition -> weight(transition, place) > weight(place, transition))
				.collect(Collectors.toUnmodifiableSet()));
	}

	public static Set<Place> postset(Transition transition) {
		return cached(transition, "caching_postset", transition::getPostset);
	}

	public static Set<Transition> postset(Place place) {
		return cached(place, "caching_postset", place::getPostset);
	}

	public static Set<Transition> decreasingPostset(Place place) {
		return cached(place, "caching_decreasingPostset", () -> postset(place).stream()
				.filter(transition -> weight(transition, place) < weight(place, transition))
				.collect(Collectors.toUnmodifiableSet()));
	}

	public static Table<Place, Transition, Integer> forwardMatrix(PetriNet net) {
		return cached(net, "caching_forwardMatrix", () -> net.getEdges().stream()
				.filter(flow -> flow.getSource() instanceof Place)
				.collect(ImmutableTable.toImmutableTable(
						Flow::getPlace,
						Flow::getTransition,
						Flow::getWeight,
						(integer, integer2) -> {throw new IllegalArgumentException();}
				)));
	}

	public static Table<Transition, Place, Integer> backwardMatrix(PetriNet net) {
		return cached(net, "caching_backwardMatrix", () -> net.getEdges().stream()
				.filter(flow -> flow.getSource() instanceof Transition)
				.collect(ImmutableTable.toImmutableTable(
						Flow::getTransition,
						Flow::getPlace,
						Flow::getWeight,
						(integer, integer2) -> {throw new IllegalArgumentException();}
				)));
	}

	public static int weight(Place from, Transition to) {
		PetriNet net = from.getGraph();
		assert net == to.getGraph(); // actually the same instance!

		Integer weight = forwardMatrix(net).get(from, to);
		return weight == null ? 0 : weight;
	}

	public static int weight(Transition from, Place to) {
		PetriNet net = from.getGraph();
		assert net == to.getGraph(); // actually the same instance!

		Integer weight = backwardMatrix(net).get(from, to);
		return weight == null ? 0 : weight;
	}

	private static int weight_slow(Node from, Node to) {
		assert from.getGraph().equals(to.getGraph());
		return from.getPostsetEdges().stream()
				.filter(flow -> flow.getTarget().equals(to))
				.map(Flow::getWeight)
				.findAny()
				.orElse(0);
	}

	public static long tokens(Place place, Marking marking) {
		Token token = marking.getToken(place);
		if (token.isOmega()) {
			throw new IllegalStateException();
		}
		if (token.getValue() < 0) {
			throw new IllegalStateException();
		}
		return token.getValue();
	}

	public static boolean fireable(Transition transition, Marking marking) {
		//return transition.isFireable(marking);
		for (Place place : preset(transition)) {
			if (tokens(place, marking) < weight(place, transition)) {
				return false;
			}
		}
		return true;
	}

	public static Marking fire(Marking from, Transition transition) {
		return transition.fire(from);
	}

	private static <T> T cached(Extensible extensible, String name, Supplier<T> ifAbsent) {
		T ret;
		if (!extensible.hasExtension(name)) {
			extensible.putExtension(name, ret = ifAbsent.get());
		} else {
			ret = (T) extensible.getExtension(name);
		}
		return ret;
	}
}
