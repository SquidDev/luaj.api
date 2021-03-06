package org.squiddev.luaj.api.validation;

import org.luaj.vm2.LuaValue;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.squiddev.luaj.api.builder.BuilderException;

import static org.objectweb.asm.Opcodes.INSTANCEOF;

/**
 * Validates values using <code>instanceof</code>
 *
 * All number types are compared against {@link org.luaj.vm2.LuaNumber}
 */
public class StrictValidator extends DefaultLuaValidator {
	@Override
	public boolean addValidation(MethodVisitor mv, Class<?> type) {
		if (type.equals(boolean.class)) {
			mv.visitTypeInsn(INSTANCEOF, "org/luaj/vm2/LuaBoolean");
		} else if (
			type.equals(byte.class) || type.equals(int.class) || type.equals(char.class) || type.equals(short.class) ||
				type.equals(float.class) || type.equals(double.class) || type.equals(long.class)
			) {
			mv.visitTypeInsn(INSTANCEOF, "org/luaj/vm2/LuaNumber");
		} else if (type.equals(String.class)) {
			mv.visitTypeInsn(INSTANCEOF, "org/luaj/vm2/LuaString");
		} else if (LuaValue.class.isAssignableFrom(type)) {
			mv.visitTypeInsn(INSTANCEOF, Type.getInternalName(type));
		} else {
			throw new BuilderException("Cannot validate " + type.getName());
		}

		return true;
	}
}
