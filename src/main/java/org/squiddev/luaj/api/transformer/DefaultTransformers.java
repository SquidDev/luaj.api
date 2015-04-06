package org.squiddev.luaj.api.transformer;

import org.squiddev.luaj.api.Alias;
import org.squiddev.luaj.api.LuaAPI;
import org.squiddev.luaj.api.builder.tree.LuaArgument;
import org.squiddev.luaj.api.builder.tree.LuaClass;
import org.squiddev.luaj.api.builder.tree.LuaMethod;
import org.squiddev.luaj.api.validation.ValidationClass;

import java.util.Collections;

/**
 * A series of useful transformers
 */
public class DefaultTransformers extends Transformer {
	public DefaultTransformers() {
		addMethodTransformer(Alias.class, new ITransformer<LuaMethod, Alias>() {
			@Override
			public void transform(LuaMethod target, Alias annotation) {
				String[] names = annotation.value();
				if (names == null) return;
				Collections.addAll(target.names, names);
			}
		});

		addClassTransformer(LuaAPI.class, new ITransformer<LuaClass, LuaAPI>() {
			@Override
			public void transform(LuaClass target, LuaAPI annotation) {
				String[] names = annotation.value();
				if (names != null && names.length != 0 && (names.length != 1 || !names[0].isEmpty())) {
					Collections.addAll(target.names, names);
				}
			}
		});

		addClassTransformer(ValidationClass.class, new ITransformer<LuaClass, ValidationClass>() {
			@Override
			public void transform(LuaClass target, ValidationClass annotation) {
				target.validator = annotation.value();
			}
		});
		addMethodTransformer(ValidationClass.class, new ITransformer<LuaMethod, ValidationClass>() {
			@Override
			public void transform(LuaMethod target, ValidationClass annotation) {
				target.validator = annotation.value();
			}
		});
		addArgumentTransformer(ValidationClass.class, new ITransformer<LuaArgument, ValidationClass>() {
			@Override
			public void transform(LuaArgument target, ValidationClass annotation) {
				target.validator = annotation.value();
			}
		});
	}
}
