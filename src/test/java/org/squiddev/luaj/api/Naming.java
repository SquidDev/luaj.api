package org.squiddev.luaj.api;

import org.junit.BeforeClass;
import org.junit.Test;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.squiddev.luaj.api.builder.APIClassLoader;

import static org.junit.Assert.*;

/**
 * Tests if all names are assigned
 */
public class Naming {
	private static LuaTable table;
	private static LuaTable env;

	@BeforeClass
	public static void testCreateAPI() throws Exception {
		LuaObject api = APIClassLoader.createLoader().makeInstance(new EmbedClass());

		// Set environment and bind to a variable
		env = JsePlatform.debugGlobals();
		api.bind(env);

		table = api.getTable();
	}

	@Test
	public void setsGlobals() {
		assertEquals(table, env.get("embedded"));
		assertEquals(table, env.get("embed"));
	}

	@Test
	public void setsFunctions() {
		assertTrue(table.get("bar").isfunction());
	}

	@Test
	public void setsRenamedFunctions() {
		assertTrue(table.get("one").isfunction());
		assertTrue(table.get("two").isfunction());
	}

	@Test
	public void setsAliases() {
		assertTrue(table.get("baz").isfunction());
		assertTrue(table.get("qux").isfunction());
	}

	/**
	 * Renamed functions shouldn't be the same
	 */
	@Test
	public void renamedNotEqual() {
		assertNotEquals(table.get("one"), table.get("two"));
	}

	@LuaAPI({"embedded", "embed"})
	public static class EmbedClass {
		@LuaFunction(value = {"one", "two"})
		public Varargs foo(Varargs args) {
			return args;
		}

		@LuaFunction
		public Varargs bar(Varargs args) {
			return args;
		}

		@LuaFunction
		@Alias("qux")
		public Varargs baz(Varargs args) {
			return args;
		}
	}
}
