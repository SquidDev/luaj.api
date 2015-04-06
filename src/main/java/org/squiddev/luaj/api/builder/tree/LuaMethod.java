package org.squiddev.luaj.api.builder.tree;

import org.squiddev.luaj.api.LuaFunction;
import org.squiddev.luaj.api.builder.APIBuilder;
import org.squiddev.luaj.api.builder.Parameter;
import org.squiddev.luaj.api.validation.ILuaValidator;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Stores all data associated with a Lua function
 */
public class LuaMethod implements Iterable<LuaArgument> {
	/**
	 * The parent class for this method
	 */
	public final LuaClass klass;

	/**
	 * The actual method this will call
	 */
	public final Method method;

	/**
	 * If this method returns a varargs
	 */
	public boolean returnsVarags;

	/**
	 * The error message this function should produce
	 * Null if it should be generated automatically
	 */
	public String errorMessage;

	/**
	 * The arguments this function takes
	 */
	public final LuaArgument[] arguments;

	/**
	 * The names used to call this function
	 */
	public final Set<String> names = new HashSet<>();

	/**
	 * The validator for this method
	 */
	public Class<? extends ILuaValidator> validator;

	public LuaMethod(LuaClass klass, Method method) {
		this.klass = klass;
		this.method = method;

		this.validator = klass.validator;

		LuaFunction function = method.getAnnotation(LuaFunction.class);

		// Get default isVarArgs
		returnsVarags = function.isVarArgs();

		// Get default error message
		String errorMessage = function.error();
		this.errorMessage = errorMessage != null && errorMessage.isEmpty() ? null : errorMessage;

		// Create the names of this function
		String[] luaName = function.value();
		if (luaName == null || luaName.length == 0 || (luaName.length == 1 && luaName[0].isEmpty())) {
			names.add(method.getName());
		} else {
			Collections.addAll(names, luaName);
		}

		// Run transformers on this method
		if (klass.transformer != null) klass.transformer.transform(this);

		// Check if this function is a varargs function
		Parameter[] params = Parameter.getParameters(method);
		LuaArgument[] arguments = this.arguments = new LuaArgument[params.length];
		for (int i = 0; i < params.length; i++) {
			arguments[i] = new LuaArgument(this, params[i]);
		}
	}

	@Override
	public Iterator<LuaArgument> iterator() {
		return validationIterator();
	}

	/**
	 * Get a custom iterator that adds utilities
	 *
	 * @return The custom iterator
	 * @see #iterator()
	 */
	public ValidationIterator validationIterator() {
		return new ValidationIterator();
	}

	/**
	 * An iterator that checks if there is another item to be validated
	 */
	public class ValidationIterator implements Iterator<LuaArgument>, Iterable<LuaArgument> {
		private int index = 0;
		private final LuaArgument[] items;
		private final int length;

		public ValidationIterator() {
			items = arguments;

			// Calculate the non-optional argument length
			int length = 0;
			boolean hasOptional = false;
			for (LuaArgument argument : arguments) {
				if (argument.optional) {
					hasOptional = true;
				} else if (hasOptional) {
					throw new APIBuilder.BuilderException("Non-Optional item after an optional one", LuaMethod.this);
				} else {
					length++;
				}
			}
			this.length = length;
		}

		/**
		 * Checks if there is another item to be validated
		 *
		 * @return If there is another item
		 */
		public boolean hasValidateNext() {
			LuaArgument[] items = this.items;
			for (int i = index; i < items.length; i++) {
				LuaArgument arg = items[i];
				if (arg.getValidator().shouldValidate(arg.parameter.getType())) return true;
			}

			return false;
		}

		/**
		 * Get the length of required arguments
		 *
		 * @return The length of required arguments
		 */
		public int requiredLength() {
			return length;
		}

		/**
		 * Reset back to the beginning
		 */
		public void rewind() {
			index = 0;
		}

		@Override
		public boolean hasNext() {
			return index < items.length;
		}

		@Override
		public LuaArgument next() {
			return items[index++];
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Iterator<LuaArgument> iterator() {
			return this;
		}
	}
}

