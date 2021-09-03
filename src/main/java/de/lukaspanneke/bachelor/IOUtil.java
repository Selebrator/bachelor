package de.lukaspanneke.bachelor;

import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.ts.TransitionSystem;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.parser.impl.AptPNParser;
import uniol.apt.io.parser.impl.PnmlPNParser;
import uniol.apt.io.renderer.RenderException;
import uniol.apt.io.renderer.impl.AptPNRenderer;
import uniol.apt.io.renderer.impl.DotLTSRenderer;
import uniol.apt.io.renderer.impl.DotPNRenderer;

import java.io.IOException;

public class IOUtil {

	public static PetriNet parseAptFile(String path) {
		try {
			return new AptPNParser().parseFile(path);
		} catch (ParseException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static PetriNet parsePnmlFile(String path) {
		try {
			return new PnmlPNParser().parseFile(path);
		} catch (ParseException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String renderDot(PetriNet net) {
		try {
			return new DotPNRenderer().render(net);
		} catch (RenderException e) {
			throw new RuntimeException(e);
		}
	}

	public static String renderDot(TransitionSystem ts) {
		try {
			return new DotLTSRenderer().render(ts);
		} catch (RenderException e) {
			throw new RuntimeException(e);
		}
	}

	public static String renderApt(PetriNet net) {
		try {
			return new AptPNRenderer().render(net);
		} catch (RenderException e) {
			throw new RuntimeException(e);
		}
	}
}
