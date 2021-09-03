package de.lukaspanneke.bachelor;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Token;

import java.util.*;
import java.util.function.BiPredicate;

public class Comparison {

	private static boolean markingAllSatisfy(Marking a, Marking b, BiPredicate<Token, Token> toSatisfy) {
		assert a.getNet().equals(b.getNet());
		return a.getNet().getPlaces().stream()
				.allMatch(place -> toSatisfy.test(a.getToken(place), b.getToken(place)));
	}

	public static boolean markingLessEqual(Marking a, Marking b) {
		return markingAllSatisfy(a, b, Comparison::tokenLessEqual);
	}

	public static boolean markingLessThan(Marking a, Marking b) {
		return !a.equals(b) && markingLessEqual(a, b);
	}

	public static boolean markingStrictlyLess(Marking a, Marking b) {
		return markingAllSatisfy(a, b, Comparison::tokenLessThan);
	}

	public static boolean markingGreaterEqual(Marking a, Marking b) {
		return markingAllSatisfy(a, b, Comparison::tokenGreaterEqual);
	}

	public static boolean markingGreaterThan(Marking a, Marking b) {
		return !a.equals(b) && markingGreaterEqual(a, b);
	}

	public static boolean markingStrictlyGreater(Marking a, Marking b) {
		return markingAllSatisfy(a, b, Comparison::tokenGreaterThan);
	}

	public static Comparator<Token> comparingTokens() {
		return (a, b) -> tokenLessThan(a, b) ? -1 : (a.equals(b) ? 0 : +1);
	}

	public static boolean tokenLessThan(Token a, Token b) {
		if (a.isOmega()) {
			return false;
		} else if (b.isOmega()) {
			return !a.isOmega();
		} else {
			return a.getValue() < b.getValue();
		}
	}

	public static boolean tokenLessEqual(Token a, Token b) {
		return a.equals(b) || tokenLessThan(a, b);
	}

	public static boolean tokenGreaterThan(Token a, Token b) {
		return tokenLessThan(b, a);
	}

	public static boolean tokenGreaterEqual(Token a, Token b) {
		return tokenLessEqual(b, a);
	}
}
