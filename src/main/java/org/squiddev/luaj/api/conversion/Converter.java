package org.squiddev.luaj.api.conversion;

import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaDouble;
import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaValue;
import org.squiddev.luaj.api.utils.TinyMethod;

import java.util.HashMap;
import java.util.Map;

/**
 * A registry for managing conversions
 */
public class Converter {
	private static Converter instance;

	/**
	 * Methods that convert Java objects to {@link org.luaj.vm2.LuaValue}
	 */
	public final Map<Class<?>, TinyMethod> toLua = new HashMap<>();

	/**
	 * Methods that convert {@link org.luaj.vm2.LuaValue} to Java objects
	 */
	public final Map<Class<?>, TinyMethod> fromLua = new HashMap<>();

	public Converter() {
		initFromLua();
		initToLua();
	}

	/**
	 * Initialise the {@see #toLua} table
	 */
	protected void initToLua() {
		Map<Class<?>, TinyMethod> toLua = this.toLua;

		// Boolean
		toLua.put(boolean.class, new TinyMethod(LuaBoolean.class, "valueOf", boolean.class));
		toLua.put(boolean[].class, new TinyMethod(ConversionHelpers.class, "valueOf", boolean[].class));

		// Integers
		toLua.put(int.class, new TinyMethod(LuaInteger.class, "valueOf", int.class));
		toLua.put(int[].class, new TinyMethod(ConversionHelpers.class, "valueOf", int[].class));

		toLua.put(byte.class, new TinyMethod(LuaInteger.class, "valueOf", int.class));
		toLua.put(byte[].class, new TinyMethod(ConversionHelpers.class, "valueOf", byte[].class));

		toLua.put(short.class, new TinyMethod(LuaInteger.class, "valueOf", int.class));
		toLua.put(short[].class, new TinyMethod(ConversionHelpers.class, "valueOf", short[].class));

		toLua.put(char.class, new TinyMethod(LuaInteger.class, "valueOf", int.class));
		toLua.put(char[].class, new TinyMethod(ConversionHelpers.class, "valueOf", char[].class));

		toLua.put(long.class, new TinyMethod(LuaInteger.class, "valueOf", long.class));
		toLua.put(long[].class, new TinyMethod(ConversionHelpers.class, "valueOf", long[].class));

		// Floats
		toLua.put(double.class, new TinyMethod(LuaDouble.class, "valueOf", double.class));
		toLua.put(double[].class, new TinyMethod(ConversionHelpers.class, "valueOf", double[].class));

		toLua.put(float.class, new TinyMethod(LuaDouble.class, "valueOf", double.class));
		toLua.put(float[].class, new TinyMethod(ConversionHelpers.class, "valueOf", float[].class));

		// String
		toLua.put(String.class, new TinyMethod(ConversionHelpers.class, "valueOf", String.class));
		toLua.put(String[].class, new TinyMethod(ConversionHelpers.class, "valueOf", String[].class));
	}

	/**
	 * Initialise the {@see #fromLua} table
	 */
	protected void initFromLua() {
		Map<Class<?>, TinyMethod> fromLua = this.fromLua;

		fromLua.put(boolean.class, new TinyMethod(LuaValue.class, "toboolean"));
		fromLua.put(byte.class, new TinyMethod(LuaValue.class, "tobyte"));
		fromLua.put(char.class, new TinyMethod(LuaValue.class, "tochar"));
		fromLua.put(double.class, new TinyMethod(LuaValue.class, "todouble"));
		fromLua.put(float.class, new TinyMethod(LuaValue.class, "tofloat"));
		fromLua.put(int.class, new TinyMethod(LuaValue.class, "toint"));
		fromLua.put(long.class, new TinyMethod(LuaValue.class, "tolong"));
		fromLua.put(short.class, new TinyMethod(LuaValue.class, "toshort"));
		fromLua.put(String.class, new TinyMethod(LuaValue.class, "tojstring"));
	}

	/**
	 * Get the default converter
	 *
	 * @return A converter instance that works with the default
	 */
	public static Converter getDefault() {
		Converter current = instance;
		if (current == null) {
			return instance = new Converter();
		}
		return current;
	}
}
