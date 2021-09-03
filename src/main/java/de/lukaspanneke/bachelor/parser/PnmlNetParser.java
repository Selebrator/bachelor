package de.lukaspanneke.bachelor.parser;

import de.lukaspanneke.bachelor.pn.sparse.mutable.SparsePetriNet;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.parser.Parser;
import uniol.apt.io.parser.impl.AbstractParser;
import uniol.apt.io.parser.impl.PnmlPNParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public interface PnmlNetParser {

	static Parser<PetriNet> apt() {
		return new AbstractParser<>() {
			@Override
			public String getFormat() {
				return "pnml";
			}

			@Override
			public List<String> getFileExtensions() {
				return List.of("pnml", "xml");
			}

			@Override
			public PetriNet parse(InputStream inputStream) throws ParseException, IOException {
				return new PnmlPNParser().parse(inputStream, false);
			}
		};
	}

	static Parser<SparsePetriNet> sparse() {
		return new AbstractParser<SparsePetriNet>() {
			@Override
			public String getFormat() {
				return "pnml";
			}

			@Override
			public List<String> getFileExtensions() {
				return List.of("pnml", "xml");
			}

			@Override
			public SparsePetriNet parse(InputStream inputStream) throws ParseException, IOException {
				return new SparsePetriNet(new PnmlPNParser().parse(inputStream, false));
			}
		};
	}
}
