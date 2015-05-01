package org.squiddev.luaj.api.builder.tree;

import org.squiddev.luaj.api.LuaAPI;
import org.squiddev.luaj.api.LuaFunction;
import org.squiddev.luaj.api.builder.BuilderException;
import org.squiddev.luaj.api.builder.BuilderSettings;
import org.squiddev.luaj.api.transformer.Transformer;
import org.squiddev.luaj.api.validation.ILuaValidator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Data about the class we are generating data about
 */
public class LuaClass {
	/**
	 * The name of the generated class
	 */
	public final String name;

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
	 * List of fields this class uses
	 */
	public final Set<LuaField> fields = new HashSet<>();

	/**
	 * The transformer this class uses
	 */
	public final BuilderSettings settings;

	/**
	 * The validator for this class
	 */
	public Class<? extends ILuaValidator> validator;

	public LuaClass(String name, Class<?> klass, BuilderSettings settings) {
		this.name = name;
		this.klass = klass;
		this.validator = settings.validator;

		// Run the transformer on this class
		this.settings = settings;
		Transformer transformer = settings.transformer;
		if (transformer != null) transformer.transform(this);

		// Gather methods
		Set<LuaMethod> methods = this.methods;
		for (Method method : klass.getMethods()) {
			if (method.isAnnotationPresent(LuaFunction.class)) {
				methods.add(new LuaMethod(this, method));
			}
		}

		if (methods.size() == 0) throw new BuilderException("No @LuaFunction methods", this);

		Set<LuaField> fields = this.fields;
		for (Field field : klass.getFields()) {
			LuaField f = new LuaField(this, field);
			if (f.setup != null) fields.add(f);
		}
	}
}
