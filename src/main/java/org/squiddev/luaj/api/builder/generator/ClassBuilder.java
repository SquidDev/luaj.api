package org.squiddev.luaj.api.builder.generator;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.objectweb.asm.*;
import org.squiddev.luaj.api.LuaObject;
import org.squiddev.luaj.api.builder.BuilderException;
import org.squiddev.luaj.api.builder.BuilderSettings;
import org.squiddev.luaj.api.builder.IInjector;
import org.squiddev.luaj.api.builder.tree.LuaArgument;
import org.squiddev.luaj.api.builder.tree.LuaClass;
import org.squiddev.luaj.api.builder.tree.LuaField;
import org.squiddev.luaj.api.builder.tree.LuaMethod;
import org.squiddev.luaj.api.validation.ILuaValidator;

import java.util.Set;

import static org.objectweb.asm.Opcodes.*;
import static org.squiddev.luaj.api.builder.BuilderConstants.*;
import static org.squiddev.luaj.api.utils.AsmUtils.constantOpcode;


/**
 * Builds ASM code to call an API
 */
public class ClassBuilder {
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
	 * Settings for class generation
	 */
	public final BuilderSettings settings;

	/**
	 * Data about the class
	 */
	public final LuaClass klass;

	/**
	 * Create a new {@link ClassBuilder}
	 *
	 * @param name     The name of the generated class
	 * @param klass    The class we build a wrapper around
	 * @param settings Settings for various generation options
	 */
	public ClassBuilder(String name, Class<?> klass, BuilderSettings settings) {
		this.klass = new LuaClass(className = name.replace('.', '/'), klass, settings);
		this.settings = settings;

		originalName = Type.getInternalName(klass);
		originalWhole = Type.getDescriptor(klass);

		writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);

		write();
	}

	/**
	 * Write everything!
	 */
	protected void write() {
		// Declare class name
		writer.visit(V1_6, ACC_PUBLIC + ACC_SUPER, className, null, Type.getInternalName(settings.parentClass), null);

		// Declare METHOD_NAMES
		writer.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, METHOD_NAMES, METHOD_NAMES_SIGNATURE, null, null).visitEnd();

		// Declare NAMES
		writer.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, NAMES, NAMES_SIGNATURE, null, null).visitEnd();

		// LOADER is setup in the class loader. This allows us to load other classes
		writer.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, LOADER, CLASS_LOADER, null, null).visitEnd();

		writeInit();
		writeStaticInit();
		writeSetup();
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
		mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(settings.parentClass), "<init>", "(Ljava/lang/Object;)V", false);

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

	/**
	 * Write an override to the
	 * {@link LuaObject#setup()} method
	 */
	protected void writeSetup() {
		Set<LuaField> fields = klass.fields;
		if (fields.size() == 0) return;

		MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "setup", "()V", null, null);
		mv.visitCode();

		// Parent setup
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(settings.parentClass), "setup", "()V", false);

		for (LuaField field : fields) {
			field.setup.inject(mv, field);

			// Load instance
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, className, "instance", "Ljava/lang/Object;");
			mv.visitTypeInsn(CHECKCAST, originalName);
			mv.visitInsn(SWAP);
			mv.visitFieldInsn(PUTFIELD, originalName, field.field.getName(), Type.getDescriptor(field.field.getType()));
		}

		// And return
		mv.visitInsn(RETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();

	}

	/**
	 * Create the main {@link org.squiddev.luaj.api.LuaObjectWrapper#invoke(Varargs, int)} method
	 */
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

					IInjector<LuaMethod> type = settings.converter.getFromLua(argType);
					if (type == null) {
						throw new BuilderException("Cannot convert LuaValue to " + argType, method);
					}

					type.inject(mv, method);
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
					IInjector<LuaMethod> type = settings.converter.getToLua(returns);
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
}
