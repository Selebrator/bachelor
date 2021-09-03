package de.lukaspanneke.bachelor.mcc;

import de.lukaspanneke.bachelor.reachability.logic.generic.Formula;

public record MccReachabilityCardinalityProperty<N, M, P, T>
		(String id,
		 String description,
		 Formula<N, M, P, T> formula) {
}
