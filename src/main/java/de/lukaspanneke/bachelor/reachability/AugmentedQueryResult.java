package de.lukaspanneke.bachelor.reachability;

public class AugmentedQueryResult<T> {

	private final QueryResult result;
	private final T detail;
	private final Throwable cause;

	public AugmentedQueryResult(QueryResult result, T detail) {
		this.result = result;
		this.detail = detail;
		this.cause = null;
	}

	public AugmentedQueryResult(QueryResult result, Throwable cause) {
		this.result = result;
		this.detail = null;
		this.cause = cause;
	}

	public static <T> AugmentedQueryResult<T> satisfied(T detail) {
		return new AugmentedQueryResult<>(QueryResult.SATISFIED, detail);
	}

	public static <T> AugmentedQueryResult<T> unsatisfied(T detail) {
		return new AugmentedQueryResult<>(QueryResult.UNSATISFIED, detail);
	}

	public static <T> AugmentedQueryResult<T> dnf(T detail) {
		return new AugmentedQueryResult<>(QueryResult.DID_NOT_FINISH, detail);
	}

	public QueryResult result() {
		return this.result;
	}

	public T detail() {
		return this.detail;
	}

	public Throwable errorCause() {
		return this.cause;
	}

	@Override
	public String toString() {
		return this.result + "[" + this.detail + "]";
	}
}
