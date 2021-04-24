package at.ac.tuwien.kr.alpha.api.programs;

import java.util.List;

import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.rules.Head;
import at.ac.tuwien.kr.alpha.api.rules.Rule;

public interface Program<R extends Rule<? extends Head>> {

	List<Atom> getFacts();

	InlineDirectives getInlineDirectives();

	List<R> getRules();

}
