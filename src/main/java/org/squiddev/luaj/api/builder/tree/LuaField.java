package org.squiddev.luaj.api.builder.tree;

import org.squiddev.luaj.api.builder.IInjector;

import java.lang.reflect.Field;

/**
 * A field of a Lua class
 */
public class LuaField {
	/**
	 * The parent method for this argument
	 */
	public final LuaClass klass;

	/**
	 * The parameter for this argument
	 */
	public final Field field;

	/**
	 * A method to call to set this value.
	 */
	public IInjector<LuaField> setup = null;

	public LuaField(LuaClass klass, Field field) {
		this.klass = klass;
		this.field = field;

		if (klass.transformer != null) klass.transformer.transform(this);
	}
}
