package org.squiddev.luaj.api.builder.generator;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.squiddev.luaj.api.builder.BuilderException;
import org.squiddev.luaj.api.builder.IInjector;
import org.squiddev.luaj.api.builder.tree.LuaArgument;
import org.squiddev.luaj.api.builder.tree.LuaMethod;
import org.squiddev.luaj.api.validation.ILuaValidator;

import static org.objectweb.asm.Opcodes.*;
import static org.squiddev.luaj.api.builder.BuilderConstants.*;

/**
 * Used to write individual methods
 */
public abstract class MethodBuilder {
	public final LuaMethod method;
	public final ClassBuilder builder;

	/**
	 * Create a MethodBuilder
	 *
	 * @param method  The method we are writing
	 * @param builder The settings for the builder
	 */
	public MethodBuilder(LuaMethod method, ClassBuilder builder) {
		this.method = method;
		this.builder = builder;
	}

	/**
	 * Get the method visitor to write the invoke method to
	 *
	 * @return The method visitor to write
	 */
	protected abstract MethodVisitor getInvokeVisitor();

	/**
	 * Write the method
	 */
	public void write() {
		MethodVisitor mv = getInvokeVisitor();

		// Validate the arguments
		writeValidation();

		// Load the instance and validate its type
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, builder.className, "instance", "Ljava/lang/Object;");
		mv.visitTypeInsn(CHECKCAST, builder.originalName);

		// Convert the arguments
		writeArgumentConversions();

		// And call the method
		mv.visitMethodInsn(INVOKEVIRTUAL, builder.originalName, method.method.getName(), Type.getMethodDescriptor(method.method), false);

		// And return
		writeReturn();
	}

	/**
	 * Validate arguments
	 */
	protected void writeValidation() {
		MethodVisitor mv = getInvokeVisitor();

		LuaMethod.ValidationIterator iterator = method.validationIterator();
		StringBuilder exception = new StringBuilder("Expected ");
		boolean needsValidation = iterator.hasValidateNext();

		Label doException = new Label();
		Label noException = new Label();

		// If we should validate then assert how many values there are
		if (needsValidation && iterator.requiredLength() > 0) {
			validateArgLength(iterator.requiredLength(), doException);
		}

		int index = 1;
		while (iterator.hasNext()) {
			LuaArgument arg = iterator.next();
			Class<?> type = arg.parameter.getType();
			ILuaValidator validator = arg.getValidator();

			// If the item is a varargs then we shouldn't give it a name. Varargs will always be the last item
			if (!type.equals(Varargs.class)) exception.append(validator.getName(type)).append(", ");

			if (validator.shouldValidate(type)) {
				loadArgument(index);
				validator.addValidation(mv, type);

				if (iterator.hasValidateNext()) {
					// If (condition) is false (== 0) then go to exception, else continue
					mv.visitJumpInsn(IFEQ, doException);
				} else {
					// If (condition) is true (== 1) then no exception
					mv.visitJumpInsn(IFNE, noException);
				}
			}

			++index;
		}

		if (needsValidation) {
			// Do exception
			mv.visitLabel(doException);
			mv.visitFrame(F_SAME, 0, null, 0, null);
			mv.visitTypeInsn(NEW, TYPE_LUAERROR);
			mv.visitInsn(DUP);

			String error = method.errorMessage;
			String text = exception.toString();
			if (error == null) {
				if (text.endsWith(", ")) text = text.substring(0, text.length() - 2);
				error = text;
			}
			mv.visitLdcInsn(error);
			mv.visitMethodInsn(INVOKESPECIAL, TYPE_LUAERROR, "<init>", "(Ljava/lang/String;)V", false);
			mv.visitInsn(ATHROW);

			// Continue
			mv.visitLabel(noException);
			mv.visitFrame(F_SAME, 0, null, 0, null);
		}
	}

	/**
	 * Load the arguments and convert them
	 */
	protected void writeArgumentConversions() {
		MethodVisitor mv = getInvokeVisitor();
		int argCounter = 1;

		for (LuaArgument arg : method.validationIterator()) {
			Class<?> argType = arg.parameter.getType();
			if (argType.equals(Varargs.class)) {
				loadVarArg(argCounter);
			} else {
				loadArgument(argCounter);

				IInjector<LuaMethod> type = builder.settings.converter.getFromLua(argType);
				if (type == null) throw new BuilderException("Cannot convert LuaValue to " + argType, method);
				type.inject(mv, method);
			}

			++argCounter;
		}
	}

	/**
	 * Convert the returned variable to a {@link org.luaj.vm2.Varargs}
	 */
	protected void writeReturn() {
		MethodVisitor mv = getInvokeVisitor();
		Class<?> returns = method.method.getReturnType();

		if (returns.equals(Void.TYPE)) {
			// If no result, return None
			mv.visitFieldInsn(GETSTATIC, TYPE_LUAVALUE, "NONE", CLASS_LUAVALUE);
		} else if (!Varargs.class.isAssignableFrom(returns)) { // Don't need to convert if returning a LuaValue
			// If it isn't an array or if it is and the array type isn't a subclass of LuaValue
			if (!returns.isArray() || !LuaValue.class.isAssignableFrom(returns.getComponentType())) {
				// Check if we have a converter
				IInjector<LuaMethod> type = builder.settings.converter.getToLua(returns);
				if (type == null) {
					throw new BuilderException("Cannot convert " + returns.getName() + " to LuaValue for ", method);
				}

				type.inject(mv, method);
			}

			// If we return an array then try return a {@link LuaTable} or {@link Varargs}
			if (returns.isArray()) {
				if (method.returnsVarags) {
					VARARGS_OF.inject(mv);
				} else {
					LIST_OF.inject(mv);
				}
			}
		}

		mv.visitInsn(ARETURN);
	}

	/**
	 * Validate the length of the arguments
	 *
	 * @param nArgs   Number of arguments
	 * @param onError Label to jump to on an error
	 */
	protected abstract void validateArgLength(int nArgs, Label onError);

	/**
	 * Load an argument
	 *
	 * @param arg The argument to load
	 * @see org.luaj.vm2.Varargs#arg(int)
	 */
	protected abstract void loadArgument(int arg);

	/**
	 * Load subargs
	 *
	 * @param offset The subargs offset
	 * @see org.luaj.vm2.Varargs#subargs(int)
	 */
	protected abstract void loadVarArg(int offset);
}
