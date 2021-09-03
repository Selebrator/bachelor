package de.lukaspanneke.bachelor.reachability.logic.apt;

import de.lukaspanneke.bachelor.reachability.logic.CalculationOperator;
import de.lukaspanneke.bachelor.reachability.logic.ComparisonOperator;
import de.lukaspanneke.bachelor.reachability.logic.generic.expression.ArithmeticExpression;
import de.lukaspanneke.bachelor.reachability.logic.generic.expression.CalculationExpression;
import de.lukaspanneke.bachelor.reachability.logic.generic.expression.ConstantExpression;
import de.lukaspanneke.bachelor.reachability.logic.generic.expression.PlaceExpression;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.ComparisonFormula;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record LinearCombination<N, M, P, T>(long constant,
                                            Map<PlaceExpression<N, M, P, T>, Long> factors) {

	public static <N, M, P, T> LinearCombination<N, M, P, T> fromArithmeticExpression(ArithmeticExpression<N, M, P, T> e) {
		if (e instanceof ConstantExpression<N, M, P, T> c) {
			return new LinearCombination<>(c.constant(), Map.of());
		} else if (e instanceof PlaceExpression<N, M, P, T> p) {
			return new LinearCombination<>(0, Map.of(p, 1L));
		} else if (e instanceof CalculationExpression<N, M, P, T> calc) {
			ArithmeticExpression<N, M, P, T> left = calc.left();
			ArithmeticExpression<N, M, P, T> right = calc.right();

			if (left instanceof ConstantExpression<N, M, P, T> c1 && right instanceof ConstantExpression<N, M, P, T> c2) {
				return new LinearCombination<>(calc.operator().apply(c1.constant(), c2.constant()), Map.of());
			} else if (left instanceof ConstantExpression && right instanceof PlaceExpression
					|| left instanceof PlaceExpression && right instanceof ConstantExpression) {
				boolean constantLeft = left instanceof ConstantExpression;
				long c = ((ConstantExpression<N, M, P, T>) (constantLeft ? left : right)).constant();
				PlaceExpression<N, M, P, T> p = (PlaceExpression<N, M, P, T>) (constantLeft ? right : left);
				switch (calc.operator()) {
					case PLUS -> {
						return new LinearCombination<>(c, Map.of(p, 1L));
					}
					case MINUS -> {
						if (constantLeft) {
							return new LinearCombination<>(c, Map.of(p, -1L));
						} else {
							return new LinearCombination<>(-c, Map.of(p, 1L));
						}
					}
					case TIMES -> {
						return new LinearCombination<>(0, Map.of(p, c));
					}
					default -> throw new UnsupportedOperationException();
				}
			} else if (left instanceof PlaceExpression<N, M, P, T> p1 && right instanceof PlaceExpression<N, M, P, T> p2) {
				switch (calc.operator()) {
					case PLUS -> {
						return new LinearCombination<>(0L, Map.of(p1, 1L, p2, 1L));
					}
					case MINUS -> {
						return new LinearCombination<>(0L, Map.of(p1, 1L, p2, -1L));
					}
					case TIMES -> throw new IllegalArgumentException("Linear combinations don't allow variable * variable");
					default -> throw new UnsupportedOperationException();
				}
			} else if (left instanceof ConstantExpression && right instanceof CalculationExpression
					|| left instanceof CalculationExpression && right instanceof ConstantExpression) {
				boolean constantLeft = left instanceof ConstantExpression;
				long c = ((ConstantExpression<N, M, P, T>) (constantLeft ? left : right)).constant();
				LinearCombination<N, M, P, T> lin = fromArithmeticExpression(constantLeft ? right : left);
				return lin.applyWithConstant(calc.operator(), c, constantLeft);
			} else if (left instanceof PlaceExpression && right instanceof CalculationExpression
					|| left instanceof CalculationExpression && right instanceof PlaceExpression) {
				boolean variableLeft = left instanceof PlaceExpression;
				PlaceExpression<N, M, P, T> p = (PlaceExpression<N, M, P, T>) (variableLeft ? left : right);
				LinearCombination<N, M, P, T> lin = fromArithmeticExpression(variableLeft ? right : left);
				return lin.applyWithVariable(calc.operator(), p, variableLeft);
			} else if (left instanceof CalculationExpression && right instanceof CalculationExpression) {
				LinearCombination<N, M, P, T> ll = fromArithmeticExpression(left);
				LinearCombination<N, M, P, T> lr = fromArithmeticExpression(right);

				boolean constantLeft = ll.isConstant();
				if (constantLeft || lr.isConstant()) {
					long c = constantLeft ? ll.constant() : lr.constant();
					LinearCombination<N, M, P, T> lin = constantLeft ? lr : ll;
					return lin.applyWithConstant(calc.operator(), c, constantLeft);
				}

				switch (calc.operator()) {
					case PLUS -> {
						return ll.plusLinear(lr);
					}
					case MINUS -> {
						return ll.minusLinear(lr);
					}
					case TIMES -> throw new IllegalArgumentException("Linear combinations don't allow variable * variable");
					default -> throw new UnsupportedOperationException();
				}
			} else {
				throw new UnsupportedOperationException("Unknown types for " + CalculationExpression.class.getSimpleName()
						+ ": " + left.getClass().getSimpleName() + calc.operator().symbol() + right.getClass().getSimpleName());
			}
		} else {
			throw new UnsupportedOperationException("Unknown type for " + ArithmeticExpression.class.getSimpleName()
					+ ": " + e.getClass().getSimpleName());
		}
	}

	public LinearCombination<N, M, P, T> plusConstant(long constant) {
		return new LinearCombination<>(this.constant + constant, this.factors);
	}

	public LinearCombination<N, M, P, T> minusConstant(long constant) {
		return new LinearCombination<>(this.constant - constant, this.factors);
	}

	public LinearCombination<N, M, P, T> timesConstant(long constant) {
		return new LinearCombination<>(this.constant * constant, this.factors.entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						entry -> entry.getValue() * constant
				)));
	}

	public LinearCombination<N, M, P, T> applyWithConstant(CalculationOperator operator, long constant, boolean constantLeft) {
		switch (operator) {
			case PLUS -> {
				return this.plusConstant(constant);
			}
			case MINUS -> {
				if (constantLeft) {
					return this.negate().plusConstant(constant);
				} else {
					return this.minusConstant(constant);
				}
			}
			case TIMES -> {
				return this.timesConstant(constant);
			}
			default -> throw new UnsupportedOperationException();
		}
	}

	public LinearCombination<N, M, P, T> plusVariable(PlaceExpression<N, M, P, T> variable) {
		Map<PlaceExpression<N, M, P, T>, Long> factors = new HashMap<>(this.factors);
		factors.compute(variable, (p, c) -> c == null ? 1 : c + 1);
		return new LinearCombination<>(this.constant, Collections.unmodifiableMap(factors));
	}

	public LinearCombination<N, M, P, T> minusVariable(PlaceExpression<N, M, P, T> variable) {
		Map<PlaceExpression<N, M, P, T>, Long> factors = new HashMap<>(this.factors);
		factors.compute(variable, (p, c) -> c == null ? -1 : c - 1);
		return new LinearCombination<>(this.constant, Collections.unmodifiableMap(factors));
	}

	public LinearCombination<N, M, P, T> applyWithVariable(CalculationOperator operator, PlaceExpression<N, M, P, T> variable, boolean variableLeft) {
		switch (operator) {
			case PLUS -> {
				return this.plusVariable(variable);
			}
			case MINUS -> {
				if (variableLeft) {
					return this.negate().plusVariable(variable);
				} else {
					return this.minusVariable(variable);
				}
			}
			case TIMES -> {
				if (this.isConstant()) {
					return new LinearCombination<>(0L, Map.of(variable, this.constant()));
				} else {
					throw new IllegalArgumentException("Linear combinations don't allow variable * variable");
				}
			}
			default -> throw new UnsupportedOperationException();
		}
	}

	public LinearCombination<N, M, P, T> plusLinear(LinearCombination<N, M, P, T> that) {
		long constant = this.constant() + that.constant();

		Map<PlaceExpression<N, M, P, T>, Long> factors = Stream.concat(
				this.factors().entrySet().stream(),
				that.factors().entrySet().stream()
		).collect(Collectors.toMap(
				Map.Entry::getKey,
				Map.Entry::getValue,
				Long::sum
		));

		return new LinearCombination<>(constant, factors);
	}

	public LinearCombination<N, M, P, T> minusLinear(LinearCombination<N, M, P, T> that) {
		return this.plusLinear(that.negate());
	}

	public boolean isConstant() {
		return this.factors.isEmpty() || this.factors.values().stream().allMatch(l -> l == null || l == 0);
	}

	public LinearCombination<N, M, P, T> negate() {
		return new LinearCombination<>(-this.constant, this.factors.entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						entry -> -entry.getValue()
				)));
	}

	public String format() {
		StringJoiner sb = new StringJoiner(" ");
		for (var entry : this.factors.entrySet()) {
			String p = entry.getKey().toString();
			long f = entry.getValue();
			if (f != 0L) {
				int signum = Long.signum(f);
				String sign;
				if (sb.length() == 0) {
					if (signum == 1) {
						sign = "";
					} else {
						sign = "-";
					}
				} else {
					if (signum == 1) {
						sign = "+ ";
					} else {
						sign = "- ";
					}
				}

				long unsignedF = Math.abs(f);
				String unsignedFactor;
				if (unsignedF == 1L) {
					unsignedFactor = "";
				} else {
					unsignedFactor = String.valueOf(unsignedF);
				}
				sb.add(sign + unsignedFactor + p);
			}
		}
		if (this.constant == 0 && sb.length() == 0) {
			return "0";
		}
		if (this.constant != 0) {
			if (sb.length() == 0) {
				return String.valueOf(this.constant);
			} else {
				if (Long.signum(this.constant) == 1) {
					sb.add("+ " + this.constant);
				} else {
					sb.add("- " + Math.abs(this.constant));
				}
			}
		}
		return sb.toString();
	}

	public static <N, M, P, T> LinearCombinationConstraint<N, M, P, T> asLinearCombinationConstraint(ComparisonFormula<N, M, P, T> formula) {
		LinearCombination<N, M, P, T> left = LinearCombination.fromArithmeticExpression(formula.left());
		LinearCombination<N, M, P, T> right = LinearCombination.fromArithmeticExpression(formula.right());
		LinearCombination<N, M, P, T> allOnLeft = left.minusLinear(right);
		return new LinearCombinationConstraint<>(allOnLeft, formula.operator());
	}

	public static record LinearCombinationConstraint<N, M, P, T>(LinearCombination<N, M, P, T> linearCombination,
	                                                             ComparisonOperator operator) {
		public String format() {
			return this.linearCombination.minusConstant(this.linearCombination.constant()).format()
					+ " " + this.operator.symbol() + " "
					+ -this.linearCombination.constant();
		}

		public LinearCombinationConstraint<N, M, P, T> timesNegativeOne() {
			ComparisonOperator operator = switch (this.operator) {
				case LESS_THEN -> ComparisonOperator.GREATER_THEN;
				case LESS_EQUALS -> ComparisonOperator.GREATER_EQUALS;
				case EQUALS -> ComparisonOperator.EQUALS;
				case NOT_EQUALS -> ComparisonOperator.NOT_EQUALS;
				case GREATER_EQUALS -> ComparisonOperator.LESS_EQUALS;
				case GREATER_THEN -> ComparisonOperator.LESS_THEN;
			};
			return new LinearCombinationConstraint<>(this.linearCombination.timesConstant(-1), operator);
		}

		@Override
		public String toString() {
			return this.format();
		}
	}
}