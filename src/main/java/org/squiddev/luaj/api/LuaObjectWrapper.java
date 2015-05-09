package org.squiddev.luaj.api;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

/**
 * A wrapper for using one `invoke` method for multiple lua methods
 */
public abstract class LuaObjectWrapper extends LuaObject {
	/**
	 * Call a particular function with arguments
	 *
	 * @param args  The arguments to call the function with
	 * @param index The index of the function
	 * @return The return values of calling the function
	 */
	protected abstract Varargs invoke(Varargs args, int index);

	/**
	 * Create the API's function table
	 *
	 * @return The created table
	 * @see #getTable()
	 */
	protected LuaTable createTable() {
		String[][] methodNames = getMethodNames();
		
		LuaTable table = new LuaTable();
		LuaTable meta = null;
		try {
			for (int i = 0, n = methodNames.length; i < n; i++) {
				// Allow multiple names
				for (String name : methodNames[i]) {
					// Each function should be a different object, even if it is identical.
					LuaValue function = createFunction(i);

					// Add support for metamethods
					if (name.startsWith("__")) {
						if (meta == null) {
							meta = new LuaTable();
							table.setmetatable(meta);
						}

						meta.rawset(name, function);
					} else {
						table.rawset(name, function);
					}

				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Bind failed", e);
		}
		return table;
	}

	/**
	 * Create a function with the specified index.
	 * Override to use custom functions
	 *
	 * @param index The function's index
	 * @return The created function
	 */
	protected LuaValue createFunction(int index) {
		return new InvokeFunction(index);
	}

	/**
	 * A basic wrapper function that calls the invoke method with the arguments
	 * and the function's index
	 */
	protected class InvokeFunction extends VarArgFunction {
		public final int index;

		public InvokeFunction(int index) {
			this.index = index;
		}

		/**
		 * We override the invoke function (not onInvoke) to prevent changing the stack
		 */
		@Override
		public Varargs invoke(Varargs varargs) {
			return LuaObjectWrapper.this.invoke(varargs, index);
		}
	}
}
