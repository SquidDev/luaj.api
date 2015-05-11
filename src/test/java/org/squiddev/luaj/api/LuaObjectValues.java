package org.squiddev.luaj.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.squiddev.luaj.api.builder.APIClassLoader;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Check {@link LuaObject} methods work
 */
@RunWith(Parameterized.class)
public class LuaObjectValues {
	private EmbedClass embed;
	private LuaObject object;

	public LuaObjectValues(APIClassLoader loader) {
		embed = new EmbedClass();
		object = loader.makeInstance(embed);
	}

	@Parameterized.Parameters(name = "{0}")
	public static Object[] getLoaders() {
		return Loaders.getLoaderArgs();
	}

	@Test
	public void testInstance() {
		assertEquals(embed, object.getInstance());
	}

	@Test
	public void testNames() {
		assertArrayEquals(new String[]{"bar", "foo"}, object.getNames());
	}

	@Test
	public void testMethodNames() {
		String[][] methodNames = new String[][]{
			new String[]{"something"},
			new String[]{"bar", "foo"}
		};
		for (String[] names : object.getMethodNames()) {
			assertArrayEquals(methodNames[names.length - 1], names);
		}
	}

	@LuaAPI({"foo", "bar"})
	public static class EmbedClass {
		@LuaFunction
		public void something() {
		}

		@LuaFunction({"foo", "bar"})
		public void foo() {
		}
	}
}
