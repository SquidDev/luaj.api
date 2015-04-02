package org.squiddev.luaj.api;

import org.junit.BeforeClass;
import org.junit.Test;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.squiddev.luaj.api.builder.APIClassLoader;
import org.squiddev.luaj.api.validation.DefaultLuaValidator;
import org.squiddev.luaj.api.validation.StrictValidator;
import org.squiddev.luaj.api.validation.ValidationClass;

import static org.squiddev.luaj.api.LuaConversion.runMethod;

/**
 * Tests validation occurs properly
 */
public class Validation {
	private static LuaTable table;

	@BeforeClass
	public static void testCreateAPI() throws Exception {
		LuaObject api = APIClassLoader.createLoader().makeInstance(new EmbedClass());

		// Set environment and bind to a variable
		JsePlatform.debugGlobals();
		table = api.getTable();
	}

	/**
	 * Tests using {@link DefaultLuaValidator}
	 */
	@Test
	public void defaultValidation() {
		final LuaValue defaultMode = table.get("defaultMode");
		ExpectException.expect(LuaError.class, "Expected number, number",
			runMethod(defaultMode, true, 1),
			runMethod(defaultMode, "HELLO", 1),
			runMethod(defaultMode, 1, 1.12),
			runMethod(defaultMode, 1)
		);
	}

	/**
	 * Tests annotating functions with {@link LuaFunction#error()}
	 */
	@Test
	public void customErrors() {
		ExpectException.expect(LuaError.class, "I expected better of you!",
			runMethod(table.get("testingError"), true, 1)
		);
	}

	/**
	 * Tests using {@link StrictValidator}
	 */
	@Test
	public void testStrictMode() {
		LuaValue strictMode = table.get("strictMode");
		LuaValue stringNumber = LuaValue.valueOf("2");
		LuaValue normalNumber = LuaValue.valueOf(2);

		strictMode.invoke(normalNumber, normalNumber);

		ExpectException.expect(LuaError.class, "Expected number, number",
			runMethod(strictMode, stringNumber, stringNumber),
			runMethod(strictMode, stringNumber, normalNumber),
			runMethod(strictMode, normalNumber, stringNumber)
		);
	}

	/**
	 * Tests using {@link StrictValidator} and {@link DefaultLuaValidator}
	 */
	@Test
	public void testHybridMode() {
		LuaValue hybridMode = table.get("hybridMode");
		LuaValue stringNumber = LuaValue.valueOf("2");
		LuaValue normalNumber = LuaValue.valueOf(2);

		hybridMode.invoke(normalNumber, normalNumber);
		hybridMode.invoke(normalNumber, stringNumber);

		ExpectException.expect(LuaError.class, "Expected number, number",
			runMethod(hybridMode, stringNumber, stringNumber),
			runMethod(hybridMode, stringNumber, normalNumber)
		);
	}

	@LuaAPI
	public static class EmbedClass {
		@LuaFunction
		public void defaultMode(double a, int b) {
		}

		@LuaFunction(error = "I expected better of you!")
		public void testingError(double a, int b) {

		}

		@LuaFunction
		@ValidationClass(StrictValidator.class)
		public void strictMode(double a, int b) {
		}

		@LuaFunction
		@ValidationClass(StrictValidator.class)
		public void hybridMode(double a, @ValidationClass(DefaultLuaValidator.class) int b) {
		}
	}
}
