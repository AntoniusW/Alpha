package at.ac.tuwien.kr.alpha.common.program;

import at.ac.tuwien.kr.alpha.Util;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.rule.AbstractRule;
import at.ac.tuwien.kr.alpha.common.rule.head.Head;
import at.ac.tuwien.kr.alpha.grounder.parser.InlineDirectives;

import java.util.Collections;
import java.util.List;

import static at.ac.tuwien.kr.alpha.Util.join;

/**
 * The parent type for all kinds of programs. Defines a program's basic structure (facts + rules + inlineDirectives)
 * 
 * @param <R> the type of rule a program permits. This needs to be determined by implementations based on which syntax constructs an implementation permits
 *        Copyright (c) 2019, the Alpha Team.
 */
public abstract class AbstractProgram<R extends AbstractRule<? extends Head>> {

	private final List<R> rules;
	private final List<Atom> facts;
	private final InlineDirectives inlineDirectives;

	public AbstractProgram(List<R> rules, List<Atom> facts, InlineDirectives inlineDirectives) {
		this.rules = rules;
		this.facts = facts;
		this.inlineDirectives = inlineDirectives;
	}

	public List<R> getRules() {
		return Collections.unmodifiableList(rules);
	}

	public List<Atom> getFacts() {
		return Collections.unmodifiableList(facts);
	}

	public InlineDirectives getInlineDirectives() {
		return inlineDirectives;
	}

	@Override
	public String toString() {
		final String ls = System.lineSeparator();
		String result = facts.isEmpty() ? "" : Util.join("", facts, "." + ls, "." + ls);

		if (rules.isEmpty()) {
			return result;
		}

		result = join(result, rules, ls, ls);

		if (inlineDirectives == null || inlineDirectives.isEmpty()) {
			return result;
		}

		return join(result, inlineDirectives.getDirectives(), ls, ls);
	}

}
