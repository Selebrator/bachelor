package de.lukaspanneke.bachelor.reachability.logic.generic.formula;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import de.lukaspanneke.bachelor.reachability.logic.BinaryLogicOperator;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompositionFormula<N, M, P, T> extends StateFormula<N, M, P, T> {

	/* invariant: formulas.size() >= 2 */
	private final List<StateFormula<N, M, P, T>> formulas;
	private final BinaryLogicOperator operator;

	protected CompositionFormula(StateFormula<N, M, P, T> f1, BinaryLogicOperator operator, StateFormula<N, M, P, T> f2) {
		this.formulas = List.of(f1, f2);
		this.operator = operator;
	}

	private CompositionFormula(BinaryLogicOperator operator, Collection<? extends StateFormula<N, M, P, T>> formulas) {
		Preconditions.checkArgument(formulas.size() >= 2, operator.name() + " has to be applied to at least 2 arguments");
		this.formulas = List.copyOf(formulas);
		this.operator = operator;
	}

	public static <N, M, P, T> CompositionFormula<N, M, P, T> of(StateFormula<N, M, P, T> f1, BinaryLogicOperator operator, StateFormula<N, M, P, T> f2) {
		assert f1 != null && f2 != null;
		if (f1 instanceof CompositionFormula<N, M, P, T> c1 && f2 instanceof CompositionFormula<N, M, P, T> c2) {
			if (c1.operator == operator && c2.operator == operator) {
				List<StateFormula<N, M, P, T>> compose = Stream.concat(c1.formulas.stream(), c2.formulas.stream()).toList();
				return new CompositionFormula<>(operator, compose);
			} else if (c1.operator == operator || c2.operator == operator) {
				CompositionFormula<N, M, P, T> same;
				StateFormula<N, M, P, T> different;
				boolean sameFirst;
				if (c1.operator == operator) {
					same = c1;
					different = c2;
					sameFirst = true;
				} else {
					different = c1;
					same = c2;
					sameFirst = false;
				}
				List<StateFormula<N, M, P, T>> compose = (sameFirst
						? Stream.concat(same.formulas.stream(), Stream.of(different))
						: Stream.concat(Stream.of(different), same.formulas.stream()))
						.toList();
				return new CompositionFormula<>(operator, compose);
			} else {
				return new CompositionFormula<>(f1, operator, f2);
			}
		} else if (f1 instanceof CompositionFormula || f2 instanceof CompositionFormula) {
			CompositionFormula<N, M, P, T> c;
			StateFormula<N, M, P, T> f;
			boolean compositionFirst;
			if (f1 instanceof CompositionFormula) {
				c = (CompositionFormula<N, M, P, T>) f1;
				f = f2;
				compositionFirst = true;
			} else {
				f = f1;
				c = (CompositionFormula<N, M, P, T>) f2;
				compositionFirst = false;
			}
			if (c.operator == operator) {
				List<StateFormula<N, M, P, T>> compose = (compositionFirst
						? Stream.concat(c.formulas.stream(), Stream.of(f))
						: Stream.concat(Stream.of(f), c.formulas.stream()))
						.toList();
				return new CompositionFormula<>(operator, compose);
			} else {
				return new CompositionFormula<>(f1, operator, f2);
			}
		} else {
			return new CompositionFormula<>(f1, operator, f2);
		}
	}

	public List<StateFormula<N, M, P, T>> formulas() {
		return this.formulas;
	}

	public BinaryLogicOperator operator() {
		return this.operator;
	}

	@Override
	public Set<T> interestingTransitions(N net, M marking) {
		if (this.test(net, marking)) {
			return Collections.emptySet();
		}
		// TODO definition is nondeterministic, implementation not
		return switch (this.operator) {
			case AND -> InterestingTransitions.and(net, marking, this.formulas.stream());
			case OR -> InterestingTransitions.or(net, marking, this.formulas.stream());
		};
	}

	@Override
	protected Set<T> negatedInterestingTransitions(N net, M marking) {
		return switch (this.operator) {
			case AND -> InterestingTransitions.or(net, marking, this.formulas.stream().map(NegatedFormula::of));
			case OR -> InterestingTransitions.and(net, marking, this.formulas.stream().map(NegatedFormula::of));
		};
	}

	@Override
	public Set<P> support() {
		return this.formulas.stream()
				.map(StateFormula::support)
				.reduce(Sets::union)
				.orElseGet(Collections::emptySet);
	}

	@Override
	public boolean test(N net, M marking) {
		return this.operator.apply(this.formulas.stream().map(formula -> formula.test(net, marking)));
	}

	@Override
	public String toString() {
		return this.formulas.stream()
				.map(StateFormula::toString)
				.collect(Collectors.joining(" " + this.operator.symbol() + " ", "(", ")"));
	}
}
