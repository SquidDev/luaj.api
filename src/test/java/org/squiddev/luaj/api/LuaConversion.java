package org.squiddev.luaj.api;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

/**
 * Convert normal values to {@link LuaValue}s
 */
public class LuaConversion {
	public static Varargs convert(Object... objects) {
		int length = objects.length;
		LuaValue[] vars = new LuaValue[length];
		for (int i = 0; i < length; i++) {
			vars[i] = convert(objects[i]);
		}

		return LuaValue.varargsOf(vars);
	}

	public static LuaValue convert(Object object) {
		if (object == null) {
			return LuaValue.NIL;
		}
		if (object instanceof Integer) {
			return LuaValue.valueOf((int) object);
		} else if (object instanceof Double) {
			return LuaValue.valueOf((double) object);
		} else if (object instanceof Boolean) {
			return LuaValue.valueOf((Boolean) object);
		} else if (object instanceof String) {
			return LuaValue.valueOf((String) object);
		} else if (object instanceof LuaValue) {
			return (LuaValue) object;
		}

		throw new RuntimeException("Cannot cast " + object.getClass());
	}

	public static Runnable runMethod(final LuaValue method, final Object... arguments) {
		return new Runnable() {
			@Override
			public void run() {
				method.invoke(convert(arguments));
			}
		};
	}
}
