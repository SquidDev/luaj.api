package org.squiddev.luaj.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.luaj.vm2.LuaTable;
import org.squiddev.luaj.api.builder.APIClassLoader;

import static org.junit.Assert.assertEquals;

/**
 * Tests fields work correctly
 */
@RunWith(Parameterized.class)
public class Fields {
	private LuaTable table;

	public Fields(APIClassLoader loader) {
		table = loader.makeInstance(new EmbedClass()).getTable();
	}

	@Parameterized.Parameters(name = "{0}")
	public static Object[] getLoaders() {
		return Loaders.getLoaderArgs();
	}

	@Test
	public void testsFields() {
		assertEquals("fields", table.get("foo").toString());
	}

	@Test
	public void testsNames() {
		assertEquals("many", table.get("bar").toString());
		assertEquals("many", table.get("baz").toString());
	}

	@Test
	public void testConversion() {
		LuaTable table = this.table.get("numbers").checktable();
		assertEquals(1, table.get(1).toint());
		assertEquals(4, table.get(2).toint());
		assertEquals(9, table.get(3).toint());
	}

	@LuaAPI
	public static class EmbedClass {
		@Field
		public String foo = "fields";

		@Field({"bar", "baz"})
		public String another = "many";

		@Field
		public int[] numbers = {1, 4, 9};

		@LuaFunction
		public void ignore() {
		}
	}
}
