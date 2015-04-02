package org.squiddev.luaj.api;

import org.luaj.vm2.LuaValue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Show that this function is a Lua API
 *
 * This doesn't need to be explicitly used but is a useful hint
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface LuaAPI {
	/**
	 * The names of the Lua API
	 *
	 * @see LuaObject#bind(LuaValue)
	 */
	String[] value() default "";
}
