/**
 * Copyright (c) 2020, the Alpha Team.
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1) Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p>
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
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
package at.ac.tuwien.kr.alpha.core.externals;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;

import at.ac.tuwien.kr.alpha.api.common.fixedinterpretations.PredicateInterpretation;
import at.ac.tuwien.kr.alpha.api.externals.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.commons.Predicates;
import at.ac.tuwien.kr.alpha.commons.atoms.Atoms;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;
import at.ac.tuwien.kr.alpha.core.common.fixedinterpretations.BinaryPredicateInterpretation;
import at.ac.tuwien.kr.alpha.core.common.fixedinterpretations.BindingMethodPredicateInterpretation;
import at.ac.tuwien.kr.alpha.core.common.fixedinterpretations.IntPredicateInterpretation;
import at.ac.tuwien.kr.alpha.core.common.fixedinterpretations.LongPredicateInterpretation;
import at.ac.tuwien.kr.alpha.core.common.fixedinterpretations.MethodPredicateInterpretation;
import at.ac.tuwien.kr.alpha.core.common.fixedinterpretations.SuppliedPredicateInterpretation;
import at.ac.tuwien.kr.alpha.core.common.fixedinterpretations.UnaryPredicateInterpretation;

public final class Externals {

	// Private constructor since this is a utility class.
	private Externals() {

	}

	/**
	 * Returns a map of external definitions making up the "standard library" of
	 * externals that are always available in programs for Alpha.
	 * This method scans all predicate-annotated methods in the package holding the
	 * class {@link AspStandardLibrary}.
	 */
	public static Map<String, PredicateInterpretation> getStandardLibraryExternals() {
		return Externals.scan(AspStandardLibrary.class.getPackage());
	}

	public static Map<String, PredicateInterpretation> scan(Package basePackage) {
		Reflections reflections = new Reflections(basePackage.getName(), new MethodAnnotationsScanner());
		Set<Method> methods = reflections.getMethodsAnnotatedWith(Predicate.class);
		return Externals.scanMethods(methods);
	}

	public static Map<String, PredicateInterpretation> scan(Class<?> classWithPredicateMethods) {
		Method[] methods = classWithPredicateMethods.getMethods();
		Set<Method> predicateMethods = new HashSet<>();
		for (Method method : methods) {
			if (method.isAnnotationPresent(Predicate.class)) {
				predicateMethods.add(method);
			}
		}
		return Externals.scanMethods(predicateMethods);
	}

	private static Map<String, PredicateInterpretation> scanMethods(Iterable<Method> methods) {
		Map<String, PredicateInterpretation> retVal = new HashMap<>();
		String name;
		for (Method method : methods) {
			name = method.getAnnotation(Predicate.class).name();
			if (name.isEmpty()) {
				name = method.getName();
			}
			retVal.put(name, Externals.processPredicateMethod(method));
		}
		return retVal;
	}

	public static PredicateInterpretation processPredicateMethod(Method method) {
		if (method.getReturnType().equals(boolean.class)) {
			return new MethodPredicateInterpretation(method);
		}

		if (method.getGenericReturnType().getTypeName().startsWith(PredicateInterpretation.EVALUATE_RETURN_TYPE_NAME_PREFIX)) {
			return new BindingMethodPredicateInterpretation(method);
		}

		throw new IllegalArgumentException("Passed method has unexpected return type. Should be either boolean or start with "
			+ PredicateInterpretation.EVALUATE_RETURN_TYPE_NAME_PREFIX + ".");
	}

	public static <T> PredicateInterpretation processPredicate(java.util.function.Predicate<T> predicate) {
		return new UnaryPredicateInterpretation<>(predicate);
	}

	public static PredicateInterpretation processPredicate(java.util.function.IntPredicate predicate) {
		return new IntPredicateInterpretation(predicate);
	}

	public static PredicateInterpretation processPredicate(java.util.function.LongPredicate predicate) {
		return new LongPredicateInterpretation(predicate);
	}

	public static <T, U> PredicateInterpretation processPredicate(java.util.function.BiPredicate<T, U> predicate) {
		return new BinaryPredicateInterpretation<>(predicate);
	}

	public static PredicateInterpretation processPredicate(java.util.function.Supplier<Set<List<ConstantTerm<?>>>> supplier) {
		return new SuppliedPredicateInterpretation(supplier);
	}

	/**
	 * Converts a collection of objects to facts.
	 * Every item in the collection is wrapped in a {@link ConstantTerm}, which is the argument of a unary predicate whose
	 * symbol is the class name of the given class (i.e. the declared type of objects inside the collection), modified to
	 * start with a lower-case letter.
	 *
	 * @param <T>             the type of the objects to use as facts
	 * @param classOfExtFacts the {@link Class} object of the value type
	 * @param extFacts        a {@link Collection} of objects of type <code>classOfExtFacts</code>
	 * @return a list of {@link Atom}s.
	 */
	public static <T extends Comparable<T>> List<Atom> asFacts(Class<T> classOfExtFacts, Collection<T> extFacts) {
		// Use Class<T> as parameter here, taking simple name from first element might not give desired result if it is a subtype.
		List<Atom> retVal = new ArrayList<>();
		String javaName = classOfExtFacts.getSimpleName();
		String name = javaName.substring(0, 1).toLowerCase() + javaName.substring(1); // Camel-cased, but starting with lower case letter.
		for (T instance : extFacts) {
			retVal.add(Atoms.newBasicAtom(Predicates.getPredicate(name, 1), Terms.newConstant(instance)));
		}
		return retVal;
	}

}
