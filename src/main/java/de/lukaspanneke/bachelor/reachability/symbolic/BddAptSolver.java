package de.lukaspanneke.bachelor.reachability.symbolic;

import com.github.javabdd.BDD;
import com.github.javabdd.BDDDomain;
import com.github.javabdd.BDDFactory;
import com.github.javabdd.BDDVarSet;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import de.lukaspanneke.bachelor.timing.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Token;
import uniol.apt.adt.pn.Transition;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.IntStream;

import static de.lukaspanneke.bachelor.Enumerate.enumerate;
import static de.lukaspanneke.bachelor.timing.TimerIds.BDD_ENCODE_EDGES;

@Deprecated
public class BddAptSolver implements AutoCloseable {

	private static final Logger LOGGER = LogManager.getLogger(BddAptSolver.class);

	public final Timer timer = Timer.global();

	private final PetriNet net;
	private final List<Place> placeOrder;
	private final BDDFactory factory;

	/*
	 * The java source code variable (not bdd variable) 'pos' always means
	 * 0 for the predecessor bdd variables and 1 for the successor bdd variables.
	 */
	public static final int PREDECESSOR = 0;
	public static final int SUCCESSOR = 1;

	protected static final int TRUE = 1;
	protected static final int FALSE = 0;
	protected static final int UNKNOWN = -1;

	private final Collector<BDD, BDD, BDD> AND;
	private final Collector<BDD, BDD, BDD> OR;
	private final Collector<BDD, BDD, BDD> XOR;

	/* [pos] */
	protected BDDDomain[][] TOKENS;

	protected Fixpoint stateSpace;

	protected final BDDVarSet predecessorVariables;
	protected final BDDVarSet successorVariables;

	public BDDFactory getFactory() {
		return this.factory;
	}

	public BddAptSolver(PetriNet net) {
		this.net = net;
		this.factory = BDDFactory.init(1 << 23, 1 << 12);
		this.placeOrder = List.copyOf(net.getPlaces());

		AND = BddUtil.and(this.factory);
		OR = BddUtil.or(this.factory);
		XOR = BddUtil.xor(this.factory);

		TOKENS = new BDDDomain[2][];
		TOKENS[PREDECESSOR] = new BDDDomain[this.placeOrder.size()];
		TOKENS[SUCCESSOR] = new BDDDomain[this.placeOrder.size()];
		int placeIndex = 0;
		for (Place place : this.placeOrder) {
			assert placeIndex == this.placeIndex(place);
			TOKENS[PREDECESSOR][placeIndex] = this.booleanDomain("." + place.getId());
			TOKENS[SUCCESSOR][placeIndex] = this.booleanDomain(place.getId() + ".");
			placeIndex++;
		}

		this.predecessorVariables = this.calcVarSet(PREDECESSOR);
		this.successorVariables = this.calcVarSet(SUCCESSOR);
	}

	public BDDVarSet getPredecessorVariables() {
		return this.predecessorVariables;
	}

	public BDDVarSet getSuccessorVariables() {
		return this.successorVariables;
	}

	protected BDDVarSet calcVarSet(int pos) {
		return Arrays.stream(TOKENS[pos]).map(BDDDomain::set).reduce(this.getFactory().emptySet(), BDDVarSet::union);
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
		return IntStream.range(0, this.placeOrder.size())
				.mapToObj(placeIndex -> TOKENS[PREDECESSOR][placeIndex].buildEquals(TOKENS[SUCCESSOR][placeIndex]))
				.collect(AND);
		//return this.enumeratePlaces((placeIndex, place) -> TOKENS[PREDECESSOR][placeIndex].buildEquals(TOKENS[SUCCESSOR][placeIndex])).collect(AND);
	}

	protected BDD onlySuccessors(BDD edge) {
		return edge.exist(this.predecessorVariables);
	}

	protected BDD onlyPredecessors(BDD edge) {
		return edge.exist(this.successorVariables);
	}

	protected BDD onlySuccessorsAsPredecessors(BDD edge) {
		return edge.exist(this.predecessorVariables).and(this.preBimpSucc_cached()).exist(this.successorVariables);
	}

	private BDDDomain booleanDomain(String name) {
		BDDDomain ret = this.getFactory().extDomain(2);
		ret.setName(name);
		return ret;
	}

	protected int placeIndex(Place place) {
		int i = this.placeOrder.indexOf(place);
		if (i >= 0) {
			return i;
		} else {
			throw new NoSuchElementException();
		}
	}

	protected BDD firable(Transition transition) {
		return transition.getPreset().stream()
				.map(place -> encodeMarkedPlace(place, PREDECESSOR))
				.collect(AND);
	}

	protected BDD transitions_calc() throws InterruptedException {
		try (var ignored = this.timer.measure(BDD_ENCODE_EDGES)) {
			LOGGER.debug("Calculating edges ...");
			Set<Transition> transitions = this.net.getTransitions();
			final int size = transitions.size();
			BDD ret = this.getFactory().zero();
			for (var transition : enumerate(transitions)) {
				if (Thread.interrupted()) {
					LOGGER.error("... aborted calculation of edges on transition {} / {}", (transition.index() + 1), size);
					throw new InterruptedException();
				}
				//LOGGER.trace("... {} / {}: {}", transition.index() + 1, size, transition.item().getId());
				ret.orWith(this.transition(transition.item()));
			}
			LOGGER.debug("... calculation of edges done.");
			return ret;
		}
	}

