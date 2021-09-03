package de.lukaspanneke.bachelor.reachability;

public enum QueryResult {
	SATISFIED(true, false, false, false),
	//PROBABLY_SATISFIED(true, false, false, true),
	UNSATISFIED(false, true, false, false),
	//PROBABLY_UNSATISFIED(false, true, false, true),
	UNSUPPORTED(false, false, true, false),
	DID_NOT_FINISH(false, false, true, false),
	ERROR(false, false, true, false);

	public QueryResult negate() {
		QueryResult result;
		if (this == UNSATISFIED) {
			return SATISFIED;
		} else if (this == SATISFIED) {
			return UNSATISFIED;
		} else {
			return this;
		}
	}

	QueryResult(boolean positive, boolean negative, boolean unknown, boolean uncertainty) {
	}
}
