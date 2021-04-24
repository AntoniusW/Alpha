package at.ac.tuwien.kr.alpha.commons.util;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

public class Interner<T> {
	private WeakHashMap<T, WeakReference<T>> pool = new WeakHashMap<>();

	public synchronized T intern(T object) {
		T res;
		// (The loop is needed to deal with race
		// conditions where the GC runs while we are
		// accessing the 'pool' map or the 'ref' object.)
		do {
			WeakReference<T> ref = pool.get(object);
			if (ref == null) {
				ref = new WeakReference<>(object);
				pool.put(object, ref);
				res = object;
			} else {
				res = ref.get();
			}
		} while (res == null);
		return res;
	}
}