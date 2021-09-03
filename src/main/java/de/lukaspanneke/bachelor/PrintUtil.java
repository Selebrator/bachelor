package de.lukaspanneke.bachelor;

import de.lukaspanneke.bachelor.lts.Edge;
import de.lukaspanneke.bachelor.reachability.AptNetUtil;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Node;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Token;
import uniol.apt.adt.pn.Transition;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class PrintUtil {

	public static String renderMarking(List<Place> placeOrder, Marking marking) {
		return placeOrder.stream().map(marking::getToken).map(Token::toString).collect(Collectors.joining(",", "(", ")"));
	}

	public static String renderMarking(Marking marking) {
		StringJoiner ret = new StringJoiner(" ");
		for (Place place : marking.getNet().getPlaces()) {
			long tokens = AptNetUtil.tokens(place, marking);
			if (tokens == 1) {
				ret.add(place.getId());
			} else if (tokens != 0){
				ret.add(tokens + place.getId());
			}
		}
		return ret.toString();
	}

	public static String renderPath(List<Edge<Marking, Transition>> path) {
		if (path.size() == 0) {
			return "Ɛ";
		}
		return path.stream()
				.map(Edge::label)
				.map(Transition::getLabel)
				.collect(Collectors.joining(" "));
	}

	public static String renderPathWithMarkings(List<Edge<Marking, Transition>> path, List<Place> placeOrder) {
		if (path.size() == 0) {
			return "Ɛ";
		}
		return renderMarking(placeOrder, path.get(0).from().state()) + path.stream()
				.map(edge -> " [" + edge.label().getLabel() + "> " + renderMarking(placeOrder, edge.to().state()))
				.collect(Collectors.joining());
	}

	public static String renderNodes(Iterable<? extends Node> nodes) {
		return StreamSupport.stream(nodes.spliterator(), false)
				.map(uniol.apt.adt.Node::getId)
				.collect(Collectors.joining(", ", "[", "]"));
	}

	public static <V> String renderVector(Map<V, Integer> vector) {
		return vector.values().stream()
				.map(integer -> Integer.toString(integer))
				.collect(Collectors.joining(",", "(", ")"));
	}

	public static <V> String renderVectors(Iterable<Map<V, Integer>> vectors) {
		return StreamSupport.stream(vectors.spliterator(), false)
				.map(PrintUtil::renderVector)
				.collect(Collectors.joining("\n"));
	}

	public static String renderSet(Set<Place> places) {
		if (places.size() == 0) {
			return "{}";
		}
		List<Place> order = new ArrayList<>(places.iterator().next().getGraph().getPlaces());
		order.retainAll(places);
		return order.stream()
				.map(Place::getId)
				.collect(Collectors.joining(",", "{", "}"));
	}

	public static String formatDuration(long millis) {
		return formatDuration(Duration.ofMillis(millis));
	}

	public static String formatDurationForHuman(Duration duration) {
		//return duration.toString();
		if (duration.toDays() != 0) {
			return String.format("%dd %dh %02dm", duration.toDays(), duration.toHoursPart(), duration.toMinutesPart());
		} else if (duration.toHours() != 0) {
			return String.format("%dh %02dm", duration.toHours(), duration.toMinutesPart());
		} else if (duration.toMinutes() != 0){
			return String.format("%dm %02ds", duration.toMinutes(), duration.toSecondsPart());
		} else if (duration.toSeconds() != 0) {
			return String.format("%d.%03ds", duration.toSeconds(), duration.toMillisPart());
		} else if (duration.toMillis() != 0){
			return String.format("%d.%03dms", duration.toMillis(), (duration.toNanosPart() / 1_000) - (duration.toMillis() * 1_000));
		} else if (duration.toNanos() > 1_000) {
			return (long) (duration.toNanosPart() / 1_000) + "us";
		} else {
			return duration.toNanosPart() + "ns";
		}
	}

	public static String formatDuration(Duration duration) {
		return formatDurationForHuman(duration) + " <" + duration.toString() + ">";
	}

	public static String formatDurationAsNanos(Duration duration) {
		return String.valueOf(duration.toNanos());
	}
}
