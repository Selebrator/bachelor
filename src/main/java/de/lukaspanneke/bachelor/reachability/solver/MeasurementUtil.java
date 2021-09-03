package de.lukaspanneke.bachelor.reachability.solver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;

public class MeasurementUtil {
	private static final Logger LOGGER = LogManager.getLogger(MeasurementUtil.class);

	private static final Duration ONE_SECOND = Duration.ofSeconds(1);

	private Instant startTime = null;
	private Instant lastTime = null;
	private int nextMeasure = 1000;
	private int lastMeasure = 0;

	public void start() {
//		this.startTime = Instant.now();
//		this.lastTime = this.startTime;
	}

	public void onNewMarking(long currentDiscoveredSize) {
//		if (currentDiscoveredSize == nextMeasure) {
//			Instant now = Instant.now();
//			Duration duration = Duration.between(lastTime, now);
//			lastTime = now;
//			Duration timePerMarking = duration.dividedBy(nextMeasure - lastMeasure);
//			long markingsPerSecond = ONE_SECOND.dividedBy(timePerMarking);
//			lastMeasure = nextMeasure;
//			nextMeasure += markingsPerSecond;
//
//			LOGGER.trace("Visited " + currentDiscoveredSize + " markings. Current speed: " + markingsPerSecond + " markings per second");
//		}
	}
}
