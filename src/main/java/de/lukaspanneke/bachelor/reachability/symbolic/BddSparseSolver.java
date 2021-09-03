package de.lukaspanneke.bachelor.reachability.symbolic;

import com.github.javabdd.BDD;
import com.github.javabdd.BDDDomain;
import com.github.javabdd.BDDFactory;
import com.github.javabdd.BDDVarSet;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseIntVector;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.IntStream;

import static de.lukaspanneke.bachelor.reachability.symbolic.BddAptSolver.PREDECESSOR;
import static de.lukaspanneke.bachelor.reachability.symbolic.BddAptSolver.SUCCESSOR;

public class BddSparseSolver implements AutoCloseable {
	private static final Logger LOGGER = LogManager.getLogger(BddSparseSolver.class);
	/* [pos][place] */
	protected BDDDomain[][] TOKENS;
	public final SparseSafeNetToBddEncoder encoder;
	protected Fixpoint stateSpace;
	private final int numberOfPlaces;

	protected final BDDVarSet predecessorVariables;
	protected final BDDVarSet successorVariables;

	private final SparsePetriNet net;
	private final BDDFactory factory;

	public BddSparseSolver(SparsePetriNet net) {
		this.net = net;
		this.factory = BDDFactory.init(1 << 23, 1 << 13);
		this.numberOfPlaces = this.net.getPlaceCount();

		TOKENS = new BDDDomain[2][];
		TOKENS[PREDECESSOR] = new BDDDomain[this.numberOfPlaces];
		TOKENS[SUCCESSOR] = new BDDDomain[this.numberOfPlaces];
		for (int place = 0; place < this.numberOfPlaces; place++) {
			TOKENS[PREDECESSOR][place] = this.booleanDomain("." + this.net.getPlaceName(place));
			TOKENS[SUCCESSOR][place] = this.booleanDomain(this.net.getPlaceName(place) + ".");
		}

		this.predecessorVariables = this.calcVarSet(PREDECESSOR);
		this.successorVariables = this.calcVarSet(SUCCESSOR);

		this.encoder = new SparseSafeNetToBddEncoder(this.factory, TOKENS, this.net);
	}

	private BDDDomain booleanDomain(String name) {
		BDDDomain ret = this.factory.extDomain(2);
		ret.setName(name);
		return ret;
	}

	public Fixpoint stateSpace() throws InterruptedException {
		if (this.stateSpace == null) {
			BDD edges = this.encoder.encodeBehaviour();
			this.stateSpace = new Fixpoint(
					this.encoder.encodeMarking(this.net.getInitialMarking(), PREDECESSOR),
					states -> onlySuccessorsAsPredecessors(states.and(edges)));
		}
		return this.stateSpace;
	}

	private BDD onlySuccessorsAsPredecessors(BDD edge) {
		return edge.exist(this.predecessorVariables).and(this.preBimpSucc_cached()).exist(this.successorVariables);
	}

	public BDDVarSet getPredecessorVariables() {
		return this.predecessorVariables.id();
	}

	public BDDVarSet getSuccessorVariables() {
		return this.successorVariables.id();
	}

	protected BDDVarSet calcVarSet(int pos) {
		BDDVarSet ret = this.factory.emptySet();
		for (BDDDomain placeDomain : TOKENS[pos]) {
			ret.unionWith(placeDomain.set());
		}
		return ret;
	}

	// lazily initialised
	protected BDD preBimpSucc_cached = null;

	protected BDD preBimpSucc_cached() {
		if (this.preBimpSucc_cached == null) {
			this.preBimpSucc_cached = this.preBimpSucc_calc();
		}
		return this.preBimpSucc_cached;
	}

	protected BDD preBimpSucc_calc() {
		return IntStream.range(0, this.numberOfPlaces)
				.mapToObj(placeIndex -> TOKENS[PREDECESSOR][placeIndex].buildEquals(TOKENS[SUCCESSOR][placeIndex]))
				.collect(BddUtil.and(this.factory));
	}

	public Boolean isReachable(BDD goal) throws InterruptedException {
		if (goal.isZero()) {
			LOGGER.debug("Goal formula is contradiction, skipping analysis of net.");
			return false;
		}
		if (goal.isOne()) {
			LOGGER.debug("Goal formula is tautology, skipping analysis of net.");
			return true;
		}
		boolean contains = this.stateSpace().contains(goal);
		//LOGGER.trace("discovered {} states", BddUtil.size(this.stateSpace().current(), this.predecessorVariables)); // this seems to be incorrect
		return contains;
	}

	/* return
	 *   - true, if reachable,
	 *   - false, if unreachable and
	 *   - null if not decided
	 */
	public Boolean isReachable(StateFormula<SparsePetriNet, SparseIntVector, String, Integer> goal) {
		if (goal.test(this.net, this.net.getInitialMarking())) {
			LOGGER.trace("Initial marking is a goal marking. Skipping BDD.");
			return true;
		}
		try {
			return this.isReachable(this.encoder.encodeFormula(goal, PREDECESSOR));
		} catch (InterruptedException e) {
			return null;
		}
	}

	/* returned list at index of goal is
	 *   - true, if reachable,
	 *   - false, if unreachable and
	 *   - null if not decided
	 */
	public List<Boolean> isReachable(List<? extends StateFormula<SparsePetriNet, SparseIntVector, String, Integer>> goals) {
		BddGoalSet goalSet;
		try {
			goalSet = BddGoalSet.fromGoalFormulas(goals, this.net, goal -> {
				try {
					return this.encoder.encodeFormula(goal, PREDECESSOR);
				} catch (InterruptedException e) {
					return null;
				}
			});
		} catch (BddGoalSet.InterruptedInitializationException e) {
			LOGGER.error("Got interrupted while encoding goal formulas");
			return e.getKnownResults();
		}
		if (!goalSet.isDecided()) {
			LOGGER.trace("Starting net analysis with {} formulas undecided", goalSet.getNumUndecided());
			Fixpoint fixpoint;
			try {
				/* generated behaviour. may be interrupted. */
				fixpoint = this.stateSpace();
			} catch (InterruptedException e) {
				return goalSet.getResults();
			}
			fixpoint.containsSet(goalSet);
		} else {
			LOGGER.trace("All formulas are satisfied in the initial state, tautologies or contradictions.");
		}
		return goalSet.getResults();
	}

	@Override
	public void close() {
		this.factory.done();
	}
}
