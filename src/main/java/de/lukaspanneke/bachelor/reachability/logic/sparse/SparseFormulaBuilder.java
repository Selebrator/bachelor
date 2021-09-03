package de.lukaspanneke.bachelor.reachability.logic.sparse;

import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseIntVector;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;
import de.lukaspanneke.bachelor.reachability.logic.generic.FormulaBuilder;
import de.lukaspanneke.bachelor.reachability.logic.generic.expression.PlaceExpression;

public final class SparseFormulaBuilder implements FormulaBuilder<SparsePetriNet, SparseIntVector, String, Integer> {
	@Override
	public PlaceExpression<SparsePetriNet, SparseIntVector, String, Integer> place(String placeName) {
		return SparsePlaceExpression.of(placeName);
	}
}
