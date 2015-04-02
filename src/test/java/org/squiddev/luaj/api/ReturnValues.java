package org.squiddev.luaj.api;

import org.junit.BeforeClass;
import org.junit.Test;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.squiddev.luaj.api.builder.APIClassLoader;

import static org.junit.Assert.*;
import static org.squiddev.luaj.api.LuaConversion.convert;

/**
 * Tests functions are called correctly
 */
public class ReturnValues {
	private static APIClassLoader loader;
	private static LuaTable table;

	@BeforeClass
	public static void testCreateAPI() throws Exception {
		loader = APIClassLoader.createLoader();
		LuaObject api = loader.makeInstance(new EmbedClass());

		// Set environment and bind to a variable
		JsePlatform.debugGlobals();
		table = api.getTable();
	}

	/**
	 * Test that functions return what the are meant to
	 */
	@Test
	public void basicReturn() {
		assertEquals(LuaValue.NONE, table.get("noArgsNoReturn").invoke());
		assertEquals(LuaValue.TRUE, table.get("noArgsLuaReturn").invoke());


		assertEquals(2, table.get("varArgsLuaReturn").invoke(convert(1, 2, 3)).toint(1));
		assertEquals(2, table.get("twoArgsOneReturn").invoke(convert(1, 1)).todouble(1), 0);
	}

	/**
	 * Test that subargs for varargs works
	 */
	@Test
	public void subargs() {
		Varargs result = table.get("subArgs").invoke(convert("2", 3, "Hello", "World"));

		assertEquals(3, result.arg(1).toint());
		assertEquals(2, result.arg(2).toint());
		assertEquals("Hello", result.arg(3).toString());
		assertEquals("World", result.arg(4).toString());
		assertEquals(LuaValue.NIL, result.arg(5));
	}

	/**
	 * Test that ignoring varargs works
	 */
	@Test
	public void noVarargs() {
		Varargs result = table.get("subArgs").invoke(LuaValue.valueOf(2), LuaValue.valueOf(3));

		assertEquals(3, result.arg(1).toint());
		assertEquals(2, result.arg(2).toint());
		assertEquals(LuaValue.NIL, result.arg(3));
	}

	@Test
	public void returnVarargs() {
		Varargs result = table.get("returnVarargs").invoke();
		assertEquals(1, result.arg(1).toint());
		assertEquals(4, result.arg(2).toint());
		assertEquals(9, result.arg(3).toint());
	}

	@Test
	public void returnTable() {
		LuaTable result = (LuaTable) table.get("returnTable").invoke().arg1();
		assertEquals(1, result.get(1).toint());
		assertEquals(4, result.get(2).toint());
		assertEquals(9, result.get(3).toint());
	}

	@Test
	public void returnLuaNumberArray() {
		LuaTable result = (LuaTable) table.get("returnLuaNumberArray").invoke().arg1();
		assertEquals(1, result.get(1).toint());
		assertEquals(0, result.get(2).toint());
		assertEquals(-1, result.get(3).toint());
	}


	@Test
	public void returnLuaAPI() {
		LuaValue result = table.get("makeChild").invoke().arg1();

		assertTrue(result.istable());
		assertTrue(result.get("noArgsNoReturn").isfunction());

		// They shouldn't be the same
		assertNotEquals(result, table);
		assertNotEquals(result.get("returnVarargs"), table.get("returnVarags"));
	}

	@Test
	public void returnLuaObject() {
		LuaValue result = table.get("makeInstance").invoke().arg1();

		assertTrue(result.istable());
		assertTrue(result.get("noArgsNoReturn").isfunction());

		// They shouldn't be the same
		assertNotEquals(result, table);
		assertNotEquals(result.get("returnVarargs"), table.get("returnVarags"));
	}

	@LuaAPI
	public static class EmbedClass {
		@LuaFunction
		public void noArgsNoReturn() {
		}

		@LuaFunction
		public LuaValue noArgsLuaReturn() {
			return LuaValue.TRUE;
		}

		@LuaFunction
		public double twoArgsOneReturn(double a, int b) {
			return a + b;
		}

		@LuaFunction
		public Varargs varArgsLuaReturn(Varargs args) {
			return args.arg(2);
		}

		@LuaFunction
		public Varargs subArgs(int a, LuaNumber b, Varargs args) {
			return LuaValue.varargsOf(b, LuaValue.valueOf(a), args);
		}

		@LuaFunction
		public int[] returnTable() {
			return new int[]{1, 4, 9};
		}

		@LuaFunction(isVarArgs = true)
		public int[] returnVarargs() {
			return new int[]{1, 4, 9};
		}

		@LuaFunction
		public LuaNumber[] returnLuaNumberArray() {
			return new LuaNumber[]{
				LuaNumber.ONE,
				LuaNumber.ZERO,
				LuaNumber.MINUSONE,
			};
		}

		@LuaFunction
		public EmbedClass makeChild() {
			return new EmbedClass();
		}

		@LuaFunction
		public LuaObject makeInstance() {
			return loader.makeInstance(new EmbedClass());
		}
	}
}
