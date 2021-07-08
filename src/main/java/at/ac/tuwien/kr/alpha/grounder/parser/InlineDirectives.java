/*
 * Copyright (c) 2017-2020, the Alpha Team.
 * All rights reserved.
 * 
 * Additional changes made by Siemens.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1) Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package at.ac.tuwien.kr.alpha.grounder.parser;

import at.ac.tuwien.kr.alpha.common.Directive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores directives appearing in the ASP program. Each directive starts with # and ends with .
 * Copyright (c) 2017-2020, the Alpha Team.
 */
public class InlineDirectives {

	public enum DIRECTIVE {
		enum_predicate_is,
		heuristic,
	}

	private final LinkedHashMap<DIRECTIVE, List<Directive>> directives = new LinkedHashMap<>();

	public Directive getDirectiveValue(DIRECTIVE directive) {
		List<Directive> values = directives.get(directive);
		if (values == null) {
			return null;
		}
		if (values.size() > 1) {
			throw new RuntimeException("Inline directive multiply defined.");
		}
		return values.iterator().next();
	}

	public void addDirective(DIRECTIVE directive, Directive value) {
		directives.putIfAbsent(directive, new ArrayList<>());
		directives.get(directive).add(value);
	}

	public void replaceDirectives(DIRECTIVE directive, List<Directive> values) {
		directives.put(directive, values);
	}

	public void accumulate(InlineDirectives other) {
		for (Map.Entry<DIRECTIVE, List<Directive>> directiveEntry : other.directives.entrySet()) {
			for (Directive directiveValue : directiveEntry.getValue()) {
				addDirective(directiveEntry.getKey(), directiveValue);
			}
		}
	}

	public boolean isEmpty() {
		return directives.isEmpty();
	}

	/**
	 * @return a new collection of all directives, changes to which will <b>not</b> be reflected in the program
	 */
	public Collection<Directive> getDirectives() {
		List<Directive> flatList = new ArrayList<>();
		for (List<Directive> list : directives.values()) {
			flatList.addAll(list);
		}
		return flatList;
	}

	/**
	 * @param type
	 * @return a collection of directives of the given type, changes to which <b>will</b> be reflected in the program;
	 * 			or {@code null} if no directives of the given type exist.
	 */
	public Collection<Directive> getDirectives(DIRECTIVE type) {
		return directives.get(type);
	}

	public boolean hasDirectives(DIRECTIVE type) {
		Collection<Directive> directives = getDirectives(type);
		return directives != null && !directives.isEmpty();
	}
}
