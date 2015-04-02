package org.squiddev.luaj.api.builder;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.objectweb.asm.*;
import org.squiddev.luaj.api.LuaAPI;
import org.squiddev.luaj.api.LuaFunction;
import org.squiddev.luaj.api.conversion.Converter;
import org.squiddev.luaj.api.utils.TinyMethod;
import org.squiddev.luaj.api.validation.ILuaValidator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;
import static org.squiddev.luaj.api.utils.AsmUtils.constantOpcode;

/**
 * Builds ASM code to call an API
 * TODO: More constants, less strings
 */
public class APIBuilder {
	public static final String VARARGS = Type.getDescriptor(Varargs.class);
	public static final String INVOKE_SIGNATURE = "(" + VARARGS + "I)" + VARARGS;

	/**
	 * The class we build the wrapper for
	 */
	protected final Class<?> klass;

	/**
	 * The {@link ClassWriter} for the wrapper class
	 */
	protected final ClassWriter writer;

	/**
	 * The name of the original class
	 *
	 * @see #klass
	 */
	protected final String originalName;

	/**
	 * The whole name of the original name ('L' + ... + ';')
	 */
	protected final String originalWhole;

	/**
	 * The name of the generated class
	 */
	protected final String className;

	/**
	 * The name of the parent/super class for our generated wrapper
	 *
	 * @see APIClassLoader#parentClass
	 */
	protected final Class<?> parent;

	/**
	 * Stores the conversion
	 *
	 * @see APIClassLoader#converter
	 */
	protected final Converter converter;

	/**
	 * Globals that this should be set to
	 */
	protected String[] names = null;

	/**
	 * List of methods
	 */
	protected List<LuaMethod> methods;

	/**
	 * Create a new {@link APIBuilder}
	 *
	 * @param name   The name of the generated class
	 * @param klass  The class we build a wrapper around
	 * @param loader The class loader to load from. This stores settings about various generation options
	 */
	public APIBuilder(String name, Class<?> klass, APIClassLoader loader) {
		this.klass = klass;

		originalName = Type.getInternalName(klass);
		className = name.replace('.', '/');
		originalWhole = Type.getDescriptor(klass);

		writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		parent = loader.parentClass;
		converter = loader.getConverter();

		write();
	}

	/**
	 * Write everything!
	 */
	protected void write() {
		// Declare class name
		writer.visit(V1_6, ACC_PUBLIC + ACC_SUPER, className, null, Type.getInternalName(parent), null);

		// Declare METHOD_NAMES
		writer.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, "METHOD_NAMES", "[[Ljava/lang/String;", null, null);

