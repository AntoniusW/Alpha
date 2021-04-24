package at.ac.tuwien.kr.alpha.core.rules.heads;

import static at.ac.tuwien.kr.alpha.api.Util.join;

import java.util.List;

import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.rules.DisjunctiveHead;

/**
 * Copyright (c) 2017, the Alpha Team.
 */
public class DisjunctiveHeadImpl implements DisjunctiveHead {
	public final List<Atom> disjunctiveAtoms;

	public DisjunctiveHeadImpl(List<Atom> disjunctiveAtoms) {
		this.disjunctiveAtoms = disjunctiveAtoms;
		if (disjunctiveAtoms != null && disjunctiveAtoms.size() > 1) {
			throw new UnsupportedOperationException("Disjunction in rule heads is not yet supported");
		}
	}

	@Override
	public String toString() {
		return join("", disjunctiveAtoms, " | ", "");
	}

	public boolean isGround() {
		for (Atom atom : disjunctiveAtoms) {
			if (!atom.isGround()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.disjunctiveAtoms == null) ? 0 : this.disjunctiveAtoms.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof DisjunctiveHeadImpl)) {
			return false;
		}
		DisjunctiveHeadImpl other = (DisjunctiveHeadImpl) obj;
		if (this.disjunctiveAtoms == null) {
			if (other.disjunctiveAtoms != null) {
				return false;
			}
		} else if (!this.disjunctiveAtoms.equals(other.disjunctiveAtoms)) {
			return false;
		}
		return true;
	}

}
