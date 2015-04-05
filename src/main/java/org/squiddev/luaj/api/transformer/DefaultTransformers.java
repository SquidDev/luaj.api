package org.squiddev.luaj.api.transformer;

import org.squiddev.luaj.api.Alias;
import org.squiddev.luaj.api.builder.LuaMethod;

import java.util.Collections;

/**
 * A series of useful transformers
 */
public class DefaultTransformers extends Transformer {
	public DefaultTransformers() {
		addMethodTransformer(Alias.class, new AliasTransformer());
	}

	public static class AliasTransformer implements ITransformer<LuaMethod, Alias> {
		/**
		 * Modify an item
		 *
		 * @param target     The value to modify
		 * @param annotation The annotation about this item
		 */
		@Override
		public void transform(LuaMethod target, Alias annotation) {
			String[] names = annotation.value();
			if (names == null) return;
			Collections.addAll(target.names, names);
		}
	}
}
