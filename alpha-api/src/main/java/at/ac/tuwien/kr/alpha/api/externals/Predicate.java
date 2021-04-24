package at.ac.tuwien.kr.alpha.api.externals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/* TODO this should be javadoc, but has a problem with the ref to Externals 
 *  * In order to have your method detected by Alpha, annotate it
 * with this annotation and call {@link Externals#scan}.
 *
 * @see Externals#scan
 */

/**
 * This annotation is used for discovery of method that represent
 * external predicates at runtime.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Predicate {
	/**
	 * The name of the predicate that will be used to refer to
	 * the annotated method. If it is the empty string (which
	 * is also the default value), then the name of the annotated
	 * method will be used.
	 */
	String name() default "";
}
