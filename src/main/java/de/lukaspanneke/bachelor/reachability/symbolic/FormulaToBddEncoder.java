package de.lukaspanneke.bachelor.reachability.symbolic;

import com.github.javabdd.BDD;
import com.github.javabdd.BDDDomain;
import com.github.javabdd.BDDFactory;
import de.lukaspanneke.bachelor.reachability.logic.ComparisonOperator;
import de.lukaspanneke.bachelor.reachability.logic.generic.LinearCombination;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.ComparisonFormula;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.CompositionFormula;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.NegatedFormula;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import de.lukaspanneke.bachelor.timing.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.lukaspanneke.bachelor.reachability.logic.ComparisonOperator.GREATER_EQUALS;
import static de.lukaspanneke.bachelor.reachability.logic.ComparisonOperator.GREATER_THEN;
import static de.lukaspanneke.bachelor.reachability.logic.ComparisonOperator.LESS_EQUALS;
import static de.lukaspanneke.bachelor.reachability.logic.ComparisonOperator.LESS_THEN;
import static de.lukaspanneke.bachelor.timing.TimerIds.BDD_ENCODE_FORUMLA;

public class FormulaToBddEncoder {

	private static final Logger LOGGER = LogManager.getLogger(FormulaToBddEncoder.class);

	public static <N, M, P, T> BDD encodeFormula(StateFormula<N, M, P, T> formula, Function<P, BDDDomain> placeToVar, BDDFactory factory) throws InterruptedException {
		LOGGER.trace("Encode formula {}", formula);
		Timer.global().start(BDD_ENCODE_FORUMLA);
		try {
			return _encodeFormula_impl(formula, placeToVar, factory);
		} finally {
			String duration = Timer.global().stopFormat(BDD_ENCODE_FORUMLA);
			LOGGER.trace("Spent {} encoding formula", duration);
		}
	}

	public static <N, M, P, T> BDD _encodeFormula_impl(StateFormula<N, M, P, T> formula, Function<P, BDDDomain> placeToVar, BDDFactory factory) throws InterruptedException {
		if (formula instanceof ComparisonFormula<N, M, P, T> comparison) {
			LinearCombination.LinearCombinationConstraint<N, M, P, T> constraint = LinearCombination.asLinearCombinationConstraint(comparison).timesNegativeOne();
			int val = (int) -constraint.linearCombination().constant();
			Map<BDDDomain, Integer> varToCoef = constraint.linearCombination().factors().entrySet().stream()
					.collect(Collectors.toMap(
							entry -> placeToVar.apply(entry.getKey().place()),
							entry -> entry.getValue().intValue()
					));
			return createLinearInequalityExpression(varToCoef, constraint.operator(), val);
		} else if (formula instanceof NegatedFormula<N, M, P, T> neg) {
			return _encodeFormula_impl(neg.formula(), placeToVar, factory).not();
		} else if (formula instanceof CompositionFormula<N, M, P, T> comp) {
			BDD ret = switch (comp.operator()) {
				case AND -> factory.one();
				case OR -> factory.zero();
			};
			for (StateFormula<N, M, P, T> f : comp.formulas()) {
				BDD f_bdd = _encodeFormula_impl(f, placeToVar, factory);
				switch (comp.operator()) {
					case AND -> ret.andWith(f_bdd);
					case OR -> ret.orWith(f_bdd);
				}
			}
			return ret;
		} else {
			throw new UnsupportedOperationException();
		}
	}

	public static BDD createLinearInequalityExpression(Map<BDDDomain, Integer> varToCoef, ComparisonOperator op, int val) throws InterruptedException {
		assert op.isInequality();
		if (varToCoef.size() == 0) {
			throw new IllegalArgumentException();
		}

		if (op == LESS_EQUALS || op == GREATER_THEN) {
			/* we need to compute non-strict inequality --> increment right operand */
			val++;
		}

		BDD ret = _linear_combination_less_then_val(varToCoef, val);

		if (op == GREATER_THEN || op == GREATER_EQUALS) {
			/* negate the BDD to turn the currently encoded var <= val into var > val */
			BDD tmpInq = ret.not();
			ret.free();
			ret = tmpInq;
		}
		return ret;
	}

