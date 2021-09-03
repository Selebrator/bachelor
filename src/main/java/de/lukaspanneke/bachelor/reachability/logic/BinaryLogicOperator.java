package de.lukaspanneke.bachelor.reachability.logic;

import java.util.function.BiFunction;
import java.util.stream.Stream;

public enum BinaryLogicOperator implements BiFunction<Boolean, Boolean, Boolean> {

	AND("∧") {
		@Override
		public Boolean apply(Boolean left, Boolean right) {
			return left && right;
		}

		@Override
		public boolean apply(Stream<Boolean> bools) {
			return bools.allMatch(bool -> bool);
		}
	},
	OR("∨") {
		@Override
		public Boolean apply(Boolean left, Boolean right) {
			return left || right;
		}

		@Override
		public boolean apply(Stream<Boolean> bools) {
			return bools.anyMatch(bool -> bool);
		}
	};

	private final String symbol;

	BinaryLogicOperator(String symbol) {
		this.symbol = symbol;
	}

	public String symbol() {
		return this.symbol;
	}

	public abstract boolean apply(Stream<Boolean> bools);

}
