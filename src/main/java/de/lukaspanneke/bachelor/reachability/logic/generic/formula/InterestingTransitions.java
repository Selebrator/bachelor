package de.lukaspanneke.bachelor.reachability.logic.generic.formula;

import com.google.common.collect.Sets;
import de.lukaspanneke.bachelor.reachability.logic.generic.expression.ArithmeticExpression;

import java.util.*;
import java.util.stream.Stream;

public class InterestingTransitions<N, M, P, T> {

	public static <N, M, P, T> Set<T> less(N net, M marking, ArithmeticExpression<N, M, P, T> e1, ArithmeticExpression<N, M, P, T> e2) {
		return Sets.union(e1.decreasingTransitions(net), e2.increasingTransitions(net));
	}

	public static <N, M, P, T> Set<T> greater(N net, M marking, ArithmeticExpression<N, M, P, T> e1, ArithmeticExpression<N, M, P, T> e2) {
		return Sets.union(e1.increasingTransitions(net), e2.decreasingTransitions(net));
	}

	public static <N, M, P, T> Set<T> notEqual(N net, M marking, ArithmeticExpression<N, M, P, T> e1, ArithmeticExpression<N, M, P, T> e2) {
		return Sets.union(
				Sets.union(e1.increasingTransitions(net), e1.decreasingTransitions(net)),
				Sets.union(e2.increasingTransitions(net), e2.decreasingTransitions(net))
		);
	}

	public static <N, M, P, T> Set<T> equal(N net, M marking, ArithmeticExpression<N, M, P, T> e1, ArithmeticExpression<N, M, P, T> e2) {
		long eval1 = e1.evaluate(net, marking);
		long eval2 = e2.evaluate(net, marking);
		if (eval1 > eval2) {
			return Sets.union(e1.decreasingTransitions(net), e2.increasingTransitions(net));
		}
		if (eval1 < eval2) {
			return Sets.union(e1.increasingTransitions(net), e2.decreasingTransitions(net));
		}
		// eval1 == eval 2. The reference implementation uses the empty set.
		return Collections.emptySet();
	}

	public static <N, M, P, T> Set<T> and(N net, M marking, Stream<? extends StateFormula<N, M, P, T>> formulas) {
		return formulas
				.filter(formula -> !formula.test(net, marking))
				.map(formula -> formula.interestingTransitions(net, marking))
				.findAny()
				.orElseThrow();
		// No case from the paper matches. The reference implementation uses the empty set.
		//.orElseGet(Collections::emptySet);
	}

	public static <N, M, P, T> Set<T> or(N net, M marking, Stream<? extends StateFormula<N, M, P, T>> formulas) {
		return formulas
				.map(formula -> formula.interestingTransitions(net, marking))
				.reduce(Collections.emptySet(), Sets::union);
	}
}
