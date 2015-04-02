package org.squiddev.luaj.api.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Customise the validation type to use
 *
 * @see ILuaValidator
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidationClass {
	/**
	 * The validation class to use. Should be an instance of {@link ILuaValidator}
	 *
	 * @return The validation class to use
	 */
	Class<? extends ILuaValidator> value() default DefaultLuaValidator.class;
}
