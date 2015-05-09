package org.squiddev.luaj.api.builder.generator;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.squiddev.luaj.api.builder.BuilderException;
import org.squiddev.luaj.api.builder.tree.LuaMethod;
import org.squiddev.luaj.api.utils.TinyMethod;

import static org.objectweb.asm.Opcodes.*;
import static org.squiddev.luaj.api.builder.BuilderConstants.*;
import static org.squiddev.luaj.api.utils.AsmUtils.constantOpcode;

/**
 * A method builder that writes one class for each function
 */
public abstract class SplitMethodBuilder extends MethodBuilder {
	protected final String name;
	protected final TinyMethod invokeMethod;
	protected MethodVisitor mv;

	protected byte[] bytes;

	/**
	 * Create a MethodBuilder
	 *
	 * @param method  The method we are writing
	 * @param builder The settings for the builder
	 * @param name    The name of the generated class
	 */
	public SplitMethodBuilder(LuaMethod method, ClassBuilder builder, String name, TinyMethod invokeMethod) {
		super(method, builder);
		this.name = name;
		this.invokeMethod = invokeMethod;
	}

	@Override
	public void write() {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);

		// Declare class name
		writer.visit(V1_6, ACC_PUBLIC + ACC_SUPER, name, null, invokeMethod.className, null);

		// Declare instance
		writer.visitField(ACC_PRIVATE | ACC_FINAL, INSTANCE, builder.originalWhole, null, null).visitEnd();

		// Constructor
		{
			MethodVisitor init = writer.visitMethod(ACC_PUBLIC, "<init>", "(" + builder.originalWhole + ")V", null, null);
			init.visitCode();
			// Init parent
			init.visitVarInsn(ALOAD, 0);
			init.visitMethodInsn(INVOKESPECIAL, invokeMethod.className, "<init>", "()V", false);
			// Set instance
			init.visitVarInsn(ALOAD, 0);
			init.visitVarInsn(ALOAD, 1);
			init.visitFieldInsn(PUTFIELD, name, INSTANCE, builder.originalWhole);

			init.visitInsn(RETURN);
			init.visitMaxs(0, 0);
			init.visitEnd();
		}

		{
			MethodVisitor invoke = this.mv = invokeMethod.create(writer);
			super.write();
			invoke.visitMaxs(0, 0);
			invoke.visitEnd();
		}

		writer.visitEnd();
		bytes = writer.toByteArray();
	}

	/**
	 * Get the bytes for this visitor
	 *
	 * @return The bytes for this class
	 */
	public byte[] getBytes() {
		return bytes;
	}

	@Override
	protected MethodVisitor getInvokeVisitor() {
		return mv;
	}

	@Override
	protected String getClassName() {
		return name;
	}

	public static class FiniteArgBuilder extends SplitMethodBuilder {
		protected static final TinyMethod[] ARG_LENGTHS = new TinyMethod[]{
			new TinyMethod(OneArgFunction.class, "call", LuaValue.class),
			new TinyMethod(TwoArgFunction.class, "call", LuaValue.class, LuaValue.class),
			new TinyMethod(ThreeArgFunction.class, "call", LuaValue.class, LuaValue.class, LuaValue.class),
		};

		public FiniteArgBuilder(LuaMethod method, ClassBuilder builder, String name, TinyMethod invokeMethod) {
			super(method, builder, name, invokeMethod);
		}

		public FiniteArgBuilder(LuaMethod method, ClassBuilder builder, String name) {
			super(method, builder, name, ARG_LENGTHS[method.arguments.length - 1]);
		}

		@Override
		protected void validateArgLength(int nArgs, Label onError) {
		}

		@Override
		protected void loadArgument(int arg) {
			getInvokeVisitor().visitVarInsn(ALOAD, arg);
		}

		@Override
		protected void loadVarArg(int offset) {
			throw new BuilderException("Cannot load vararg for finite arg function", method);
		}
	}

	public static class ZeroArgBuilder extends FiniteArgBuilder {
		protected static final TinyMethod INVOKE_ZERO = new TinyMethod(ZeroArgFunction.class, "call");

		public ZeroArgBuilder(LuaMethod method, ClassBuilder builder, String name) {
			super(method, builder, name, INVOKE_ZERO);
		}

		@Override
		protected void loadArgument(int arg) {
			throw new BuilderException("Cannot load argument for 0 arg function", method);
		}
	}

	public static class VarArgBuilder extends SplitMethodBuilder {
		protected static final TinyMethod INVOKE_VAR = new TinyMethod(VarArgFunction.class, "invoke", Varargs.class);

		public VarArgBuilder(LuaMethod method, ClassBuilder builder, String name) {
			super(method, builder, name, INVOKE_VAR);
		}

		@Override
		protected void validateArgLength(int nArgs, Label onError) {
			MethodVisitor mv = getInvokeVisitor();

			mv.visitVarInsn(ALOAD, 1);
			VARARGS_NARGS.inject(mv);
			constantOpcode(mv, nArgs);
			mv.visitJumpInsn(IF_ICMPLT, onError);
		}

		@Override
		protected void loadArgument(int arg) {
			MethodVisitor mv = getInvokeVisitor();

			mv.visitVarInsn(ALOAD, 1);
			constantOpcode(mv, arg);
			VARARGS_ARG.inject(mv);
		}

		@Override
		protected void loadVarArg(int offset) {
			MethodVisitor mv = getInvokeVisitor();
			mv.visitVarInsn(ALOAD, 1);

			// If we just have varargs then we should load it, if we have varargs later then use subargs
			if (offset > 1) {
				constantOpcode(mv, offset);
				VARARGS_SUBARGS.inject(mv);
			}
		}
	}
}