	private static BDD _linear_combination_less_then_val(Map<BDDDomain, Integer> varToCoef, int val) throws InterruptedException {
		Map<Integer, BDDDomain> idxToVar = new HashMap<>();
		int idx = 1;
		for (BDDDomain var : varToCoef.keySet()) {
			idxToVar.put(idx++, var);
		}
		int maxSize = varToCoef.keySet().stream()
				.map(BDDDomain::size)
				.max(Comparator.naturalOrder())
				.orElseThrow()
				.intValueExact();
		int b = nextPowerOfTwo(maxSize);

		return _linear_combination_less_then_val_recursion(idxToVar, varToCoef, -val, varToCoef.size(), 1, 0, b);
	}

	/**
	 * Computes the BDD of any <i>linear</i> (without modulo) strict inequality expression, i.e, a0 + a1*x1 + a2*x2 + ...
	 * + an*xn < b0 + b1*y1 + b2*y2 + ... + bn*yn, where a0, ... ,an and b0, ... ,bn are integer constants and x1, ... ,xn
	 * and y1, ... ,yn are integer variables. This method is based on an algorithm presented by Bartzis and Bultan (2003).
	 *
	 * @param idxToVar Mapping of indices to the variables of the expression. The minimum index must be 1.
	 * @param varToCoef Mapping of variables to their coefficients. Each variable is mapped to its coefficient in the linear
	 * expression.
	 * @param c The carry computed so far. Its initial value is (a0-b0).
	 * @param v The number of variables that appear in the linear expression, i.e., with non zero coefficient.
	 * @param i The current level, i.e., the current variable index. The minimum index must be 1.
	 * @param j The current layer, i.e, the current bit (BDD variable) of the current variable domain, starting from 0.
	 * @param b The size of the largest domain (number of layers) among all the variable domains that appear in the linear
	 * expression.
	 * @return The BDD which encodes the specified linear inequality expression.
	 */
	private static BDD _linear_combination_less_then_val_recursion(Map<Integer, BDDDomain> idxToVar,
			Map<BDDDomain, Integer> varToCoef, long c, int v, int i,
			int j, int b) throws InterruptedException {
		if (Thread.interrupted()) {
			throw new InterruptedException();
		}
		BDDFactory factory = idxToVar.get(i).getFactory();
		int[] varIBits = idxToVar.get(i).vars();

		if (j >= varIBits.length && i < v) { /*
		 * the current variable has no bit in this layer (its length is shorter), but
		 * we can proceed to the next variable in this layer
		 */
			return _linear_combination_less_then_val_recursion(idxToVar, varToCoef, c, v, i + 1, j, b);
		}
		if (j >= varIBits.length && j < b - 1) { //&& (i == v) /*proceed to the beginning of the next layer*/
			if (c % 2 == 0) {
				return _linear_combination_less_then_val_recursion(idxToVar, varToCoef, c / 2, v, 1, j + 1, b);
			}
			return _linear_combination_less_then_val_recursion(idxToVar, varToCoef, (c - 1) / 2, v, 1, j + 1, b);
		}
		if (j >= varIBits.length) { /*
		 * this is the last variable in the last layer, but the current variable has no bit in
		 * this layer
		 */
			// recursion base
			if (c < 0) {
				return factory.one();
			}
			return factory.zero();
		}
		/* j < varIBits.length */

		BDD lo, hi;
		BDD curBit = factory.ithVar(varIBits[j]);
		int curCoef = varToCoef.get(idxToVar.get(i));

		if (i == v && j == b - 1) { // recursion base
			lo = (c < 0) ? factory.one() : factory.zero();
			hi = (c + curCoef < 0) ? factory.one() : factory.zero();
		} else if (i == v) { /* last variable in this layer --> proceed to the beginning of the next layer */
			lo = _linear_combination_less_then_val_recursion(idxToVar, varToCoef, c % 2 == 0 ? c / 2 : (c - 1) / 2, v, 1, j + 1, b);
			hi = _linear_combination_less_then_val_recursion(idxToVar, varToCoef, (c + curCoef) % 2 == 0 ? (c + curCoef) / 2 : (c + curCoef - 1) / 2, v, 1, j + 1, b);
		} else {
			lo = _linear_combination_less_then_val_recursion(idxToVar, varToCoef, c, v, i + 1, j, b);
			hi = _linear_combination_less_then_val_recursion(idxToVar, varToCoef, c + curCoef, v, i + 1, j, b);
		}

		BDD result = curBit.ite(hi, lo);
		lo.free();
		hi.free();
		return result;
	}

