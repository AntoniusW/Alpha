package at.ac.tuwien.kr.alpha.core.parser;

import java.util.LinkedHashMap;
import java.util.Map;

import at.ac.tuwien.kr.alpha.api.programs.InlineDirectives;

/**
 * Stores directives appearing in the ASP program. Each directive starts with # and ends with .
 * Copyright (c) 2017 - 2021, the Alpha Team.
 */
public class InlineDirectivesImpl implements InlineDirectives {

	private final LinkedHashMap<DIRECTIVE, String> directives = new LinkedHashMap<>();

	@Override
	public String getDirectiveValue(DIRECTIVE directive) {
		return directives.get(directive);
	}

	@Override
	public void addDirective(DIRECTIVE directive, String value) {
		if (directives.get(directive) != null) {
			throw new RuntimeException("Inline directive multiply defined.");
		}
		directives.put(directive, value);
	}

	@Override
	public void accumulate(InlineDirectives other) {
		for (Map.Entry<DIRECTIVE, String> directiveEntry : other.getDirectives().entrySet()) {
			addDirective(directiveEntry.getKey(), directiveEntry.getValue());
		}
	}

	@Override
	public LinkedHashMap<DIRECTIVE, String> getDirectives() {
		return this.directives;
	}
}
