package org.squiddev.luaj.api.builder.tree;

import org.luaj.vm2.Varargs;
import org.squiddev.luaj.api.builder.BuilderException;
import org.squiddev.luaj.api.builder.Parameter;
import org.squiddev.luaj.api.validation.ILuaValidator;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores one argument of a Lua method
 */
public class LuaArgument {
	/**
	 * Cache of validator instances
	 */
	private static final Map<Class<? extends ILuaValidator>, ILuaValidator> VALIDATORS = new HashMap<>();

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
		ILuaValidator val = VALIDATORS.get(validator);

		if (val == null) {
			try {
				val = validator.newInstance();
			} catch (ReflectiveOperationException e) {
				throw new BuilderException("Cannot create new instance of " + validator.getName(), this, e);
			}

			VALIDATORS.put(validator, val);
		}

		return val;
	}
}
