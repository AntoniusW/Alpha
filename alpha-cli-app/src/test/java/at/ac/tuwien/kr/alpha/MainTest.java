/**
 * Copyright (c) 2016-2018, the Alpha Team.
 * All rights reserved.
 * 
 * Additional changes made by Siemens.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1) Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
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
package at.ac.tuwien.kr.alpha;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;

import static at.ac.tuwien.kr.alpha.Main.main;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class MainTest {
	private static final String INPUT = "p(a). " + System.lineSeparator() + " b :- p(X)." + System.lineSeparator();

	@Parameters
	public static Iterable<? extends String[][]> data() {
		return Arrays.asList(
				new String[][][] {{{"-DebugEnableInternalChecks", "-g", "naive", "-s", "default", "-e", "1119654162577372", "-n", "20", "-str", INPUT }},
						{{"-DebugEnableInternalChecks", "-g", "naive", "-s", "default", "-n", "0", "-str", INPUT }},
						{{"-DebugEnableInternalChecks", "-g", "naive", "-s", "default", "-n", "1", "-str", INPUT }},
						{{"-g", "naive", "-s", "default", "-r", "naive", "-e", "1119654162577372", "--numAS", "1", "-str", INPUT }}});
	}

	@Parameter
	public String[] argv;

	/**
	 * Temporarily redirects System.err and System.out while running the solver from the main entry point with the
	 * given parameters.
	 * Warning: this test is fragile and may require adaptions if printing is changed anywhere in Alpha.
	 */
	@Test
	public void test() {
		PrintStream sysOut = System.out;
		ByteArrayOutputStream newOut = new ByteArrayOutputStream();
		System.setOut(new PrintStream(newOut));
		main(argv);
		System.setOut(sysOut);
		assertTrue(newOut.toString().contains("{ b, p(a) }"));
	}

	@Test
	public void filterTest() {
		PrintStream sysOut = System.out;
		ByteArrayOutputStream newOut = new ByteArrayOutputStream();
		System.setOut(new PrintStream(newOut));
		String[] args = Arrays.copyOf(argv, argv.length + 2);
		args[args.length - 2] = "-f";
		args[args.length - 1] = "b";
		main(args);
		System.setOut(sysOut);
		assertTrue(newOut.toString().contains("{ b }"));
	}

}