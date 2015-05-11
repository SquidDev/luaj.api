package org.squiddev.luaj.api.conversion;

import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaDouble;
import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaValue;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.squiddev.luaj.api.LuaAPI;
import org.squiddev.luaj.api.LuaObject;
import org.squiddev.luaj.api.builder.IInjector;
import org.squiddev.luaj.api.builder.tree.LuaClass;
import org.squiddev.luaj.api.utils.TinyMethod;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;
import static org.squiddev.luaj.api.builder.BuilderConstants.*;

/**
 * A registry for managing conversions
 */
public class Converter {
	private static Converter instance;

	protected static IInjector<LuaClass> VOID = new IInjector<LuaClass>() {
		@Override
		public void inject(MethodVisitor mv, LuaClass object) {
		}
	};

	/**
	 * Methods that convert Java objects to {@link org.luaj.vm2.LuaValue}
	 */
	protected final Map<Class<?>, IInjector<LuaClass>> toLua = new HashMap<>();

	/**
	 * Methods that convert {@link org.luaj.vm2.LuaValue} to Java objects
	 */
	protected final Map<Class<?>, IInjector<LuaClass>> fromLua = new HashMap<>();

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
		// Boolean
		toLua(boolean.class, new TinyMethod(LuaBoolean.class, "valueOf", boolean.class));
		toLua(boolean[].class, new TinyMethod(ConversionHelpers.class, "valueOf", boolean[].class));

		// Integers
		toLua(int.class, new TinyMethod(LuaInteger.class, "valueOf", int.class));
		toLua(int[].class, new TinyMethod(ConversionHelpers.class, "valueOf", int[].class));

		toLua(byte.class, new TinyMethod(LuaInteger.class, "valueOf", int.class));
		toLua(byte[].class, new TinyMethod(ConversionHelpers.class, "valueOf", byte[].class));

		toLua(short.class, new TinyMethod(LuaInteger.class, "valueOf", int.class));
		toLua(short[].class, new TinyMethod(ConversionHelpers.class, "valueOf", short[].class));

		toLua(char.class, new TinyMethod(LuaInteger.class, "valueOf", int.class));
		toLua(char[].class, new TinyMethod(ConversionHelpers.class, "valueOf", char[].class));

		toLua(long.class, new TinyMethod(LuaInteger.class, "valueOf", long.class));
		toLua(long[].class, new TinyMethod(ConversionHelpers.class, "valueOf", long[].class));

		// Floats
		toLua(double.class, new TinyMethod(LuaDouble.class, "valueOf", double.class));
		toLua(double[].class, new TinyMethod(ConversionHelpers.class, "valueOf", double[].class));

		toLua(float.class, new TinyMethod(LuaDouble.class, "valueOf", double.class));
		toLua(float[].class, new TinyMethod(ConversionHelpers.class, "valueOf", float[].class));

		// String
		toLua(String.class, new TinyMethod(ConversionHelpers.class, "valueOf", String.class));
		toLua(String[].class, new TinyMethod(ConversionHelpers.class, "valueOf", String[].class));
	}

	/**
	 * Initialise the fromLua table
	 *
	 * @see #fromLua
	 */
	protected void initFromLua() {
		fromLua(boolean.class, new TinyMethod(LuaValue.class, "toboolean"));
		fromLua(byte.class, new TinyMethod(LuaValue.class, "tobyte"));
		fromLua(char.class, new TinyMethod(LuaValue.class, "tochar"));
		fromLua(double.class, new TinyMethod(LuaValue.class, "todouble"));
		fromLua(float.class, new TinyMethod(LuaValue.class, "tofloat"));
		fromLua(int.class, new TinyMethod(LuaValue.class, "toint"));
		fromLua(long.class, new TinyMethod(LuaValue.class, "tolong"));
		fromLua(short.class, new TinyMethod(LuaValue.class, "toshort"));
		fromLua(String.class, new TinyMethod(LuaValue.class, "tojstring"));
	}

	/**
	 * Wrap a {@link TinyMethod}
	 *
	 * @param method The method to wrap
	 * @return A wrapped TinyMethod
	 */
	protected IInjector<LuaClass> wrapMethod(final TinyMethod method) {
		return new IInjector<LuaClass>() {
			@Override
			public void inject(MethodVisitor mv, LuaClass object) {
				method.inject(mv);
			}
		};
	}

	/**
	 * Add a converter from Java to Lua
	 *
	 * @param type      The Java type to convert
	 * @param converter The converter to run
	 */
	public void toLua(Class<?> type, IInjector<LuaClass> converter) {
		toLua.put(type, converter);
	}

	/**
	 * Add a converter from Java to Lua
	 *
	 * @param type      The Java type to convert
	 * @param converter The converter to run
	 */
	public void toLua(Class<?> type, TinyMethod converter) {
		toLua(type, wrapMethod(converter));
	}

	/**
	 * Add a converter from Lua to Java type
	 *
	 * @param type      The Java type to convert to
	 * @param converter The converter to run
	 */
	public void fromLua(Class<?> type, IInjector<LuaClass> converter) {
		fromLua.put(type, converter);
	}

	/**
	 * Add a converter from Lua to Java type
	 *
	 * @param type      The Java type to convert to
	 * @param converter The converter to run
	 */
	public void fromLua(Class<?> type, TinyMethod converter) {
		fromLua(type, wrapMethod(converter));
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
	public IInjector<LuaClass> getToLua(final Class<?> klass) {
		if (klass.isAnnotationPresent(LuaAPI.class)) {
			return new IInjector<LuaClass>() {
				@Override
				public void inject(MethodVisitor mv, LuaClass klass) {
					mv.visitFieldInsn(GETSTATIC, klass.name, LOADER, CLASS_LOADER);
					mv.visitInsn(SWAP);
					API_MAKE_INSTANCE.inject(mv);
					API_GET_TABLE.inject(mv);
				}
			};
		}

		if (LuaObject.class.isAssignableFrom(klass)) {
			return wrapMethod(API_GET_TABLE);
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
	public IInjector<LuaClass> getFromLua(final Class<?> klass) {
		// Allows having just LuaValue or Object
		// This allows for if you just want to package the annotations and nothing else when supplying an API
		if (klass.equals(Object.class) || klass.isInstance(LuaValue.class)) {
			return VOID;
		}

		if (LuaValue.class.isAssignableFrom(klass)) {
			return new IInjector<LuaClass>() {
				@Override
				public void inject(MethodVisitor mv, LuaClass object) {
					mv.visitTypeInsn(CHECKCAST, Type.getInternalName(klass));
				}
			};
		}

		return fromLua.get(klass);
	}
}
