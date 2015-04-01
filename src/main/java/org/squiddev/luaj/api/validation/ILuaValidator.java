package org.squiddev.luaj.api.validation;

import org.objectweb.asm.MethodVisitor;

/**
 * Handles validation of Lua values
 */
public interface ILuaValidator {
	/**
	 * Should validation occur on this argument
	 *
	 * @param type The type of the argument to validate
	 * @return Should the argument be validated?
	 */
	boolean shouldValidate(Class<?> type);

	/**
	 * Injects the validation code for an argument
	 *
	 * @param mv   The method visitor to inject to
	 * @param type The type of the argument
	 */
	void addValidation(MethodVisitor mv, Class<?> type);

	/**
	 * Get the type name of the argument
	 *
	 * @param type Argument type
	 * @return The name of the argument type
	 */
	String getName(Class<?> type);
}
