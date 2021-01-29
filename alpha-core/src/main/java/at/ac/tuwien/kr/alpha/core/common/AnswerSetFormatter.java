package at.ac.tuwien.kr.alpha.core.common;

import at.ac.tuwien.kr.alpha.api.AnswerSet;

@FunctionalInterface
public interface AnswerSetFormatter<T> {
	T format(AnswerSet answerSet);
}
