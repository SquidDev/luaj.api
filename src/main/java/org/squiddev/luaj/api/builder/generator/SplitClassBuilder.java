package org.squiddev.luaj.api.builder.generator;

import org.objectweb.asm.MethodVisitor;
import org.squiddev.luaj.api.builder.tree.LuaClass;
import org.squiddev.luaj.api.builder.tree.LuaMethod;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;
import static org.squiddev.luaj.api.builder.BuilderConstants.*;
import static org.squiddev.luaj.api.utils.AsmUtils.constantOpcode;

/**
 * A class builder that writes to one class for each function
 */
public class SplitClassBuilder extends ClassBuilder {
	protected Map<LuaMethod, String> names;
	protected Map<String, byte[]> bytes;

	public SplitClassBuilder(String name, LuaClass klass) {
		super(name, klass);
	}

	protected void setupNames() {
		Map<LuaMethod, String> names = this.names = new HashMap<>();
		bytes = new HashMap<>();

		int i = 0;
		for (LuaMethod method : klass.methods) {
			names.put(method, className + "$" + i + "_" + method.names.iterator().next());
			i++;
		}
	}

	@Override
	protected void write() {
		setupNames();

		writer.visitField(ACC_PRIVATE | ACC_FINAL, METHODS, METHODS_SIGNATURE, null, null).visitEnd();

		super.write();
	}

	@Override
	protected void writeInitFields(MethodVisitor mv) {
		super.writeInitFields(mv);


		mv.visitVarInsn(ALOAD, 0);
		constantOpcode(mv, names.size());
		mv.visitTypeInsn(ANEWARRAY, "[Ljava/lang/String;");
		int i = 0;
		for (String name : names.values()) {
			mv.visitInsn(DUP);
			constantOpcode(mv, i++);

			mv.visitTypeInsn(NEW, name);
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKESPECIAL, name, "<init>", "(" + originalWhole + ")V", false);
			mv.visitInsn(AASTORE);
		}
	}

	@Override
	protected void writeInvoke() {
		for (LuaMethod method : klass.methods) {
			SplitMethodBuilder builder = createBuilder(method);
			builder.write();
			bytes.put(names.get(method), builder.getBytes());
		}

		MethodVisitor visitor = CREATE_TABLE.create(writer);
		visitor.visitTypeInsn(NEW, TYPE_LUATABLE);
		visitor.visitInsn(DUP);
		visitor.visitMethodInsn(INVOKESTATIC, TYPE_LUATABLE, "<init>", "()V", false);

		String signature = "(" + originalWhole + ")V";
		for (LuaMethod method : klass.methods) {
			String methodClassName = names.get(method);
			for (String name : method.names) {
				// Duplicate object
				visitor.visitInsn(DUP);

				visitor.visitLdcInsn(name);

				visitor.visitTypeInsn(NEW, methodClassName);
				visitor.visitInsn(DUP);

				visitor.visitVarInsn(ALOAD, 0);
				visitor.visitFieldInsn(GETFIELD, className, "instance", originalWhole);

				visitor.visitMethodInsn(INVOKESTATIC, TYPE_LUATABLE, "<init>", signature, false);

				TABLE_SET_STRING.inject(visitor);
			}
		}

		visitor.visitInsn(ARETURN);
		visitor.visitMaxs(0, 0);
		visitor.visitEnd();
	}

	@Override
	public byte[] writeClasses(Map<String, byte[]> extras) {
		extras.putAll(bytes);
		return super.writeClasses(extras);
	}

	@Override
	public SplitMethodBuilder createBuilder(LuaMethod method) {
		String name = names.get(method);
		int length = method.validationIterator().requiredLength();
		if (method.returnsVarags || length > 3) {
			return new SplitMethodBuilder.VarArgBuilder(method, this, name);
		} else if (length == 0) {
			return new SplitMethodBuilder.ZeroArgBuilder(method, this, name);
		}

		return new SplitMethodBuilder.FiniteArgBuilder(method, this, name);
	}
}
