package at.ac.tuwien.kr.alpha.api;

import org.junit.Test;

public class UtilTest {
	@Test(expected = RuntimeException.class)
	public void oops() throws Exception {
		throw Util.oops("Ha", new UnsupportedOperationException("Ho"));
	}

	@Test(expected = RuntimeException.class)
	public void oops1() throws Exception {
		throw Util.oops("Ha");
	}

	@Test(expected = RuntimeException.class)
	public void oops2() throws Exception {
		throw Util.oops();
	}
}