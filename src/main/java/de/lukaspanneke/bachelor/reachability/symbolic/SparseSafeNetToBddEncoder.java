package de.lukaspanneke.bachelor.reachability.symbolic;

import com.github.javabdd.BDD;
import com.github.javabdd.BDDDomain;
import com.github.javabdd.BDDFactory;
import com.github.javabdd.BDDVarSet;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseIntVector;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import de.lukaspanneke.bachelor.timing.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.stream.IntStream;

import static de.lukaspanneke.bachelor.pn.sparse.mutable.SparseNetUtil.renderPlaces;
import static de.lukaspanneke.bachelor.reachability.symbolic.BddAptSolver.PREDECESSOR;
import static de.lukaspanneke.bachelor.reachability.symbolic.BddAptSolver.SUCCESSOR;
import static de.lukaspanneke.bachelor.timing.TimerIds.BDD_ENCODE_EDGES;

public class SparseSafeNetToBddEncoder {
	private static final Logger LOGGER = LogManager.getLogger(SparseSafeNetToBddEncoder.class);
	private final SparsePetriNet net;
	private final int numberOfPlaces;
	private final BDDFactory factory;
	private final BDDDomain[/* pos */][/* place */] TOKENS;
	private final BDD[/* pos */][/* place */][/* number of tokens (0 or 1) */] TOKENS_CACHED;
	private final BDD EDGE_ALL_UNMODIFIED;

	public SparseSafeNetToBddEncoder(BDDFactory factory, BDDDomain[][] tokens, SparsePetriNet net) {
		this.factory = factory;
		this.TOKENS = tokens;
		this.net = net;
		this.numberOfPlaces = this.net.getPlaceCount();

		this.TOKENS_CACHED = new BDD[2][][];
		for (int pos = 0; pos < TOKENS_CACHED.length; pos++) {
			TOKENS_CACHED[pos] = new BDD[this.numberOfPlaces][];
			for (int place = 0; place < numberOfPlaces; place++) {
				TOKENS_CACHED[pos][place] = new BDD[] {
						TOKENS[pos][place].ithVar(0),
						TOKENS[pos][place].ithVar(1)
				};
			}
		}

		EDGE_ALL_UNMODIFIED = this.factory.one();
		for (int place = 0; place < this.numberOfPlaces; place++) {
			EDGE_ALL_UNMODIFIED.andWith(TOKENS[PREDECESSOR][place].buildEquals(TOKENS[SUCCESSOR][place]));
		}
	}

	public BDD /* set of edges */ encodeBehaviour() throws InterruptedException {
		LOGGER.trace("Encode behaviour");
		Timer.global().start(BDD_ENCODE_EDGES);
		try {
			final int size = this.net.getTransitionCount();
			BDD ret = this.factory.zero();
			for (int transition = 0; transition < size; transition++) {
				//LOGGER.trace("... #{} / {}: {}", transition + 1, size, this.net.getTransitionName(transition));
				ret.orWith(this.encodeTransition(transition));
			}
			return ret;
		} finally {
			String duration = Timer.global().stopFormat(BDD_ENCODE_EDGES);
			LOGGER.trace("Spent {} encoding behaviour", duration);
		}
	}

	/* this algorithm modifies some BDD for every place */
	@Deprecated
	public BDD /* edge */ encodeTransition_old(int transition) {
		SparseIntVector pre = this.net.presetT(transition);
		SparseIntVector post = this.net.postsetT(transition);
		BDD ret = this.factory.one();

		for (int place = 0; place < this.numberOfPlaces; place++) {
			boolean preContains = pre.containsKey(place);
			boolean postContains = post.containsKey(place);

			if (!preContains && !postContains) {
				ret.andWith(TOKENS[PREDECESSOR][place].buildEquals(TOKENS[SUCCESSOR][place]));
			} else {
				ret.andWith(encodePlace(place, PREDECESSOR, preContains));
				ret.andWith(encodePlace(place, SUCCESSOR, postContains));
			}
		}
		return ret;
	}

