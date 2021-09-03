package de.lukaspanneke.bachelor;

import com.github.javabdd.BDD;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import de.lukaspanneke.bachelor.help.BigIntOrInfinity;
import de.lukaspanneke.bachelor.mcc.MccReachabilityCardinalityProperty;
import de.lukaspanneke.bachelor.mcc.MccTest;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseIntVector;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import de.lukaspanneke.bachelor.reachability.symbolic.BddSparseSolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static de.lukaspanneke.bachelor.help.BigIntOrInfinity.POSITIVE_INFINITY;

public class Analysis {

	private static final Path dir = Path.of("/home/lukas/mcc/pt-reachability-2020");

	public static Stream<MccTest> all() {
		try {
			return Files.list(dir)
					.map(MccTest::new);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Stream<MccTest> sortByStateSpaceSize(Stream<MccTest> inputs) {
		record MccInputWithSize(MccTest input, BigIntOrInfinity size) { }
		return inputs
				.map(input -> new MccInputWithSize(input, input.getNumberOfStates().orElse(POSITIVE_INFINITY)))
				.sorted(Comparator.comparing(MccInputWithSize::size))
				.map(MccInputWithSize::input);
	}
	public static void main(String[] args) throws IOException {
		//findTrivial();
		Path store = Path.of("/home/lukas/trivial-safe.txt");
		List<Integer> all = IntStream.range(0, 16).boxed().toList();
		Map<String, List<Integer>> nonTrivial = Files.lines(store)
				.map(line -> {
					String[] split = line.split(": ");
					String name = split[0];
					String trivial = split[1];
					String commaSeparatedInts = trivial.replace("[", "").replace("]", "").replace(" ", "");
					List<Integer> trivialInts = Arrays.stream(commaSeparatedInts.split(","))
							.map(Integer::parseInt)
							.toList();
					ArrayList<Integer> nonTrivialInts = new ArrayList<>(all);
					nonTrivialInts.removeAll(trivialInts);
					return Map.entry(name, nonTrivialInts);
				})
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		List<Map.Entry<String, List<Integer>>> nonTirvialSorted = sortByStateSpaceSize(all().filter(input -> input.isSafe().orElse(false)))
				.map(input -> Map.entry(input.name(), nonTrivial.getOrDefault(input.name(), all)))
				.toList();
		for (Map.Entry<String, List<Integer>> entry : nonTirvialSorted) {
			System.out.println("--net \"" + entry.getKey() + "\" --formulas \"" + entry.getValue().stream().map(Object::toString).collect(Collectors.joining(",")) + "\"");
		}
		System.out.println(nonTirvialSorted.stream().map(Map.Entry::getValue).mapToInt(List::size).summaryStatistics());
	}



	public static void findTrivial() throws IOException {
		Path store = Path.of("/home/lukas/trivial-safe");
		Set<String> doneNets = Files.lines(store)
				.map(line -> line.split(": ")[0])
				.collect(Collectors.toSet());
		Path mcc = Path.of("/home/lukas/mcc/pt-reachability-2020");
		Set<MccTest> safe = Files.list(mcc)
				.map(MccTest::new)
				.filter(mccTest -> !doneNets.contains(mccTest.name()))
				.filter(mccTest -> !mccTest.name().startsWith("AirplaneLD-PT-2000")) // they take forever to parse
				.filter(mccTest -> !mccTest.name().startsWith("Sudoku-PT-AN16")) // stackoverflow with bdd
				.filter(mccTest -> !mccTest.name().startsWith("NeoElection-PT-7")) // stackoverflow with bdd
				.filter(mccTest -> !mccTest.name().startsWith("RwMutex-PT-r2000w0010")) // stackoverflow with bdd
				.filter(mccTest -> mccTest.isSafe().orElse(false))
				.collect(Collectors.toSet());
		System.out.println(Sets.difference(safe, doneNets));
		System.exit(0);
		for (MccTest input : safe) {
			List<Integer> trivial;
			try {
				trivial = trivial(input);
			} catch (Throwable e) {
				System.out.println("! ERROR !");
				System.out.println("Unexpected error on " + input.name());
				e.printStackTrace();
				continue;
			}
			Files.write(
					store,
					List.of(input.name() + ": " + trivial),
					StandardOpenOption.APPEND);
		}
	}

	@SuppressWarnings("UnstableApiUsage")
	private static List<Integer> trivial(MccTest input) {
		System.out.println("Analysing "+ input.name());
			TimeLimiter limiter = SimpleTimeLimiter.create(Executors.newSingleThreadExecutor());
			try {
				limiter.callWithTimeout(input::sparseModel, 1, TimeUnit.MINUTES);
			} catch (TimeoutException | InterruptedException | ExecutionException e) {
				System.out.println("! ERROR !");
				System.out.println("Error on " + input.name());
				e.printStackTrace();
				return List.of();
			}
			SparsePetriNet net = input.sparseModel();
			var properties = input.reachabilityCardinalityPropertiesSparse();
			List<Integer> trivial = new ArrayList<>(16);
			BddSparseSolver bdd = new BddSparseSolver(net);
			for (int i = 0; i < properties.size(); i++) {
				MccReachabilityCardinalityProperty<SparsePetriNet, SparseIntVector, String, Integer> property = properties.get(i);
				var goal = property.formula().asGoal();
				if (goal.test(net, net.getInitialMarking())) {
					trivial.add(i);
				} else {
					try {
						if (limiter.callWithTimeout(() -> {
							BDD bddGoal = bdd.encoder.encodeFormula(goal, 0);
							return bddGoal.isZero() || bddGoal.isOne();
						}, 10, TimeUnit.SECONDS)) {
							trivial.add(i);
						}
					} catch (TimeoutException | InterruptedException | ExecutionException e) {
						System.out.println("! ERROR !");
						System.out.println("Error on " + input.name() + " #" + i);
						e.printStackTrace();
					}

				}
			}
			return trivial;
	}
}
