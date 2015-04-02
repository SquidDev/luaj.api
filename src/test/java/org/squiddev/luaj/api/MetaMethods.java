package org.squiddev.luaj.api;

import org.junit.BeforeClass;
import org.junit.Test;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.squiddev.luaj.api.builder.APIClassLoader;

import static org.junit.Assert.assertEquals;
import static org.squiddev.luaj.api.LuaConversion.convert;

/**
 * Check metamethods are implemented
 */
public class MetaMethods {
	private static LuaTable table;

	@BeforeClass
	public static void testCreateAPI() throws Exception {
		LuaObject api = APIClassLoader.createLoader().makeInstance(new EmbedClass());

		// Set environment and bind to a variable
		JsePlatform.debugGlobals();
		table = api.getTable();
	}

	@Test
	public void testsNormalFunctions() {
		assertEquals(1, table.get("foo").invoke(convert(1)).toint(1));
	}

	@Test
	public void testsSetIndex() {
		ExpectException.expect(LuaError.class, "Attempt to set HELLO",
			new SetValue(LuaValue.valueOf("HELLO"), LuaValue.ONE)
		);
	}

	private static class SetValue implements Runnable {
		public final LuaValue name;
		public final LuaValue value;

		private SetValue(LuaValue key, LuaValue value) {
			this.name = key;
			this.value = value;
		}

		@Override
		public void run() {
			table.set(name, value);
		}
	}

	@LuaAPI
	public static class EmbedClass {
		@LuaFunction
		public int foo(int x) {
			return x;
		}

		@LuaFunction
		public void __newindex(LuaValue self, LuaValue key, LuaValue value) {
			throw new LuaError("Attempt to set " + key.toString());
		}
	}
}
