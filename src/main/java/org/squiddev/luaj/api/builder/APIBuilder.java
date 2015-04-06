package org.squiddev.luaj.api.builder;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.objectweb.asm.*;
import org.squiddev.luaj.api.LuaObject;
import org.squiddev.luaj.api.builder.tree.LuaArgument;
import org.squiddev.luaj.api.builder.tree.LuaClass;
import org.squiddev.luaj.api.builder.tree.LuaMethod;
import org.squiddev.luaj.api.conversion.Converter;
import org.squiddev.luaj.api.conversion.IInjector;
import org.squiddev.luaj.api.utils.TinyMethod;
import org.squiddev.luaj.api.validation.ILuaValidator;

import java.util.Set;

import static org.objectweb.asm.Opcodes.*;
import static org.squiddev.luaj.api.utils.AsmUtils.constantOpcode;

/**
 * Builds ASM code to call an API
 */
public class APIBuilder {
	// A CLASS_ Starts is the L...; fVrmat, a TYPE_ is not
	public static final String CLASS_VARARGS = Type.getDescriptor(Varargs.class);
	public static final String CLASS_LUAVALUE = Type.getDescriptor(LuaValue.class);
	public static final String TYPE_LUAVALUE = Type.getInternalName(LuaValue.class);
	public static final String TYPE_LUAERROR = Type.getInternalName(LuaError.class);

	public static final String CLASS_LOADER = Type.getDescriptor(APIClassLoader.class);
	public static final String TYPE_LOADER = Type.getInternalName(APIClassLoader.class);

	public static final String INVOKE_SIGNATURE = "(" + CLASS_VARARGS + "I)" + CLASS_VARARGS;

	public static final String LOADER = "LOADER";

	public static final String NAMES = "NAMES";
	public static final String NAMES_SIGNATURE = "[Ljava/lang/String;";

	public static final String METHOD_NAMES = "METHOD_NAMES";
	public static final String METHOD_NAMES_SIGNATURE = "[[Ljava/lang/String;";

	public static final TinyMethod VARARGS_NARGS = new TinyMethod(Varargs.class, "narg");
	public static final TinyMethod VARARGS_ARG = new TinyMethod(Varargs.class, "arg", int.class);
	public static final TinyMethod VARARGS_SUBARGS = new TinyMethod(Varargs.class, "subargs", int.class);
	public static final TinyMethod VARARGS_OF = new TinyMethod(LuaValue.class, "varargsOf", LuaValue[].class);
	public static final TinyMethod LIST_OF = new TinyMethod(LuaValue.class, "listOf", LuaValue[].class);

	public static final TinyMethod API_MAKE_INSTANCE = new TinyMethod(APIClassLoader.class, "makeInstance", Object.class);
	public static final TinyMethod API_GET_TABLE = new TinyMethod(LuaObject.class, "getTable");

	/**
	 * The {@link ClassWriter} for the wrapper class
	 */
	protected final ClassWriter writer;

	/**
	 * The name of the original class
	 *
	 * @see #klass
	 */
	public final String originalName;

	/**
	 * The whole name of the original name ('L' + ... + ';')
	 */
	public final String originalWhole;

	/**
	 * The name of the generated class
	 */
	public final String className;

	/**
	 * The name of the parent/super class for our generated wrapper
	 *
	 * @see APIClassLoader#parentClass
	 */
	public final Class<?> parent;

	/**
	 * Stores the conversion handler
	 *
	 * @see APIClassLoader#converter
	 */
	public final Converter converter;

	/**
	 * Data about the class
	 */
	public final LuaClass klass;

	/**
	 * Create a new {@link APIBuilder}
	 *
	 * @param name   The name of the generated class
	 * @param klass  The class we build a wrapper around
	 * @param loader The class loader to load from. This stores settings about various generation options
	 */
	public APIBuilder(String name, Class<?> klass, APIClassLoader loader) {
		this.klass = new LuaClass(klass, loader.transformer);

		originalName = Type.getInternalName(klass);
		originalWhole = Type.getDescriptor(klass);
		className = name.replace('.', '/');

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
		writer.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, METHOD_NAMES, METHOD_NAMES_SIGNATURE, null, null).visitEnd();

