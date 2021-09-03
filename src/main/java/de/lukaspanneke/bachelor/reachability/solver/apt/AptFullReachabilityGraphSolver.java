package de.lukaspanneke.bachelor.reachability.solver.apt;

import de.lukaspanneke.bachelor.lts.Edge;
import de.lukaspanneke.bachelor.lts.GenericTransitionSystem;
import de.lukaspanneke.bachelor.lts.Pathfinder;
import de.lukaspanneke.bachelor.reachability.AugmentedBoolean;
import de.lukaspanneke.bachelor.reachability.AugmentedQueryResult;
import de.lukaspanneke.bachelor.reachability.QueryResult;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import de.lukaspanneke.bachelor.reachability.reachabilitygraph.ReachabilityGraphBuilder;
import de.lukaspanneke.bachelor.reachability.solver.ReachabilitySolver;
import de.lukaspanneke.bachelor.reachability.solver.result.AptWitnessPathAndNumberOfVisitedMarkings;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;

import java.util.*;
import java.util.function.Predicate;

public class AptFullReachabilityGraphSolver implements ReachabilitySolver<PetriNet, Marking, Place, Transition, AptWitnessPathAndNumberOfVisitedMarkings> {
	private final ReachabilityGraphBuilder graphBuilder;

	public AptFullReachabilityGraphSolver(ReachabilityGraphBuilder graphBuilder) {
		this.graphBuilder = graphBuilder;
	}

	@Override
	public AugmentedQueryResult<AptWitnessPathAndNumberOfVisitedMarkings> isReachable(PetriNet net, StateFormula<PetriNet, Marking, Place, Transition> goal) {
		GenericTransitionSystem<Marking, Transition> reachabilityGraph = this.graphBuilder.build(net);
		return isReachable(net, goal, reachabilityGraph);
	}

	//@Override
	//public List<AugmentedBoolean<AptWitnessPathAndNumberOfVisitedMarkings>> isReachableSequential(PetriNet net, List<? extends StateFormula<PetriNet, Marking, Place, Transition>> goals) {
	//	GenericTransitionSystem<Marking, Transition> reachabilityGraph = this.graphBuilder.build(net);
	//	return goals.stream()
	//			.map(goal -> isReachable(net, goal, reachabilityGraph))
	//			.toList();
	//}

	private AugmentedQueryResult<AptWitnessPathAndNumberOfVisitedMarkings> isReachable(PetriNet net, StateFormula<PetriNet, Marking, Place, Transition> goal, GenericTransitionSystem<Marking, Transition> reachabilityGraph) {
		Predicate<Edge<Marking, Transition>> pathfinderGoal = edge -> goal.test(net, edge.to().state());
		Optional<List<Edge<Marking, Transition>>> path = Pathfinder.findPath(reachabilityGraph, reachabilityGraph.getVertex(net.getInitialMarking()).orElseThrow(), pathfinderGoal);
		Optional<List<Transition>> witnessPath = path.map(edges -> edges.stream().map(Edge::label).toList());
		Optional<Marking> witnessMarking = path.map(edges -> edges.get(edges.size() - 1).to().state());
		return new AugmentedQueryResult<>(witnessPath.isPresent() ? QueryResult.SATISFIED : QueryResult.UNSATISFIED, new AptWitnessPathAndNumberOfVisitedMarkings(witnessPath, witnessMarking, reachabilityGraph.getVertices().size()));
	}
}
