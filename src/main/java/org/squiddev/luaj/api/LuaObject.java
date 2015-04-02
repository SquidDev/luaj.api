package org.squiddev.luaj.api;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

/**
 * A core Lua API.
 * You can override this if need be
 */
public abstract class LuaObject {
	/**
	 * The table that stores the API's functions
	 */
	protected LuaTable table = null;

	/**
	 * Get the names of globals this API should be bound to.
	 *
	 * @return An array of name to bind to. Return null or an empty array to not set globals
	 */
	public abstract String[] getNames();

	/**
	 * Create the API's function table
	 *
	 * @return The created table
	 * @see #getTable()
	 */
	protected abstract LuaTable createTable();

	/**
	 * Get or create the API's function table
	 *
	 * @return A table with each key being a function bound to the original API
	 * @see #createTable()
	 */
	public LuaTable getTable() {
		LuaTable table = this.table;
		return table == null ? this.table = createTable() : table;
	}

	/**
	 * Bind this API to an environment
	 *
	 * @param env The table/environment to bind this API to
	 */
	public void bind(LuaValue env) {
		String[] names = getNames();
		if (names != null && names.length > 0) {
			LuaTable t = getTable();
			for (String name : names) {
				env.set(name, t);
			}
		}
	}
}
