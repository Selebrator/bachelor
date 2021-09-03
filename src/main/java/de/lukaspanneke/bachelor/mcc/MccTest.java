package de.lukaspanneke.bachelor.mcc;

import de.lukaspanneke.bachelor.timing.Timer;
import de.lukaspanneke.bachelor.help.BigIntOrInfinity;
import de.lukaspanneke.bachelor.mcc.oracle.OneSafeOracleParser;
import de.lukaspanneke.bachelor.mcc.oracle.ReachabilityCardinalityOracleParser;
import de.lukaspanneke.bachelor.mcc.oracle.StateSpaceOracleParser;
import de.lukaspanneke.bachelor.parser.MccXmlReachabilityCardinalityParser;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseIntVector;
import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;
import de.lukaspanneke.bachelor.reachability.logic.apt.AptFormulaBuilder;
import de.lukaspanneke.bachelor.reachability.logic.sparse.SparseFormulaBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.parser.impl.PnmlPNParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static de.lukaspanneke.bachelor.timing.TimerIds.CONVERT_NET;
import static de.lukaspanneke.bachelor.timing.TimerIds.PARSE_NET_APT;
import static de.lukaspanneke.bachelor.mcc.oracle.StateSpaceOracleParser.StateSpaceProperty.MAX_TOKEN_IN_PLACE;
import static de.lukaspanneke.bachelor.mcc.oracle.StateSpaceOracleParser.StateSpaceProperty.STATES;

public class MccTest {

	private static final Logger LOGGER = LogManager.getLogger(MccTest.class);

	private final String name;
	private final Path pathToModel;
	private PetriNet model = null;
	private final Path pathToReachabilityCardinalityXml;
	private final Path pathToReachabilityCardinalityOracle;
	private final Path pathToStateSpaceOracle;
	private final Path pathToOneSafeOracle;
	private MccReachabilityCardinalityPropertySet<PetriNet, Marking, Place, Transition> reachabilityCardinalityPropertiesApt;
	private MccReachabilityCardinalityPropertySet<SparsePetriNet, SparseIntVector, String, Integer> reachabilityCardinalityPropertiesSparse;
	private Map<StateSpaceOracleParser.StateSpaceProperty, BigIntOrInfinity> stateSpaceProperties;
	@SuppressWarnings("OptionalUsedAsFieldOrParameterType") private Optional<Boolean> oneSafe;
	private SparsePetriNet sparseModel;
	private Map<String, Boolean> expectedResults;

	public MccTest(Path pathToInputDirectory) {
		this(pathToInputDirectory, true);
	}

	public MccTest(Path pathToInputDirectory, boolean allowOracles) {
		this.name = pathToInputDirectory.getFileName().toString();
		this.pathToModel = pathToInputDirectory.resolve("model.pnml");
		this.pathToReachabilityCardinalityXml = pathToInputDirectory.resolve("ReachabilityCardinality.xml");
		if (allowOracles) {
			this.pathToReachabilityCardinalityOracle = pathToInputDirectory.resolve("ReachabilityCardinality.oracle");
			this.pathToStateSpaceOracle = pathToInputDirectory.getParent().getParent().resolve("oracle").resolve(this.name + "-SS.out");
			this.pathToOneSafeOracle = pathToInputDirectory.getParent().getParent().resolve("oracle").resolve(this.name + "-OS.out");
		} else {
			this.pathToReachabilityCardinalityOracle = null;
			this.pathToStateSpaceOracle = null;
			this.pathToOneSafeOracle = null;
		}
	}

	public String name() {
		return this.name;
	}

	public PetriNet model() {
		if (this.model == null) {
			LOGGER.trace("Parsing model into apt format");
			Timer.global().start(PARSE_NET_APT);
			try {
				this.model = new PnmlPNParser().parse(Files.newInputStream(this.pathToModel), false);
			} catch (IOException | ParseException e) {
				throw new RuntimeException(e);
			} finally {
				String duration = Timer.global().stopFormat(PARSE_NET_APT);
				LOGGER.trace("Spent {} parsing model", duration);
			}
			this.model.setName(this.name);
		}
		return this.model;
	}