		// Declare NAMES
		writer.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, "NAMES", "[Ljava/lang/String;", null, null);

		// Read all methods
		List<LuaMethod> methods = this.methods = new ArrayList<>();
		for (Method m : klass.getMethods()) {
			if (m.isAnnotationPresent(LuaFunction.class)) {
				// Append items to the list
				methods.add(new LuaMethod(m));
			}
		}

		if (methods.size() == 0) throw new BuilderException("No LuaFunction methods", klass);

		if (klass.isAnnotationPresent(LuaAPI.class)) {
			names = klass.getAnnotation(LuaAPI.class).value();
			// If we have the LuaAPI annotation then we should ensure that this is set as an API
			if (names == null || names.length == 0) {
				names = new String[]{klass.getSimpleName().toLowerCase()};
			}
		}

		writeInit();
		writeStaticInit();
		writeInvoke();

		writer.visitEnd();
	}

	/**
	 * Write the static constructor
	 * This constructs the array of array of names.
	 */
	protected void writeStaticInit() {
		MethodVisitor mv = writer.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
		mv.visitCode();

		constantOpcode(mv, methods.size());
		mv.visitTypeInsn(ANEWARRAY, "[Ljava/lang/String;");

		int counter = 0;
		for (LuaMethod m : methods) {
			// For key <counter>
			mv.visitInsn(DUP);
			constantOpcode(mv, counter);

			String[] names = m.getLuaName();

			// Create an array of length <names.length>
			constantOpcode(mv, names.length);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/String");

			int nameCounter = 0;
			for (String name : names) {
				mv.visitInsn(DUP);
				constantOpcode(mv, nameCounter);
				mv.visitLdcInsn(name);
				mv.visitInsn(AASTORE);

				++nameCounter;
			}

			// And store
			mv.visitInsn(AASTORE);

			++counter;
		}

		mv.visitFieldInsn(PUTSTATIC, className, "METHOD_NAMES", "[[Ljava/lang/String;");

		// Visit names
		if (names == null) {
			mv.visitInsn(ACONST_NULL);
		} else {
			constantOpcode(mv, names.length);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/String");

			counter = 0;
			for (String name : names) {
				mv.visitInsn(DUP);
				constantOpcode(mv, counter);
				mv.visitLdcInsn(name);
				mv.visitInsn(AASTORE);

				++counter;
			}
		}

		mv.visitFieldInsn(PUTSTATIC, className, "NAMES", "[Ljava/lang/String;");

		mv.visitInsn(RETURN);

		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}

	/**
	 * Write the constructor. This calls the parent constructor,
	 * sets the instance and sets the method names to be the static field
	 */
	protected void writeInit() {
		MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "<init>", "(" + originalWhole + ")V", null, null);
		mv.visitCode();

		// Parent constructor with argument
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(parent), "<init>", "(Ljava/lang/Object;)V", false);

		// Set method names
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETSTATIC, className, "METHOD_NAMES", "[[Ljava/lang/String;");
		mv.visitFieldInsn(PUTFIELD, className, "methodNames", "[[Ljava/lang/String;");

		// Set method API names
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETSTATIC, className, "NAMES", "[Ljava/lang/String;");
		mv.visitFieldInsn(PUTFIELD, className, "names", "[Ljava/lang/String;");

		// And return
		mv.visitInsn(RETURN);
		mv.visitMaxs(2, 2);
		mv.visitEnd();
	}

	protected void writeInvoke() {
		MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "invoke", INVOKE_SIGNATURE, null, null);
		mv.visitCode();

		// Get index
		mv.visitVarInsn(ILOAD, 2);

		Label defaultLabel = new Label();

		int size = methods.size();
		Label[] labels = new Label[size];

		for (int i = 0; i < size; i++) {
			labels[i] = new Label();
		}

		// Create a switch
		mv.visitTableSwitchInsn(0, size - 1, defaultLabel, labels);

		int counter = 0;
		for (LuaMethod method : methods) {
			// Initial stuff
			mv.visitLabel(labels[counter]);
			mv.visitFrame(F_SAME, 0, null, 0, null);

			LuaMethod.ValidationIterator iterator = method.validationIterator();
			StringBuilder builder = new StringBuilder("Expected ");
			boolean needsValidation = iterator.hasValidateNext();

			Label doException = new Label();
			Label noException = new Label();

			if (needsValidation) {
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKEVIRTUAL, "org/luaj/vm2/Varargs", "narg", "()I", false);
				constantOpcode(mv, iterator.length());
				mv.visitJumpInsn(IF_ICMPLT, doException);
			}

			int index = 1;
			while (iterator.hasNext()) {
				LuaMethod.LuaArgument arg = iterator.next();
				Class<?> type = arg.type;
				ILuaValidator validator = arg.getValidator();

				// If the item is a varargs then we shouldn't give it a name. Varargs will always be the last item
				if (!type.equals(Varargs.class)) builder.append(validator.getName(type)).append(", ");

				if (validator.shouldValidate(type)) {
					mv.visitVarInsn(ALOAD, 1);
					constantOpcode(mv, index);
					mv.visitMethodInsn(INVOKEVIRTUAL, "org/luaj/vm2/Varargs", "arg", "(I)Lorg/luaj/vm2/LuaValue;", false);
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
				mv.visitTypeInsn(NEW, "org/luaj/vm2/LuaError");
				mv.visitInsn(DUP);

				String error = method.getError();
				String text = builder.toString();
				if (error == null) {
					if (text.endsWith(", ")) text = text.substring(0, text.length() - 2);
					error = text;
				}
				mv.visitLdcInsn(error);
				mv.visitMethodInsn(INVOKESPECIAL, "org/luaj/vm2/LuaError", "<init>", "(Ljava/lang/String;)V", false);
				mv.visitInsn(ATHROW);

				// Continue
				mv.visitLabel(noException);
				mv.visitFrame(F_SAME, 0, null, 0, null);
			}

			// Check the object is correct
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, className, "instance", "Ljava/lang/Object;");
			mv.visitTypeInsn(CHECKCAST, originalName);

			// Load the arguments
			int argCounter = 1;
			iterator.rewind();
			for (LuaMethod.LuaArgument arg : iterator) {
				mv.visitVarInsn(ALOAD, 1);

				Class<?> argType = arg.type;
				if (argType.equals(Varargs.class)) {
					// If we just have varargs then we should load it, if we have varargs later then use subargs
					if (iterator.length() > 1) {
						constantOpcode(mv, argCounter);
						mv.visitMethodInsn(INVOKEVIRTUAL, "org/luaj/vm2/Varargs", "subargs", "(I)Lorg/luaj/vm2/Varargs;", false);
					}
				} else {
					constantOpcode(mv, argCounter);
					mv.visitMethodInsn(INVOKEVIRTUAL, "org/luaj/vm2/Varargs", "arg", "(I)Lorg/luaj/vm2/LuaValue;", false);

					// Allows having just LuaValue or Object
					// This allows for if you just want to package the annotations and nothing else when supplying an API
					if (!argType.equals(Object.class) && !argType.isInstance(LuaValue.class)) {
						if (LuaValue.class.isAssignableFrom(argType)) {
							// Cast to the type required
							mv.visitTypeInsn(CHECKCAST, Type.getInternalName(argType));
						} else {
							TinyMethod type = converter.fromLua.get(argType);
							if (type == null) {
								throw new BuilderException("Cannot convert LuaValue to " + argType, method);
							}

							type.inject(mv, INVOKEVIRTUAL);
						}
					}
				}

				++argCounter;
			}

			// And call it
			mv.visitMethodInsn(INVOKEVIRTUAL, originalName, method.getJavaName(), Type.getMethodDescriptor(method.method), false);

			Class<?> returns = method.method.getReturnType();
			if (returns.equals(Void.TYPE)) {
				// If no result, return None
				mv.visitFieldInsn(GETSTATIC, "org/luaj/vm2/LuaValue", "NONE", "Lorg/luaj/vm2/LuaValue;");
			} else if (!Varargs.class.isAssignableFrom(returns)) { // Don't need to convert if returning a LuaValue
				if (!returns.isArray() || !LuaValue.class.isAssignableFrom(returns.getComponentType())) {
					// Check if we have a converter
					TinyMethod type = converter.toLua.get(returns);
					if (type == null) {
						throw new BuilderException("Cannot convert " + returns.getName() + " to LuaValue for ", method);
					}

					type.inject(mv, INVOKESTATIC);
				}

				// If we return an array then try return a {@link LuaTable} or {@link Varargs}
				if (returns.isArray()) {
					if (method.function.isVarArgs()) {
						mv.visitMethodInsn(INVOKESTATIC, "org/luaj/vm2/LuaValue", "varargsOf", "([Lorg/luaj/vm2/LuaValue;)Lorg/luaj/vm2/Varargs;", false);
					} else {
						mv.visitMethodInsn(INVOKESTATIC, "org/luaj/vm2/LuaValue", "listOf", "([Lorg/luaj/vm2/LuaValue;)Lorg/luaj/vm2/LuaTable;", false);
					}
				}
			}

			mv.visitInsn(ARETURN);
			counter++;
		}

		// default:
		mv.visitLabel(defaultLabel);
		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		// return LuaValue.NONE;
		mv.visitFieldInsn(GETSTATIC, "org/luaj/vm2/LuaValue", "NONE", "Lorg/luaj/vm2/LuaValue;");
		mv.visitInsn(ARETURN);

		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}

	public byte[] toByteArray() {
		return writer.toByteArray();
	}

	/**
	 * Exception thrown on generating classes
	 */
	public static class BuilderException extends RuntimeException {
		public BuilderException(String message, Throwable cause) {
			super(message, cause);
		}

		public BuilderException(String message) {
			super(message, null);
		}

		public BuilderException(String message, Class javaClass, Throwable cause) {
			this(javaClass.getName() + ": " + message, cause);
		}

		public BuilderException(String message, Class javaClass) {
			this(message, javaClass, null);
		}

		public BuilderException(String message, Method method, Throwable cause) {
			this(method.getName() + ": " + message, method.getDeclaringClass(), cause);
		}

		public BuilderException(String message, Method method) {
			this(message, method, null);
		}

		public BuilderException(String message, LuaMethod method, Throwable cause) {
			this(message, method.method, cause);
		}

		public BuilderException(String message, LuaMethod method) {
			this(message, method, null);
		}
	}
}
