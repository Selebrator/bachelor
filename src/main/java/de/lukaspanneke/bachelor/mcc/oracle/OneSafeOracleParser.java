package de.lukaspanneke.bachelor.mcc.oracle;

import uniol.apt.io.parser.ParseException;
import uniol.apt.io.parser.impl.AbstractParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class OneSafeOracleParser extends AbstractParser<Boolean> {

	@Override
	public String getFormat() {
		return "oracle";
	}

	@Override
	public List<String> getFileExtensions() {
		return List.of("out", "oracle");
	}

	@Override
	public Boolean parse(InputStream inputStream) throws ParseException, IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		String[] header = reader.readLine().split(" ");
		if (header.length != 2 || !header[1].equals("OneSafe")) {
			throw new ParseException("Header must have the format '<net name> OneSafe'");
		}

		String content = reader.readLine();
		String[] parts = content.split(" ");

		if (parts.length < 3 || !parts[0].equals("FORMULA") || !parts[1].equals("OneSafe") || !(parts[2].equals("TRUE") || parts[2].equals("FALSE"))) {
			throw new ParseException("Line must be of the format 'FORMULA OneSafe <boolean> ...'");
		}
		return parts[2].equals("TRUE");
	}
}