		// Declare NAMES
		writer.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, NAMES, NAMES_SIGNATURE, null, null).visitEnd();

		// LOADER is setup in the class loader. This allows us to load other classes
		writer.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, LOADER, CLASS_LOADER, null, null).visitEnd();

		writeInit();
		writeStaticInit();
		writeInvoke();

		writer.visitEnd();
	}

	/**
	 * Write the static constructor
	 * This constructs the array of array of names and method names
	 */
	protected void writeStaticInit() {
		MethodVisitor mv = writer.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
		mv.visitCode();

		constantOpcode(mv, klass.methods.size());
		mv.visitTypeInsn(ANEWARRAY, "[Ljava/lang/String;");

		int counter = 0;
		for (LuaMethod m : klass.methods) {
			// For key <counter>
			mv.visitInsn(DUP);
			constantOpcode(mv, counter);

			Set<String> names = m.names;

			// Create an array of length <names.length>
			constantOpcode(mv, names.size());
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

		mv.visitFieldInsn(PUTSTATIC, className, METHOD_NAMES, METHOD_NAMES_SIGNATURE);

		// Visit names
		if (klass.names.size() == 0) {
			mv.visitInsn(ACONST_NULL);
		} else {
			constantOpcode(mv, klass.names.size());
			mv.visitTypeInsn(ANEWARRAY, "java/lang/String");

			counter = 0;
			for (String name : klass.names) {
				mv.visitInsn(DUP);
				constantOpcode(mv, counter);
				mv.visitLdcInsn(name);
				mv.visitInsn(AASTORE);

				++counter;
			}
		}

		mv.visitFieldInsn(PUTSTATIC, className, NAMES, NAMES_SIGNATURE);

		// Setup the class loader
		mv.visitLdcInsn(Type.getType("L" + className + ";"));
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
		mv.visitTypeInsn(CHECKCAST, TYPE_LOADER);
		mv.visitFieldInsn(PUTSTATIC, className, LOADER, CLASS_LOADER);

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
		mv.visitFieldInsn(GETSTATIC, className, METHOD_NAMES, METHOD_NAMES_SIGNATURE);
		mv.visitFieldInsn(PUTFIELD, className, "methodNames", METHOD_NAMES_SIGNATURE);

		// Set method API names
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETSTATIC, className, NAMES, NAMES_SIGNATURE);
		mv.visitFieldInsn(PUTFIELD, className, "names", NAMES_SIGNATURE);

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

			LuaMethod.ValidationIterator iterator = method.validationIterator();
			StringBuilder builder = new StringBuilder("Expected ");
			boolean needsValidation = iterator.hasValidateNext();

			Label doException = new Label();
			Label noException = new Label();

			// If we should validate then assert how many values there are
			if (needsValidation && iterator.requiredLength() > 0) {
				mv.visitVarInsn(ALOAD, 1);
				VARARGS_NARGS.inject(mv);
				constantOpcode(mv, iterator.requiredLength());
				mv.visitJumpInsn(IF_ICMPLT, doException);
			}

			int index = 1;
			while (iterator.hasNext()) {
				LuaArgument arg = iterator.next();
				Class<?> type = arg.parameter.getType();
				ILuaValidator validator = arg.getValidator();

				// If the item is a varargs then we shouldn't give it a name. Varargs will always be the last item
				if (!type.equals(Varargs.class)) builder.append(validator.getName(type)).append(", ");

				if (validator.shouldValidate(type)) {
					mv.visitVarInsn(ALOAD, 1);
					constantOpcode(mv, index);
					VARARGS_ARG.inject(mv);
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
				String text = builder.toString();
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

			// Load the instance and validate its type
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, className, "instance", "Ljava/lang/Object;");
			mv.visitTypeInsn(CHECKCAST, originalName);

			// Load and convert the arguments
			int argCounter = 1;
			iterator.rewind();
			for (LuaArgument arg : iterator) {
				mv.visitVarInsn(ALOAD, 1);

				Class<?> argType = arg.parameter.getType();
				if (argType.equals(Varargs.class)) {
					// If we just have varargs then we should load it, if we have varargs later then use subargs
					if (argCounter > 1) {
						constantOpcode(mv, argCounter);
						VARARGS_SUBARGS.inject(mv);
					}
				} else {
					constantOpcode(mv, argCounter);
					VARARGS_ARG.inject(mv);

					IInjector type = converter.getFromLua(argType);
					if (type == null) {
						throw new BuilderException("Cannot convert LuaValue to " + argType, method);
					}

					type.inject(mv, this);
				}

				++argCounter;
			}

			// And call it
			mv.visitMethodInsn(INVOKEVIRTUAL, originalName, method.method.getName(), Type.getMethodDescriptor(method.method), false);

			Class<?> returns = method.method.getReturnType();
			if (returns.equals(Void.TYPE)) {
				// If no result, return None
				mv.visitFieldInsn(GETSTATIC, TYPE_LUAVALUE, "NONE", CLASS_LUAVALUE);
			} else if (!Varargs.class.isAssignableFrom(returns)) { // Don't need to convert if returning a LuaValue

				// If it isn't an array or if it is and the array type isn't a subclass of LuaValue
				if (!returns.isArray() || !LuaValue.class.isAssignableFrom(returns.getComponentType())) {
					// Check if we have a converter
					IInjector type = converter.getToLua(returns);
					if (type == null) {
						throw new BuilderException("Cannot convert " + returns.getName() + " to LuaValue for ", method);
					}

					type.inject(mv, this);
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

		public BuilderException(String message, LuaClass klass, Throwable cause) {
			this(klass.klass.getName() + ": " + message, cause);
		}

		public BuilderException(String message, LuaClass klass) {
			this(message, klass, null);
		}

		public BuilderException(String message, LuaMethod method, Throwable cause) {
			this(method.method.getName() + ": " + message, method.klass, cause);
		}

		public BuilderException(String message, LuaMethod method) {
			this(message, method, null);
		}

		public BuilderException(String message, LuaArgument argument, Throwable cause) {
			this("Argument " + argument.parameter.getType() + ": " + message, argument.method, cause);
		}

		public BuilderException(String message, LuaArgument argument) {
			this(message, argument, null);
		}
	}
}
