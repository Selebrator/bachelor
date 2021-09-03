package de.lukaspanneke.bachelor.reachability.solver.sparse;

import de.lukaspanneke.bachelor.help.InteruptableTask;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseIntVector;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;
import de.lukaspanneke.bachelor.reachability.AugmentedQueryResult;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import de.lukaspanneke.bachelor.reachability.reduction.structural.StructuralReduction;
import de.lukaspanneke.bachelor.reachability.solver.ReachabilitySolver;
import de.lukaspanneke.bachelor.timing.Timer;
import de.lukaspanneke.bachelor.timing.TimerIds;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class StructuralReductionPreprocessingSolver<R> implements ReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, R> {

	private static final Logger LOGGER = LogManager.getLogger(StructuralReductionPreprocessingSolver.class);

	private final List<StructuralReduction> reductions;
	private final ReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, R> solver;
	private final Duration maxReductionTime;

	public StructuralReductionPreprocessingSolver(List<StructuralReduction> reductions, ReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, R> solver, Duration maxReductionTime) {
		this.reductions = reductions;
		this.solver = solver;
		this.maxReductionTime = maxReductionTime;
	}

	@Override
	public AugmentedQueryResult<R> isReachable(SparsePetriNet net, StateFormula<SparsePetriNet, SparseIntVector, String, Integer> goal, Duration timeout) {
		Instant start = Instant.now();
		Set<String> visible = goal.support().stream()
				.collect(Collectors.toUnmodifiableSet());
		SparsePetriNet reducedNet = new SparsePetriNet(net);
		try {
			InteruptableTask.interruptAfterTimeout(Executors.callable(() -> this.reduce(reducedNet, visible)), this.maxReductionTime);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
		return this.solver.isReachable(reducedNet, goal, timeout.minus(Duration.between(start, Instant.now())));
	}

	@Override
	public AugmentedQueryResult<R> isReachable(SparsePetriNet net, StateFormula<SparsePetriNet, SparseIntVector, String, Integer> goal) {
		Set<String> visible = goal.support().stream()
				.collect(Collectors.toUnmodifiableSet());
		SparsePetriNet reducedNet = new SparsePetriNet(net);
		try {
			InteruptableTask.interruptAfterTimeout(Executors.callable(() -> this.reduce(reducedNet, visible)), this.maxReductionTime);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
		return this.solver.isReachable(reducedNet, goal);
	}

	public void reduce(SparsePetriNet net, Set<String> visible) {
		LOGGER.trace("Start applying structural reductions");
		Timer.global().start(TimerIds.STRUCTURAL_REDUCTIONS);
		//Instant start = Instant.now();
		try {
			boolean change = true;
			while (change) {
				change = false;
				for (StructuralReduction reduction : this.reductions) {
					//Duration passedTime = Duration.between(start, Instant.now());
					//if (passedTime.compareTo(this.maxReductionTime) > 0) {
					if (Thread.interrupted()) {
						LOGGER.info("Thread was interrupted. Abort application of structural reductions");
						return;
					}
					if (reduction.reduce(net, visible)) {
						change = true;
						break;
					}
				}
			}
			LOGGER.info("Done applying structural reductions");
		} finally {
			LOGGER.trace("Spent {} applying structural reductions", Timer.global().stopFormat(TimerIds.STRUCTURAL_REDUCTIONS));
		}
	}
}
