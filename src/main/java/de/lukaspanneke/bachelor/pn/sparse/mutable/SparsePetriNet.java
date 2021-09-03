package de.lukaspanneke.bachelor.pn.sparse.mutable;

import uniol.apt.adt.pn.Flow;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;

import java.util.*;

public class SparsePetriNet {
	private SparseIntVector initialMarking;
	private List<String> transitionNames;
	private List<String> placeNames;
	private IntMatrixSparseColumns flowMatrixPT;
	private IntMatrixSparseColumns flowMatrixTP;

	public SparsePetriNet(SparsePetriNet original) {
		this.initialMarking = new SparseIntVector(original.initialMarking);
		this.transitionNames = new ArrayList<>(original.transitionNames);
		this.placeNames = new ArrayList<>(original.placeNames);
		this.flowMatrixPT = new IntMatrixSparseColumns(original.flowMatrixPT);
		this.flowMatrixTP = new IntMatrixSparseColumns(original.flowMatrixTP);
	}

	public SparsePetriNet(PetriNet aptNet) {
		List<Transition> transitions = List.copyOf(aptNet.getTransitions());
		List<Place> places = List.copyOf(aptNet.getPlaces());
		List<Flow> edges = new ArrayList<>(aptNet.getEdges());
		Marking initialMarking = aptNet.getInitialMarking();

		this.transitionNames = new ArrayList<>(transitions.size());
		this.placeNames = new ArrayList<>(places.size());
		this.initialMarking = new SparseIntVector(places.size());
		this.flowMatrixPT = new IntMatrixSparseColumns(places.size(), transitions.size());
		this.flowMatrixTP = new IntMatrixSparseColumns(places.size(), transitions.size());

		Map<Transition, Integer> transitionIds = new HashMap<>(transitions.size());
		for (int i = 0; i < transitions.size(); i++) {
			Transition transition = transitions.get(i);
			transitionIds.put(transition, i);
			this.transitionNames.add(transition.getId());
		}

		Map<Place, Integer> placeIds = new HashMap<>(places.size());
		for (int i = 0; i < places.size(); i++) {
			Place place = places.get(i);
			placeIds.put(place, i);
			this.placeNames.add(place.getId());
			this.initialMarking.set(i, (int) initialMarking.getToken(place).getValue());
		}

		Comparator<Flow> order = Comparator.comparing(flow -> transitionIds.get(flow.getTransition()));
		order = order.thenComparing(flow -> placeIds.get(flow.getPlace()));
		edges.sort(order);
		for (Flow edge : edges) {
			if (edge.getSource() instanceof Place) {
				this.addPreFlowPT(placeIds.get(edge.getPlace()), transitionIds.get(edge.getTransition()), edge.getWeight());
			} else {
				this.addPostFlowTP(transitionIds.get(edge.getTransition()), placeIds.get(edge.getPlace()), edge.getWeight());
			}
		}
	}

	public PetriNet toApt() {
		PetriNet aptNet = new PetriNet();
		this.placeNames.forEach(aptNet::createPlace);
		this.transitionNames.forEach(aptNet::createTransition);
		for (int p = 0; p < this.getPlaceCount(); p++) {
			for (int t = 0; t < this.getTransitionCount(); t++) {
				int weightPT = this.flowMatrixPT.get(p, t);
				if (weightPT > 0) {
					aptNet.createFlow(this.getPlaceName(p), this.getTransitionName(t), weightPT);
				}
				int weightTP = this.flowMatrixTP.get(p, t);
				if (weightTP > 0) {
					aptNet.createFlow(this.getTransitionName(t), this.getPlaceName(p), weightTP);
				}
			}
		}
		for (int i = 0; i < this.initialMarking.size(); i++) {
			aptNet.getPlace(this.getPlaceName(this.initialMarking.keyAt(i))).setInitialToken(this.initialMarking.valueAt(i));
		}
		return aptNet;
	}

	public int addTransition(String name) {
		this.transitionNames.add(name);
		this.flowMatrixPT.appendColumn(new SparseIntVector(0));
		this.flowMatrixTP.appendColumn(new SparseIntVector(0));
		int id = this.transitionNames.size() - 1;
		return id;
	}

	public int addPlace(String name, int initialTokens) {
		this.placeNames.add(name);
		this.flowMatrixPT.addRow();
		this.flowMatrixTP.addRow();
		int id = this.placeNames.size() - 1;
		this.initialMarking.set(id, initialTokens);
		return id;
	}

