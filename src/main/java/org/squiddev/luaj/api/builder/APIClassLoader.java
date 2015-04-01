package org.squiddev.luaj.api.builder;

import org.squiddev.luaj.api.LuaObject;
import org.squiddev.luaj.api.LuaObjectWrapper;
import org.squiddev.luaj.api.utils.AsmUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles loading and generating APIs
 */
public class APIClassLoader extends ClassLoader {
	/**
	 * A string to suffix every class with
	 */
	protected String suffix = "_GenAPI";

	/**
	 * The class every generated API will inherit from
	 */
	protected Class<? extends LuaObject> parentClass = LuaObjectWrapper.class;

	/**
	 * Should verify sources
	 */
	protected boolean verify = false;

	private final Map<Class<?>, Class<?>> cache = new HashMap<>();

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		if (name.endsWith(suffix)) {
			// We want to remove the _GenAPI part of the string
			return createClass(name, Class.forName(name.substring(0, name.length() - suffix.length())));
		}

		return super.findClass(name);
	}

	/**
	 * Make a class based of a class
	 *
	 * @param rootClass The class to base it of
	 * @return The wrapper class
	 */
	public Class<?> makeClass(Class<?> rootClass) {
		return createClass(rootClass.getName() + suffix, rootClass);
	}

	/**
	 * Make an instance of the wrapper
	 *
	 * @param rootInstance The class instance to base it off
	 * @return The resulting instance
	 */
	public LuaObject makeInstance(Object rootInstance) {
		Class<?> rootClass = rootInstance.getClass();
		Class<?> wrapper = makeClass(rootClass);

		try {
			return (LuaObject) wrapper.getConstructor(rootClass).newInstance(rootInstance);
		} catch (ReflectiveOperationException e) {
			// This should NEVER happen. We've made this class, so we should never get any errors
			throw new RuntimeException("Cannot create API", e);
		}
	}

	/**
	 * Attempt to load the class from the cache, and if not then create it
	 *
	 * @param name     The name of the class to create
	 * @param original The original class to base it off
	 * @return The created class
	 */
	protected Class<?> createClass(String name, Class<?> original) {
		Class<?> result = cache.get(original);
		if (result == null) {
			byte[] bytes = new APIBuilder(original, this).toByteArray();
			if (verify) {
				AsmUtils.validateClass(bytes);
			}

			result = defineClass(name, bytes, 0, bytes.length);
			cache.put(original, result);
		}

		return result;
	}
}
