package org.squiddev.luaj.api.validation;

import org.luaj.vm2.LuaValue;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

/**
 * A validator that uses {@link LuaValue#checkboolean()} types
 */
public class ArgErrorValidation extends DefaultLuaValidator {
	@Override
	public boolean addValidation(MethodVisitor mv, Class<?> type) {
		if (type.equals(boolean.class)) {
			mv.visitMethodInsn(INVOKEVIRTUAL, "org/luaj/vm2/LuaValue", "checkboolean", "()Z", false);
			mv.visitInsn(POP);
			return false;
		} else if (type.equals(byte.class) || type.equals(int.class) || type.equals(char.class) || type.equals(short.class)) {
			mv.visitMethodInsn(INVOKEVIRTUAL, "org/luaj/vm2/LuaValue", "checkint", "()I", false);
			mv.visitInsn(POP);
			return false;
		} else if (type.equals(float.class) || type.equals(double.class)) {
			mv.visitMethodInsn(INVOKEVIRTUAL, "org/luaj/vm2/LuaValue", "checknumber", "()Lorg/luaj/vm2/LuaNumber;", false);
			mv.visitInsn(POP);
			return false;
		} else if (type.equals(long.class)) {
			mv.visitMethodInsn(INVOKEVIRTUAL, "org/luaj/vm2/LuaValue", "checklong", "()J", false);
			mv.visitInsn(POP2);
			return false;
		} else if (type.equals(String.class)) {
			mv.visitMethodInsn(INVOKEVIRTUAL, "org/luaj/vm2/LuaValue", "checkstring", "()Lorg/luaj/vm2/LuaString;", false);
			mv.visitInsn(POP);
			return false;
		} else {
			return super.addValidation(mv, type);
		}
	}
}
