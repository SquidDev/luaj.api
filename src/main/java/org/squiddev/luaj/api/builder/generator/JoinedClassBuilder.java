package org.squiddev.luaj.api.builder.generator;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.squiddev.luaj.api.builder.tree.LuaClass;
import org.squiddev.luaj.api.builder.tree.LuaMethod;

import static org.objectweb.asm.Opcodes.*;
import static org.squiddev.luaj.api.builder.BuilderConstants.*;

/**
 * An API builder that writes all methods to one class
 */
public class JoinedClassBuilder extends ClassBuilder {
	protected MethodVisitor invokeVisitor;

	public JoinedClassBuilder(String name, LuaClass klass) {
		super(name, klass);
	}

	@Override
	public MethodBuilder createBuilder(LuaMethod method) {
		return new JoinedMethodBuilder(method, this, invokeVisitor);
	}

	/**
	 * Create the main {@link org.squiddev.luaj.api.LuaObjectWrapper#invoke(org.luaj.vm2.Varargs, int)} method
	 */
	protected void writeInvoke() {
		MethodVisitor mv = invokeVisitor = writer.visitMethod(ACC_PUBLIC, "invoke", INVOKE_SIGNATURE, null, null);
		mv.visitCode();

		// Get index
		mv.visitVarInsn(ILOAD, 2);

		Label defaultLabel = new Label();

		int size = klass.methods.size();
		Label[] labels = new Label[size];

		for (int i = 0; i < size; i++) {
			labels[i] = new Label();
		}

		// Create a switch
		mv.visitTableSwitchInsn(0, size - 1, defaultLabel, labels);

		int counter = 0;
		for (LuaMethod method : klass.methods) {
			// Setup the jump for this method
			mv.visitLabel(labels[counter]);
			mv.visitFrame(F_SAME, 0, null, 0, null);
			createBuilder(method).visit();

			counter++;
		}

		// default:
		mv.visitLabel(defaultLabel);
		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		// return LuaValue.NONE;
		mv.visitFieldInsn(GETSTATIC, TYPE_LUAVALUE, "NONE", CLASS_LUAVALUE);
		mv.visitInsn(ARETURN);

		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}
}
