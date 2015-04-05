package org.squiddev.luaj.api.builder;

import org.luaj.vm2.Varargs;
import org.squiddev.luaj.api.LuaFunction;
import org.squiddev.luaj.api.validation.DefaultLuaValidator;
import org.squiddev.luaj.api.validation.ILuaValidator;
import org.squiddev.luaj.api.validation.ValidationClass;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Stores all data associated with a Lua function
 */
public class LuaMethod implements Iterable<LuaMethod.LuaArgument> {
	/**
	 * The actual method this will call
	 */
	public final Method method;

	/**
	 * If this method's last argument is a varargs
	 */
	public final boolean takesVarargs;

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
	 * The names this function should be called
	 */
	public final List<String> names;

	public LuaMethod(Method m) {
		LuaFunction function = m.getAnnotation(LuaFunction.class);
		method = m;

		returnsVarags = function.isVarArgs();
		errorMessage = function.error();
		if (errorMessage != null && errorMessage.isEmpty()) {
			errorMessage = null;
		}

		// Create the names of this function
		String[] luaName = function.value();
		if (luaName == null || luaName.length == 0 || (luaName.length == 1 && luaName[0].isEmpty())) {
			names = new ArrayList<>();
			names.add(m.getName());
		} else {
			names = Arrays.asList(luaName);
		}

		// Check if this function is a varargs function
		boolean varargs = false;
		Parameter[] params = Parameter.getParameters(m);
		LuaArgument[] arguments = this.arguments = new LuaArgument[params.length];
		for (int i = 0; i < params.length; i++) {
			Parameter param = params[i];
			if (param.getType().equals(Varargs.class)) {
				varargs = true;
				if (i + 1 < params.length) {
					throw new APIBuilder.BuilderException("Varargs must be last item", m);
				}
			}


			arguments[i] = new LuaArgument(param);
		}

		this.takesVarargs = varargs;

	}

	/**
	 * Get the names of the Lua function
	 *
	 * @return A list of method names
	 */
	public String[] getLuaName() {
		return (String[]) names.toArray();
	}

	/**
	 * Get the name of the Java method
	 *
	 * @return The Java method name
	 */
	public String getJavaName() {
		return method.getName();
	}

	@Override
	public Iterator<LuaArgument> iterator() {
		return validationIterator();
	}

	public ValidationIterator validationIterator() {
		return new ValidationIterator();
	}

	/**
	 * Stores one argument of a Lua method
	 */
	public static class LuaArgument {
		private static final Map<Class<? extends ILuaValidator>, ILuaValidator> VALIDATORS = new HashMap<>();

		public final Class<?> type;
		public final Class<? extends ILuaValidator> validatorType;

		public final Parameter parameter;

		public LuaArgument(Parameter parameter) {
			this.parameter = parameter;
			type = parameter.getType();
			validatorType = getValidator(parameter);
		}

		public ILuaValidator getValidator() {
			ILuaValidator val = VALIDATORS.get(validatorType);

			if (val == null) {
				try {
					val = validatorType.newInstance();
				} catch (ReflectiveOperationException e) {
					throw new APIBuilder.BuilderException("Cannot create new instance of " + validatorType.getName(), e);
				}

				VALIDATORS.put(validatorType, val);
			}

			return val;
		}

		protected static Class<? extends ILuaValidator> getValidator(Parameter parameter) {
			ValidationClass validator = parameter.getAnnotation(ValidationClass.class);
			if (validator != null) return validator.value();

			validator = parameter.getDeclaringExecutable().getAnnotation(ValidationClass.class);
			if (validator != null) return validator.value();

			validator = parameter.getDeclaringExecutable().getDeclaringClass().getAnnotation(ValidationClass.class);
			if (validator != null) return validator.value();

			return DefaultLuaValidator.class;
		}
	}

	/**
	 * An iterator that checks if there is another item to be validated
	 */
	public class ValidationIterator implements Iterator<LuaArgument>, Iterable<LuaArgument> {
		private int index = 0;
		private final LuaArgument[] items;

		public ValidationIterator() {
			items = arguments;
		}

		@Override
		public boolean hasNext() {
			return index < items.length;
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
				if (arg.getValidator().shouldValidate(arg.type)) return true;
			}

			return false;
		}

		@Override
		public LuaArgument next() {
			return items[index++];
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		public int length() {
			return items.length - (takesVarargs ? 1 : 0);
		}

		public void rewind() {
			index = 0;
		}

		@Override
		public Iterator<LuaArgument> iterator() {
			return this;
		}
	}
}

