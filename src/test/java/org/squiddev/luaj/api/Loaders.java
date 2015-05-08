package org.squiddev.luaj.api;

import org.squiddev.luaj.api.builder.APIClassLoader;
import org.squiddev.luaj.api.builder.generator.ClassBuilder;
import org.squiddev.luaj.api.builder.generator.JoinedClassBuilder;
import org.squiddev.luaj.api.builder.generator.SplitClassBuilder;
import org.squiddev.luaj.api.builder.tree.LuaClass;

/**
 * Holds all the loaders required
 */
public final class Loaders {
	private static final APIClassLoader[] loaders = new APIClassLoader[]{
		new APIClassLoader<LuaObjectWrapper>(LuaObjectWrapper.class) {
			{
				settings.verify = true;
			}

			@Override
			protected ClassBuilder createBuilder(String name, Class<?> original) {
				name = name.replace('.', '/');
				LuaClass klass = new LuaClass(name, original, settings);

				return new JoinedClassBuilder(name, klass);
			}

			@Override
			public String toString() {
				return "JoinedClassBuilder";
			}
		},
		new APIClassLoader<LuaObject>(LuaObject.class) {
			{
				settings.verify = true;
			}

			@Override
			protected ClassBuilder createBuilder(String name, Class<?> original) {
				name = name.replace('.', '/');
				LuaClass klass = new LuaClass(name, original, settings);

				return new SplitClassBuilder(name, klass);
			}

			@Override
			public String toString() {
				return "SplitClassBuilder";
			}
		}
	};

	public static APIClassLoader[][] getLoaderArgs() {
		APIClassLoader[][] args = new APIClassLoader[loaders.length][];
		for (int i = 0; i < loaders.length; i++) {
			args[i] = new APIClassLoader[]{loaders[i]};
		}
		return args;
	}
}