	public void removeTransition(int transitionId) {
		this.flowMatrixPT.removeColumn(transitionId);
		this.flowMatrixTP.removeColumn(transitionId);
		this.transitionNames.remove(transitionId);
	}

	public void removeTransitions(int... transitionIds) {
		Arrays.sort(transitionIds);
		for (int i = transitionIds.length - 1; i >= 0; i--) {
			this.removeTransition(transitionIds[i]);
		}
	}

	public void removePlace(int placeId) {
		this.flowMatrixPT.removeRow(placeId);
		this.flowMatrixTP.removeRow(placeId);
		this.initialMarking.removeAndDecrementFollowingKeys(placeId);
		this.placeNames.remove(placeId);
	}

	public void removePlaces(int... placeIds) {
		Arrays.sort(placeIds);
		for (int i = placeIds.length - 1; i >= 0; i--) {
			this.removePlace(placeIds[i]);
		}
	}

	public void addPreFlowPT(int placeId, int transitionId, int weight) {
		this.flowMatrixPT.set(placeId, transitionId, weight);
	}

	public void addPostFlowTP(int transitionId, int placeId, int weight) {
		this.flowMatrixTP.set(placeId, transitionId, weight);
	}

	public SparseIntVector getInitialMarking() {
		return this.initialMarking;
	}

	public IntMatrixSparseColumns getFlowMatrixPT() {
		return this.flowMatrixPT;
	}

	public IntMatrixSparseColumns getFlowMatrixTP() {
		return this.flowMatrixTP;
	}

	public int getTransitionCount() {
		return this.transitionNames.size();
	}

	public int getPlaceCount() {
		return this.placeNames.size();
	}

	public String getTransitionName(int transitionId) {
		return this.transitionNames.get(transitionId);
	}

	public String getPlaceName(int placeId) {
		return this.placeNames.get(placeId);
	}

	public int getTransitionId(String transitionName) {
		return this.transitionNames.indexOf(transitionName);
	}

	public int getPlaceId(String placeName) {
		return this.placeNames.indexOf(placeName);
	}

	public SparseIntVector presetT(int transitionId) {
		return this.flowMatrixPT.getColumns().get(transitionId);
	}

	public SparseIntVector postsetT(int transitionId) {
		return this.flowMatrixTP.getColumns().get(transitionId);
	}

	public SparseIntVector presetP_copy(int placeId) {
		return this.flowMatrixTP.getRow_copy(placeId);
	}

	public SparseIntVector presetP_copy_cached(int placeId) {
		return this.flowMatrixTP.getRow_copy_cached(placeId);
	}

	public Optional<SparseIntVector> presetP_copy(int placeId, int minSizeInclusive, int maxSizeInclusive) {
		return this.flowMatrixTP.getRow_copy(placeId, minSizeInclusive, maxSizeInclusive);
	}

	public SparseIntVector postsetP_copy(int placeId) {
		return this.flowMatrixPT.getRow_copy(placeId);
	}

	public SparseIntVector postsetP_copy_cached(int placeId) {
		return this.flowMatrixPT.getRow_copy_cached(placeId);
	}

	public Optional<SparseIntVector> postsetP_copy(int placeId, int minSizeInclusive, int maxSizeInclusive) {
		return this.flowMatrixPT.getRow_copy(placeId, minSizeInclusive, maxSizeInclusive);
	}

	public int weightPT(int placeId, int transitionId) {
		return this.flowMatrixPT.get(placeId, transitionId);
	}

	public int weightTP(int transitionId, int placeId) {
		return this.flowMatrixTP.get(placeId, transitionId);
	}

	public boolean firable(SparseIntVector marking, int transitionId) {
		return marking.greaterEquals(this.presetT(transitionId));
	}

	/** Return the successor marking, if firable, empty otherwise */
	public Optional<SparseIntVector> fire(SparseIntVector marking, int transitionId) {
		SparseIntVector t_pre = this.presetT(transitionId);
		if (marking.greaterEquals(t_pre)) {
			SparseIntVector afterConsume = SparseIntVector.weightedSum(1, marking, -1, t_pre);
			SparseIntVector nextMarking = SparseIntVector.weightedSum(1, afterConsume, 1, this.postsetT(transitionId));
			return Optional.of(nextMarking);
		} else {
			return Optional.empty();
		}
	}
}
