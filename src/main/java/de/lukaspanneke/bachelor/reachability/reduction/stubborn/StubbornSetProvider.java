package de.lukaspanneke.bachelor.reachability.reduction.stubborn;

import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;

import java.util.*;

public interface StubbornSetProvider<N,M,P,T> {
	Set<T> get(N net, M marking, StateFormula<N, M, P, T> goal) throws InterruptedException;
}
