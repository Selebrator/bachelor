import com.github.javabdd.BDD;
import de.lukaspanneke.bachelor.Benchmark;
import de.lukaspanneke.bachelor.help.BigIntOrInfinity;
import de.lukaspanneke.bachelor.mcc.MccReachabilityCardinalityProperty;
import de.lukaspanneke.bachelor.mcc.MccReachabilityCardinalityPropertySet;
import de.lukaspanneke.bachelor.mcc.MccTest;
import de.lukaspanneke.bachelor.mcc.oracle.StateSpaceOracleParser;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseIntVector;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseNetUtil;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;
import de.lukaspanneke.bachelor.reachability.AugmentedQueryResult;
import de.lukaspanneke.bachelor.reachability.QueryResult;
import de.lukaspanneke.bachelor.reachability.logic.generic.Formula;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import de.lukaspanneke.bachelor.reachability.reachabilitygraph.BreadthFirstReachabilityGraphBuilder;
import de.lukaspanneke.bachelor.reachability.reachabilitygraph.BreadthFirstReachabilitySetBuilder;
import de.lukaspanneke.bachelor.reachability.reachabilitygraph.DepthFirstReachabilityGraphBuilder;
import de.lukaspanneke.bachelor.reachability.reachabilitygraph.DepthFirstReachabilitySetBuilder;
import de.lukaspanneke.bachelor.reachability.reduction.structural.StructuralReduction;
import de.lukaspanneke.bachelor.reachability.reduction.structural.TapaalB_FusionOfSequentialTransitions;
import de.lukaspanneke.bachelor.reachability.reduction.stubborn.TapaalLikeStubbornSetProvider;
import de.lukaspanneke.bachelor.reachability.solver.GenericSolver;
import de.lukaspanneke.bachelor.reachability.solver.NetAccessor;
import de.lukaspanneke.bachelor.reachability.solver.ParallelReachabilitySolver;
import de.lukaspanneke.bachelor.reachability.solver.ReachabilitySolver;
import de.lukaspanneke.bachelor.reachability.solver.apt.AptBddReachabilitySolver;
import de.lukaspanneke.bachelor.reachability.solver.apt.AptDynamicBreadthFirstParallelBulkReachabilitySolver;
import de.lukaspanneke.bachelor.reachability.solver.apt.AptDynamicBreadthFirstReachabilityGraphSolver;
import de.lukaspanneke.bachelor.reachability.solver.apt.AptDynamicBreadthFirstReachabilitySetSolver;
import de.lukaspanneke.bachelor.reachability.solver.apt.AptFullReachabilityGraphSolver;
import de.lukaspanneke.bachelor.reachability.solver.apt.AptFullReachabilitySetSolver;
import de.lukaspanneke.bachelor.reachability.solver.apt.AptStructuralReductionPreprocessingSolver;
import de.lukaspanneke.bachelor.reachability.solver.apt.AptStubbornReachabilitySolver;
import de.lukaspanneke.bachelor.reachability.solver.result.AptWitnessPathAndNumberOfVisitedMarkings;
import de.lukaspanneke.bachelor.reachability.solver.result.GenericWitnessMarkingAndNumberOfVisitedMarkings;
import de.lukaspanneke.bachelor.reachability.solver.result.SparseWitnessMarkingAndNumberOfVisitedMarkings;
import de.lukaspanneke.bachelor.reachability.symbolic.BddAptSolver;
import de.lukaspanneke.bachelor.reachability.symbolic.BddSparseSolver;
import de.lukaspanneke.bachelor.reachability.symbolic.BddUtil;
import mccdata.mcc2020.MccInputs;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.IncompleteExecutionException;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.parser.impl.PnmlPNParser;
import uniol.apt.io.renderer.RenderException;
import uniol.apt.io.renderer.impl.AptPNRenderer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static de.lukaspanneke.bachelor.reachability.QueryResult.DID_NOT_FINISH;
import static de.lukaspanneke.bachelor.reachability.QueryResult.ERROR;
import static de.lukaspanneke.bachelor.reachability.QueryResult.SATISFIED;
import static de.lukaspanneke.bachelor.reachability.QueryResult.UNSATISFIED;
import static de.lukaspanneke.bachelor.reachability.QueryResult.UNSUPPORTED;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class Mcc {

	private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(Mcc.class);

	private static final Path mccDir = Path.of("/home/lukas/mcc/pt-reachability-2020");

	private static final List<StructuralReduction> reductions = List.of(
			//(net, visible) -> false
			//new TapaalG_DiscardRedundantTransition()
			//, new TapaalH_DiscardSimplifyCycle()
			//, new Its4_DiscardNeutralTransition()
			new TapaalB_FusionOfSequentialTransitions()
			//, new Its9_DiscardConstantPlace()
			//, new TapaalF_DiscardRedundantPlace()
	);

	private static final AptFullReachabilitySetSolver fullSetDepthFirstSolver = new AptFullReachabilitySetSolver(new DepthFirstReachabilitySetBuilder());
	private static final AptFullReachabilitySetSolver fullSetBreadthFirstSolver = new AptFullReachabilitySetSolver(new BreadthFirstReachabilitySetBuilder());
	private static final AptFullReachabilityGraphSolver fullGraphDepthFirstSolver = new AptFullReachabilityGraphSolver(new DepthFirstReachabilityGraphBuilder());
	private static final AptFullReachabilityGraphSolver fullGraphBreadthFirstSolver = new AptFullReachabilityGraphSolver(new BreadthFirstReachabilityGraphBuilder());
	private static final AptDynamicBreadthFirstReachabilitySetSolver dynamicSetBreadthFirstSolver = new AptDynamicBreadthFirstReachabilitySetSolver();
	private static final AptDynamicBreadthFirstParallelBulkReachabilitySolver bulk = new AptDynamicBreadthFirstParallelBulkReachabilitySolver();
	private static final AptDynamicBreadthFirstReachabilityGraphSolver dynamicGraphBreadthFirstSolver = new AptDynamicBreadthFirstReachabilityGraphSolver();
	private static final AptStubbornReachabilitySolver stubbornSolver = new AptStubbornReachabilitySolver(new TapaalLikeStubbornSetProvider());
	private static final AptStructuralReductionPreprocessingSolver<AptWitnessPathAndNumberOfVisitedMarkings> structuralAndCorrect = new AptStructuralReductionPreprocessingSolver<>(reductions, dynamicGraphBreadthFirstSolver);
	private static final AptStructuralReductionPreprocessingSolver<AptWitnessPathAndNumberOfVisitedMarkings> structuralAndStubborn = new AptStructuralReductionPreprocessingSolver<>(reductions, stubbornSolver);
	private static final AptBddReachabilitySolver aptBddSolver = new AptBddReachabilitySolver();
	private static final ReachabilitySolver<PetriNet, Marking, Place, Transition, GenericWitnessMarkingAndNumberOfVisitedMarkings<Marking>> genericAptStubborn = new GenericSolver<>(NetAccessor.apt(), new TapaalLikeStubbornSetProvider());

	private static final ReachabilitySolver<PetriNet, Marking, Place, Transition, ?> apt = bulk;

	private static final ReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, SparseWitnessMarkingAndNumberOfVisitedMarkings> base = Benchmark.Solvers.base();
	private static final ReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, SparseWitnessMarkingAndNumberOfVisitedMarkings> stubborn = Benchmark.Solvers.stubborn();
	private static final ReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, Void> bdd = Benchmark.Solvers.bdd();
	private static final ReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, SparseWitnessMarkingAndNumberOfVisitedMarkings> probabilistic = Benchmark.Solvers.probabilistic();
	private static final ReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, Void> structBdd = Benchmark.Solvers.structBdd(Duration.ofSeconds(5));
	private static final ReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, SparseWitnessMarkingAndNumberOfVisitedMarkings> structProbabilistic = Benchmark.Solvers.structProbabilistic(Duration.ofSeconds(5));

	private static final ReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, ?> sparse = Benchmark.Solvers.struct(Duration.ofSeconds(4));

	public static Stream<MccTest> inputs() {
		return MccInputs.sortByStateSpaceSize(
				MccInputs.all()
						.filter(mccTest -> !mccTest.name().startsWith("AirplaneLD-PT")) // they take forever to parse
						.filter(mccTest -> mccTest.isSafe().orElse(false))
		)
				//.filter(input -> {
				//	BigIntOrInfinity size = input.getNumberOfStates().orElse(BigIntOrInfinity.POSITIVE_INFINITY);
				//	return size.isFinite() && size.getFiniteValue().compareTo(BigInteger.valueOf(1_000_000_000)) < 0;
				//})
				;
	}

	private static void renderNet(String path) throws IOException, ParseException, RenderException {
		System.out.println(new AptPNRenderer().render(new PnmlPNParser().parse(Files.newInputStream(mccDir.resolve(path).resolve("model.pnml")))));
	}

	public static Stream<Arguments> all() {
		return inputs()
				.map(input -> arguments(input, sparse));
	}

	public static Stream<Arguments> allIndividually() {
		return inputs()
				.flatMap(input -> IntStream.range(0, input.reachabilityCardinalityPropertiesSparse().size())
						.mapToObj(propertyIndex -> arguments(input, propertyIndex, sparse)))
				;
	}

	public static Stream<Arguments> oneRandom() {
		return inputs()
				.flatMap(input -> Stream.of(arguments(input, 3, sparse)))
				;
	}

	@Test
	public void listAll() {
		System.out.println(inputs().count());
	}

	@ParameterizedTest
	@MethodSource("specific")
	@Timeout(20)
	public void testSize(MccTest input) throws InterruptedException {
		BigIntOrInfinity expected = input.stateSpaceProperties().getOrDefault(StateSpaceOracleParser.StateSpaceProperty.STATES, null);
		assumeTrue(expected != null);
		assumeTrue(expected.isFinite());
		BddAptSolver bdd = new BddAptSolver(input.model());
		BDD stateSpace = bdd.stateSpace().full();
		double actual = BddUtil.size(stateSpace, bdd.getPredecessorVariables());
		assertEquals(expected.getFiniteValue().longValueExact(), actual);
	}

	@Test
	public void getSize() {
		MccInputs.all()
				.forEach(
						input -> {
							PetriNet model = input.model();
							int p = model.getPlaces().size();
							int t = model.getTransitions().size();
							System.out.println(p + " * " + t + " = " + p * t + " in " + input.name());
						}
				);
	}

//	@ParameterizedTest
//	@MethodSource("all")
//	@Timeout(20)
//	public void testBulk(MccTest input, ReachabilitySolver<PetriNet, StateFormula<PetriNet, Marking, Place, Transition>, ?> solver) {
//		LOGGER.info("## {}", input);
//		MccReachabilityCardinalityPropertySet<PetriNet, Marking, Place, Transition> properties;
//		try {
//			properties = input.reachabilityCardinalityPropertiesApt();
//		} catch (Exception e) {
//			LOGGER.error("Aborting: " + e.getMessage());
//			e.printStackTrace();
//			assumeTrue(false);
//			return;
//		}
//		List<Formula<PetriNet, Marking, Place, Transition>> formulas = properties.stream()
//				.map(MccReachabilityCardinalityProperty::formula)
//				.collect(Collectors.toList());
//		List<? extends AugmentedQueryResult<?>> reachable = Formula.test(formulas, input.model(), solver);
//		List<GenericStateFormula<PetriNet, Marking, Place, Transition>> queries = properties.properties().stream()
//				.map(property -> property.formula().asGoal())
//				.toList();
//		//List<? extends AugmentedBoolean<?>> reachable = solver.isReachable(input.model(), queries);
//		for (var result : enumerate(reachable)) {
//			MccReachabilityCardinalityProperty<PetriNet, Marking, Place, Transition> property = input.reachabilityCardinalityPropertiesApt().properties().get(result.index());
//
//			StringBuilder sb = new StringBuilder().append(property.id()).append(" ").append(result.item().result());
//			if (result.item().detail() != null) {
//				sb.append("  ").append(result.item().detail());
//			}
//			System.out.println(sb);
//		}
//		String actual = IntStream.range(0, properties.properties().size())
//				.mapToObj(i -> reachable.get(i).result())
//				.map(qr -> switch (qr) {
//					case SATISFIED -> "T";
//					case UNSATISFIED -> "X";
//					case UNSUPPORTED, DID_NOT_FINISH, ERROR -> "?";
//				})
//				.collect(Collectors.joining());
//
//		String expected = properties.properties().stream()
//				.map(property -> {
//					Optional<Boolean> expectedResult = property.getExpectedResult();
//					assumeTrue(expectedResult.isPresent(), property + " has no expected result");
//					return expectedResult.get();
//				})
//				.map(b -> b ? "T" : "X")
//				.collect(Collectors.joining());
//		assertEquals(expected, actual);
//	}

	@ParameterizedTest
	@MethodSource("all")
	public void testBulkSparse(MccTest input, ParallelReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, ?> solver) throws InterruptedException {
		LOGGER.info("# Begin bulk test on {}", input.name());
		testBulk(input, input.sparseModel(), input.reachabilityCardinalityPropertiesSparse(), solver);
	}

	@ParameterizedTest
	@MethodSource("all")
	@Timeout(20)
	public void testBulkApt(MccTest input, ParallelReachabilitySolver<PetriNet, Marking, Place, Transition, ?> solver) throws InterruptedException {
		LOGGER.info("# Begin bulk test on {}", input.name());
		testBulk(input, input.model(), input.reachabilityCardinalityPropertiesApt(), solver);
	}

	private <N, M, P, T, R> void
	testBulk(
			MccTest input,
			N model,
			MccReachabilityCardinalityPropertySet<N, M, P, T> properties,
			ParallelReachabilitySolver<N, M, P, T, R> solver
	) throws InterruptedException {
		List<Formula<N, M, P, T>> formulas = properties.stream()
				.map(MccReachabilityCardinalityProperty::formula)
				.collect(Collectors.toList());
		List<AugmentedQueryResult<R>> reachable = Formula.testParallel(formulas, model, solver, Duration.ofSeconds(1));
		List<Boolean> expectedResults = properties.stream()
				.map(MccReachabilityCardinalityProperty::id)
				.map(input.reachabilityCardinalityExpectedResults()::get)
				.collect(Collectors.toList());
		for (Boolean expectedResult : expectedResults) {
			assumeTrue(expectedResult != null, "no expected result");
		}

		int incorrect = 0;
		for (int i = 0; i < reachable.size(); i++) {
			QueryResult expected = expectedResults.get(i) ? SATISFIED : UNSATISFIED;
			QueryResult actual = reachable.get(i).result();
			if (actual != DID_NOT_FINISH) {
				if (!expected.equals(actual)) {
					LOGGER.error("Incorrect answer for property {}. Expected {}, but got {}", properties.get(i).id(), expected, actual);
					incorrect++;
				} else {
					LOGGER.debug("Got correct answer {} for property {}", actual, properties.get(i).id());
				}
			} else {
				LOGGER.debug("Got {} for property {}", actual, properties.get(i).id());
			}
		}
		assertEquals(0, incorrect);

		for (AugmentedQueryResult<R> result : reachable) {
			if (result.result().equals(DID_NOT_FINISH)) {
				throw new IncompleteExecutionException("Did not finish");
			}
		}
	}

	@ParameterizedTest
	@MethodSource("oneRandom")
	@Timeout(5)
	public void testIndividuallyApt(MccTest input, int propertyIndex, ReachabilitySolver<PetriNet, Marking, Place, Transition, ?> solver) throws InterruptedException {
		LOGGER.info("# Begin test on {} with formula #{}", input.name(), propertyIndex);
		testIndividually(input, input.model(), input.reachabilityCardinalityPropertiesApt().get(propertyIndex), solver);
	}

	@ParameterizedTest
	@MethodSource("specific")
	@Timeout(10)
	public void testIndividuallySparse(MccTest input, int propertyIndex, ReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, ?> solver) throws InterruptedException {
		LOGGER.info("# Begin test on {} with formula #{}", input.name(), propertyIndex);
		testIndividually(input, input.sparseModel(), input.reachabilityCardinalityPropertiesSparse().get(propertyIndex), solver);
	}

	private <N, M, P, T, R> void
	testIndividually(
			MccTest input,
			N model,
			MccReachabilityCardinalityProperty<N, M, P, T> property,
			ReachabilitySolver<N, M, P, T, R> solver
	) throws InterruptedException {
		LOGGER.info("Checking property {}", property);

		Boolean expectedResult = input.reachabilityCardinalityExpectedResults().get(property.id());
		assumeTrue(expectedResult != null, "no expected result");
		QueryResult expected = expectedResult ? SATISFIED : UNSATISFIED;

		AugmentedQueryResult<R> result = property.formula().test(model, solver);
		LOGGER.info(result);

		assumeFalse(result.result() == UNSUPPORTED, "unsupported");
		assumeFalse(result.result() == ERROR, "error");
		if (result.result() == DID_NOT_FINISH) {
			throw new IncompleteExecutionException("Did not finish");
		}
		assertSame(expected, result.result());
	}

	private static Arguments specific(MccTest input, int property) {
		return arguments(input, property, sparse);
	}

	public static Stream<Arguments> specific() {
		return Stream.of(
				specific(new MccTest(mccDir.resolve("CircadianClock-PT-000001")), 2),
				specific(new MccTest(mccDir.resolve("LamportFastMutEx-PT-2")), 8),
				specific(new MccTest(mccDir.resolve("SafeBus-PT-03")), 0),
				specific(new MccTest(mccDir.resolve("SafeBus-PT-03")), 2),
				specific(new MccTest(mccDir.resolve("SafeBus-PT-03")), 6),
				specific(new MccTest(mccDir.resolve("SafeBus-PT-03")), 15),
				specific(new MccTest(mccDir.resolve("CloudDeployment-PT-2a")), 0),
				specific(new MccTest(mccDir.resolve("CloudDeployment-PT-2a")), 15),
				specific(new MccTest(mccDir.resolve("LamportFastMutEx-PT-3")), 10),
				specific(new MccTest(mccDir.resolve("LamportFastMutEx-PT-3")), 12),
				specific(new MccTest(mccDir.resolve("FlexibleBarrier-PT-04a")), 9),
				specific(new MccTest(mccDir.resolve("FlexibleBarrier-PT-04a")), 15),
				specific(new MccTest(mccDir.resolve("Parking-PT-104")), 13),
				specific(new MccTest(mccDir.resolve("Parking-PT-104")), 14),
				specific(new MccTest(mccDir.resolve("SmartHome-PT-01")), 11),
				specific(new MccTest(mccDir.resolve("SmartHome-PT-02")), 1),
				specific(new MccTest(mccDir.resolve("SmartHome-PT-02")), 3),
				specific(new MccTest(mccDir.resolve("SmartHome-PT-02")), 4),
				specific(new MccTest(mccDir.resolve("SmartHome-PT-02")), 6),
				specific(new MccTest(mccDir.resolve("SmartHome-PT-02")), 11),
				specific(new MccTest(mccDir.resolve("SmartHome-PT-02")), 12)
		);
	}

	@ParameterizedTest
	@MethodSource("allIndividually")
	public void testEncodeFormula(MccTest input, int propertyIndex) throws InterruptedException {
		SparsePetriNet net = input.sparseModel();
		StateFormula<SparsePetriNet, SparseIntVector, String, Integer> goal = input.reachabilityCardinalityPropertiesSparse().get(propertyIndex).formula().asGoal();
		SparseIntVector initialMarking = net.getInitialMarking();
		BddSparseSolver solver = new BddSparseSolver(net);
		BDD goalBdd = solver.encoder.encodeFormula(goal, 0);
		BDD initialBdd = solver.encoder.encodeMarking(initialMarking, 0);
		System.out.println("goal = " + goal);
		System.out.println("initial = " + SparseNetUtil.renderPlaces(initialMarking, net));
		System.out.print("goalBdd = ");
		goalBdd.printSetWithDomains();
		System.out.print("initialBdd = ");
		initialBdd.printSetWithDomains();
		assertEquals(goal.test(net, initialMarking), BddUtil.contains(goalBdd, initialBdd));
	}

	@Test
	public void testThis() throws Exception {
		this.testSize(new MccTest(mccDir.resolve("DrinkVendingMachine-PT-02")));
	}
}
