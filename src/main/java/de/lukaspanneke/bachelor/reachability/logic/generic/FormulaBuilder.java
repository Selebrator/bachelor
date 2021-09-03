package de.lukaspanneke.bachelor.reachability.logic.generic;

import de.lukaspanneke.bachelor.reachability.logic.BinaryLogicOperator;
import de.lukaspanneke.bachelor.reachability.logic.CalculationOperator;
import de.lukaspanneke.bachelor.reachability.logic.ComparisonOperator;
import de.lukaspanneke.bachelor.reachability.logic.generic.expression.ArithmeticExpression;
import de.lukaspanneke.bachelor.reachability.logic.generic.expression.CalculationExpression;
import de.lukaspanneke.bachelor.reachability.logic.generic.expression.ConstantExpression;
import de.lukaspanneke.bachelor.reachability.logic.generic.expression.PlaceExpression;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.ComparisonFormula;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.CompositionFormula;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.NegatedFormula;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;

public interface FormulaBuilder<N, M, P, T> {
	PlaceExpression<N, M, P, T> place(String placeName);

	default ConstantExpression<N, M, P, T> constant(long constant) {
		return ConstantExpression.of(constant);
	}

	default CalculationExpression<N, M, P, T> calculate(ArithmeticExpression<N, M, P, T> e1, CalculationOperator operator, ArithmeticExpression<N, M, P, T> e2) {
		return CalculationExpression.of(e1, operator, e2);
	}

	default ComparisonFormula<N, M, P, T> compare(ArithmeticExpression<N, M, P, T> e1, ComparisonOperator operator, ArithmeticExpression<N, M, P, T> e2) {
		return ComparisonFormula.of(e1, operator, e2);
	}

	default CompositionFormula<N, M, P, T> compose(StateFormula<N, M, P, T> f1, BinaryLogicOperator operator, StateFormula<N, M, P, T> f2) {
		return CompositionFormula.of(f1, operator, f2);
	}

	default StateFormula<N, M, P, T> negate(StateFormula<N, M, P, T> f) {
		return NegatedFormula.of(f);
	}
}
