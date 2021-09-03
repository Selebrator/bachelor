package de.lukaspanneke.bachelor.reachability.logic.generic.expression;

public interface PlaceExpression<N, M, P, T> extends ArithmeticExpression<N, M, P, T> {
	P place();
}
