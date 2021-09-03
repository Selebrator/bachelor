package de.lukaspanneke.bachelor.parser;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.io.parser.impl.AbstractParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PnStringParser extends AbstractParser<PetriNet> {

	private static final class Parser {

		private final PetriNet pn = new PetriNet("net");
		private final List<String> placeOrder = new ArrayList<>();

		public PetriNet parse(String nodes, String initialMarking) {
			String[] nodeParts = nodes.split(";;");
			parseNodes(nodeParts[0], this::createPlace, this::createTransition);
			parseNodes(nodeParts[1], this::createTransition, this::createPlace);

			parseInitialMarking(initialMarking);
			return this.pn;
		}

		private void createTransition(String id) {
			if (!this.pn.containsTransition(id)) {
				this.pn.createTransition(id);
			}
		}

		private void createPlace(String id) {
			if (!this.pn.containsPlace(id)) {
				this.pn.createPlace(id);
				this.placeOrder.add(id);
			}
		}

		private void parseNodes(String nodesString, Consumer<String> nodeCreator, Consumer<String> successorCreator) {
			for (String nodeString : nodesString.split(";")) {
				parseNode(nodeString, nodeCreator, successorCreator);
			}
		}

		private void parseNode(String nodeString, Consumer<String> nodeCreator, Consumer<String> successorCreator) {
			String[] split = nodeString.split(":");
			String nodeName = split[0];
			nodeCreator.accept(nodeName);
			if (split.length != 1) { // otherwise this node has no successor
				String successorsString = split[1];
				Arrays.stream(successorsString.split(","))
						.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
						.forEach((successorName, weight) -> {
							successorCreator.accept(successorName);
							this.pn.createFlow(nodeName, successorName, Math.toIntExact(weight));
						});
			}
		}

		private void parseInitialMarking(String markingString) {
			Map<String, Integer> marking = new HashMap<>();
			String[] split = markingString.split(",");
			for (int i = 0; i < split.length; i++) {
				marking.put(this.placeOrder.get(i), Integer.parseInt(split[i]));
			}
			this.pn.setInitialMarking(new Marking(this.pn, marking));
		}
	}

	@Override
	public String getFormat() {
		return "pn-string";
	}

	@Override
	public List<String> getFileExtensions() {
		return List.of();
	}

	@Override
	public PetriNet parse(InputStream inputStream) {
		String pnString = new BufferedReader(new InputStreamReader(inputStream))
				.lines()
				.collect(Collectors.joining());
		return new Parser().parse(getQuotedOptionStyleSubstring(pnString, "-p"), getQuotedOptionStyleSubstring(pnString, "-m"));
	}

	public PetriNet parse(String nodes, String initialMarking) {
		return new Parser().parse(nodes, initialMarking);
	}

	private static String getQuotedOptionStyleSubstring(String in, String option) {
		int nodesStart = in.indexOf(option + "\"") + option.length() + 1;
		int nodesEnd = in.indexOf("\"", nodesStart);
		return in.substring(nodesStart, nodesEnd);
	}
}
