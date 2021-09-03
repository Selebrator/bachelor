package de.lukaspanneke.bachelor.reachability.solver;

import de.lukaspanneke.bachelor.PrintUtil;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseIntVector;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseNetUtil;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;
import de.lukaspanneke.bachelor.reachability.AptNetUtil;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;

import java.util.*;

public interface NetAccessor<N, M, P, T> {

	M getInitialMarking(N net);

	Optional<M> fire(N net, M marking, T transition);

	boolean isFirable(N net, M marking, T transition);

	String renderMarking(N net, M marking);

	String renderPath(N net, List<T> transitions);

	static NetAccessor<PetriNet, Marking, Place, Transition> apt() {
		return new NetAccessor<>() {
			@Override
			public Marking getInitialMarking(PetriNet net) {
				return net.getInitialMarking();
			}

			@Override
			public Optional<Marking> fire(PetriNet net, Marking marking, Transition transition) {
				return AptNetUtil.fireable(transition, marking) ? Optional.of(AptNetUtil.fire(marking, transition)) : Optional.empty();
			}

			@Override
			public boolean isFirable(PetriNet net, Marking marking, Transition transition) {
				return AptNetUtil.fireable(transition, marking);
			}

			@Override
			public String renderMarking(PetriNet net, Marking marking) {
				return PrintUtil.renderMarking(marking);
			}

			@Override
			public String renderPath(PetriNet net, List<Transition> transitions) {
				return PrintUtil.renderNodes(transitions);
			}
		};
	}

	static NetAccessor<SparsePetriNet, SparseIntVector, String, Integer> sparse() {
		return new NetAccessor<>() {
			@Override
			public SparseIntVector getInitialMarking(SparsePetriNet net) {
				return net.getInitialMarking();
			}

			@Override
			public Optional<SparseIntVector> fire(SparsePetriNet net, SparseIntVector marking, Integer transition) {
				return net.fire(marking, transition);
			}

			@Override
			public boolean isFirable(SparsePetriNet net, SparseIntVector marking, Integer transition) {
				return net.firable(marking, transition);
			}

			@Override
			public String renderMarking(SparsePetriNet net, SparseIntVector marking) {
				return SparseNetUtil.renderPlaces(marking, net);
			}

			@Override
			public String renderPath(SparsePetriNet net, List<Integer> transitions) {
				return transitions.stream()
						.map(net::getTransitionName)
						.toList()
						.toString();
			}
		};
	}
}
