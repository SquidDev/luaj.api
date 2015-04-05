package org.squiddev.luaj.api;

import org.junit.BeforeClass;
import org.junit.Test;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.squiddev.luaj.api.builder.APIClassLoader;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Check metamethods are implemented
 */
public class Transformers {
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
		assertTrue(table.get("foo").isfunction());
		assertTrue(table.get("bar").isfunction());
		assertNotEquals(table.get("foo"), table.get("bar"));
	}

	@LuaAPI
	public static class EmbedClass {
		@LuaFunction
		@Alias("bar")
		public int foo(int x) {
			return x;
		}
	}
}
