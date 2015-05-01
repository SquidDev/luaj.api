package org.squiddev.luaj.api;

import org.junit.BeforeClass;
import org.junit.Test;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.Varargs;
import org.squiddev.luaj.api.builder.APIClassLoader;
import org.squiddev.luaj.api.builder.BuilderException;
import org.squiddev.luaj.api.setters.LoaderSetter;
import org.squiddev.luaj.api.setters.Setter;
import org.squiddev.luaj.api.setters.TableSetter;

import static org.junit.Assert.assertEquals;

/**
 * Tests setters work correctly
 */
public class Setters {
	private static APIClassLoader loader;
	private static EmbedClass object;
	private static LuaTable table;

	@BeforeClass
	public static void testCreateAPI() throws Exception {
		loader = APIClassLoader.createLoader();
		object = new EmbedClass();
		LuaObject api = loader.makeInstance(object);
		table = api.getTable();
	}

	@Test
	public void setsLoader() {
		assertEquals(loader, object.loader);
	}

	@Test
	public void setsTable() {
		assertEquals(table, object.table);
	}

	@Test
	public void validatesLoader() {
		ExpectException.expect(BuilderException.class, "Cannot convert java.lang.String to APIClassLoader", true, new Runnable() {
			@Override
			public void run() {
				loader.makeInstance(new EmbedClass() {
					@Setter(LoaderSetter.class)
					public String loader;
				}).getTable();
			}
		});

		ExpectException.expect(BuilderException.class, "Cannot convert java.lang.String to LuaTable", true, new Runnable() {
			@Override
			public void run() {
				loader.makeInstance(new EmbedClass() {
					@Setter(TableSetter.class)
					public String table;
				});
			}
		});

		loader.makeInstance(new EmbedClass() {
			@Setter(TableSetter.class)
			public Varargs table;
		});
	}

	@LuaAPI
	public static class EmbedClass {
		@Setter(LoaderSetter.class)
		public APIClassLoader loader;

		@Setter(TableSetter.class)
		public LuaTable table;

		@LuaFunction
		public void placeholder() {
		}
	}
}
