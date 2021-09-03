package de.lukaspanneke.bachelor;

import de.lukaspanneke.bachelor.mcc.MccReachabilityCardinalityProperty;
import de.lukaspanneke.bachelor.mcc.MccReachabilityCardinalityPropertySet;
import de.lukaspanneke.bachelor.mcc.MccTest;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseIntVector;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;
import de.lukaspanneke.bachelor.reachability.AugmentedQueryResult;
import de.lukaspanneke.bachelor.reachability.logic.generic.Formula;
import de.lukaspanneke.bachelor.reachability.reduction.structural.TapaalA_FusionOfSequentialPlaces;
import de.lukaspanneke.bachelor.reachability.reduction.structural.TapaalB_FusionOfSequentialTransitions;
import de.lukaspanneke.bachelor.reachability.reduction.structural.TapaalC_FusionOfParallelPlaces;
import de.lukaspanneke.bachelor.reachability.reduction.structural.TapaalD_FusionOfParallelTransitions;
import de.lukaspanneke.bachelor.reachability.reduction.structural.TapaalF_EliminationOfSelfLoopPlace;
import de.lukaspanneke.bachelor.reachability.reduction.structural.TapaalG_EliminationOfSelfLoopTransition;
import de.lukaspanneke.bachelor.reachability.reduction.structural.TapaalH_SimplifyCycle;
import de.lukaspanneke.bachelor.reachability.reduction.stubborn.SparseTapaalLikeStubbornSetProvider;
import de.lukaspanneke.bachelor.reachability.solver.ParallelReachabilitySolver;
import de.lukaspanneke.bachelor.reachability.solver.ReachabilitySolver;
import de.lukaspanneke.bachelor.reachability.solver.result.SparseWitnessMarkingAndNumberOfVisitedMarkings;
import de.lukaspanneke.bachelor.reachability.solver.sparse.DynamicBreadthFirstReachabilitySetSolver;
import de.lukaspanneke.bachelor.reachability.solver.sparse.SafeBddReachabilitySolver;
import de.lukaspanneke.bachelor.reachability.solver.sparse.ProbabilisticDepthFirstReachabilitySolver;
import de.lukaspanneke.bachelor.reachability.solver.sparse.StructuralReductionPreprocessingSolver;
import de.lukaspanneke.bachelor.reachability.solver.sparse.StubbornBreadthFirstReachabilitySolver;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

public class Benchmark {
	private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(Benchmark.class);

	public static void main(String[] args) throws ParseException {
		LOGGER.debug("args: {}", Arrays.toString(args));
		Options options = new Options();
		options.addOption("n", "net", true, "name of the net's directory.");
		options.addOption("f", "formulas", true, "comma separated list of formulas to test");
		options.addOption("s", "solver", true, "name of the solver to use");
		options.addOption("p", "parallel", false, "name of the solver to use");
		options.addOption("t", "timeout", true, "timeout per formula in seconds");
		options.addOption("r", "structural", true, "use structural reductions for x seconds");
		CommandLineParser parser = new DefaultParser();
		CommandLine cli = parser.parse(options, args);
		String netName = cli.getOptionValue("net");

		Path mccDir = Path.of("");
		MccTest mccTest = new MccTest(mccDir.resolve(netName), false);
		String formulasIn = cli.getOptionValue("formulas");
		String solverName = cli.getOptionValue("solver");
		boolean parallel = cli.hasOption("parallel");
		Duration timeout = Duration.ofSeconds(Integer.parseInt(cli.getOptionValue("timeout")));

		List<MccReachabilityCardinalityProperty<SparsePetriNet, SparseIntVector, String, Integer>> properties;
		MccReachabilityCardinalityPropertySet<SparsePetriNet, SparseIntVector, String, Integer> mccProperties = mccTest.reachabilityCardinalityPropertiesSparse();
		if (formulasIn == null) {
			properties = List.copyOf(mccProperties);
		} else {
			List<Integer> formulaIndices = Arrays.stream(formulasIn.split(","))
					.map(Integer::parseInt)
					.toList();
			properties = new ArrayList<>(formulaIndices.size());
			for (Integer formulaIndex : formulaIndices) {
				properties.add(mccProperties.get(formulaIndex));
			}
		}
		List<Formula<SparsePetriNet, SparseIntVector, String, Integer>> formulas = properties.stream()
				.map(MccReachabilityCardinalityProperty::formula)
				.toList();
		List<? extends AugmentedQueryResult<?>> results;
		if (parallel) {
			results = Formula.testParallel(formulas, mccTest.sparseModel(), Solvers.getParallelSolver(solverName), timeout);
		} else {
			if (cli.hasOption("structural")) {
				Duration structuralDuration = Duration.ofSeconds(Integer.parseInt(cli.getOptionValue("structural")));
				results = Formula.testSequential(formulas, mccTest.sparseModel(), Solvers.getStructuralSolver(solverName, structuralDuration), timeout);
			} else {
				results = Formula.testSequential(formulas, mccTest.sparseModel(), Solvers.getSolver(solverName), timeout);
			}
		}
		LOGGER.info("# Results #");
		for (int i = 0; i < results.size(); i++) {
			AugmentedQueryResult<?> result = results.get(i);
			LOGGER.info(properties.get(i).id() + " " + result);
		}
	}

