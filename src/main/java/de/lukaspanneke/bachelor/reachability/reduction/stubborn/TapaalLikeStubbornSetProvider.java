package de.lukaspanneke.bachelor.reachability.reduction.stubborn;

import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;

import java.util.*;

import static com.google.common.collect.Sets.difference;
import static de.lukaspanneke.bachelor.reachability.AptNetUtil.fireable;
import static de.lukaspanneke.bachelor.reachability.AptNetUtil.increasingPreset;
import static de.lukaspanneke.bachelor.reachability.AptNetUtil.postset;
import static de.lukaspanneke.bachelor.reachability.AptNetUtil.preset;
import static de.lukaspanneke.bachelor.reachability.AptNetUtil.tokens;
import static de.lukaspanneke.bachelor.reachability.AptNetUtil.weight;

@Deprecated
public class TapaalLikeStubbornSetProvider implements StubbornSetProvider<PetriNet, Marking, Place, Transition> {
	private static final Logger LOGGER = LogManager.getLogger(TapaalLikeStubbornSetProvider.class);

	@Override
	public Set<Transition> get(PetriNet net, Marking marking, StateFormula<PetriNet, Marking, Place, Transition> goal) throws InterruptedException {
		Set<Transition> interesting = goal.interestingTransitions(marking.getNet(), marking);
		Set<Transition> unprocessed = new LinkedHashSet<>(interesting);
		Set<Transition> stubborn = new HashSet<>();
		while (!unprocessed.isEmpty()) {
			if (Thread.interrupted()) {
				LOGGER.error("Thread was interrupted. Abort calculation of stubborn set");
				throw new InterruptedException();
			}
			Iterator<Transition> iterator = unprocessed.iterator();
			Transition t = iterator.next();
			if (fireable(t, marking)) {
				for (Place p : preset(t)) {
					if (weight(t, p) < weight(p, t)) { /* decreasingPostset(p).contains(t) */
						unprocessed.addAll(difference(postset(p), stubborn));
					}
				}
			} else {
				for (Place p : preset(t)) {
					if (tokens(p, marking) < weight(p, t)) {
						unprocessed.addAll(difference(increasingPreset(p), stubborn));
						break;
					}
				}
			}
			unprocessed.remove(t);
			stubborn.add(t);
		}
//		if (LOGGER.isDebugEnabled()) {
//			Set<Transition> firable = marking.getNet().getTransitions().stream()
//					.filter(transition -> fireable(transition, marking))
//					.collect(Collectors.toUnmodifiableSet());
//
//			LOGGER.debug("St({}) = {}, ignoring {}", renderMarking(marking), renderNodes(Sets.intersection(firable, stubborn)), renderNodes(Sets.difference(firable, stubborn)));
//
//		}
		return stubborn;
	}
}
