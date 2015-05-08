package org.squiddev.luaj.api.builder;

import org.squiddev.luaj.api.LuaObject;
import org.squiddev.luaj.api.LuaObjectWrapper;
import org.squiddev.luaj.api.builder.generator.ClassBuilder;
import org.squiddev.luaj.api.builder.generator.JoinedClassBuilder;
import org.squiddev.luaj.api.builder.tree.LuaClass;
import org.squiddev.luaj.api.utils.AsmUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Handles loading and generating APIs
 */
public class APIClassLoader<T extends LuaObject> extends ClassLoader {
	protected BuilderSettings settings = new BuilderSettings();

	public APIClassLoader(Class<T> parentClass) {
		settings.parentClass = parentClass;
	}

	/**
	 * The cache for {@link org.squiddev.luaj.api.LuaAPI} classes to {@link LuaObject} classes
	 */
	protected final Map<Class<?>, Class<? extends T>> cache = new WeakHashMap<>();

	/**
	 * Cache for class names to bytes
	 */
	protected Map<String, byte[]> byteCache = new HashMap<>();

	/**
	 * A cache for created instances instead
	 *
	 * @see #makeInstance(Object)
	 */
	protected final Map<Object, T> instanceCache = new WeakHashMap<>();

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
			wrapper = createClass(rootClass.getName() + settings.suffix, rootClass);
			cache.put(rootClass, wrapper);
		}
		return wrapper;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		byte[] bytes = byteCache.get(name);
		if (bytes != null) return defineClass(name, bytes);

		bytes = byteCache.get(name.replace('.', '/'));
		if (bytes != null) return defineClass(name, bytes);

		return super.findClass(name);
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
		return (Class<? extends T>) defineClass(name, createBuilder(name, original).writeClasses(byteCache));
	}

	/**
	 * Create a builder for this class
	 *
	 * @param name     Name of the class to load
	 * @param original The original class we are using
	 * @return The created class builder
	 */
	protected ClassBuilder createBuilder(String name, Class<?> original) {
		name = name.replace('.', '/');
		LuaClass klass = new LuaClass(name, original, settings);

		return new JoinedClassBuilder(name, klass);
	}

	/**
	 * Define a class and verify it before loading
	 *
	 * @param name  The name of the class
	 * @param bytes The bytes of the class to load
	 * @return The generated class
	 */
	protected Class<?> defineClass(String name, byte[] bytes) {
		if (settings.verify) AsmUtils.validateClass(bytes, this);
		return defineClass(name, bytes, 0, bytes.length);
	}

	/**
	 * Create a loader from the specified type
	 *
	 * @param parentClass The parent class to create from
	 * @param <T>         The parent class to create from. This should be filled in automatically by parentClass
	 * @return The creates {@link APIClassLoader}
	 * @see BuilderSettings#parentClass
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
