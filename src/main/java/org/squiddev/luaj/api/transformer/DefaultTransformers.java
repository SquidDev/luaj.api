package org.squiddev.luaj.api.transformer;

import org.luaj.vm2.LuaValue;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.squiddev.luaj.api.Alias;
import org.squiddev.luaj.api.Field;
import org.squiddev.luaj.api.LuaAPI;
import org.squiddev.luaj.api.builder.BuilderException;
import org.squiddev.luaj.api.builder.IInjector;
import org.squiddev.luaj.api.builder.tree.LuaArgument;
import org.squiddev.luaj.api.builder.tree.LuaClass;
import org.squiddev.luaj.api.builder.tree.LuaField;
import org.squiddev.luaj.api.builder.tree.LuaMethod;
import org.squiddev.luaj.api.setters.Setter;
import org.squiddev.luaj.api.validation.ValidationClass;

import java.util.Collections;

import static org.objectweb.asm.Opcodes.*;
import static org.squiddev.luaj.api.builder.BuilderConstants.*;

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

		addFieldTransformer(Setter.class, new ITransformer<LuaField, Setter>() {
			@Override
			public void transform(LuaField target, Setter annotation) {
				target.setup = Setter.SetterCache.getInstance(annotation.value());
			}
		});

		addFieldTransformer(Field.class, new ITransformer<LuaField, Field>() {
			@Override
			public void transform(final LuaField target, final Field field) {
				String[] names = field.value();
				if (names == null || (names.length == 1 && names[0].isEmpty())) {
					names = new String[]{target.field.getName()};
				}

				final String[] finalNames = names;

				target.klass.setup.add(new IInjector<LuaClass>() {
					@Override
					public void inject(MethodVisitor mv, LuaClass klass) {
						Class<?> type = target.field.getType();

						mv.visitVarInsn(ALOAD, 0);
						mv.visitFieldInsn(GETFIELD, klass.name, INSTANCE, Type.getDescriptor(klass.klass));
						mv.visitFieldInsn(GETFIELD, Type.getInternalName(klass.klass), target.field.getName(), Type.getDescriptor(type));

						// If it isn't an array or if it is and the array type isn't a subclass of LuaValue
						if (!type.isArray() || !LuaValue.class.isAssignableFrom(type.getComponentType())) {
							// Check if we have a converter
							IInjector<LuaClass> converter = klass.settings.converter.getToLua(type);
							if (converter == null) {
								throw new BuilderException("Cannot convert " + type.getName() + " to LuaValue for ", klass);
							}

							converter.inject(mv, klass);
						}

						// If we return an array then try return a {@link LuaTable} or {@link Varargs}
						if (type.isArray()) LIST_OF.inject(mv);

						mv.visitVarInsn(ASTORE, 1);

						for (String finalName : finalNames) {
							mv.visitVarInsn(ALOAD, 0);
							mv.visitFieldInsn(GETFIELD, klass.name, "table", CLASS_LUATABLE);
							mv.visitLdcInsn(finalName);
							mv.visitVarInsn(ALOAD, 1);
							TABLE_SET_STRING.inject(mv);
						}
					}
				});
			}
		});
	}
}
