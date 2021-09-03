package de.lukaspanneke.bachelor.reachability.symbolic;

import com.github.javabdd.BDD;
import com.github.javabdd.BDDFactory;
import com.github.javabdd.BDDVarSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.stream.Collector;

public class BddUtil {

	/**
	 * Stream collector for or-ing a stream of BDDs.
	 */
	public static Collector<BDD, BDD, BDD> or(BDDFactory factory) {
		return Collector.of(
				factory::zero,
				BDD::orWith,
				BDD::orWith
		);
	}

	/**
	 * Stream collector for or-ing a stream of BDDs.
	 */
	public static Collector<BDD, BDD, BDD> xor(BDDFactory factory) {
		return Collector.of(
				factory::zero,
				BDD::xorWith,
				BDD::xorWith
		);
	}

	/**
	 * Stream collector for and-ing a stream of BDDs.
	 */
	public static Collector<BDD, BDD, BDD> and(BDDFactory factory) {
		return Collector.of(
				factory::one,
				BDD::andWith,
				BDD::andWith
		);
	}

	public static double size(BDD set, BDDVarSet domain) {
		return set.satCount(domain);
	}

	public static boolean contains(BDD set, BDD element) {
		return !set.and(element).isZero();
	}
}
