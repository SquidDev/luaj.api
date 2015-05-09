package org.squiddev.luaj.api.builder;

import org.squiddev.luaj.api.builder.tree.LuaArgument;
import org.squiddev.luaj.api.builder.tree.LuaClass;
import org.squiddev.luaj.api.builder.tree.LuaField;
import org.squiddev.luaj.api.builder.tree.LuaMethod;

/**
 * Exception thrown on generating classes
 */
public class BuilderException extends RuntimeException {
	public BuilderException(String message, Throwable cause) {
		super(message, cause);
	}

	public BuilderException(String message) {
		super(message, null);
	}

	public BuilderException(String message, LuaClass klass, Throwable cause) {
		this(klass.klass.getName() + ": " + message, cause);
	}

	public BuilderException(String message, LuaClass klass) {
		this(message, klass, null);
	}

	public BuilderException(String message, LuaMethod method, Throwable cause) {
		this(method.method.getName() + ": " + message, method.klass, cause);
	}

	public BuilderException(String message, LuaMethod method) {
		this(message, method, null);
	}

	public BuilderException(String message, LuaArgument argument, Throwable cause) {
		this("Argument " + argument.parameter.getType() + ": " + message, argument.method, cause);
	}

	public BuilderException(String message, LuaArgument argument) {
		this(message, argument, null);
	}

	public BuilderException(String message, LuaField field, Throwable cause) {
		this(field.field.getName() + ": " + message, field.klass, cause);
	}

	public BuilderException(String message, LuaField field) {
		this(message, field, null);
	}
}
