package de.lukaspanneke.bachelor.reachability.logic;

import java.util.function.BiPredicate;

public enum ComparisonOperator implements BiPredicate<Long, Long> {
	LESS_THEN("<", true) {
		@Override
		public boolean test(Long x, Long y) {
			return x < y;
		}
	},
	LESS_EQUALS("<=", true) {
		@Override
		public boolean test(Long x, Long y) {
			return x <= y;
		}
	},
	EQUALS("==", false) {
		@Override
		public boolean test(Long x, Long y) {
			return x.equals(y);
		}
	},
	NOT_EQUALS("!=", false) {
		@Override
		public boolean test(Long x, Long y) {
			return !x.equals(y);
		}
	},
	GREATER_EQUALS(">=", true) {
		@Override
		public boolean test(Long x, Long y) {
			return x >= y;
		}
	},
	GREATER_THEN(">", true) {
		@Override
		public boolean test(Long x, Long y) {
			return x > y;
		}
	};

	private final String symbol;
	private final boolean inequality;

	ComparisonOperator(String symbol, boolean inequality) {
		this.symbol = symbol;
		this.inequality = inequality;
	}

	public String symbol() {
		return this.symbol;
	}

	public boolean isInequality() {
		return this.inequality;
	}
}
