package at.ac.tuwien.kr.alpha.api;

import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface Solver {
	
	Spliterator<AnswerSet> spliterator();

	default Stream<AnswerSet> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	default Set<AnswerSet> collectSet() {
		return stream().collect(Collectors.toSet());
	}

	default List<AnswerSet> collectList() {
		return stream().collect(Collectors.toList());
	}
	
}