	/* this algorithm modifies some BDD for every place connected to the transition */
	public BDD /* edge */ encodeTransition(int transition) throws InterruptedException {
		SparseIntVector pre = this.net.presetT(transition);
		SparseIntVector post = this.net.postsetT(transition);
		BDD modifications = this.factory.one();
		BDDVarSet connectedDomains = this.factory.emptySet();

		int preIndex = 0, postIndex = 0;
		while (preIndex < pre.size() && postIndex < post.size()) {
			if (Thread.interrupted()) {
				LOGGER.error("Thread was interrupted. Aborting calculation of transition");
				throw new InterruptedException();
			}
			int prePlace = pre.keyAt(preIndex);
			int postPlace = post.keyAt(postIndex);
			if (prePlace == postPlace) {
				/* place is predecessor and successor */
				modifications.andWith(encodeMarkedPlace(prePlace, PREDECESSOR));
				modifications.andWith(encodeMarkedPlace(prePlace, SUCCESSOR));
				connectedDomains.unionWith(TOKENS[PREDECESSOR][prePlace].set());
				connectedDomains.unionWith(TOKENS[SUCCESSOR][prePlace].set());
				preIndex++;
				postIndex++;
			} else if (prePlace < postPlace) {
				/* prePlace is predecessor, but not successor. postPlace is successor. */
				modifications.andWith(encodeMarkedPlace(prePlace, PREDECESSOR));
				modifications.andWith(encodeUnmarkedPlace(prePlace, SUCCESSOR));
				modifications.andWith(encodeMarkedPlace(postPlace, SUCCESSOR));
				connectedDomains.unionWith(TOKENS[PREDECESSOR][prePlace].set());
				connectedDomains.unionWith(TOKENS[SUCCESSOR][prePlace].set());
				preIndex++;
			} else /* prePlace > postPlace */ {
				/* prePlace is predecessor. postPlace is successor, but not predecessor. */
				modifications.andWith(encodeMarkedPlace(prePlace, PREDECESSOR));
				modifications.andWith(encodeUnmarkedPlace(postPlace, PREDECESSOR));
				modifications.andWith(encodeMarkedPlace(postPlace, SUCCESSOR));
				connectedDomains.unionWith(TOKENS[PREDECESSOR][postPlace].set());
				connectedDomains.unionWith(TOKENS[SUCCESSOR][postPlace].set());
				postIndex++;
			}
		}

		while (preIndex < pre.size()) {
			if (Thread.interrupted()) {
				LOGGER.error("Thread was interrupted. Aborting calculation of transition");
				throw new InterruptedException();
			}
			int prePlace = pre.keyAt(preIndex);
			/* prePlace is predecessor, but not successor. */
			modifications.andWith(encodeMarkedPlace(prePlace, PREDECESSOR));
			modifications.andWith(encodeUnmarkedPlace(prePlace, SUCCESSOR));
			connectedDomains.unionWith(TOKENS[PREDECESSOR][prePlace].set());
			connectedDomains.unionWith(TOKENS[SUCCESSOR][prePlace].set());
			preIndex++;
		}

		while (postIndex < post.size()) {
			if (Thread.interrupted()) {
				LOGGER.error("Thread was interrupted. Aborting calculation of transition");
				throw new InterruptedException();
			}
			int postPlace = post.keyAt(postIndex);
			/* postPlace is successor, but not predecessor */
			modifications.andWith(encodeUnmarkedPlace(postPlace, PREDECESSOR));
			modifications.andWith(encodeMarkedPlace(postPlace, SUCCESSOR));
			connectedDomains.unionWith(TOKENS[PREDECESSOR][postPlace].set());
			connectedDomains.unionWith(TOKENS[SUCCESSOR][postPlace].set());
			postIndex++;
		}

		return EDGE_ALL_UNMODIFIED.exist(connectedDomains).andWith(modifications);
	}

	public BDD /* vertex */ encodeMarking(SparseIntVector marking, int pos) {
		assert IntStream.range(0, marking.size())
				.map(marking::valueAt)
				.allMatch(value -> value == 0 || value == 1)
				: " marking " + renderPlaces(marking, this.net) + " is not safe";

		BDD ret = this.factory.one();
		int markingIndex = 0;
		int nextMarkedPlace = marking.size() == 0 ? this.numberOfPlaces : marking.keyAt(0);
		for (int place = 0; place < this.numberOfPlaces; place++) {
			if (place == nextMarkedPlace) {
				ret.andWith(this.encodeMarkedPlace(place, pos));
				markingIndex++;
				nextMarkedPlace = markingIndex < marking.size() ? marking.keyAt(markingIndex) : this.numberOfPlaces;
			} else {
				ret.andWith(this.encodeUnmarkedPlace(place, pos));
			}
		}
		return ret;
	}

	public BDD encodePlace(int place, int pos, boolean hasToken) {
		return hasToken ? encodeMarkedPlace(place, pos) : encodeUnmarkedPlace(place, pos);
	}

	public BDD encodeUnmarkedPlace(int place, int pos) {
		return TOKENS_CACHED[pos][place][0].id();
	}

	public BDD encodeMarkedPlace(int place, int pos) {
		return TOKENS_CACHED[pos][place][1].id();
	}

	public BDD encodeFormula(StateFormula<SparsePetriNet, SparseIntVector, String, Integer> goal, int pos) throws InterruptedException {
		return FormulaToBddEncoder.encodeFormula(goal, placeName -> TOKENS[pos][this.net.getPlaceId(placeName)], this.factory);
	}

}
