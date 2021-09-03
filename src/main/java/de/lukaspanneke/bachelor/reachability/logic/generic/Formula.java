package de.lukaspanneke.bachelor.reachability.logic.generic;

import de.lukaspanneke.bachelor.Enumerate;
import de.lukaspanneke.bachelor.IndexedItem;
import de.lukaspanneke.bachelor.help.InteruptableTask;
import de.lukaspanneke.bachelor.reachability.AugmentedQueryResult;
import de.lukaspanneke.bachelor.reachability.QueryResult;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import de.lukaspanneke.bachelor.reachability.solver.ParallelReachabilitySolver;
import de.lukaspanneke.bachelor.reachability.solver.ReachabilitySolver;
import de.lukaspanneke.bachelor.timing.Timer;
import de.lukaspanneke.bachelor.timing.TimerIds;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public abstract class Formula<N, M, P, T> {
	private static final Logger LOGGER = LogManager.getLogger(Formula.class);

	private final StateFormula<N, M, P, T> formula;

	public Formula(StateFormula<N, M, P, T> formula) {
		this.formula = formula;
	}

	public StateFormula<N, M, P, T> formula() {
		return formula;
	}

	public <R> AugmentedQueryResult<R> test(N net, ReachabilitySolver<N, M, P, T, R> solver) {
		try {
			StateFormula<N, M, P, T> goal = this.asGoal();
			return this.interpretGoalResult(solver.isReachable(net, goal));
		} catch (UnsupportedOperationException e) {
			return new AugmentedQueryResult<>(QueryResult.UNSUPPORTED, null);
		} catch (Exception e) {
			e.printStackTrace();
			return new AugmentedQueryResult<>(QueryResult.ERROR, null);
		}
	}

	public static <R, N, M, P, T> List<AugmentedQueryResult<R>> testSequential(List<Formula<N, M, P, T>> formulas, N net, ReachabilitySolver<N, M, P, T, R> solver, Duration timeoutPerFormula) {
		Timer.global().start(TimerIds.BULK_TOTAL);
		try {
			return testMany(formulas, goals -> solver.isReachableSequential(net, goals, timeoutPerFormula));
		} finally {
			LOGGER.trace("Spent {} on {} formulas", Timer.global().stopFormat(TimerIds.BULK_TOTAL), formulas.size());
		}
	}

	public static <R, N, M, P, T> List<AugmentedQueryResult<R>> testParallel(List<Formula<N, M, P, T>> formulas, N net, ParallelReachabilitySolver<N, M, P, T, R> solver, Duration timeoutPerFormula) {
		Timer.global().start(TimerIds.BULK_TOTAL);
		try {
			return InteruptableTask.interruptAfterTimeout(() -> testMany(formulas, goals -> solver.isReachableParallel(net, goals)), timeoutPerFormula.multipliedBy(formulas.size()));
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		} finally {
			LOGGER.trace("Spent {} on {} formulas", Timer.global().stopFormat(TimerIds.BULK_TOTAL), formulas.size());
		}
	}

	public static <R, N, M, P, T> List<AugmentedQueryResult<R>> testMany(List<Formula<N, M, P, T>> formulas, Function<List<StateFormula<N, M, P, T>>, List<AugmentedQueryResult<R>>> solver) {
		List<StateFormula<N, M, P, T>> goals = formulas.stream()
				.map(Formula::asGoal)
				.toList();
		List<AugmentedQueryResult<R>> results = solver.apply(goals);
		List<AugmentedQueryResult<R>> ret = new ArrayList<>();
		for (IndexedItem<Formula<N, M, P, T>> formula : Enumerate.enumerate(formulas)) {
			ret.add(formula.index(), formula.item().interpretGoalResult(results.get(formula.index())));
		}
		return Collections.unmodifiableList(ret);
	}

	public static <N, M, P, T> Goal<N, M, P, T> existsFinally(StateFormula<N, M, P, T> formula) {
		return new Goal<>(formula);
	}

	public static <N, M, P, T> Invariant<N, M, P, T> allGlobally(StateFormula<N, M, P, T> formula) {
		return new Invariant<>(formula);
	}

	public abstract StateFormula<N, M, P, T> asGoal();

	public abstract <R> AugmentedQueryResult<R> interpretGoalResult(AugmentedQueryResult<R> original);
}
