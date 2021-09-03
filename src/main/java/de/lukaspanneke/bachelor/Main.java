package de.lukaspanneke.bachelor;

import de.lukaspanneke.bachelor.help.BigIntOrInfinity;
import de.lukaspanneke.bachelor.mcc.MccReachabilityCardinalityProperty;
import de.lukaspanneke.bachelor.mcc.MccReachabilityCardinalityPropertySet;
import de.lukaspanneke.bachelor.mcc.MccTest;
import de.lukaspanneke.bachelor.parser.MccXmlReachabilityCardinalityParser;
import de.lukaspanneke.bachelor.parser.PnmlNetParser;
import de.lukaspanneke.bachelor.reachability.AugmentedQueryResult;
import de.lukaspanneke.bachelor.reachability.logic.generic.Formula;
import de.lukaspanneke.bachelor.reachability.logic.generic.FormulaBuilder;
import de.lukaspanneke.bachelor.reachability.logic.sparse.SparseFormulaBuilder;
import de.lukaspanneke.bachelor.reachability.reduction.stubborn.SparseTapaalLikeStubbornSetProvider;
import de.lukaspanneke.bachelor.reachability.solver.ParallelReachabilitySolver;
import de.lukaspanneke.bachelor.reachability.solver.ReachabilitySolver;
import de.lukaspanneke.bachelor.reachability.solver.sparse.StubbornBreadthFirstReachabilitySolver;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.parser.Parser;
import uniol.apt.io.renderer.RenderException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static de.lukaspanneke.bachelor.help.BigIntOrInfinity.POSITIVE_INFINITY;
import static de.lukaspanneke.bachelor.mcc.oracle.StateSpaceOracleParser.StateSpaceProperty.STATES;

// https://yanntm.github.io/pnmcc-models-2020/oracle.tar.gz
public class Main {

	public static void main(String[] args) throws ParseException, IOException, InterruptedException, RenderException {
		System.out.println("> enter program");
		Path mcc = Path.of("/home/lukas/mcc/pt-reachability-2020");

		/* AirplaneLD-PT-4000 takes 45 minutes to parse into apt */
		String netDirectory = "Philosophers-PT-000050";
		int propertyIndex = 8;

		//var net = PnmlNetParser.apt().parse(Files.newInputStream(mcc.resolve(netDirectory).resolve("model.pnml")));
		//String pn = new PnStringRenderer().render(net);
		//System.out.println(pn);
		//PetriNet parsedNet = new PnStringParser().parse(new ByteArrayInputStream(pn.getBytes()));
		//System.out.println(net.getTransitions().stream().map(Node::getId).collect(Collectors.toSet())
		//		.equals(parsedNet.getTransitions().stream().map(Node::getId).collect(Collectors.toSet())));
		//System.out.println(net.getPlaces().stream().map(Node::getId).collect(Collectors.toSet())
		//		.equals(parsedNet.getPlaces().stream().map(Node::getId).collect(Collectors.toSet())));
		//System.out.println(net.getEdges().stream().map(Flow::toString).collect(Collectors.toSet())
		//		.equals(parsedNet.getEdges().stream().map(Flow::toString).collect(Collectors.toSet())));
		//System.out.println("places: " + net.getPlaceCount() + " transitions: " + net.getTransitionCount());
		//System.out.println(new ProbabilisticIncompleteDepthFirstReachabilitySolver().isReachable(net, StateFormula.bottom()));

		//bulk(mcc.resolve(netDirectory), PnmlNetParser.sparse(), net -> new SparseFormulaBuilder(), net -> new ExperimentSolver());
		single(mcc.resolve(netDirectory), propertyIndex, PnmlNetParser.sparse(), net -> new SparseFormulaBuilder(), net -> new StubbornBreadthFirstReachabilitySolver(SparseTapaalLikeStubbornSetProvider.INSTANCE));
	}

	public static Stream<MccTest> sortByStateSpaceSize(Stream<MccTest> inputs) {
		record MccInputWithSize(MccTest input, BigIntOrInfinity size) {
		}
		return inputs
				.map(input -> new MccInputWithSize(input, input.stateSpaceProperties().getOrDefault(STATES, POSITIVE_INFINITY)))
				.sorted(Comparator.comparing(MccInputWithSize::size))
				.map(MccInputWithSize::input);
	}

	private static <N, M, P, T, R> void
	single(
			Path inputDir,
			int propertyIndex,
			Parser<N> netParser,
			Function<N, FormulaBuilder<N, M, P, T>> formulaBuilder,
			Function<N, ReachabilitySolver<N, M, P, T, R>> solver
	) throws IOException, ParseException {
		System.out.println("> parse net");
		N net = netParser.parse(Files.newInputStream(inputDir.resolve("model.pnml")));
		System.out.println("> parse property");
		MccReachabilityCardinalityProperty<N, M, P, T> property = new MccXmlReachabilityCardinalityParser<>(formulaBuilder.apply(net))
				.parse(Files.newInputStream(inputDir.resolve("ReachabilityCardinality.xml")))
				.get(propertyIndex);
		single(net, property, solver.apply(net));
	}

	private static <N, M, P, T, R> void
	single(N net, MccReachabilityCardinalityProperty<N, M, P, T> property, ReachabilitySolver<N, M, P, T, R> solver) {
		System.out.println("> test property");
		AugmentedQueryResult<R> result = property.formula().test(net, solver);
		System.out.println(property.id() + ": " + result);
	}

	private static <N, M, P, T, R> void
	bulk(
			Path inputDir,
			Parser<N> netParser,
			Function<N, FormulaBuilder<N, M, P, T>> formulaBuilder,
			Function<N, ParallelReachabilitySolver<N, M, P, T, R>> solver
	) throws IOException, ParseException {
		N net = netParser.parse(Files.newInputStream(inputDir.resolve("model.pnml")));
		MccReachabilityCardinalityPropertySet<N, M, P, T> properties = new MccXmlReachabilityCardinalityParser<>(formulaBuilder.apply(net))
				.parse(Files.newInputStream(inputDir.resolve("ReachabilityCardinality.xml")));
		bulk(net, properties, solver.apply(net));
	}

	private static <N, M, P, T, R> void
	bulk(N net, MccReachabilityCardinalityPropertySet<N, M, P, T> properties, ParallelReachabilitySolver<N, M, P, T, R> solver) {
		List<AugmentedQueryResult<R>> results = Formula.testParallel(properties.stream().map(MccReachabilityCardinalityProperty::formula).toList(), net, solver, Duration.ofDays(1));
		for (int i = 0; i < results.size(); i++) {
			System.out.println(properties.get(i).id() + ": " + results.get(i));
		}
	}
}
