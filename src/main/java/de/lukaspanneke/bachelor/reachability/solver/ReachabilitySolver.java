package de.lukaspanneke.bachelor.reachability.solver;

import com.google.common.util.concurrent.UncheckedExecutionException;
import de.lukaspanneke.bachelor.help.InteruptableTask;
import de.lukaspanneke.bachelor.reachability.AugmentedQueryResult;
import de.lukaspanneke.bachelor.reachability.QueryResult;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import de.lukaspanneke.bachelor.timing.Timer;
import de.lukaspanneke.bachelor.timing.TimerIds;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;

public interface ReachabilitySolver<N, M, P, T, R> {

	Logger LOGGER = LogManager.getLogger(ReachabilitySolver.class);

	default AugmentedQueryResult<R> isReachable(N net, StateFormula<N, M, P, T> goal, Duration timeout) {
		try {
			return InteruptableTask.interruptAfterTimeout(() -> this.isReachable(net, goal), timeout);
		} catch (ExecutionException e) {
			throw new UncheckedExecutionException(e);
		}
	}

	AugmentedQueryResult<R> isReachable(N net, StateFormula<N, M, P, T> goal);

//	@SuppressWarnings("UnstableApiUsage")
//	default List<AugmentedQueryResult<R>> isReachableSequential(N net, List<? extends StateFormula<N, M, P, T>> goals, Duration timeoutPerFormula) {
//		@SuppressWarnings("unchecked") List<AugmentedQueryResult<R>> results = Arrays.asList(new AugmentedQueryResult[goals.size()]);
//		for (int i = 0; i < goals.size(); i++) {
//			results.set(i, new AugmentedQueryResult<>(QueryResult.DID_NOT_FINISH, null));
//		}
//		ExecutorService executorService = Executors.newSingleThreadExecutor();
//		try {
//			TimeLimiter limiter = SimpleTimeLimiter.create(executorService);
//			for (int i = 0; i < goals.size(); i++) {
//				LOGGER.info("Start processing formula #{}", i);
//				StateFormula<N, M, P, T> goal = goals.get(i);
//				try {
//					AugmentedBoolean<R> reachable = limiter.callWithTimeout(() -> this.isReachable(net, goal, timeoutPerFormula), timeoutPerFormula);
//					results.set(i, new AugmentedQueryResult<>(reachable.bool() ? QueryResult.SATISFIED : QueryResult.UNSATISFIED, reachable.detail()));
//				} catch (TimeoutException e) {
//					results.set(i, new AugmentedQueryResult<>(QueryResult.DID_NOT_FINISH, null));
//				} catch (ExecutionException e) {
//					results.set(i, new AugmentedQueryResult<>(QueryResult.ERROR, e));
//				} catch (InterruptedException e) {
//					return results;
//				}
//			}
//		} finally {
//			executorService.shutdown();
//		}
//		return results;
//	}

	default List<AugmentedQueryResult<R>> isReachableSequential(N net, List<? extends StateFormula<N, M, P, T>> goals, Duration timeoutPerFormula) {
		@SuppressWarnings("unchecked") List<AugmentedQueryResult<R>> results = Arrays.asList(new AugmentedQueryResult[goals.size()]);
		for (int i = 0; i < goals.size(); i++) {
			results.set(i, new AugmentedQueryResult<>(QueryResult.DID_NOT_FINISH, null));
		}
		for (int i = 0; i < goals.size(); i++) {
			LOGGER.info("Start processing formula #{}", i);
			StateFormula<N, M, P, T> goal = goals.get(i);
			Timer.global().start(TimerIds.SEQUENTIAL_INDIVIDUAL);
			try {
				AugmentedQueryResult<R> reachable = this.isReachable(net, goal, timeoutPerFormula);
				results.set(i, reachable);
			} finally {
				LOGGER.trace("Spent {} on formula #{}", Timer.global().stopFormat(TimerIds.SEQUENTIAL_INDIVIDUAL), i);
			}
		}
		return results;
	}

}
