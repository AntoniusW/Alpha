package at.ac.tuwien.kr.alpha.api.programs;

import java.util.Map;

public interface InlineDirectives {

	public enum DIRECTIVE {
		enum_predicate_is
	}

	void accumulate(InlineDirectives other);

	Map<DIRECTIVE, String> getDirectives();

	void addDirective(DIRECTIVE directive, String text);
	
	String getDirectiveValue(DIRECTIVE directive);

}
