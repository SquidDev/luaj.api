package org.squiddev.luaj.api.builder.tree;

import org.luaj.vm2.Varargs;
import org.squiddev.luaj.api.builder.Parameter;
import org.squiddev.luaj.api.validation.ILuaValidator;
import org.squiddev.luaj.api.validation.ValidatorCache;

/**
 * Stores one argument of a Lua method
 */
public class LuaArgument {
	/**
	 * The parent method for this argument
	 */
	public final LuaMethod method;

	/**
	 * The parameter for this argument
	 */
	public final Parameter parameter;

	/**
	 * The validator for this class
	 */
	public Class<? extends ILuaValidator> validator;

	/**
	 * Should this be included in the count of required arguments
	 * It will still be validated using the {@link #validator}
	 */
	public boolean optional = false;

	public LuaArgument(LuaMethod method, Parameter parameter) {
		this.parameter = parameter;

		this.method = method;
		validator = method.validator;

		// Varargs should allow null
		if (parameter.getType().equals(Varargs.class)) optional = true;

		// Run transformers on this argument
		if (method.klass.settings.transformer != null) method.klass.settings.transformer.transform(this);
	}

	public ILuaValidator getValidator() {
		return ValidatorCache.getValidator(validator);
	}

	public boolean isVarargs() {
		return parameter.getType().equals(Varargs.class);
	}

	@Override
	public String toString() {
		return "LuaArgument<" + parameter.getType() + ">";
	}
}
