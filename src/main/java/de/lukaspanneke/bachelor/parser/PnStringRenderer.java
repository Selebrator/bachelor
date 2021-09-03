package de.lukaspanneke.bachelor.parser;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Node;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.io.renderer.RenderException;
import uniol.apt.io.renderer.Renderer;
import uniol.apt.io.renderer.impl.AbstractRenderer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PnStringRenderer implements Renderer<PetriNet> {
	@Override
	public String getFormat() {
		return "pn-string";
	}

	@Override
	public List<String> getFileExtensions() {
		return List.of();
	}

	@Override
	public void renderFile(PetriNet net, String path) throws IOException {
		this.render(net, new FileWriter(path));
	}

	@Override
	public void renderFile(PetriNet net, File file) throws IOException {
		this.render(net, new FileWriter(file));
	}

	@Override
	public void render(PetriNet net, Writer writer) throws IOException {
		Set<Place> places = net.getPlaces();
		Marking initialMarking = net.getInitialMarking();
		String placesPart = places.stream()
				.map(place -> place.getId() + ":" + renderPostset(net, place, place.getPostset()))
				.collect(Collectors.joining(";"));
		String transitionsPart = net.getTransitions().stream()
				.map(transition -> transition.getId() + ":" + renderPostset(net, transition, transition.getPostset()))
				.collect(Collectors.joining(";"));
		String markingPart = places.stream()
				.map(place -> initialMarking.getToken(place).toString())
				.collect(Collectors.joining(","));
		writer.append("-p\"")
				.append(placesPart)
				.append(";;")
				.append(transitionsPart)
				.append(";;\" -m\"")
				.append(markingPart).append("\"");
	}

	@Override
	public String render(PetriNet net) {
		try {
			StringWriter string = new StringWriter();
			this.render(net, string);
			return string.toString();
		} catch (IOException e) {
			return null;
		}
	}

	private String renderPostset(PetriNet net, Node node, Set<? extends Node> postset) {
		return postset.stream()
				.flatMap(successor -> IntStream.rangeClosed(1, net.getFlow(node, successor).getWeight())
						.mapToObj(i -> successor))
				.map(Node::getId)
				.collect(Collectors.joining(","));
	}
}
