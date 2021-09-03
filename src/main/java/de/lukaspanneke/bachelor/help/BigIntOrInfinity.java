package de.lukaspanneke.bachelor.help;

import java.math.BigInteger;
import java.util.*;

public class BigIntOrInfinity implements Comparable<BigIntOrInfinity> {

	public static BigIntOrInfinity POSITIVE_INFINITY = new BigIntOrInfinity(null);

	private final BigInteger value;

	private BigIntOrInfinity(BigInteger value) {
		this.value = value;
	}

	public static BigIntOrInfinity of(long value) {
		return new BigIntOrInfinity(BigInteger.valueOf(value));
	}

	public static BigIntOrInfinity of(BigInteger value) {
		return new BigIntOrInfinity(value);
	}

	public boolean isFinite() {
		return this.value != null;
	}

	public boolean isPositiveInfinity() {
		return this.value == null;
	}

	public Optional<BigInteger> get() {
		return Optional.ofNullable(this.value);
	}

	public BigInteger getFiniteValue() throws NoSuchElementException {
		if (this.value == null) {
			throw new NoSuchElementException();
		}
		return this.value;
	}

	@Override
	public int compareTo(BigIntOrInfinity that) {
		if (this.isPositiveInfinity() || that.isPositiveInfinity()) {
			return this.isFinite() ? -1 : (that.isFinite() ? 1 : 0);
		} else {
			return this.getFiniteValue().compareTo(that.getFiniteValue());
		}
	}

	@Override
	public String toString() {
		if (isPositiveInfinity()) {
			return "+inf";
		} else {
			return this.value.toString();
		}
	}
}
