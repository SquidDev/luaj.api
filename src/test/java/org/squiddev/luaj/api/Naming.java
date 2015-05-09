package org.squiddev.luaj.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.squiddev.luaj.api.builder.APIClassLoader;

import static org.junit.Assert.*;

/**
 * Tests if all names are assigned
 */
@RunWith(Parameterized.class)
public class Naming {
	private LuaTable table;
	private LuaTable env;

	public Naming(APIClassLoader loader) {
		LuaObject api = loader.makeInstance(new EmbedClass());

		env = JsePlatform.debugGlobals();
		api.bind(env);
		table = api.getTable();
	}

	@Parameterized.Parameters(name = "{0}")
	public static Object[] getLoaders() {
		return Loaders.getLoaderArgs();
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
