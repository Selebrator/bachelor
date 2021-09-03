package de.lukaspanneke.bachelor.help;

@FunctionalInterface
public interface BiIntPredicate {
	boolean test(int key, int value);
}
