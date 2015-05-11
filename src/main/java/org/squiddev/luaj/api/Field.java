package org.squiddev.luaj.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A field that is set when the table is created
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Field {
	/**
	 * The name(s) of the field
	 *
	 * @return An array of names or "" if no name is specified
	 */
	String[] value() default "";
}
