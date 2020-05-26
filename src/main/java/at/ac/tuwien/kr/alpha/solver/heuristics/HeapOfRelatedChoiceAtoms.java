package at.ac.tuwien.kr.alpha.solver.heuristics;

import at.ac.tuwien.kr.alpha.common.NoGood;
import at.ac.tuwien.kr.alpha.grounder.structure.AtomChoiceRelation;
import at.ac.tuwien.kr.alpha.solver.ChoiceManager;

import static at.ac.tuwien.kr.alpha.common.Literals.atomOf;

/**
 * A heap of active choice points that uses {@link AtomChoiceRelation} for initializing activities of related choice points.
 *
 * Copyright (c) 2019, the Alpha Team.
 */
public class HeapOfRelatedChoiceAtoms extends HeapOfActiveAtoms {
	private final AtomChoiceRelation atomChoiceRelation;

	HeapOfRelatedChoiceAtoms(int decayPeriod, double decayFactor, ChoiceManager choiceManager, AtomChoiceRelation atomChoiceRelation) {
		super(decayPeriod, decayFactor, choiceManager);
		this.atomChoiceRelation = atomChoiceRelation;
	}

	@Override
	protected void initActivityMOMs(NoGood newNoGood) {
		LOGGER.debug("Initializing activity scores with MOMs");
		for (int literal : newNoGood) {
			for (Integer relatedChoiceAtom : atomChoiceRelation.getRelatedChoiceAtoms(atomOf(literal))) {
				if (!incrementedActivityScores[relatedChoiceAtom]) { // update initial value as long as not incremented yet by VSIDS
					double score = moms.getScore(relatedChoiceAtom);
					if (score > 0.0) {
						double newActivity = 1 - 1 / (Math.log(score + 1.01));
						if (newActivity - activityScores[relatedChoiceAtom] > SCORE_EPSILON) {        // avoid computation overhead if score does not increase
							if (numberOfNormalizations > 0) {
								newActivity = normalizeNewActivityScore(newActivity);
							}
							setActivity(relatedChoiceAtom, newActivity);
						}
					}
				}
			}
		}
	}
}
