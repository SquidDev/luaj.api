package org.squiddev.luaj.api.builder.tree;

import org.squiddev.luaj.api.LuaAPI;
import org.squiddev.luaj.api.LuaFunction;
import org.squiddev.luaj.api.builder.APIBuilder;
import org.squiddev.luaj.api.transformer.Transformer;
import org.squiddev.luaj.api.validation.DefaultLuaValidator;
import org.squiddev.luaj.api.validation.ILuaValidator;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Data about the class we are generating data about
 */
public class LuaClass {
	/**
	 * The class we are generating method from
	 */
	public final Class<?> klass;

	/**
	 * Names this API should be bound to
	 *
	 * @see LuaAPI#value()
	 */
	public final Set<String> names = new HashSet<>();

	/**
	 * List of methods this class will have
	 */
	public final Set<LuaMethod> methods = new HashSet<>();

	/**
	 * The transformer this class uses
	 */
	public final Transformer transformer;

	/**
	 * The validator for this class
	 */
	public Class<? extends ILuaValidator> validator = DefaultLuaValidator.class;

	public LuaClass(Class<?> klass, Transformer transformer) {
		this.klass = klass;

		// Run the transformer on this class
		this.transformer = transformer;
		if (transformer != null) transformer.transform(this);

		// Gather methods
		Set<LuaMethod> methods = this.methods;
		for (Method method : klass.getMethods()) {
			if (method.isAnnotationPresent(LuaFunction.class)) {
				methods.add(new LuaMethod(this, method));
			}
		}

		if (methods.size() == 0) throw new APIBuilder.BuilderException("No @LuaFunction methods", this);
	}
}
