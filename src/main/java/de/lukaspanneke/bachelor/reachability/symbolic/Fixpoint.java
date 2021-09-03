package de.lukaspanneke.bachelor.reachability.symbolic;

import com.github.javabdd.BDD;
import de.lukaspanneke.bachelor.timing.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Function;

import static de.lukaspanneke.bachelor.timing.TimerIds.BDD_STATESPACE_FIXPOINT;

public class Fixpoint {
	private static final Logger LOGGER = LogManager.getLogger(Fixpoint.class);

	private final Function<BDD, BDD> iteratedFunction;
	private BDD currentDiscovered;
	private BDD currentDiscoveredUnexplored;
	private boolean isComplete = false;

	public Fixpoint(BDD start, Function<BDD, BDD> iteratedFunction) {
		this.currentDiscovered = start;
		this.currentDiscoveredUnexplored = start;
		this.iteratedFunction = iteratedFunction;
	}

	//private int depth = 0;

	public boolean contains(BDD goal) throws InterruptedException {
		if (BddUtil.contains(this.currentDiscovered, goal)) {
			LOGGER.debug("Already found gaol state before (approach does not yield a witness)");
			return true;
		} else if (this.isComplete) {
			LOGGER.debug("Fixpoint already found. No goal state present");
			return false;
		}

		LOGGER.info("Calculating fixpoint");
		Timer.global().start(BDD_STATESPACE_FIXPOINT);
		try {
			BDD lastDiscovered;
			BDD lastDiscoveredUnexplored;
			do {
				//LOGGER.trace("depth = " + depth++);
				if (Thread.interrupted()) {
					LOGGER.error("Thread was interrupted. Aborting fixpoint calculation. Still in a good state; calculation can be resumed");
					throw new InterruptedException();
				}
				lastDiscovered = this.currentDiscovered;
				lastDiscoveredUnexplored = this.currentDiscoveredUnexplored;
				this.currentDiscoveredUnexplored = this.iteratedFunction.apply(lastDiscoveredUnexplored).and(lastDiscovered.not());
				this.currentDiscovered = this.currentDiscovered.or(this.currentDiscoveredUnexplored);
				if (BddUtil.contains(lastDiscovered, goal)) {
					LOGGER.info("Found goal state (approach does not yield a witness). Pausing fixpoint calculation");
					return true;
				}
			} while (!this.currentDiscoveredUnexplored.isZero());

			this.isComplete = true;
			LOGGER.info("Fixpoint found. No goal state present");
			return false;
		} finally {
			String duration = Timer.global().stopFormat(BDD_STATESPACE_FIXPOINT);
			LOGGER.trace("Spent {} in fixpoint calculation", duration);
		}
	}

	public void containsSet(BddGoalSet goals) {
		goals.update(this.currentDiscovered);
		if (goals.isDecided()) {
			LOGGER.debug("Set is decided by already reached states");
			return;
		}

		LOGGER.info("Calculating fixpoint");
		Timer.global().start(BDD_STATESPACE_FIXPOINT);
		try {
			BDD lastDiscovered;
			BDD lastDiscoveredUnexplored;
			do {
				//LOGGER.trace("depth = " + depth++);
				if (Thread.interrupted()) {
					LOGGER.error("Thread was interrupted. Aborting fixpoint calculation. Still in a good state; calculation can be resumed");
					return;
				}
				lastDiscovered = this.currentDiscovered;
				lastDiscoveredUnexplored = this.currentDiscoveredUnexplored;
				this.currentDiscoveredUnexplored = this.iteratedFunction.apply(lastDiscoveredUnexplored).and(lastDiscovered.not());
				this.currentDiscovered = this.currentDiscovered.or(this.currentDiscoveredUnexplored);
				goals.update(lastDiscovered);
				if (goals.isDecided()) {
					LOGGER.info("Decided set. Pausing fixpoint calculation");
					return;
				}
			} while (!this.currentDiscoveredUnexplored.isZero());

			this.isComplete = true;
			LOGGER.info("Fixpoint found.");
			goals.markRemainingUnreachable();
			return;
		} finally {
			String duration = Timer.global().stopFormat(BDD_STATESPACE_FIXPOINT);
			LOGGER.trace("Spent {} in fixpoint calculation", duration);
		}
	}

	public BDD current() {
		return this.currentDiscovered;
	}

	public BDD full() throws InterruptedException {
		if (!this.isComplete) {
			this.contains(this.currentDiscovered.getFactory().zero());
		}
		return this.currentDiscovered;
	}
}