	private static int nextPowerOfTwo(int i) {
		int h = Integer.highestOneBit(i);
		if (h == i) {
			return h;
		} else {
			return h << 1;
		}
	}

	/**
	 * Computes a BDD of a simple inequality expression.
	 * <p>
	 * Supports
	 * <ul>
	 *     <li>{@code var < val}</li>
	 *     <li>{@code var <= val}</li>
	 *     <li>{@code var > val}</li>
	 *     <li>{@code var >= val}</li>
	 * </ul>
	 * where the left operand {@code var} is a binary encoded integer variable
	 * with lower bound {@code 0} inclusive and the upper bound {@code var.size()} exclusive.
	 * The right operand is an integer constant.
	 * <p>
	 * The time complexity is linear in the number of the BDD variables
	 * that encode the domain of the referenced variable.
	 */
	public static BDD createSimpleInequalityExpression(BDDDomain var, ComparisonOperator op, int val) {
		assert op.isInequality();
		BDDFactory factory = var.getFactory();
		int upperBoundInclusive = var.size().intValueExact() - 1;

		/* First, check for trivial cases */
		if (op == LESS_EQUALS && upperBoundInclusive <= val) {
			return factory.one();
		}
		if (op == LESS_THEN && upperBoundInclusive < val) {
			return factory.one();
		}
		if (op == GREATER_EQUALS && upperBoundInclusive < val) {
			return factory.zero();
		}
		if (op == GREATER_THEN && upperBoundInclusive <= val) {
			return factory.zero();
		}

		if (op == LESS_THEN || op == GREATER_EQUALS) {
			/* we need to compute strict inequality --> decrement right operand */
			val--;
		}

		/* encode var <= val */
		BDD ret = _var_strict_less_then_val(var, val);

		if (op == GREATER_THEN || op == GREATER_EQUALS) {
			/* negate the BDD to turn the currently encoded var <= val into var > val */
			BDD tmpInq = ret.not();
			ret.free();
			ret = tmpInq;
		}

		/*
		 * Note: We do not restrict here the resulting BDD
		 * to the domain of the integer variable.
		 * This means the resulting BDD may not behave correct
		 * outside of the domains definition.
		 * For example with a domain of 12 values and the expression x > 9,
		 * the resulting BDD is true for 10 (inclusive) to 15 (inclusive).
		 * This choice is made, because the restricted BDD is much bigger.
		 */
		/* TODO evaluate if it is a good idea to omit the restriction. */
		ret.andWith(var.domain());

		return ret;
	}

	private static BDD _var_strict_less_then_val(BDDDomain var, int val) {
		BDDFactory factory = var.getFactory();
		BDD ret = factory.one();
		int[] ivar = var.vars();
		for (int n = 0; n < var.varNum(); n++) {
			if ((val & 1) != 0) {
				ret.orWith(factory.nithVar(ivar[n]));
			} else {
				ret.andWith(factory.nithVar(ivar[n]));
			}
			val >>= 1;
		}
		return ret;
	}
}
