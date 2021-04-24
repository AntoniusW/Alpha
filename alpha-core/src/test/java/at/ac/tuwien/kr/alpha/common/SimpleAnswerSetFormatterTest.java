package at.ac.tuwien.kr.alpha.common;

import org.junit.Assert;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.api.AnswerSet;
import at.ac.tuwien.kr.alpha.api.util.AnswerSetFormatter;
import at.ac.tuwien.kr.alpha.commons.AnswerSetBuilder;
import at.ac.tuwien.kr.alpha.commons.util.SimpleAnswerSetFormatter;

public class SimpleAnswerSetFormatterTest {

	@Test
	public void basicFormatterWithSeparator() {
		AnswerSetFormatter<String> fmt = new SimpleAnswerSetFormatter(" bla ");
		AnswerSet as = new AnswerSetBuilder().predicate("p").instance("a").predicate("q").instance("b").build();
		String formatted = fmt.format(as);
		Assert.assertEquals("{ p(\"a\") bla q(\"b\") }", formatted);
	}

}