	// lazily initialised
	protected BDD transitions_cached = null;

	protected BDD transitions_cached() throws InterruptedException {
		if (this.transitions_cached == null) {
			this.transitions_cached = this.transitions_calc();
		} else {
			LOGGER.debug("Reusing cached edges");
		}
		return this.transitions_cached;
	}

	protected BDD transition(Transition transition) {
		Set<Place> pre = transition.getPreset();
		Set<Place> post = transition.getPostset();
		BDD ret = this.getFactory().one();
		for (var p : enumerate(this.placeOrder)) {
			Place place = p.item();
			int index = p.index();
			if (!pre.contains(place) && !post.contains(place)) {
				ret.andWith(TOKENS[PREDECESSOR][index].buildEquals(TOKENS[SUCCESSOR][index]));
			} else {
				ret.andWith(encodePlace(index, PREDECESSOR, pre.contains(place)));
				ret.andWith(encodePlace(index, SUCCESSOR, post.contains(place)));
			}
		}
		return ret;
	}

	protected BDD initialVertex() {
		return encodeMarking(this.net.getInitialMarking(), PREDECESSOR);
	}

	public Fixpoint stateSpace() throws InterruptedException {
		if (this.stateSpace == null) {
			BDD edges = this.transitions_cached();
			this.stateSpace = new Fixpoint(this.initialVertex(), states -> onlySuccessorsAsPredecessors(states.and(edges)));
		}
		return this.stateSpace;
	}

//	protected BDD reachableMarkings(BDD startVertex, Map<Integer, BDD> steps) {
//		BDD edges = this.transitions_cached();
//		return fixpoint(startVertex, Q -> Q.or(onlySuccessorsAsPredecessors(Q.and(edges))), steps);
//	}

	public boolean isReachable(BDD goal) throws InterruptedException {
		if (goal.isZero()) {
			LOGGER.debug("Goal formula is unsatisfiable, skipping analysis of net.");
			return false;
		}
		if (goal.isOne()) {
			LOGGER.debug("Goal formula is tautology, skipping analysis of net.");
			return true;
		}
		return this.stateSpace().contains(goal);
	}

	public Boolean isReachable(StateFormula<PetriNet, Marking, Place, Transition> formula) {
		try {
			BDD goal = this.encodeFormula(formula, PREDECESSOR);
			return this.isReachable(goal);
		} catch (InterruptedException e) {
			return null;
		}
	}

	public BDD encodeFormula(StateFormula<PetriNet, Marking, Place, Transition> formula, int pos) throws InterruptedException {
		return FormulaToBddEncoder.encodeFormula(formula, place -> TOKENS[pos][this.placeIndex(place)], this.factory);
	}

	protected BDD encodeMarking(Marking marking, int pos) {
		BDD ret = this.getFactory().one();
		int placeIndex = 0;
		for (Place place : this.placeOrder) {
			Token token = marking.getToken(place);
			assert !token.isOmega();
			long numberOfTokens = token.getValue();
			assert numberOfTokens == 0 || numberOfTokens == 1;
			ret.andWith(this.encodePlace(placeIndex, pos, numberOfTokens));
			placeIndex++;
		}
		return ret;
	}

	protected BDD encodePlace(int placeIndex, int pos, long numberOfTokens) {
		assert numberOfTokens == 0 || numberOfTokens == 1;
		return numberOfTokens == 1 ? this.encodeMarkedPlace(placeIndex, pos) : this.encodeUnmarkedPlace(placeIndex, pos);
	}

	protected BDD encodePlace(Place place, int pos, long numberOfTokens) {
		assert numberOfTokens == 0 || numberOfTokens == 1;
		return numberOfTokens == 1 ? this.encodeMarkedPlace(place, pos) : this.encodeUnmarkedPlace(place, pos);
	}

	protected BDD encodePlace(int placeIndex, int pos, boolean hasToken) {
		return hasToken ? this.encodeMarkedPlace(placeIndex, pos) : this.encodeUnmarkedPlace(placeIndex, pos);
	}

	protected BDD encodeMarkedPlace(int placeIndex, int pos) {
		return TOKENS[pos][placeIndex].ithVar(1);
	}

	protected BDD encodeUnmarkedPlace(int placeIndex, int pos) {
		return TOKENS[pos][placeIndex].ithVar(0);
	}

	protected BDD encodePlace(Place place, int pos, boolean hasToken) {
		return hasToken ? this.encodeMarkedPlace(place, pos) : this.encodeUnmarkedPlace(place, pos);
	}

	protected BDD encodeMarkedPlace(Place place, int pos) {
		return this.encodeMarkedPlace(this.placeIndex(place), pos);
	}

	protected BDD encodeUnmarkedPlace(Place place, int pos) {
		return this.encodeUnmarkedPlace(this.placeIndex(place), pos);
	}

	@Override
	public void close() {
		this.factory.done();
	}
}
