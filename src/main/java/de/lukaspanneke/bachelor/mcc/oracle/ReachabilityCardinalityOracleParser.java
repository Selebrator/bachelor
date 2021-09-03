package de.lukaspanneke.bachelor.mcc.oracle;

import uniol.apt.io.parser.ParseException;
import uniol.apt.io.parser.impl.AbstractParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class ReachabilityCardinalityOracleParser extends AbstractParser<Map<String, Boolean>> {

	@Override
	public String getFormat() {
		return "oracle";
	}

	@Override
	public List<String> getFileExtensions() {
		return List.of("out", "oracle");
	}

	@Override
	public Map<String, Boolean> parse(InputStream inputStream) throws ParseException, IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		String[] header = reader.readLine().split(" ");
		if (header.length != 2 || !header[1].equals("ReachabilityCardinality")) {
			throw new ParseException("Header must have the format '<net name> ReachabilityCardinality'");
		}

		Map<String, Boolean> ret = new LinkedHashMap<>();
		String line;
		while ((line = reader.readLine()) != null) {
			String[] parts = line.split(" ");
			if (parts.length < 3 || !parts[0].equals("FORMULA") || !(parts[2].equals("TRUE") || parts[2].equals("FALSE"))) {
				throw new ParseException("Line must be of the format 'FORMULA <net name>-<property name> TRUE|FALSE ...'");
			}
			ret.put(parts[1], parts[2].equals("TRUE"));
		}

		return ret;
	}
}