	public static class Solvers {

		public static ReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, ?> getSolver(String name) {
			return switch (name) {
				case "base" -> base();
				case "stubborn" -> stubborn();
				case "bdd" -> bdd();
				case "probabilistic" -> probabilistic();
				default -> throw new IllegalArgumentException("no solver with that name");
			};
		}

		public static ReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, ?> getStructuralSolver(String name, Duration maxPreprocessingTime) {

			return switch (name) {
				case "base" -> struct(maxPreprocessingTime);
				case "stubborn" -> structStubborn(maxPreprocessingTime);
				case "bdd" -> structBdd(maxPreprocessingTime);
				case "probabilistic" -> structProbabilistic(maxPreprocessingTime);
				default -> throw new IllegalArgumentException("no solver with that name");
			};
		}

		public static ParallelReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, ?> getParallelSolver(String name) {
			return switch (name) {
				case "base" -> base();
				case "bdd" -> bdd();
				case "probabilistic" -> probabilistic();
				default -> throw new IllegalArgumentException("no parallel solver with that name");
			};
		}

		public static ParallelReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, SparseWitnessMarkingAndNumberOfVisitedMarkings> base() {
			return new DynamicBreadthFirstReachabilitySetSolver();
		}

		public static ReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, SparseWitnessMarkingAndNumberOfVisitedMarkings> stubborn() {
			return new StubbornBreadthFirstReachabilitySolver(SparseTapaalLikeStubbornSetProvider.INSTANCE);
		}

		public static ParallelReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, Void> bdd() {
			return new SafeBddReachabilitySolver();
		}

		public static ParallelReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, SparseWitnessMarkingAndNumberOfVisitedMarkings> probabilistic() {
			return new ProbabilisticDepthFirstReachabilitySolver();
		}

		private static <R> ReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, R> _struct_sparse(ReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, R> solver, Duration maxPreprocessingTime) {
			return new StructuralReductionPreprocessingSolver<>(List.of(
					new TapaalA_FusionOfSequentialPlaces(),
					//new TapaalB_FusionOfSequentialTransitions(), /* is expensive and very rarely applied */
					//new TapaalC_FusionOfParallelPlaces(), /* is not fully implemented */
					new TapaalD_FusionOfParallelTransitions(),
					/* missing E */
					new TapaalF_EliminationOfSelfLoopPlace(),
					new TapaalG_EliminationOfSelfLoopTransition(),
					new TapaalH_SimplifyCycle()
					/* missing I */
			), solver, maxPreprocessingTime);
		}

		public static ReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, SparseWitnessMarkingAndNumberOfVisitedMarkings> struct(Duration maxPreprocessingTime) {
			return _struct_sparse(base(), maxPreprocessingTime);
		}

		public static ReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, SparseWitnessMarkingAndNumberOfVisitedMarkings> structStubborn(Duration maxPreprocessingTime) {
			return _struct_sparse(stubborn(), maxPreprocessingTime);
		}

		public static ReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, Void> structBdd(Duration maxPreprocessingTime) {
			return _struct_sparse(bdd(), maxPreprocessingTime);
		}

		public static ReachabilitySolver<SparsePetriNet, SparseIntVector, String, Integer, SparseWitnessMarkingAndNumberOfVisitedMarkings> structProbabilistic(Duration maxPreprocessingTime) {
			return _struct_sparse(probabilistic(), maxPreprocessingTime);
		}
	}
}
