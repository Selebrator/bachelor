package de.lukaspanneke.bachelor.timing;

import de.lukaspanneke.bachelor.PrintUtil;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class Timer {

	private static final Timer global = new Timer();

	private Timer() {

	}

	private final Map<String, Duration> totals = new HashMap<>();
	private final Map<String, Instant> sectionStart = new HashMap<>();

	public static Timer global() {
		return global;
	}

	public void start(String id) {
		this.sectionStart.putIfAbsent(id, Instant.now());
	}

	public Duration stop(String id) {
		Duration duration = this.getDurationInCurrentSession(id);
		this.totals.put(id, this.totals.getOrDefault(id, Duration.ZERO).plus(duration));
		this.sectionStart.remove(id);
		return duration;
	}

	public String stopFormat(String id) {
		return PrintUtil.formatDuration(this.stop(id));
	}

	public Duration getDurationInCurrentSession(String id) {
		if (this.sectionStart.containsKey(id)) {
			return Duration.between(this.sectionStart.get(id), Instant.now());
		} else {
			return Duration.ZERO;
		}
	}

	public Duration getTotal(String id) {
		return this.totals.getOrDefault(id, Duration.ZERO).plus(this.getDurationInCurrentSession(id));
	}

	public String formatTotal(String id) {
		return PrintUtil.formatDuration(this.getTotal(id));
	}

	public String formatTotals() {
		return this.totals.keySet().stream()
				.map(id -> PrintUtil.formatDuration(this.getTotal(id)))
				.collect(Collectors.joining("\n"));
	}

	public void reset(String id) {
		this.sectionStart.remove(id);
		this.totals.remove(id);
	}

	public void reset() {
		this.sectionStart.clear();
		this.totals.clear();
	}

	public Measure measure(String id) {
		this.start(id);
		return new Measure() {
			@Override
			public void close() {
				stop(id);
			}
		};
	}

	public static abstract class Measure implements AutoCloseable {
		@Override
		public abstract void close();
	}
}
