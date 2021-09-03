package de.lukaspanneke.bachelor.reachability.logic.generic.expression;

import java.util.*;

public interface ArithmeticExpression<N, M, P, T> {

	Set<T> increasingTransitions(N net);

	Set<T> decreasingTransitions(N net);

	Set<P> support();

	long evaluate(N net, M marking);
}
