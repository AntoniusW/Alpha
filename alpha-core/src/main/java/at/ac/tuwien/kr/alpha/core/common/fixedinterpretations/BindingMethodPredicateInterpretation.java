/**
 * Copyright (c) 2017-2018, the Alpha Team.
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
package at.ac.tuwien.kr.alpha.core.common.fixedinterpretations;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import at.ac.tuwien.kr.alpha.api.common.fixedinterpretations.BindingPredicateInterpretation;
import at.ac.tuwien.kr.alpha.api.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.api.terms.Term;

public class BindingMethodPredicateInterpretation implements BindingPredicateInterpretation {
	private final Method method;

	public BindingMethodPredicateInterpretation(Method method) {
		if (!method.getReturnType().equals(Set.class)) {
			throw new IllegalArgumentException("method must return Set");
		}

		this.method = method;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<List<ConstantTerm<?>>> evaluate(List<Term> terms) {
		if (terms.size() != method.getParameterCount()) {
			throw new IllegalArgumentException(
					"Parameter count mismatch when calling " + method.getName() + ". " +
							"Expected " + method.getParameterCount() + " parameters but got " + terms.size() + ".");
		}

		final Class<?>[] parameterTypes = method.getParameterTypes();

		final Object[] arguments = new Object[terms.size()];

		for (int i = 0; i < arguments.length; i++) {
			if (!(terms.get(i) instanceof ConstantTerm)) {
				throw new IllegalArgumentException(
						"Expected only constants as input for " + method.getName() + ", but got " +
								"something else at position " + i + ".");
			}

			arguments[i] = ((ConstantTerm<?>) terms.get(i)).getObject();

			final Class<?> expected = parameterTypes[i];
			final Class<?> actual = arguments[i].getClass();

			if (expected.isAssignableFrom(actual)) {
				continue;
			}

			if (expected.isPrimitive() && ClassUtils.primitiveToWrapper(expected).isAssignableFrom(actual)) {
				continue;
			}

			throw new IllegalArgumentException(
					"Parameter type mismatch when calling " + method.getName() +
							" at position " + i + ". Expected " + expected + " but got " +
							actual + ".");
		}

		try {
			return (Set<List<ConstantTerm<?>>>) method.invoke(null, arguments);
		} catch (IllegalAccessException | InvocationTargetException ex) {
			throw new RuntimeException("Error invoking method " + method + "with args [" + StringUtils.join(arguments) + "], expection is: " + ex.getMessage());
		}
	}
}
