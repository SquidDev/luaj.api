package org.squiddev.luaj.api.setters;

import org.objectweb.asm.MethodVisitor;
import org.squiddev.luaj.api.builder.APIBuilder;
import org.squiddev.luaj.api.builder.APIClassLoader;
import org.squiddev.luaj.api.builder.IInjector;
import org.squiddev.luaj.api.builder.tree.LuaField;

import static org.objectweb.asm.Opcodes.GETSTATIC;

/**
 * Sets the field to be the class loader
 */
public class LoaderSetter implements IInjector<LuaField> {
	/**
	 * Inject bytecode into a visitor
	 *
	 * @param visitor The method visitor to inject into
	 * @param field   The field that we are injecting with
	 */
	@Override
	public void inject(MethodVisitor visitor, LuaField field) {
		if (!field.field.getType().isAssignableFrom(APIClassLoader.class)) {
			throw new APIBuilder.BuilderException("Cannot convert " + field.field.getType().getName() + " to APIClassLoader", field);
		}

		visitor.visitFieldInsn(GETSTATIC, field.klass.name, "LOADER", APIBuilder.CLASS_LOADER);
	}
}