	public SparsePetriNet sparseModel() {
		if (this.sparseModel == null) {
			PetriNet model = this.model();
			LOGGER.info("Converting model from apt format to sparse representation");
			Timer.global().start(CONVERT_NET);
			try {
				this.sparseModel = new SparsePetriNet(model);
			} finally {
				String duration = Timer.global().stopFormat(CONVERT_NET);
				LOGGER.trace("Spent {} converting model", duration);
			}
		}
		return this.sparseModel;
	}

	public MccReachabilityCardinalityPropertySet<PetriNet, Marking, Place, Transition> reachabilityCardinalityPropertiesApt() {
		if (this.reachabilityCardinalityPropertiesApt == null) {
			try {
				LOGGER.info("Parsing ReachabilityCardinality properties into apt format");
				this.reachabilityCardinalityPropertiesApt = new MccXmlReachabilityCardinalityParser<>(new AptFormulaBuilder(this.model())).parse(Files.newInputStream(this.pathToReachabilityCardinalityXml));
			} catch (IOException | ParseException e) {
				throw new RuntimeException(e);
			}
		}
		return this.reachabilityCardinalityPropertiesApt;
	}

	public MccReachabilityCardinalityPropertySet<SparsePetriNet, SparseIntVector, String, Integer> reachabilityCardinalityPropertiesSparse() {
		if (this.reachabilityCardinalityPropertiesSparse == null) {
			try {
				LOGGER.info("Parsing ReachabilityCardinality properties into sparse format");
				this.reachabilityCardinalityPropertiesSparse = new MccXmlReachabilityCardinalityParser<>(new SparseFormulaBuilder()).parse(Files.newInputStream(this.pathToReachabilityCardinalityXml));
			} catch (IOException | ParseException e) {
				throw new RuntimeException(e);
			}
		}
		return this.reachabilityCardinalityPropertiesSparse;
	}

	public Map<String, Boolean> reachabilityCardinalityExpectedResults() {
		if (this.expectedResults == null) {
			try {
				this.expectedResults = new ReachabilityCardinalityOracleParser().parse(Files.newInputStream(this.pathToReachabilityCardinalityOracle));
			} catch (IOException | ParseException e) {
				LOGGER.warn("Expected result for all properties of {} unknown", this.name);
				this.expectedResults = Map.of();
			}
		}
		return this.expectedResults;
	}

	public Map<StateSpaceOracleParser.StateSpaceProperty, BigIntOrInfinity> stateSpaceProperties() {
		if (this.stateSpaceProperties == null) {
			try {
				this.stateSpaceProperties = new StateSpaceOracleParser().parse(Files.newInputStream(this.pathToStateSpaceOracle));
			} catch (ParseException e) {
				LOGGER.warn("StateSpace properties for {} unparsable: {}", this.name, e);
				this.stateSpaceProperties = Map.of();
			} catch (IOException e) {
				LOGGER.warn("StateSpace properties file absent. Expected path: {}", this.pathToStateSpaceOracle);
				this.stateSpaceProperties = Map.of();
			}
		}
		return this.stateSpaceProperties;
	}

	public Optional<Boolean> isSafe() {
		//noinspection OptionalAssignedToNull
		if (this.oneSafe == null) {
			try {
				this.oneSafe = Optional.of(new OneSafeOracleParser().parse(Files.newInputStream(this.pathToOneSafeOracle)));
			} catch (ParseException e) {
				LOGGER.warn("OneSafe global property for {} unparsable: {}", this.name, e);
				this.oneSafe = Optional.empty();
			} catch (IOException e) {
				LOGGER.warn("OneSafe global property file absent. Expected path: {}", this.pathToOneSafeOracle);
				this.oneSafe = Optional.empty();
			}
		}
		return this.oneSafe;
	}

	public Optional<BigIntOrInfinity> getBound() {
		return Optional.ofNullable(this.stateSpaceProperties().getOrDefault(MAX_TOKEN_IN_PLACE, null));
	}

	public Optional<Boolean> isBounded() {
		BigIntOrInfinity states = this.stateSpaceProperties().getOrDefault(STATES, null);
		if (states == null) {
			return Optional.empty();
		}
		return Optional.of(states.isFinite());
	}

	public Optional<BigIntOrInfinity> getNumberOfStates() {
		return Optional.ofNullable(this.stateSpaceProperties().getOrDefault(STATES, null));
	}

	@Override
	public String toString() {
		return "MccInput{" + this.name + "}";
	}
}
