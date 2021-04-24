package at.ac.tuwien.kr.alpha.commons.terms;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.api.terms.ConstantTerm;

public class TermsTest {

	@Test
	public void integersAsTermList() {
		List<ConstantTerm<Integer>> intTerms = Terms.asTermList(1, 2, 3, 4, 5, 6);
		Assert.assertEquals(6, intTerms.size());
		Assert.assertEquals(Terms.newConstant(1), intTerms.get(0));
		Assert.assertEquals(Terms.newConstant(2), intTerms.get(1));
		Assert.assertEquals(Terms.newConstant(3), intTerms.get(2));
		Assert.assertEquals(Terms.newConstant(4), intTerms.get(3));
		Assert.assertEquals(Terms.newConstant(5), intTerms.get(4));
		Assert.assertEquals(Terms.newConstant(6), intTerms.get(5));
	}

	@Test
	public void stringsAsTermList() {
		List<ConstantTerm<String>> terms = Terms.asTermList("bla", "blubb");
		Assert.assertEquals(2, terms.size());
		Assert.assertEquals("\"bla\"", terms.get(0).toString());
		Assert.assertEquals("\"blubb\"", terms.get(1).toString());
	}

}
