package org.squiddev.luaj.api.builder;

import org.squiddev.luaj.api.LuaObject;
import org.squiddev.luaj.api.LuaObjectWrapper;
import org.squiddev.luaj.api.conversion.Converter;
import org.squiddev.luaj.api.transformer.DefaultTransformers;
import org.squiddev.luaj.api.transformer.Transformer;
import org.squiddev.luaj.api.utils.AsmUtils;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Handles loading and generating APIs
 */
public class APIClassLoader<T extends LuaObject> extends ClassLoader {
	/**
	 * A string to suffix every class with
	 */
	protected String suffix = "_GenAPI";

	/**
	 * The class every generated API will inherit from
	 */
	public final Class<T> parentClass;

	/**
	 * Should verify sources
	 */
	protected boolean verify = false;

	/**
	 * The method lookup for conversions
	 */
	protected Converter converter = Converter.getDefault();

	protected Transformer transformer = new DefaultTransformers();

	public APIClassLoader(Class<T> parentClass) {
		this.parentClass = parentClass;
	}

	/**
	 * The cache for {@link org.squiddev.luaj.api.LuaAPI} classes to {@link LuaObject} classes
	 */
	protected final Map<Class<?>, Class<? extends T>> cache = new WeakHashMap<>();

	/**
	 * A cache for created instances instead
	 *
	 * @see #makeInstance(Object)
	 */
	protected final Map<Object, T> instanceCache = new WeakHashMap<>();

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		if (name.endsWith(suffix)) {
			// We want to remove the _GenAPI part of the string
			return createClass(name, Class.forName(name.substring(0, name.length() - suffix.length())));
		}

		return super.findClass(name);
	}

	/**
	 * Make a class based off a {@link org.squiddev.luaj.api.LuaAPI} class
	 * If it already exists in the cache then use that
	 *
	 * @param rootClass The class to base it of
	 * @return The wrapper class
	 * @see #cache
	 */
	public Class<? extends T> makeClass(Class<?> rootClass) {
		Class<? extends T> wrapper = cache.get(rootClass);
		if (wrapper == null) {
			wrapper = createClass(rootClass.getName() + suffix, rootClass);
			cache.put(rootClass, wrapper);
		}
		return wrapper;
	}

	/**
	 * Create an API from the specified object
	 * If the instance is in the cache then use that
	 *
	 * @param rootInstance The class instance to base it off
	 * @return The resulting instance
	 * @see #instanceCache
	 * @see #makeClass(Class)
	 */
	@SuppressWarnings("unchecked")
	public T makeInstance(Object rootInstance) {
		// Support loading from the cache
		T instance = instanceCache.get(rootInstance);
		if (instance == null) {
			Class<?> rootClass = rootInstance.getClass();
			Class<?> wrapper = makeClass(rootClass);

			try {
				instance = (T) wrapper.getConstructor(rootClass).newInstance(rootInstance);
				instanceCache.put(rootInstance, instance);
			} catch (ReflectiveOperationException e) {
				// This should NEVER happen. We've made this class, so we should never get any errors
				throw new RuntimeException("Cannot create API", e);
			}
		}

		return instance;
	}

	/**
	 * Make a new wrapper class
	 *
	 * @param name     The name of the class to create
	 * @param original The original class to base it off
	 * @return The created class
	 */
	@SuppressWarnings("unchecked")
	protected Class<? extends T> createClass(String name, Class<?> original) {
		byte[] bytes = new APIBuilder(name, original, this).toByteArray();
		if (verify) {
			AsmUtils.validateClass(bytes);
		}

		return (Class<? extends T>) defineClass(name, bytes, 0, bytes.length);
	}

	/**
	 * Get the type converter to use
	 *
	 * @return The current converter
	 * @see #converter
	 */
	public Converter getConverter() {
		return converter;
	}

	/**
	 * Get the method transformer to use
	 *
	 * @return The transformer
	 * @see #transformer
	 */
	public Transformer getTransformer() {
		return transformer;
	}

	/**
	 * Create a loader from the specified type
	 *
	 * @param parentClass The parent class to create from
	 * @param <T>         The parent class to create from. This should be filled in automatically by parentClass
	 * @return The creates {@link APIClassLoader}
	 * @see #parentClass
	 */
	public static <T extends LuaObject> APIClassLoader<T> createLoader(Class<T> parentClass) {
		return new APIClassLoader<>(parentClass);
	}

	/**
	 * Create a loader using {@link LuaObjectWrapper} as the parent class
	 *
	 * @return The creates {@link APIClassLoader}
	 * @see #createLoader(Class)
	 */
	public static APIClassLoader<LuaObjectWrapper> createLoader() {
		return createLoader(LuaObjectWrapper.class);
	}
}
