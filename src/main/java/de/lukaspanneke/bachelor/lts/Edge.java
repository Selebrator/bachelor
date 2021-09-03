package de.lukaspanneke.bachelor.lts;

public record Edge<S, L>(L label, Vertex<S> from, Vertex<S> to) {
}
