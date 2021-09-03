package de.lukaspanneke.bachelor.reachability.logic;

import java.util.function.BiFunction;

public enum CalculationOperator implements BiFunction<Long, Long, Long> {
	PLUS("+") {
		@Override
		public Long apply(Long left, Long right) {
			return left + right;
		}
	},
	MINUS("-") {
		@Override
		public Long apply(Long left, Long right) {
			return left - right;
		}
	},
	TIMES("*") {
		@Override
		public Long apply(Long left, Long right) {
			return left * right;
		}
	};

	private final String symbol;

	CalculationOperator(String symbol) {
		this.symbol = symbol;
	}

	public String symbol() {
		return this.symbol;
	}
}
