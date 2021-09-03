package de.lukaspanneke.bachelor.mcc.oracle;

import de.lukaspanneke.bachelor.help.BigIntOrInfinity;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.parser.impl.AbstractParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.*;

public class StateSpaceOracleParser extends AbstractParser<Map<StateSpaceOracleParser.StateSpaceProperty, BigIntOrInfinity>> {

	public enum StateSpaceProperty {
		STATES,
		TRANSITIONS,
		MAX_TOKEN_IN_PLACE,
		MAX_TOKEN_PER_MARKING
	}

	@Override
	public String getFormat() {
		return "oracle";
	}

	@Override
	public List<String> getFileExtensions() {
		return List.of("out", "oracle");
	}

	@Override
	public Map<StateSpaceProperty, BigIntOrInfinity> parse(InputStream inputStream) throws ParseException, IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		String[] header = reader.readLine().split(" ");
		if (header.length != 2 || !header[1].equals("StateSpace")) {
			throw new ParseException("Header must have the format '<net name> StateSpace'");
		}

		Map<StateSpaceProperty, BigIntOrInfinity> ret = new LinkedHashMap<>();
		String line;
		while ((line = reader.readLine()) != null) {

			String[] parts = line.split(" ");

			if (parts.length < 3 || !parts[0].equals("STATE_SPACE")) {
				throw new ParseException("Line must be of the format 'STATE_SPACE <property name> <integer> ...'");
			}
			if ("+inf".equalsIgnoreCase(parts[2])) {
				ret.put(StateSpaceProperty.valueOf(parts[1]), BigIntOrInfinity.POSITIVE_INFINITY);
			} else {
				ret.put(StateSpaceProperty.valueOf(parts[1]), BigIntOrInfinity.of(new BigInteger(parts[2])));
			}
		}
		return ret;
	}
}