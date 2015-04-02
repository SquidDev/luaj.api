package org.squiddev.luaj.api.conversion;

import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaDouble;
import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaValue;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.squiddev.luaj.api.LuaAPI;
import org.squiddev.luaj.api.LuaObject;
import org.squiddev.luaj.api.builder.APIBuilder;
import org.squiddev.luaj.api.utils.TinyMethod;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;
import static org.squiddev.luaj.api.builder.APIBuilder.*;

/**
 * A registry for managing conversions
 */
public class Converter {
	private static Converter instance;

	/**
	 * Methods that convert Java objects to {@link org.luaj.vm2.LuaValue}
	 */
	public final Map<Class<?>, IInjector> toLua = new HashMap<>();

	/**
	 * Methods that convert {@link org.luaj.vm2.LuaValue} to Java objects
	 */
	public final Map<Class<?>, IInjector> fromLua = new HashMap<>();

	public Converter() {
		initFromLua();
		initToLua();
	}

	/**
	 * Initialise the toLua table
	 *
	 * @see #toLua
	 */
	protected void initToLua() {
		Map<Class<?>, IInjector> toLua = this.toLua;

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
	 * Initialise the fromLua table
	 *
	 * @see #fromLua
	 */
	protected void initFromLua() {
		Map<Class<?>, IInjector> fromLua = this.fromLua;

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

	/**
	 * Get a converter to convert from Java to Lua
	 *
	 * @param klass The class to convert
	 * @return The converter to use
	 * @see #toLua
	 */
	public IInjector getToLua(final Class<?> klass) {
		if (klass.isAnnotationPresent(LuaAPI.class)) {
			return new IInjector() {
				@Override
				public void inject(MethodVisitor mv, APIBuilder builder) {
					mv.visitFieldInsn(GETSTATIC, builder.className, LOADER, CLASS_LOADER);
					mv.visitInsn(SWAP);
					API_MAKE_INSTANCE.inject(mv);
					API_GET_TABLE.inject(mv);
				}
			};
		}

		if (LuaObject.class.isAssignableFrom(klass)) {
			return API_GET_TABLE;
		}

		return toLua.get(klass);
	}

	/**
	 * Get a converter to convert from Lua to Java
	 *
	 * @param klass The class to convert
	 * @return The converter to use
	 * @see #fromLua
	 */
	public IInjector getFromLua(final Class<?> klass) {
		// Allows having just LuaValue or Object
		// This allows for if you just want to package the annotations and nothing else when supplying an API
		if (klass.equals(Object.class) || klass.isInstance(LuaValue.class)) {
			return IInjector.VOID;
		}

		if (LuaValue.class.isAssignableFrom(klass)) {
			return new IInjector() {
				@Override
				public void inject(MethodVisitor mv, APIBuilder builder) {
					mv.visitTypeInsn(CHECKCAST, Type.getInternalName(klass));
				}
			};
		}

		return fromLua.get(klass);
	}
}
