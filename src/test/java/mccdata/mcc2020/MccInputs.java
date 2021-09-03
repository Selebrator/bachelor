package mccdata.mcc2020;

import de.lukaspanneke.bachelor.help.BigIntOrInfinity;
import de.lukaspanneke.bachelor.mcc.MccTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static de.lukaspanneke.bachelor.help.BigIntOrInfinity.POSITIVE_INFINITY;
import static de.lukaspanneke.bachelor.mcc.oracle.StateSpaceOracleParser.StateSpaceProperty.STATES;

public class MccInputs {

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
}
