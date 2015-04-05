package org.squiddev.luaj.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a series of aliases for a function
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Alias {
	/**
	 * The extra names of the Lua Function
	 *
	 * @return The names of this function
	 */
	String[] value();
}
