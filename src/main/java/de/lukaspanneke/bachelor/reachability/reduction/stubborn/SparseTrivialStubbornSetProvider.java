package de.lukaspanneke.bachelor.reachability.reduction.stubborn;

import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseIntVector;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SparseTrivialStubbornSetProvider implements StubbornSetProvider<SparsePetriNet, SparseIntVector, String, Integer> {

	public static final SparseTrivialStubbornSetProvider INSTANCE = new SparseTrivialStubbornSetProvider();

	private SparseTrivialStubbornSetProvider() {
	}

	@Override
	public Set<Integer> get(SparsePetriNet net, SparseIntVector marking, StateFormula<SparsePetriNet, SparseIntVector, String, Integer> goal) {
		return IntStream.range(0, net.getTransitionCount()).boxed().collect(Collectors.toSet());
	}
}
