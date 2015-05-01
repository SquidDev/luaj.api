package org.squiddev.luaj.api.builder.generator;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.squiddev.luaj.api.builder.tree.LuaMethod;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.IF_ICMPLT;
import static org.squiddev.luaj.api.builder.BuilderConstants.*;
import static org.squiddev.luaj.api.utils.AsmUtils.constantOpcode;

/**
 * A method visitor that writes every method into the
 */
public class JoinedMethodBuilder extends MethodBuilder {
	protected final MethodVisitor mv;

	/**
	 * Create a MethodBuilder
	 *
	 * @param method  The method we are writing
	 * @param builder The settings for the builder
	 * @param visitor The visitor we are writing to
	 */
	public JoinedMethodBuilder(LuaMethod method, ClassBuilder builder, MethodVisitor visitor) {
		super(method, builder);
		mv = visitor;
	}

	@Override
	protected MethodVisitor getInvokeVisitor() {
		return mv;
	}

	/**
	 * Validate the length of the arguments
	 *
	 * @param nArgs   Number of arguments
	 * @param onError Label to jump to on an error
	 */
	@Override
	protected void validateArgLength(int nArgs, Label onError) {
		MethodVisitor mv = getInvokeVisitor();

		mv.visitVarInsn(ALOAD, 1);
		VARARGS_NARGS.inject(mv);
		constantOpcode(mv, nArgs);
		mv.visitJumpInsn(IF_ICMPLT, onError);
	}

	/**
	 * Load an argument
	 *
	 * @param arg The argument to load
	 * @see org.luaj.vm2.Varargs#arg(int)
	 */
	@Override
	protected void loadArgument(int arg) {
		MethodVisitor mv = getInvokeVisitor();

		mv.visitVarInsn(ALOAD, 1);
		constantOpcode(mv, arg);
		VARARGS_ARG.inject(mv);
	}

	/**
	 * Load subargs
	 *
	 * @param offset The subargs offset
	 * @see org.luaj.vm2.Varargs#subargs(int)
	 */
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
