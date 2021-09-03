package de.lukaspanneke.bachelor.reachability;

public class AugmentedBoolean<T> {
	public static final AugmentedBoolean<Void> TRUE = new AugmentedBoolean<>(true, null);
	public static final AugmentedBoolean<Void> FALSE = new AugmentedBoolean<>(false, null);

	private final boolean bool;
	private final T detail;

	private AugmentedBoolean(boolean bool, T detail) {
		this.bool = bool;
		this.detail = detail;
	}

	public static AugmentedBoolean<Void> of(boolean result) {
		return result ? TRUE : FALSE;
	}

	public static <T> AugmentedBoolean<T> of(boolean result, T detail) {
		return new AugmentedBoolean<>(result, detail);
	}

	public boolean bool() {
		return this.bool;
	}

	public T detail() {
		return this.detail;
	}

	@Override
	public String toString() {
		return this.bool + " [" + this.detail + "]";
	}
}
