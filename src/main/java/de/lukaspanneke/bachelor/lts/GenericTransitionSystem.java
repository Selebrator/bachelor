package de.lukaspanneke.bachelor.lts;

import uniol.apt.adt.ts.TransitionSystem;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GenericTransitionSystem<S, L> {

	private final S initial;
	private final Map<S, Vertex<S>> vertices = new HashMap<>();
	private final Map<Vertex<S>, Set<Edge<S, L>>> preset = new HashMap<>();
	private final Map<Vertex<S>, Set<Edge<S, L>>> postset = new HashMap<>();

	public GenericTransitionSystem(S initial) {
		this.initial = initial;
	}

	public TransitionSystem toAptLts(Function<S, String> stateToString, Function<L, String> labelToString) {
		TransitionSystem ts = new TransitionSystem();
		for (Vertex<S> vertex : this.getVertices()) {
			ts.createState(stateToString.apply(vertex.state()));
		}
		for (Edge<S, L> edge : this.getEdges()) {
			ts.createArc(
					stateToString.apply(edge.from().state()),
					stateToString.apply(edge.to().state()),
					labelToString.apply(edge.label())
			);
		}
		ts.setInitialState(stateToString.apply(this.initial));
		return ts;
	}

	public Vertex<S> createVertex(S state) {
		return this.vertices.computeIfAbsent(state, Vertex::new);
	}

	public Optional<Vertex<S>> getVertex(S state) {
		return Optional.ofNullable(this.vertices.get(state));
	}

	public Edge<S, L> createEdge(Vertex<S> from, Vertex<S> to, L label) {
		Set<Edge<S, L>> postset = this.postset.computeIfAbsent(from, key -> new HashSet<>());
		Set<Edge<S, L>> preset = this.preset.computeIfAbsent(to, key -> new HashSet<>());

		Edge<S, L> edge = Stream.concat(postset.stream(), preset.stream())
				.filter(e -> e.from().equals(from) && e.to().equals(to) && e.label().equals(label))
				.findAny()
				.orElseGet(() -> new Edge<>(label, from, to));
		postset.add(edge);
		preset.add(edge);
		return edge;
	}

	public Set<Vertex<S>> getVertices() {
		return Set.copyOf(this.vertices.values());
	}

	public Set<Edge<S, L>> getEdges() {
		return Stream.concat(this.preset.values().stream(), this.postset.values().stream())
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());
	}

	public Set<Edge<S, L>> getOutgoingEdges(Vertex<S> vertex) {
		return Collections.unmodifiableSet(this.postset.getOrDefault(vertex, Collections.emptySet()));
	}

	public Set<Edge<S, L>> getIncomingEdges(Vertex<S> vertex) {
		return Collections.unmodifiableSet(this.preset.getOrDefault(vertex, Collections.emptySet()));
	}
}
