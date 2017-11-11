/**
 * Copyright (c) 2016, the Alpha Team.
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
package at.ac.tuwien.kr.alpha;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Util {
	public static <K, V> Map.Entry<K, V> entry(K key, V value) {
		return new AbstractMap.SimpleEntry<>(key, value);
	}

	public static <K, U> Collector<Map.Entry<K, U>, ?, Map<K, U>> entriesToMap() {
		return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue);
	}

	public static <E> String join(String prefix, Iterable<E> iterable, String suffix) {
		return join(prefix, iterable, ", ", suffix);
	}

	public static <E> String join(String prefix, Iterable<E> iterable, String delimiter, String suffix) {
		StringJoiner joiner = new StringJoiner(delimiter, prefix, suffix);
		for (E element : iterable) {
			joiner.add(element.toString());
		}
		return joiner.toString();
	}

	public static <U extends T, T extends Comparable<T>> int compareSortedSets(SortedSet<U> a, SortedSet<U> b) {
		if (a.size() != b.size()) {
			return a.size() - b.size();
		}

		if (a.isEmpty() && b.isEmpty()) {
			return 0;
		}

		final Iterator<U> ita = a.iterator();
		final Iterator<U> itb = b.iterator();

		do {
			final int result = ita.next().compareTo(itb.next());

			if (result != 0) {
				return result;
			}
		} while (ita.hasNext() && itb.hasNext());

		return 0;
	}
}
